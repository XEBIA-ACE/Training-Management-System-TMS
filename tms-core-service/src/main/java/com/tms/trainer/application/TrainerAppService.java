package com.tms.trainer.application;

import com.tms.crosscutting.audit.AuditLogClient;
import com.tms.crosscutting.audit.AuditLogEntry;
import com.tms.crosscutting.audit.AuditLogEntryType;
import com.tms.crosscutting.security.AuthContext;
import com.tms.crosscutting.security.AuthContextProvider;
import com.tms.crosscutting.web.CorrelationIdFilter;
import com.tms.trainer.domain.DuplicateTrainerException;
import com.tms.trainer.domain.MsaNdaStatus;
import com.tms.trainer.domain.Trainer;
import com.tms.trainer.domain.TrainerDomainService;
import com.tms.trainer.domain.TrainerNotFoundException;
import com.tms.trainer.domain.TrainerRepository;
import com.tms.trainer.domain.TrainerSearchCriteria;
import com.tms.trainer.domain.TrainerType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Trainer use cases. Directory (US-002): {@code GET /api/trainers} and {@code GET
 * /api/trainers/{id}}. Creation (US-003): {@code POST /api/v1/trainers}. Each enforces its RBAC
 * through {@link AuthContextProvider} before touching data, emits Micrometer metrics and writes
 * non-sensitive audit entries.
 */
@Service
public class TrainerAppService {

  public static final String SEARCH_TIMER = "tms.trainer.search";
  public static final String PROFILE_TIMER = "tms.trainer.profile";
  static final String SEARCH_ACTION = "GET /api/trainers";
  static final String PROFILE_ACTION = "GET /api/trainers/{id}";
  static final String CREATE_ACTION = "POST /api/v1/trainers";
  static final String RESOURCE_TYPE = "Trainer";

  private static final Logger log = LoggerFactory.getLogger(TrainerAppService.class);
  private static final Sort SORT = Sort.by("name").ascending().and(Sort.by("id"));

  private final TrainerRepository repository;
  private final AuthContextProvider authContextProvider;
  private final AuditLogClient auditLogClient;
  private final TrainerDirectoryProperties properties;
  private final TrainerManagementProperties managementProperties;
  private final TrainerCreateValidator createValidator;
  private final TrainerDomainService domainService;
  private final MeterRegistry meterRegistry;
  private final TransactionTemplate readOnlyTx;
  private final TransactionTemplate writeTx;
  private final Clock clock;

  public TrainerAppService(
      TrainerRepository repository,
      AuthContextProvider authContextProvider,
      AuditLogClient auditLogClient,
      TrainerDirectoryProperties properties,
      TrainerManagementProperties managementProperties,
      TrainerCreateValidator createValidator,
      TrainerDomainService domainService,
      MeterRegistry meterRegistry,
      PlatformTransactionManager transactionManager) {
    this.repository = repository;
    this.authContextProvider = authContextProvider;
    this.auditLogClient = auditLogClient;
    this.properties = properties;
    this.managementProperties = managementProperties;
    this.createValidator = createValidator;
    this.domainService = domainService;
    this.meterRegistry = meterRegistry;
    this.readOnlyTx = new TransactionTemplate(transactionManager);
    this.readOnlyTx.setReadOnly(true);
    this.writeTx = new TransactionTemplate(transactionManager);
    this.clock = Clock.systemUTC();
  }

