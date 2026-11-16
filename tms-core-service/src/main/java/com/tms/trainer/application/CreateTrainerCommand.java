package com.tms.trainer.application;

import com.tms.trainer.domain.TrainerType;
import java.math.BigDecimal;
import java.util.List;

/** Validated, normalized create-trainer input; has no status (FR-US-003-007). */
public record CreateTrainerCommand(
    TrainerType trainerType,
    String name,
    String email,
    String employeeId,
    String location,
    boolean weekendAvailable,
    List<String> certifications,
    BigDecimal ratePerDay,
    String msaReference,
    String ndaReference) {}
