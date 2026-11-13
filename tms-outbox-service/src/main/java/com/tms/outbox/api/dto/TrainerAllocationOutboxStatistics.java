package com.tms.outbox.api.dto;

import java.util.Map;

/**
 * US-001 (FR-US-001-009): trainer allocation/deallocation section of {@link OutboxStatistics}.
 * Status keys are {@code OutboxStatus} names (DEAD_LETTER = retries exhausted); event type keys are
 * {@code allocation} and {@code deallocation}. All keys are always present (zero-filled).
 *
 * @param totalEntries allocation + deallocation entries in the requested range
 * @param backlogCount entries still awaiting publication (PENDING, PROCESSING, FAILED)
 * @param countsByStatus counts per status across both event types
 * @param countsByEventType counts per event type across all statuses
 * @param countsByEventTypeAndStatus counts per event type, then status
 */
public record TrainerAllocationOutboxStatistics(
    long totalEntries,
    long backlogCount,
    Map<String, Long> countsByStatus,
    Map<String, Long> countsByEventType,
    Map<String, Map<String, Long>> countsByEventTypeAndStatus) {}
