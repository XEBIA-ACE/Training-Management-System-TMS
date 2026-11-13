package com.tms.outbox.infrastructure.acl;

import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/** Default sender until a provider adapter exists (preserves the previous stub behaviour). */
@Component
public class UnconfiguredTrainerNotificationSender implements TrainerNotificationSender {

  @Override
  public CompletableFuture<Void> send(OutboxMessage message) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("Email ACL delivery is not implemented yet"));
  }
}
