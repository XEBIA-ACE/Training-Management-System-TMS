package com.tms.trainer.application;

import com.tms.scheduling.application.TrainerAvailabilityPort;
import com.tms.trainer.domain.TrainerRepository;
import com.tms.trainer.domain.TrainerStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supplies trainer availability to AllocateTrainer (FR-US-002-006).
 *
 * <p>TODO(FR-US-002-006): minimal default (trainer is ACTIVE and the availability id matches).
 */
@Component
@Transactional(readOnly = true)
class TrainerAvailabilityAdapter implements TrainerAvailabilityPort {

  private final TrainerRepository trainers;

  TrainerAvailabilityAdapter(TrainerRepository trainers) {
    this.trainers = trainers;
  }

  @Override
  public boolean isAvailable(UUID trainerId, UUID availabilityId) {
    return trainers
        .findById(trainerId)
        .filter(t -> t.getStatus() == TrainerStatus.ACTIVE)
        .filter(t -> t.getAvailabilityId().equals(availabilityId))
        .isPresent();
  }
}
