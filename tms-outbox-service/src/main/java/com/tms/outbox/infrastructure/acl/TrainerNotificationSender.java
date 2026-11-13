package com.tms.outbox.infrastructure.acl;

import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Port for the actual provider send (email/SMS gateway). Only reached after the ACL allowed the
 * call (FR-US-001-002: no provider invocation on deny).
 *
 * <p>TODO SCAFFOLD-SVC-002: provider endpoint, auth and payload mapping are unspecified in the
 * export; the only adapter fails with {@link UnsupportedOperationException}.
 */
public interface TrainerNotificationSender {

  CompletableFuture<Void> send(OutboxMessage message);
}
