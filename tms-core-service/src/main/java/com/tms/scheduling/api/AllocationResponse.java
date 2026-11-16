package com.tms.scheduling.api;

import com.tms.scheduling.domain.Allocation;
import com.tms.scheduling.domain.AllocationStatus;
import java.util.UUID;

public record AllocationResponse(UUID id, UUID workshopId, UUID trainerId, AllocationStatus status) {

  static AllocationResponse from(Allocation a) {
    return new AllocationResponse(a.getId(), a.getWorkshopId(), a.getTrainerId(), a.getStatus());
  }
}
