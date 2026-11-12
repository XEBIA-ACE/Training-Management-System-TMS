package com.tms.outbox.application;

/** Aggregated outbox metrics (C4 {@code OutboxMetricsDTO}). */
public record OutboxMetricsDTO(
    long pendingCount,
    long processingCount,
    long completedCount,
    long failedCount,
    long deadLetterCount,
    double avgProcessingTimeMs) {}
