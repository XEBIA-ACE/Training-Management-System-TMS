package com.tms.outbox.infrastructure.publisher;

import com.tms.outbox.domain.MessagePublisher;
import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Default when {@code tms.outbox.publisher-type=none}. Processing is skipped entirely in this mode
 * (see {@code OutboxProcessingService}), so this adapter is never expected to be called.
 */
public class UnconfiguredMessagePublisher implements MessagePublisher {

  @Override
  public CompletableFuture<Void> publish(OutboxMessage message) {
    return CompletableFuture.failedFuture(
        new IllegalStateException("No message publisher configured (TMS_OUTBOX_PUBLISHER_TYPE)"));
  }

  /** No broker client is bound, so the message bus is never reachable. */
  @Override
  public boolean isConnected() {
    return false;
  }
}
