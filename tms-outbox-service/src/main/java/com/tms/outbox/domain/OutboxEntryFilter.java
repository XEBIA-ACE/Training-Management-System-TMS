package com.tms.outbox.domain;

import java.time.Instant;

/** Optional filters of {@code GET /api/v1/outbox/entries}. */
public record OutboxEntryFilter(
    OutboxStatus status, String eventType, Instant fromDate, Instant toDate) {}
