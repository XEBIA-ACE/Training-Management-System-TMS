package com.tms.outbox.api.dto;

import java.time.Instant;

/** API model {@code ProcessorStatus}. */
public record ProcessorStatus(
    boolean running,
    Instant lastRunAt,
    Instant nextScheduledRunAt,
    int currentBatchSize,
    long pollingIntervalMs,
    int entriesProcessedLastRun) {}
