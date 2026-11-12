package com.tms.outbox.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** API data model {@code OutboxEntry} (payload as JSON string, free-form metadata). */
public record OutboxEntry(
    UUID id,
    String aggregateType,
    String aggregateId,
    String eventType,
    String payload,
    Map<String, Object> metadata,
    String status,
    int retryCount,
    int maxRetries,
    Instant nextRetryAt,
    String lastError,
    Instant createdAt,
    Instant processedAt,
    String correlationId) {}
