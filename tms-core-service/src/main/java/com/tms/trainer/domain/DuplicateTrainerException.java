package com.tms.trainer.domain;

/** A trainer with the same employeeId or email already exists (FR-US-003-003, HTTP 409). */
public class DuplicateTrainerException extends RuntimeException {

  /** Which uniqueness rule was violated, with the API error code and offending field. */
  public enum Conflict {
    EMPLOYEE_ID("employeeId", "TRAINER_DUPLICATE_EMPLOYEE_ID"),
    EMAIL("email", "TRAINER_DUPLICATE_EMAIL");

    private final String field;
    private final String errorCode;

    Conflict(String field, String errorCode) {
      this.field = field;
      this.errorCode = errorCode;
    }

    public String field() {
      return field;
    }

    public String errorCode() {
      return errorCode;
    }
  }

  private final Conflict conflict;

  public DuplicateTrainerException(Conflict conflict) {
    super("A trainer with this " + conflict.field() + " already exists.");
    this.conflict = conflict;
  }

  public Conflict conflict() {
    return conflict;
  }
}
