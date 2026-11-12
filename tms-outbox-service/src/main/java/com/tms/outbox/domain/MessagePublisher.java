package com.tms.outbox.domain;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Broker publishing port (C4 {@code MessagePublisher}). The adapter is selected by {@code
 * tms.outbox.publisher-type} in {@link com.tms.outbox.config.OutboxConfiguration}.
 */
public interface MessagePublisher {

  CompletableFuture<Void> publish(OutboxMessage message);

  default CompletableFuture<Void> publishBatch(List<OutboxMessage> messages) {
    return CompletableFuture.allOf(
        messages.stream().map(this::publish).toArray(CompletableFuture[]::new));
  }
}
