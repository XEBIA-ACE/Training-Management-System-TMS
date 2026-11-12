package com.tms.outbox.infrastructure.acl;

import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Anti-corruption-layer port towards an external integration (email, calendar, survey, BI).
 *
 * <p>TODO SCAFFOLD-SVC-002: ACL endpoints, request schemas and the event-type to ACL routing table
 * are unspecified in the export. Adapters are stubs that support no event types.
 */
public interface IntegrationAclClient {

  String name();

  boolean supports(String eventType);

  CompletableFuture<Void> deliver(OutboxMessage message);
}
