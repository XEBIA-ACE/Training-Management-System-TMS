package com.tms.trainer.domain;

import com.tms.crosscutting.web.ValidationErrorItem;
import java.util.List;

/** Create/update request rejected with HTTP 400 and per-field {@link ValidationErrorItem}s. */
public class TrainerValidationException extends RuntimeException {

  private final transient List<ValidationErrorItem> details;

  public TrainerValidationException(List<ValidationErrorItem> details) {
    super("Request validation failed.");
    this.details = List.copyOf(details);
  }

  public List<ValidationErrorItem> details() {
    return details;
  }
}
