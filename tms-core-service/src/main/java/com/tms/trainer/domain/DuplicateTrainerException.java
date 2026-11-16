package com.tms.trainer.domain;

/** Employee ID or Email already used by an active or inactive trainer (FR-US-003-003). */
public class DuplicateTrainerException extends RuntimeException {

  private final String field;

  public DuplicateTrainerException(String field) {
    super("A trainer with this " + field + " already exists.");
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
