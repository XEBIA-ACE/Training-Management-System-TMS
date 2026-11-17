package com.tms.trainer.api;

import com.tms.crosscutting.audit.AuditLogEntry;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** One entry of {@code GET /api/v1/trainers/{trainerId}/audit-logs} (non-document fields only). */
public record TrainerAuditLogResponse(
    UUID id,
    Instant occurredAt,
    String entryType,
    String actor,
    String action,
    String resourceId,
    String traceId,
    Map<String, Object> details) {

  static TrainerAuditLogResponse from(AuditLogEntry e) {
    return new TrainerAuditLogResponse(
        e.getId(),
        e.getOccurredAt(),
        e.getEntryType().name(),
        e.getActor(),
        e.getAction(),
        e.getResourceId(),
        e.getTraceId(),
        e.getDetails());
  }
}
