package com.tms.trainer.domain;

import com.tms.crosscutting.web.ValidationErrorItem;
import java.util.List;

/**
 * FR-US-003-003: create rejected with HTTP 409 because {@code employeeId} or {@code email} is
 * already used by an existing trainer (any status). {@link #code()} is {@value
 * #DUPLICATE_EMPLOYEE_ID} or {@value #DUPLICATE_EMAIL}.
 */
public class TrainerConflictException extends RuntimeException {

  public static final String DUPLICATE_EMPLOYEE_ID = "DUPLICATE_EMPLOYEE_ID";
  public static final String DUPLICATE_EMAIL = "DUPLICATE_EMAIL";

  private final String code;
  private final transient List<ValidationErrorItem> details;

  public TrainerConflictException(String code, List<ValidationErrorItem> details) {
    super("A trainer with the same " + details.get(0).field() + " already exists.");
    this.code = code;
    this.details = List.copyOf(details);
  }

  public String code() {
    return code;
  }

  public List<ValidationErrorItem> details() {
    return details;
  }
}
