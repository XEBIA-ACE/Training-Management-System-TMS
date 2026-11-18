package com.tms.trainer.domain;

import java.util.UUID;

/**
 * FR-US-004-AC3-001/002: raised when an INACTIVE trainer is targeted by a new allocation. The
 * message is the business-level text for the ErrorResponse; {@link #CODE} is its error code.
 */
public class TrainerInactiveException extends RuntimeException {

  public static final String CODE = "TRAINER_INACTIVE";

  public TrainerInactiveException(UUID id) {
    super("Trainer " + id + " is inactive and cannot be allocated.");
  }
}
