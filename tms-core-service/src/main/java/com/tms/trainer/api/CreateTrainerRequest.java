package com.tms.trainer.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * POST /api/v1/trainers payload. Required fields depend on {@code trainerType}, so they are
 * checked by {@link CreateTrainerValidator} rather than Bean Validation. Any client-sent {@code
 * status} is ignored (FR-US-003-007).
 */
public record CreateTrainerRequest(
    String trainerType,
    String name,
    String email,
    String employeeId,
    String location,
    Boolean weekendAvailable,
    List<String> certifications,
    BigDecimal ratePerDay,
    String msaReference,
    String ndaReference) {}
