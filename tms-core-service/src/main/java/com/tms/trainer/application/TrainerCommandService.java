package com.tms.trainer.application;

import com.tms.crosscutting.audit.AuditLogClient;
import com.tms.crosscutting.audit.AuditLogEntry;
import com.tms.crosscutting.audit.AuditLogEntryType;
import com.tms.crosscutting.security.AuthContext;
import com.tms.crosscutting.security.AuthContextProvider;
import com.tms.crosscutting.web.CorrelationIdFilter;
import com.tms.crosscutting.web.ValidationErrorItem;
import com.tms.trainer.domain.Location;
import com.tms.trainer.domain.LocationRepository;
import com.tms.trainer.domain.MsaNdaStatus;
import com.tms.trainer.domain.Trainer;
import com.tms.trainer.domain.TrainerConflictException;
import com.tms.trainer.domain.TrainerNotFoundException;
import com.tms.trainer.domain.TrainerRepository;
import com.tms.trainer.domain.TrainerType;
import com.tms.trainer.domain.TrainerValidationException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Trainer write use cases (US-003): {@code POST /api/v1/trainers} and {@code PUT
 * /api/v1/trainers/{trainerId}}, plus the trainer audit trail read ({@code GET
 * /api/v1/trainers/{trainerId}/audit-logs}).
 *
 * <p>Only non-document profile fields are ever written to audit entries (FR-US-003-006); trainer
 * documents are never touched by this service.
 *
 * <p>FR-US-003-001/002: {@code type}, {@code name}, {@code email} and {@code locationId} are always
 * required, {@code employeeId} additionally for INTERNAL; {@code msaNdaStatus} is optional and must
 * be a {@link MsaNdaStatus} value. GAP(artifact-SVC-005-api_spec): the exact format rules are not
 * in the repository; email must look like an address, max lengths follow the {@code trainer} table
 * columns and {@code locationId} must be a UUID.
 */
@Service
public class TrainerCommandService {

  static final String CREATE_ACTION = "POST /api/v1/trainers";
  static final String UPDATE_ACTION = "PUT /api/v1/trainers/{trainerId}";
  static final String RESOURCE_TYPE = "Trainer";
  static final String AUDIT_MODULE = "trainer";
  static final String AUDIT_ACTION = "GET /api/v1/trainers/{trainerId}/audit-logs";

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final TrainerRepository repository;
  private final LocationRepository locations;
  private final AuthContextProvider authContextProvider;
  private final AuditLogClient auditLogClient;
  private final TrainerDirectoryProperties properties;
  private final TransactionTemplate tx;
  private final TransactionTemplate readOnlyTx;
  private final Clock clock = Clock.systemUTC();

  public TrainerCommandService(
      TrainerRepository repository,
      LocationRepository locations,
      AuthContextProvider authContextProvider,
      AuditLogClient auditLogClient,
      TrainerDirectoryProperties properties,
      PlatformTransactionManager transactionManager) {
    this.repository = repository;
    this.locations = locations;
    this.authContextProvider = authContextProvider;
    this.auditLogClient = auditLogClient;
    this.properties = properties;
    this.tx = new TransactionTemplate(transactionManager);
    this.readOnlyTx = new TransactionTemplate(transactionManager);
    this.readOnlyTx.setReadOnly(true);
  }

  /** Raw create input; validated here so every failure can be audited (FR-US-003-007). */
  public record CreateCommand(
      String type,
      String employeeId,
      String name,
      String email,
      String locationId,
      Boolean weekendAvailable,
      String msaNdaStatus) {}

  /** Raw update input; absent fields are left unchanged. */
  public record UpdateCommand(String name, String locationId) {}

