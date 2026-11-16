package com.tms.scheduling.api;

import com.tms.scheduling.domain.AllocationStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Body of the allocate-trainer call; {@code trainerId} and {@code availabilityId} are the
 * identifiers returned per trainer by GET /api/v1/trainers (FR-US-002-006). A missing {@code
 * status} defaults to TENTATIVE (provisional choice).
 */
public record AllocateTrainerRequest(
    @NotNull UUID trainerId, @NotNull UUID availabilityId, AllocationStatus status) {}
