package com.tms.trainer.domain;

import java.util.List;
import java.util.UUID;

/**
 * SearchTrainers request model (FR-US-002-001/002). A {@code null} (or empty) field means "no
 * constraint".
 *
 * <p>TODO(FR-US-002-001, gap "behavior when a filter parameter is omitted or null"): treating a
 * missing filter as "no constraint" is a conservative default, not a product decision.
 *
 * @param certificationIds OEM certification identifiers; a trainer must hold all of them
 *     (TODO(FR-US-002-001): all-vs-any is not specified; "every specified criterion" is read as
 *     all).
 */
public record TrainerSearchCriteria(
    TrainerStatus status,
    List<UUID> certificationIds,
    AllocationState allocationState,
    String location,
    Boolean weekendAvailable,
    String text) {

  public boolean hasText() {
    return text != null && !text.isBlank();
  }
}
