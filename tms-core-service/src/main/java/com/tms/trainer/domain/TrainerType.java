package com.tms.trainer.domain;

import java.util.Arrays;
import java.util.Optional;

/** Trainer type of a TrainerCreateRequest (FR-US-003-001/002). */
public enum TrainerType {
  INTERNAL,
  EXTERNAL;

  /** Exact (case-sensitive) match on the enum name; empty for unknown values. */
  public static Optional<TrainerType> parse(String value) {
    return Arrays.stream(values()).filter(t -> t.name().equals(value)).findFirst();
  }
}
