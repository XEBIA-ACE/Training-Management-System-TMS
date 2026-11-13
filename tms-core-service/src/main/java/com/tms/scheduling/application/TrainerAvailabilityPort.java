package com.tms.scheduling.application;

import java.util.UUID;

/**
 * Availability lookup implemented by the trainer module so that scheduling does not depend on it.
 *
 * <p>TODO(FR-US-002-006, gap "Detailed availability and conflict rule set for AllocateTrainer"):
 * the rule set is an external dependency; implementations apply only a minimal default.
 */
public interface TrainerAvailabilityPort {

  /** True when the trainer exists, is allocatable and the id is that trainer's availability. */
  boolean isAvailable(UUID trainerId, UUID availabilityId);
}
