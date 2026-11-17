package com.tms.trainer.api;

/**
 * Body of {@code PUT /api/v1/trainers/{trainerId}} (US-003-ac-4).
 *
 * <p>GAP(FR-US-003-004): the story only defines the locationId behavior; {@code name} is the one
 * other editable field provisionally accepted. Absent fields are unchanged.
 */
public record UpdateTrainerRequest(String name, String locationId) {}
