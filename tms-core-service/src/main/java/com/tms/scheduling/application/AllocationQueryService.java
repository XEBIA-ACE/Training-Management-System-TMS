package com.tms.scheduling.application;

import com.tms.scheduling.domain.AllocationRepository;
import com.tms.scheduling.domain.AllocationStatus;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of the Workshop and Allocation application service (FR-US-002-004). */
@Service
@Transactional(readOnly = true)
public class AllocationQueryService {

  private final AllocationRepository allocations;

  public AllocationQueryService(AllocationRepository allocations) {
    this.allocations = allocations;
  }

  /** GetTrainerAllocations: current (not yet ended) allocations of the given trainers. */
  public List<TrainerAllocationView> getTrainerAllocations(Collection<UUID> trainerIds) {
    if (trainerIds.isEmpty()) {
      return List.of();
    }
    return allocations.findCurrentByTrainerIds(trainerIds, today()).stream()
        .map(a -> new TrainerAllocationView(a.getTrainerId(), a.getWorkshopId(), a.getStatus()))
        .toList();
  }

  public Set<UUID> trainerIdsWithStatus(AllocationStatus status) {
    return new HashSet<>(allocations.findTrainerIdsWithStatus(status, today()));
  }

  public Set<UUID> trainerIdsWithAnyAllocation() {
    return new HashSet<>(allocations.findTrainerIdsWithAnyCurrentAllocation(today()));
  }

  private static LocalDate today() {
    return LocalDate.now(ZoneOffset.UTC);
  }
}
