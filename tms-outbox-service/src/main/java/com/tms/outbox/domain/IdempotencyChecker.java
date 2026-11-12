package com.tms.outbox.domain;

import org.springframework.stereotype.Component;

/**
 * C3 Idempotency Checker: prevents re-publishing an event that was already delivered.
 *
 * <p>TODO SCAFFOLD-SVC-002: the deduplication store/key strategy for downstream side effects is
 * unspecified in the export; this scaffold only relies on the persisted message status.
 */
@Component
public class IdempotencyChecker {

  public boolean isAlreadyProcessed(OutboxMessage message) {
    return message.getStatus() == OutboxStatus.COMPLETED;
  }

  public void markProcessed(OutboxMessage message) {
    // TODO SCAFFOLD-SVC-002: record message.getMetadata().getEventId() in a dedup store.
  }
}