  /** FR-US-002-001/002/003/006/007: one page of trainers matching every supplied filter. */
  public Page<TrainerProfile> search(TrainerSearchCriteria criteria, int page, int size) {
    AuthContext actor = authorizeDirectory(SEARCH_ACTION, null);
    long start = System.nanoTime();
    Page<TrainerProfile> result =
        readOnlyTx.execute(
            status ->
                repository
                    .search(criteria, PageRequest.of(page, size, SORT))
                    .map(TrainerProfile::from));
    long elapsedNanos = System.nanoTime() - start;
    searchTimer(criteria.hasFreeText()).record(elapsedNanos, TimeUnit.NANOSECONDS);

    // Never log or audit the raw free-text term; only whether one was supplied.
    log.info(
        "Trainer search completed: filters={} freeTextPresent={} page={} size={} results={}"
            + " totalElements={} durationMs={}",
        criteria.usedFilterFields(),
        criteria.hasFreeText(),
        page,
        size,
        result.getNumberOfElements(),
        result.getTotalElements(),
        elapsedNanos / 1_000_000);

    if (properties.audit().searchEnabled()) {
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("filters", criteria.usedFilterFields());
      details.put("freeTextPresent", criteria.hasFreeText());
      details.put("page", page);
      details.put("size", size);
      details.put("resultCount", result.getNumberOfElements());
      auditLogClient.appendAuditEntry(
          entry(AuditLogEntryType.SEARCH, actor.actorId(), SEARCH_ACTION, null, details));
    }
    return result;
  }

  /** FR-US-002-004/007: the trainer profile, only after the caller passed the directory RBAC. */
  public TrainerProfile get(UUID id) {
    authorizeDirectory(PROFILE_ACTION, id);
    Timer.Sample sample = Timer.start(meterRegistry);
    TrainerProfile profile =
        readOnlyTx.execute(
            status -> repository.findById(id).map(TrainerProfile::from).orElse(null));
    sample.stop(profileTimer(profile != null ? "found" : "not_found"));
    if (profile == null) {
      throw new TrainerNotFoundException(id);
    }
    return profile;
  }

  /**
   * US-003: creates an ACTIVE trainer (FR-US-003-005) with its derived timeZone (FR-US-003-004).
   * Order: TRAINER.MANAGE check (FR-US-003-007), TrainerCreateRequest validation
   * (FR-US-003-001/002), uniqueness across all trainers (FR-US-003-003), then the trainer and its
   * CREATE audit entry (FR-US-003-006) are written in one transaction, so a failure at any step
   * persists neither.
   */
  public TrainerProfile create(TrainerCreateCommand command) {
    AuthContext actor =
        authorize(
            CREATE_ACTION,
            null,
            managementProperties.roles(),
            managementProperties.audit().unauthorizedAccessEnabled(),
            "MISSING_TRAINER_MANAGE_PERMISSION",
            "Trainer management access denied");
    List<TrainerValidationException.FieldViolation> violations = createValidator.validate(command);
    if (!violations.isEmpty()) {
      meterRegistry.counter("tms.trainer.create.rejected", "reason", "validation").increment();
      throw new TrainerValidationException(violations);
    }
    TrainerType type = TrainerType.valueOf(command.trainerType());
    String employeeId = trimToNull(command.employeeId());
    String email = command.email().trim();
    String location = command.location().trim();

    TrainerProfile created;
    try {
      created =
          writeTx.execute(
              status -> {
                domainService.ensureUnique(type, employeeId, email);
                Trainer trainer =
                    Trainer.create(
                        UUID.randomUUID(),
                        type,
                        employeeId,
                        command.name().trim(),
                        email,
                        location,
                        domainService.deriveTimeZone(location).orElse(null),
                        command.msaNdaStatus() != null
                            ? MsaNdaStatus.valueOf(command.msaNdaStatus())
                            : null,
                        trimToNull(command.msaNdaReference()),
                        Boolean.TRUE.equals(command.weekendAvailable()),
                        command.certificationIds() != null ? command.certificationIds() : List.of(),
                        clock.instant());
                repository.saveAndFlush(trainer);
                auditLogClient.appendInCurrentTransaction(
                    entry(
                        AuditLogEntryType.CREATE,
                        actor.actorId(),
                        CREATE_ACTION,
                        trainer.getId(),
                        createdDetails(trainer)));
                return TrainerProfile.from(trainer);
              });
    } catch (DataIntegrityViolationException ex) {
      throw domainService.duplicateFrom(ex).orElseThrow(() -> ex);
    } catch (RuntimeException ex) {
      if (ex instanceof DuplicateTrainerException duplicate) {
        meterRegistry
            .counter("tms.trainer.create.rejected", "reason", duplicate.conflict().errorCode())
            .increment();
      }
      throw ex;
    }
    meterRegistry.counter("tms.trainer.created", "trainerType", type.name()).increment();
    log.info(
        "Trainer created: trainerId={} trainerType={} timeZoneDerived={} actor={}",
        created.id(),
        type,
        created.timeZone() != null,
        actor.actorId());
    return created;
  }

