package com.tms.outbox.api.dto;

import java.time.Instant;
import java.util.Map;

/** API model {@code OutboxStatistics}. */
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
    Instant toDate) {}