  /** FR-US-003-001..007. */
  public TrainerProfile create(CreateCommand cmd) {
    AuthContext actor = authorize(CREATE_ACTION, null);

    List<ValidationErrorItem> errors = new ArrayList<>();
    TrainerType type = validateCreate(cmd, errors);
    UUID locationId = parseLocation(cmd.locationId(), errors);
    MsaNdaStatus msaNdaStatus = parseMsaNdaStatus(cmd.msaNdaStatus(), errors);
    if (!errors.isEmpty()) {
      auditRejectedCreate(actor, cmd, errors, null);
      throw new TrainerValidationException(errors);
    }

    String employeeId = trimToNull(cmd.employeeId());
    String email = cmd.email().trim();
    // FR-US-003-003: employeeId (INTERNAL, any status) then email (all types).
    Optional<Trainer> byEmployee =
        type == TrainerType.INTERNAL
            ? readOnlyTx.execute(s -> repository.findFirstByEmployeeId(employeeId))
            : Optional.empty();
    Optional<Trainer> byEmail =
        readOnlyTx.execute(s -> repository.findFirstByEmailIgnoreCase(email));
    if (byEmployee.isPresent()) {
      errors.add(new ValidationErrorItem("employeeId", "employeeId is already in use."));
    }
    if (byEmail.isPresent()) {
      errors.add(new ValidationErrorItem("email", "email is already in use."));
    }
    if (!errors.isEmpty()) {
      // Attach the rejection to the conflicting trainer's audit trail so it is retrievable there.
      UUID conflicting = byEmployee.or(() -> byEmail).map(Trainer::getId).orElse(null);
      auditRejectedCreate(actor, cmd, errors, conflicting);
      // FR-US-003-003: 409. GAP: when both collide, employeeId (checked first) names the code.
      throw new TrainerConflictException(
          byEmployee.isPresent()
              ? TrainerConflictException.DUPLICATE_EMPLOYEE_ID
              : TrainerConflictException.DUPLICATE_EMAIL,
          errors);
    }

    String timezone = timezoneOf(locationId);
    Trainer saved =
        tx.execute(
            s ->
                repository.save(
                    Trainer.create(
                        type,
                        employeeId,
                        cmd.name().trim(),
                        email,
                        locationId,
                        timezone,
                        Boolean.TRUE.equals(cmd.weekendAvailable()),
                        msaNdaStatus,
                        clock.instant())));

    Map<String, Object> details = profileFields(saved);
    details.put("outcome", "SUCCESS");
    auditLogClient.appendAuditEntry(
        entry(AuditLogEntryType.CREATE, actor.actorId(), CREATE_ACTION, saved.getId(), details));
    return TrainerProfile.from(saved);
  }

  /** FR-US-003-004: PUT recalculates the timezone when the location changes. */
  public TrainerProfile update(UUID id, UpdateCommand cmd) {
    AuthContext actor = authorize(UPDATE_ACTION, id);
    List<ValidationErrorItem> errors = new ArrayList<>();
    UUID locationId = parseLocation(cmd.locationId(), errors);
    if (cmd.name() != null && (cmd.name().isBlank() || cmd.name().length() > 200)) {
      errors.add(new ValidationErrorItem("name", "name must be 1-200 characters."));
    }
    if (!errors.isEmpty()) {
      throw new TrainerValidationException(errors);
    }
    String timezone = locationId != null ? timezoneOf(locationId) : null;
    // The profile is built inside the transaction: certificationIds is lazily loaded.
    Map<String, Object> details = new LinkedHashMap<>();
    TrainerProfile updated =
        tx.execute(
            s -> {
              Trainer t =
                  repository.findById(id).orElseThrow(() -> new TrainerNotFoundException(id));
              t.update(
                  cmd.name() != null ? cmd.name().trim() : null,
                  locationId,
                  timezone,
                  clock.instant());
              Trainer saved = repository.save(t);
              details.putAll(profileFields(saved));
              return TrainerProfile.from(saved);
            });
    details.put("outcome", "SUCCESS");
    auditLogClient.appendAuditEntry(
        entry(AuditLogEntryType.UPDATE, actor.actorId(), UPDATE_ACTION, id, details));
    return updated;
  }

  /** FR-US-003-006/007: the immutable audit trail of one trainer, oldest first. */
  public List<AuditLogEntry> auditLogs(UUID id) {
    AuthContext actor = authContextProvider.current();
    if (!actor.authenticated()
        || !(actor.hasAnyRole(properties.roles()) || actor.hasAnyRole(properties.createRoles()))) {
      deny(actor, AUDIT_ACTION, id);
    }
    if (!Boolean.TRUE.equals(readOnlyTx.execute(s -> repository.existsById(id)))) {
      throw new TrainerNotFoundException(id);
    }
    return auditLogClient.findByResource(RESOURCE_TYPE, id.toString());
  }

  // --- validation -------------------------------------------------------------------------------

  private TrainerType validateCreate(CreateCommand cmd, List<ValidationErrorItem> errors) {
    TrainerType type = null;
    if (isBlank(cmd.type())) {
      errors.add(new ValidationErrorItem("type", "type is required (INTERNAL or EXTERNAL)."));
    } else {
      try {
        type = TrainerType.valueOf(cmd.type().trim());
      } catch (IllegalArgumentException ex) {
        errors.add(new ValidationErrorItem("type", "type must be INTERNAL or EXTERNAL."));
      }
    }
    if (isBlank(cmd.name())) {
      errors.add(new ValidationErrorItem("name", "name is required."));
    } else if (cmd.name().length() > 200) {
      errors.add(new ValidationErrorItem("name", "name must be at most 200 characters."));
    }
    if (isBlank(cmd.email())) {
      errors.add(new ValidationErrorItem("email", "email is required."));
    } else if (cmd.email().trim().length() > 254 || !EMAIL.matcher(cmd.email().trim()).matches()) {
      errors.add(new ValidationErrorItem("email", "email must be a valid email address."));
    }
    if (type == TrainerType.INTERNAL && isBlank(cmd.employeeId())) {
      errors.add(new ValidationErrorItem("employeeId", "employeeId is required for INTERNAL."));
    }
    if (!isBlank(cmd.employeeId()) && cmd.employeeId().trim().length() > 60) {
      errors.add(
          new ValidationErrorItem("employeeId", "employeeId must be at most 60 characters."));
    }
    if (isBlank(cmd.locationId())) {
      errors.add(new ValidationErrorItem("locationId", "locationId is required."));
    }
    return type;
  }

