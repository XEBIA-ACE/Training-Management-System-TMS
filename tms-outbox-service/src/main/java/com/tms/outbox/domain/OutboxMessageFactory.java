package com.tms.outbox.domain;

import java.time.Instant;
import java.util.UUID;

/** Creates properly initialised {@link OutboxMessage} instances (C4 factory). */
public final class OutboxMessageFactory {

  private OutboxMessageFactory() {}

  public static OutboxMessage create(
      String aggregateType,
      String aggregateId,
      String eventType,
      String payload,
      int maxRetries,
      String source,
      String correlationId,
      String causationId) {
    UUID id = UUID.randomUUID();
    OutboxEventMetadata metadata =
        new OutboxEventMetadata(id, Instant.now(), source, correlationId, causationId);
    return new OutboxMessage(
        id, aggregateType, aggregateId, eventType, payload, maxRetries, metadata);
  }
}
