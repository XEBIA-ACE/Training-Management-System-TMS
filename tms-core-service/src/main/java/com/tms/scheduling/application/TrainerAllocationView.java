package com.tms.scheduling.application;

import com.tms.scheduling.domain.AllocationStatus;
import java.util.UUID;

/** Read model returned by GetTrainerAllocations. */
public record TrainerAllocationView(UUID trainerId, UUID workshopId, AllocationStatus status) {}
