package com.tms.outbox.api.dto;

import java.time.Instant;
import java.util.Map;

/**
 * API model {@code OutboxStatistics}. {@code trainerAllocation} (US-001, FR-US-001-008/009) adds
 * the allocation/deallocation counts grouped by status and event type.
 */
public record OutboxStatistics(
    long totalEntries,
    long pendingCount,
    long processingCount,
    long completedCount,
    long failedCount,
    long skippedCount,
    double averageProcessingTimeMs,
    Map<String, Long> entriesByEventType,
    Instant fromDate,
    Instant toDate,
    TrainerAllocationOutboxStatistics trainerAllocation) {}