  /**
   * FR-US-003-006: the non-sensitive fields of the created trainer. Document references/contents
   * and commercial data are never included.
   */
  private static Map<String, Object> createdDetails(Trainer trainer) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("module", "trainer");
    details.put("recordId", trainer.getId().toString());
    details.put("trainerType", trainer.getTrainerType().name());
    if (trainer.getEmployeeId() != null) {
      details.put("employeeId", trainer.getEmployeeId());
    }
    details.put("name", trainer.getName());
    details.put("email", trainer.getEmail());
    details.put("location", trainer.getLocation());
    if (trainer.getTimeZone() != null) {
      details.put("timeZone", trainer.getTimeZone());
    }
    details.put("status", trainer.getStatus());
    return details;
  }

  private static String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /**
   * Denies callers lacking a trainer directory role with {@link AccessDeniedException} (HTTP 403)
   * before any trainer data is read, so neither data nor trainer existence leaks.
   */
  private AuthContext authorizeDirectory(String action, UUID trainerId) {
    return authorize(
        action,
        trainerId,
        properties.roles(),
        properties.audit().unauthorizedAccessEnabled(),
        "MISSING_TRAINER_DIRECTORY_ROLE",
        "Trainer directory access denied");
  }

  /**
   * Returns the caller when authenticated with one of {@code roles}; otherwise counts, logs and
   * (when enabled) audits an UnauthorizedAccessEntry, then throws {@link AccessDeniedException}.
   */
  private AuthContext authorize(
      String action,
      UUID trainerId,
      List<String> roles,
      boolean auditDenied,
      String reason,
      String denialMessage) {
    AuthContext actor = authContextProvider.current();
    if (actor.authenticated() && actor.hasAnyRole(roles)) {
      return actor;
    }
    meterRegistry.counter("tms.trainer.access.denied", "action", action).increment();
    log.warn("{}: actor={} action={}", denialMessage, actor.actorId(), action);
    if (auditDenied) {
      auditLogClient.appendAuditEntry(
          entry(
              AuditLogEntryType.UNAUTHORIZED_ACCESS,
              actor.actorId(),
              action,
              trainerId,
              Map.of("reason", reason)));
    }
    throw new AccessDeniedException(denialMessage);
  }

  private AuditLogEntry entry(
      AuditLogEntryType type,
      String actor,
      String action,
      UUID resourceId,
      Map<String, Object> details) {
    return new AuditLogEntry(
        type,
        actor,
        action,
        RESOURCE_TYPE,
        resourceId != null ? resourceId.toString() : null,
        MDC.get(CorrelationIdFilter.MDC_KEY),
        details,
        clock.instant());
  }

  private Timer searchTimer(boolean freeText) {
    return Timer.builder(SEARCH_TIMER)
        .description("TrainerAppService search latency for GET /api/trainers")
        .tag("freeText", Boolean.toString(freeText))
        .publishPercentileHistogram()
        .register(meterRegistry);
  }

  private Timer profileTimer(String outcome) {
    return Timer.builder(PROFILE_TIMER)
        .description("TrainerAppService profile lookup latency for GET /api/trainers/{id}")
        .tag("outcome", outcome)
        .publishPercentileHistogram()
        .register(meterRegistry);
  }
}
