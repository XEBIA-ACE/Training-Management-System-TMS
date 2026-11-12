package com.tms.outbox.api.dto;

import java.time.Instant;
import java.util.UUID;

/** API model {@code RetryResponse}. */
public record RetryResponse(UUID entryId, String status, String message, Instant scheduledAt) {}
