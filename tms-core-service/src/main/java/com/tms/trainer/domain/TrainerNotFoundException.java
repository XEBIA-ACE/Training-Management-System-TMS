package com.tms.trainer.domain;

import java.util.UUID;

public class TrainerNotFoundException extends RuntimeException {

  public TrainerNotFoundException(UUID id) {
    super("Trainer " + id + " was not found.");
  }
}
