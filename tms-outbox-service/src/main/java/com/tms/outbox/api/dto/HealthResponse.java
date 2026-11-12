package com.tms.outbox.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * API model {@code HealthResponse} for {@code /health}, {@code /health/ready}, {@code
 * /health/live}.
 */
public record HealthResponse(String status, Instant timestamp, Components components) {

  public static final String UP = "UP";
  public static final String DOWN = "DOWN";

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Components(
      ComponentStatus database, ComponentStatus processor, ComponentStatus messageBroker) {}

  public record ComponentStatus(String status) {}
}
