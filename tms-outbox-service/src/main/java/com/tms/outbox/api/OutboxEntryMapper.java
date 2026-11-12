package com.tms.outbox.api;

import com.tms.outbox.api.dto.OutboxEntry;
import com.tms.outbox.api.dto.OutboxEntryResponse;
import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.infrastructure.messaging.EventSerializer;
import org.springframework.stereotype.Component;

/** Maps {@link OutboxMessage} to the API models. */
@Component
public class OutboxEntryMapper {

  private final EventSerializer eventSerializer;

  public OutboxEntryMapper(EventSerializer eventSerializer) {
    this.eventSerializer = eventSerializer;
  }

  public OutboxEntryResponse toResponse(OutboxMessage message) {
    return new OutboxEntryResponse(
        message.getId(),
        message.getAggregateType(),
        message.getAggregateId(),
        message.getEventType(),
        eventSerializer.deserializePayload(message.getPayload()),
        message.getStatus().name(),
        message.getRetryCount(),
        message.getMaxRetries(),
        message.getNextRetryAt(),
        message.getLastError(),
        message.getCreatedAt(),
        message.getProcessedAt(),
        message.getCorrelationId());
  }

  public OutboxEntry toEntry(OutboxMessage message) {
    return new OutboxEntry(
        message.getId(),
        message.getAggregateType(),
        message.getAggregateId(),
        message.getEventType(),
        message.getPayload(),
        message.getMetadata().toHeaders(),
        message.getStatus().name(),
        message.getRetryCount(),
        message.getMaxRetries(),
        message.getNextRetryAt(),
        message.getLastError(),
        message.getCreatedAt(),
        message.getProcessedAt(),
        message.getCorrelationId());
  }
}
