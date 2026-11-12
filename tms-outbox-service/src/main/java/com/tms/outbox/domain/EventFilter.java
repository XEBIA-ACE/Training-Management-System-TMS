package com.tms.outbox.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * C3 Event Filter: validates events before dispatch. Returns the rejection reason, if any.
 *
 * <p>TODO SCAFFOLD-SVC-002: event-type allow-lists and per-type payload schemas are unspecified in
 * the export; only structural checks are applied.
 */
@Component
public class EventFilter {

  private final ObjectMapper objectMapper;

  public EventFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Optional<String> validate(OutboxMessage message) {
    if (!StringUtils.hasText(message.getEventType())) {
      return Optional.of("eventType is required");
    }
    if (!StringUtils.hasText(message.getAggregateType())
        || !StringUtils.hasText(message.getAggregateId())) {
      return Optional.of("aggregateType and aggregateId are required");
    }
    if (!StringUtils.hasText(message.getPayload())) {
      return Optional.of("payload is required");
    }
    try {
      objectMapper.readTree(message.getPayload());
    } catch (JsonProcessingException e) {
      return Optional.of("payload is not valid JSON");
    }
    return Optional.empty();
  }
}
