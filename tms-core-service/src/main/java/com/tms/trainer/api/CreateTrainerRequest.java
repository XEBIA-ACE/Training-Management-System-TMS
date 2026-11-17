package com.tms.trainer.api;

/**
 * Body of {@code POST /api/v1/trainers} (US-003). All fields are plain strings so that type- and
 * format-specific validation is reported per field in the ErrorResponse details (400), never as an
 * opaque unreadable-body error. Required-field rules live in {@code TrainerCommandService}.
 */
public record CreateTrainerRequest(
    String type,
    String employeeId,
    String name,
    String email,
    String locationId,
    Boolean weekendAvailable) {}
