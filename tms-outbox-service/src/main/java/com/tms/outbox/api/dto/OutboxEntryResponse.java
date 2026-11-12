package com.tms.outbox.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/** API response model {@code OutboxEntryResponse} (payload deserialised to a JSON object). */
public record OutboxEntryResponse(
    UUID id,
    String aggregateType,
    String aggregateId,
    String eventType,
    JsonNode payload,
    String status,
    int retryCount,
    int maxRetries,
    Instant nextRetryAt,
    String lastError,
    Instant createdAt,
    Instant processedAt,
    String correlationId) {}
