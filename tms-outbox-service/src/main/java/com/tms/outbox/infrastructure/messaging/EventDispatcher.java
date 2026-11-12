package com.tms.outbox.infrastructure.messaging;

import com.tms.outbox.domain.IdempotencyChecker;
import com.tms.outbox.domain.MessagePublisher;
import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.infrastructure.acl.IntegrationAclClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * C3 Event Dispatcher: routes an outbox message to the broker through the {@link MessagePublisher}
 * port, guarded by the {@link IdempotencyChecker}.
 *
 * <p>TODO SCAFFOLD-SVC-002: topic naming and event-type routing to the Email/Calendar/Survey/BI
 * ACLs are unspecified in the export; {@link #aclClientsFor(String)} is the extension point.
 */
@Component
public class EventDispatcher {

  private final MessagePublisher messagePublisher;
  private final IdempotencyChecker idempotencyChecker;
  private final List<IntegrationAclClient> aclClients;

  public EventDispatcher(
      MessagePublisher messagePublisher,
      IdempotencyChecker idempotencyChecker,
      List<IntegrationAclClient> aclClients) {
    this.messagePublisher = messagePublisher;
    this.idempotencyChecker = idempotencyChecker;
    this.aclClients = aclClients;
  }

  public CompletableFuture<Void> dispatch(OutboxMessage message) {
    if (idempotencyChecker.isAlreadyProcessed(message)) {
      return CompletableFuture.completedFuture(null);
    }
    return messagePublisher.publish(message);
  }

  public List<IntegrationAclClient> aclClientsFor(String eventType) {
    return aclClients.stream().filter(client -> client.supports(eventType)).toList();
  }
}
