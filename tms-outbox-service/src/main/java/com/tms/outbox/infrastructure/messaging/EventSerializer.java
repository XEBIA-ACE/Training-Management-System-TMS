package com.tms.outbox.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.outbox.domain.OutboxMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * C3 Event Serializer: JSON (Jackson) conversion of outbox payloads.
 *
 * <p>TODO SCAFFOLD-SVC-002: the broker message envelope is unspecified in the export; the stored
 * payload is published as-is with {@link #headers(OutboxMessage)} as message headers.
 */
@Component
public class EventSerializer {

  private final ObjectMapper objectMapper;

  public EventSerializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public byte[] serializePayload(OutboxMessage message) {
    return message.getPayload().getBytes(StandardCharsets.UTF_8);
  }

  public Map<String, Object> headers(OutboxMessage message) {
    return message.getMetadata().toHeaders();
  }

  /** Deserialises a stored JSON payload; returns the raw text node when it is not valid JSON. */
  public JsonNode deserializePayload(String payload) {
    if (payload == null) {
      return null;
    }
    try {
      return objectMapper.readTree(payload);
    } catch (JsonProcessingException e) {
      return objectMapper.getNodeFactory().textNode(payload);
    }
  }
}
