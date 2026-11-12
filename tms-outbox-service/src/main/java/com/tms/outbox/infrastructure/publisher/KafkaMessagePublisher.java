package com.tms.outbox.infrastructure.publisher;

import com.tms.outbox.domain.MessagePublisher;
import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka adapter (C4 {@code KafkaMessagePublisher}).
 *
 * <p>TODO SCAFFOLD-SVC-002: topic naming (TopicResolver), keys and producer settings are
 * unspecified in the export; no Kafka client is bound yet. Publishing fails so messages are retried
 * and eventually dead-lettered rather than silently dropped.
 */
public class KafkaMessagePublisher implements MessagePublisher {

  @Override
  public CompletableFuture<Void> publish(OutboxMessage message) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("Kafka publishing is not implemented yet"));
  }
}
