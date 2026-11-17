package com.tms.trainer.application;

import com.tms.trainer.domain.Trainer;
import java.util.List;
import java.util.UUID;

/**
 * Read model of a trainer returned by both the directory and the profile query. It carries the
 * trainer id and the attributes allocation workflows need (FR-US-002-005), so callers do not need
 * another lookup to resolve the trainer's identity.
 */
public record TrainerProfile(
    UUID id,
    String employeeId,
    String name,
    String email,
    String status,
    String availabilityState,
    UUID locationId,
    boolean weekendAvailable,
    List<UUID> certificationIds,
    String type,
    String timezone,
    String msaNdaStatus) {

  static TrainerProfile from(Trainer trainer) {
    return new TrainerProfile(
        trainer.getId(),
        trainer.getEmployeeId(),
        trainer.getName(),
        trainer.getEmail(),
        trainer.getStatus(),
        trainer.getAvailabilityState(),
        trainer.getLocationId(),
        trainer.isWeekendAvailable(),
        trainer.getCertificationIds().stream().sorted().toList(),
        trainer.getType(),
        trainer.getTimezone(),
        trainer.getMsaNdaStatus());
  }
}
