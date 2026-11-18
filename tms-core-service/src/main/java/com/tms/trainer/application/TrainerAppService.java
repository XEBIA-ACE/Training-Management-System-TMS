package com.tms.trainer.application;

import com.tms.crosscutting.audit.AuditLogClient;
import com.tms.crosscutting.audit.AuditLogEntry;
import com.tms.crosscutting.audit.AuditLogEntryType;
import com.tms.crosscutting.security.AuthContext;
import com.tms.crosscutting.security.AuthContextProvider;
import com.tms.crosscutting.web.CorrelationIdFilter;
import com.tms.trainer.domain.Trainer;
import com.tms.trainer.domain.TrainerInactiveException;
import com.tms.trainer.domain.TrainerNotFoundException;
import com.tms.trainer.domain.TrainerRepository;
import com.tms.trainer.domain.TrainerSearchCriteria;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Trainer directory use cases (US-002): {@code GET /api/trainers} and {@code GET
 * /api/trainers/{id}}. Both enforce the trainer directory RBAC through {@link AuthContextProvider}
 * before touching data, emit Micrometer timers and write non-sensitive audit entries.
 */
@Service
public class TrainerAppService {

  public static final String SEARCH_TIMER = "tms.trainer.search";
  public static final String PROFILE_TIMER = "tms.trainer.profile";
  static final String SEARCH_ACTION = "GET /api/trainers";
  static final String PROFILE_ACTION = "GET /api/trainers/{id}";
  static final String RESOURCE_TYPE = "Trainer";

  private static final Logger log = LoggerFactory.getLogger(TrainerAppService.class);
  private static final Sort SORT = Sort.by("name").ascending().and(Sort.by("id"));

  private final TrainerRepository repository;
  private final AuthContextProvider authContextProvider;
  private final AuditLogClient auditLogClient;
  private final TrainerDirectoryProperties properties;
  private final MeterRegistry meterRegistry;
  private final TransactionTemplate readOnlyTx;
  private final Clock clock;

  public TrainerAppService(
      TrainerRepository repository,
      AuthContextProvider authContextProvider,
      AuditLogClient auditLogClient,
      TrainerDirectoryProperties properties,
      MeterRegistry meterRegistry,
      PlatformTransactionManager transactionManager) {
    this.repository = repository;
    this.authContextProvider = authContextProvider;
    this.auditLogClient = auditLogClient;
    this.properties = properties;
    this.meterRegistry = meterRegistry;
    this.readOnlyTx = new TransactionTemplate(transactionManager);
    this.readOnlyTx.setReadOnly(true);
    this.clock = Clock.systemUTC();
  }

  /** FR-US-002-001/002/003/006/007: one page of trainers matching every supplied filter. */
  public Page<TrainerProfile> search(TrainerSearchCriteria criteria, int page, int size) {
    AuthContext actor = authorize(SEARCH_ACTION, null);
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
    authorize(PROFILE_ACTION, id);
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
   * FR-US-004-AC3-001/002: guard for the allocation module before it creates an Allocation. Throws
   * {@link TrainerInactiveException} for an INACTIVE trainer (no RBAC: an internal module call).
   * GAP: this repository has no allocation endpoint yet, so nothing calls this method (see
   * docs/adr/US-004-trainer-status-gaps.md).
   */
  public void requireAllocatable(UUID id) {
    Trainer trainer =
        readOnlyTx.execute(s -> repository.findById(id).orElse(null));
    if (trainer == null) {
      throw new TrainerNotFoundException(id);
    }
    if (trainer.isInactive()) {
      throw new TrainerInactiveException(id);
    }
  }

  /**
   * Denies callers lacking a trainer directory role with {@link AccessDeniedException} (HTTP 403)
   * before any trainer data is read, so neither data nor trainer existence leaks.
   */
  private AuthContext authorize(String action, UUID trainerId) {
    AuthContext actor = authContextProvider.current();
    if (actor.authenticated() && actor.hasAnyRole(properties.roles())) {
      return actor;
    }
    meterRegistry.counter("tms.trainer.access.denied", "action", action).increment();
    log.warn("Trainer directory access denied: actor={} action={}", actor.actorId(), action);
    if (properties.audit().unauthorizedAccessEnabled()) {
      auditLogClient.appendAuditEntry(
          entry(
              AuditLogEntryType.UNAUTHORIZED_ACCESS,
              actor.actorId(),
              action,
              trainerId,
              Map.of("reason", "MISSING_TRAINER_DIRECTORY_ROLE")));
    }
    throw new AccessDeniedException("Trainer directory access denied");
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
