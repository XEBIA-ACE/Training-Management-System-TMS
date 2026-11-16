package com.tms.trainer.application;

import java.util.List;

/** TrainerCreateRequest failed validation (FR-US-003-001/002, HTTP 400). */
public class TrainerValidationException extends RuntimeException {

  /** One invalid or missing field. */
  public record FieldViolation(String field, String message) {}

  private final List<FieldViolation> violations;

  public TrainerValidationException(List<FieldViolation> violations) {
    super("Trainer request validation failed.");
    this.violations = List.copyOf(violations);
  }

  public List<FieldViolation> violations() {
    return violations;
  }
}
