package com.tms.trainer.api;

/**
 * Body of {@code POST /api/v1/trainers} (US-003, TrainerCreateRequest). All fields are plain
 * strings so that type- and format-specific validation is reported per field in the ErrorResponse
 * details (400), never as an opaque unreadable-body error. Required-field rules live in {@code
 * TrainerCommandService}.
 *
 * <p>GAP(artifact-SVC-005-api_spec): the spec names the field {@code location}; the existing
 * contract (US-002 filters, V5/V7 schema) identifies a location by {@code locationId}, which is
 * kept.
 */
public record CreateTrainerRequest(
    String type,
    String employeeId,
    String name,
    String email,
    String locationId,
    Boolean weekendAvailable,
    String msaNdaStatus) {}
