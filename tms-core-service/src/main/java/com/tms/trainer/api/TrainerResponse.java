package com.tms.trainer.api;

import com.tms.trainer.application.TrainerProfile;
import java.util.List;
import java.util.UUID;

/**
 * Trainer as returned by {@code GET /api/trainers} ({@code PagedTrainerList.items}) and {@code GET
 * /api/trainers/{id}}. Both use the same shape, so the {@code id} and basic attributes from a
 * directory row can be passed straight to allocation APIs (FR-US-002-005).
 *
 * <p>GAP(FR-US-002-005): the story does not enumerate "basic" trainer attributes; this set is the
 * provisional choice (see docs/adr/US-002-trainer-directory-gaps.md).
 *
 * <p>US-003 adds the TrainerCreateRequest attributes ({@code trainerType}, {@code location},
 * derived {@code timeZone}, {@code msaNdaStatus}, {@code msaNdaReference}); {@code POST
 * /api/v1/trainers} returns this shape. They are {@code null} for trainers created before US-003.
 */
public record TrainerResponse(
    UUID id,
    String employeeId,
    String name,
    String email,
    String status,
    String availabilityState,
    UUID locationId,
    boolean weekendAvailable,
    List<UUID> certificationIds,
    String trainerType,
    String location,
    String timeZone,
    String msaNdaStatus,
    String msaNdaReference) {

  static TrainerResponse from(TrainerProfile p) {
    return new TrainerResponse(
        p.id(),
        p.employeeId(),
        p.name(),
        p.email(),
        p.status(),
        p.availabilityState(),
        p.locationId(),
        p.weekendAvailable(),
        p.certificationIds(),
        p.trainerType(),
        p.location(),
        p.timeZone(),
        p.msaNdaStatus(),
        p.msaNdaReference());
  }
}
