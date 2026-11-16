package com.tms.trainer.domain;

import com.tms.trainer.application.CreateTrainerCommand;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Trainer business rules for US-003: uniqueness, time zone derivation, default Active status. */
@Component
public class TrainerDomainService {

  private final TrainerRepository repository;
  private final TimeZoneResolver timeZoneResolver;

  public TrainerDomainService(TrainerRepository repository, TimeZoneResolver timeZoneResolver) {
    this.repository = repository;
    this.timeZoneResolver = timeZoneResolver;
  }

  /**
   * Builds a new ACTIVE trainer (FR-US-003-007) after enforcing uniqueness (FR-US-003-003) and
   * deriving the time zone from the location when resolvable (FR-US-003-005). Does not persist.
   */
  public Trainer newTrainer(CreateTrainerCommand command, Instant now) {
    if (command.trainerType() == TrainerType.INTERNAL
        && repository.existsByEmployeeId(command.employeeId())) {
      throw new DuplicateTrainerException("employeeId");
    }
    if (repository.existsByEmail(command.email())) {
      throw new DuplicateTrainerException("email");
    }
    String timeZone = timeZoneResolver.resolve(command.location()).orElse(null);
    return new Trainer(
        command.trainerType(),
        command.name(),
        command.email(),
        command.employeeId(),
        command.location(),
        timeZone,
        command.weekendAvailable(),
        command.ratePerDay(),
        command.msaReference(),
        command.ndaReference(),
        command.certifications(),
        now);
  }
}
