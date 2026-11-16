package com.tms.trainer.api;

import com.tms.crosscutting.web.ValidationErrorItem;
import java.util.List;

/** Thrown by {@link CreateTrainerValidator} when the payload is invalid. */
class TrainerValidationException extends RuntimeException {

  private final transient List<ValidationErrorItem> details;

  TrainerValidationException(List<ValidationErrorItem> details) {
    super("Request validation failed.");
    this.details = details;
  }

  List<ValidationErrorItem> details() {
    return details;
  }
}