  private static UUID parseLocation(String raw, List<ValidationErrorItem> errors) {
    if (isBlank(raw)) {
      return null;
    }
    try {
      return UUID.fromString(raw.trim());
    } catch (IllegalArgumentException ex) {
      errors.add(new ValidationErrorItem("locationId", "locationId must be a valid UUID."));
      return null;
    }
  }

  /**
   * FR-US-003-002: optional; when supplied it must be a {@link MsaNdaStatus} value.
   *
   * <p>GAP(FR-US-003-002): the story only names msaNdaStatus for EXTERNAL trainers and does not say
   * whether INTERNAL trainers may carry it; the enum constraint is applied for both types.
   */
  private static MsaNdaStatus parseMsaNdaStatus(String raw, List<ValidationErrorItem> errors) {
    if (raw == null) {
      return null;
    }
    try {
      return MsaNdaStatus.valueOf(raw.trim());
    } catch (IllegalArgumentException ex) {
      errors.add(
          new ValidationErrorItem(
              "msaNdaStatus",
              "msaNdaStatus must be one of " + Arrays.toString(MsaNdaStatus.values()) + "."));
      return null;
    }
  }

  /** GAP(FR-US-003-004): unknown location / no timezone yields null; the story defines nothing. */
  private String timezoneOf(UUID locationId) {
    if (locationId == null) {
      return null;
    }
    return readOnlyTx.execute(
        s -> locations.findById(locationId).map(Location::getTimezone).orElse(null));
  }

  // --- auth / audit -----------------------------------------------------------------------------

  private AuthContext authorize(String action, UUID trainerId) {
    AuthContext actor = authContextProvider.current();
    if (actor.authenticated() && actor.hasAnyRole(properties.createRoles())) {
      return actor;
    }
    deny(actor, action, trainerId);
    return actor;
  }

  private void deny(AuthContext actor, String action, UUID trainerId) {
    if (properties.audit().unauthorizedAccessEnabled()) {
      auditLogClient.appendAuditEntry(
          entry(
              AuditLogEntryType.UNAUTHORIZED_ACCESS,
              actor.actorId(),
              action,
              trainerId,
              Map.of("module", AUDIT_MODULE, "reason", "MISSING_TRAINER_ROLE")));
    }
    throw new AccessDeniedException("Trainer access denied");
  }

  /**
   * Records a blocked create (400/409) in the global audit model. Only field names, type and the
   * (non-document) rejection reasons are stored, never the submitted values.
   */
  private void auditRejectedCreate(
      AuthContext actor, CreateCommand cmd, List<ValidationErrorItem> errors, UUID resourceId) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("module", AUDIT_MODULE);
    details.put("outcome", "REJECTED");
    if (cmd.type() != null && cmd.type().length() <= 20) {
      details.put("type", cmd.type());
    }
    details.put("fields", errors.stream().map(ValidationErrorItem::field).distinct().toList());
    details.put("errors", errors.stream().map(e -> e.field() + ": " + e.message()).toList());
    auditLogClient.appendAuditEntry(
        entry(AuditLogEntryType.CREATE, actor.actorId(), CREATE_ACTION, resourceId, details));
  }

  /**
   * FR-US-003-006: non-sensitive profile fields of an audit entry ({@code module=trainer}).
   * Document contents are never part of the Trainer aggregate and therefore never audited.
   */
  private static Map<String, Object> profileFields(Trainer t) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("module", AUDIT_MODULE);
    m.put("trainerId", t.getId().toString());
    m.put("type", t.getType());
    m.put("employeeId", t.getEmployeeId());
    m.put("name", t.getName());
    m.put("email", t.getEmail());
    m.put("status", t.getStatus());
    m.put("locationId", t.getLocationId() != null ? t.getLocationId().toString() : null);
    m.put("timezone", t.getTimezone());
    m.put("weekendAvailable", t.isWeekendAvailable());
    m.put("msaNdaStatus", t.getMsaNdaStatus());
    m.values().removeIf(java.util.Objects::isNull);
    return m;
  }

  private AuditLogEntry entry(
      AuditLogEntryType type, String actor, String action, UUID resourceId, Map<String, Object> d) {
    return new AuditLogEntry(
        type,
        actor,
        action,
        RESOURCE_TYPE,
        resourceId != null ? resourceId.toString() : null,
        MDC.get(CorrelationIdFilter.MDC_KEY),
        d,
        clock.instant());
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String trimToNull(String s) {
    return isBlank(s) ? null : s.trim();
  }
}
