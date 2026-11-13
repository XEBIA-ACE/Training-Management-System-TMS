package com.tms.outbox.infrastructure.publisher;

import com.tms.outbox.domain.MessagePublisher;
import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;

/**
 * RabbitMQ adapter (C4 {@code RabbitMQMessagePublisher}).
 *
 * <p>TODO SCAFFOLD-SVC-002: exchange name, routing-key scheme and publisher-confirm settings are
 * unspecified in the export; no AMQP client is bound yet. Publishing fails so messages are retried
 * and eventually dead-lettered rather than silently dropped.
 */
public class RabbitMQMessagePublisher implements MessagePublisher {

  @Override
  public CompletableFuture<Void> publish(OutboxMessage message) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("RabbitMQ publishing is not implemented yet"));
  }

  /** No broker client is bound, so the message bus is never reachable. */
  @Override
  public boolean isConnected() {
    return false;
  }
}
