package com.tms.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Tracing/correlation metadata embedded in each {@link OutboxMessage} (C4 value object). */
@Embeddable
public class OutboxEventMetadata {

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "event_timestamp", nullable = false)
  private Instant timestamp;

  @Column(name = "source")
  private String source;

  @Column(name = "correlation_id")
  private String correlationId;

  @Column(name = "causation_id")
  private String causationId;

  protected OutboxEventMetadata() {}

  public OutboxEventMetadata(
      UUID eventId, Instant timestamp, String source, String correlationId, String causationId) {
    this.eventId = eventId;
    this.timestamp = timestamp;
    this.source = source;
    this.correlationId = correlationId;
    this.causationId = causationId;
  }

  /** Broker message headers. TODO SCAFFOLD-SVC-002: header naming is unspecified in the export. */
  public Map<String, Object> toHeaders() {
    Map<String, Object> headers = new LinkedHashMap<>();
    headers.put("eventId", eventId);
    headers.put("timestamp", timestamp);
    if (source != null) {
      headers.put("source", source);
    }
    if (correlationId != null) {
      headers.put("correlationId", correlationId);
    }
    if (causationId != null) {
      headers.put("causationId", causationId);
    }
    return headers;
  }

  public UUID getEventId() {
    return eventId;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getSource() {
    return source;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getCausationId() {
    return causationId;
  }
}
