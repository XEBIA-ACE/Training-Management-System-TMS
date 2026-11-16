package com.tms.trainer.domain;

import java.time.ZoneId;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Trainer creation rules: uniqueness (FR-US-003-003) and time zone derivation (FR-US-003-004). */
@Service
public class TrainerDomainService {

  static final String UQ_EMAIL = "uq_trainer_email_lower";
  static final String UQ_INTERNAL_EMPLOYEE_ID = "uq_trainer_internal_employee_id";

  private final TrainerRepository repository;
  private final LocationTimeZoneRules timeZoneRules;

  public TrainerDomainService(TrainerRepository repository, LocationTimeZoneRules timeZoneRules) {
    this.repository = repository;
    this.timeZoneRules = timeZoneRules;
  }

  /**
   * FR-US-003-003: employeeId must be unique for INTERNAL trainers and email for every trainer,
   * checked against all trainers regardless of status (inactive included). employeeId is checked
   * first, so a request duplicating both reports {@code TRAINER_DUPLICATE_EMPLOYEE_ID}.
   */
  public void ensureUnique(TrainerType type, String employeeId, String email) {
    if (type == TrainerType.INTERNAL && repository.existsByEmployeeId(employeeId)) {
      throw new DuplicateTrainerException(DuplicateTrainerException.Conflict.EMPLOYEE_ID);
    }
    if (repository.existsByEmailIgnoreCase(email)) {
      throw new DuplicateTrainerException(DuplicateTrainerException.Conflict.EMAIL);
    }
  }

  /** FR-US-003-004: the IANA time zone id for {@code location}, when a rule maps it. */
  public Optional<String> deriveTimeZone(String location) {
    return timeZoneRules.resolve(location).map(ZoneId::getId);
  }

  /**
   * Maps a unique-index violation raised by a concurrent create (race after {@link #ensureUnique})
   * onto the same 409 outcome.
   */
  public Optional<DuplicateTrainerException> duplicateFrom(DataIntegrityViolationException ex) {
    String message = String.valueOf(ex.getMostSpecificCause().getMessage());
    if (message.contains(UQ_INTERNAL_EMPLOYEE_ID)) {
      return Optional.of(
          new DuplicateTrainerException(DuplicateTrainerException.Conflict.EMPLOYEE_ID));
    }
    if (message.contains(UQ_EMAIL)) {
      return Optional.of(new DuplicateTrainerException(DuplicateTrainerException.Conflict.EMAIL));
    }
    return Optional.empty();
  }
}
