package com.tms.trainer.api;

import com.tms.trainer.application.AllocationFlags;
import com.tms.trainer.application.TrainerDirectoryEntry;
import com.tms.trainer.domain.TrainerStatus;
import java.util.List;
import java.util.UUID;

/**
 * Trainer entry of GET /api/v1/trainers. {@code id}, {@code employeeId}, {@code name}, {@code
 * email} and {@code profileRoute} cover profile navigation (FR-US-002-005); {@code id} plus {@code
 * availabilityId} are the identifiers for allocate-trainer (FR-US-002-006); {@code allocation}
 * carries the free/tentative/confirmed flags (FR-US-002-004).
 *
 * <p>TODO(FR-US-002-005/006): the field set is provisional. {@code profileRoute} points at the
 * GetTrainerById resource path, which is not part of this story.
 */
public record TrainerSummaryResponse(
    UUID id,
    String employeeId,
    String name,
    String email,
    TrainerStatus status,
    String location,
    boolean weekendAvailable,
    List<UUID> certificationIds,
    String profileRoute,
    UUID availabilityId,
    AllocationFlags allocation) {

  static TrainerSummaryResponse from(TrainerDirectoryEntry e) {
    return new TrainerSummaryResponse(
        e.id(),
        e.employeeId(),
        e.name(),
        e.email(),
        e.status(),
        e.location(),
        e.weekendAvailable(),
        e.certificationIds(),
        TrainerController.BASE_PATH + "/" + e.id(),
        e.availabilityId(),
        e.allocation());
  }
}
