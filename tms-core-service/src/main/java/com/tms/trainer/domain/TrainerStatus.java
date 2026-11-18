package com.tms.trainer.domain;

/**
 * Trainer lifecycle statuses written by US-004. {@code Trainer.status} stays an opaque string in
 * storage (US-002 GAP); these are the only values PATCH /api/v1/trainers/{trainerId}/status
 * accepts. {@link #ALL} is a search filter sentinel only, never a stored status.
 */
public final class TrainerStatus {

  public static final String ACTIVE = "ACTIVE";
  public static final String INACTIVE = "INACTIVE";
  /** Filter value (FR-US-004-AC2-003): no status restriction, INACTIVE trainers included. */
  public static final String ALL = "ALL";

  private TrainerStatus() {}

  public static boolean isSettable(String value) {
    return ACTIVE.equals(value) || INACTIVE.equals(value);
  }
}
