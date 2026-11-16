package com.tms.trainer.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tms.trainer.domain.Trainer;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrainerResponse(
    UUID id,
    String trainerType,
    String status,
    String name,
    String email,
    String employeeId,
    String location,
    String timeZone,
    Instant createdAt) {

  static TrainerResponse from(Trainer t) {
    return new TrainerResponse(
        t.getId(),
        t.getTrainerType().name(),
        t.getStatus().name(),
        t.getName(),
        t.getEmail(),
        t.getEmployeeId(),
        t.getLocation(),
        t.getTimeZone(),
        t.getCreatedAt());
  }
}
