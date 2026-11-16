package com.tms.scheduling.application;

import com.tms.scheduling.domain.Allocation;
import com.tms.scheduling.domain.AllocationConflictException;
import com.tms.scheduling.domain.AllocationRepository;
import com.tms.scheduling.domain.AllocationStatus;
import com.tms.scheduling.domain.TrainerUnavailableException;
import com.tms.scheduling.domain.Workshop;
import com.tms.scheduling.domain.WorkshopNotFoundException;
import com.tms.scheduling.domain.WorkshopRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Workshop and Allocation application service: AllocateTrainer (FR-US-002-006). */
@Service
@Transactional
public class AllocationService {

  private final WorkshopRepository workshops;
  private final AllocationRepository allocations;
  private final TrainerAvailabilityPort availability;

  public AllocationService(
      WorkshopRepository workshops,
      AllocationRepository allocations,
      TrainerAvailabilityPort availability) {
    this.workshops = workshops;
    this.allocations = allocations;
    this.availability = availability;
  }

  /**
   * AllocateTrainer: checks availability, then date-overlap conflicts, before saving anything.
   *
   * <p>TODO(FR-US-002-006): minimal default rules (trainer allocatable + availability id matches +
   * no overlapping allocation of any status); the real rule set is an external dependency.
   */
  public Allocation allocateTrainer(
      UUID workshopId, UUID trainerId, UUID availabilityId, AllocationStatus status) {
    Workshop workshop =
        workshops.findById(workshopId).orElseThrow(() -> new WorkshopNotFoundException(workshopId));
    if (!availability.isAvailable(trainerId, availabilityId)) {
      throw new TrainerUnavailableException(
          "Trainer " + trainerId + " is not available for allocation.");
    }
    if (allocations.existsByTrainerIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        trainerId, workshop.getEndDate(), workshop.getStartDate())) {
      throw new AllocationConflictException(
          "Trainer " + trainerId + " already has an overlapping allocation.");
    }
    return allocations.save(new Allocation(workshop, trainerId, status, Instant.now()));
  }
}
