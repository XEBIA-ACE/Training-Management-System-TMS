package com.tms.trainer.application;

import com.tms.trainer.domain.Trainer;
import com.tms.trainer.domain.TrainerStatus;
import java.util.List;
import java.util.UUID;

/**
 * One trainer in the directory: identity fields (FR-US-002-005), allocation flags
 * (FR-US-002-004) and the {@code availabilityId} needed for allocate-trainer (FR-US-002-006).
 *
 * <p>TODO(FR-US-002-005/006): field set is provisional; the story does not enumerate it.
 */
public record TrainerDirectoryEntry(
    UUID id,
    String employeeId,
    String name,
    String email,
    TrainerStatus status,
    String location,
    boolean weekendAvailable,
    List<UUID> certificationIds,
    UUID availabilityId,
    AllocationFlags allocation) {

  static TrainerDirectoryEntry of(Trainer t, AllocationFlags allocation) {
    return new TrainerDirectoryEntry(
        t.getId(),
        t.getEmployeeId(),
        t.getName(),
        t.getEmail(),
        t.getStatus(),
        t.getLocation(),
        t.isWeekendAvailable(),
        t.getCertificationIds().stream().sorted().toList(),
        t.getAvailabilityId(),
        allocation);
  }
}
