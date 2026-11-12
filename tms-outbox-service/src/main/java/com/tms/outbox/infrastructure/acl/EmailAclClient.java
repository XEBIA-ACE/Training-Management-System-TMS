package com.tms.outbox.infrastructure.acl;

import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * Email ACL: allocation, deallocation, confirmation, reminder, feedback-invitation and admin-alert
 * notifications.
 *
 * <p>TODO SCAFFOLD-SVC-002: endpoint, auth and payload mapping are unspecified in the export.
 */
@Component
public class EmailAclClient implements IntegrationAclClient {

  @Override
  public String name() {
    return "email";
  }

  @Override
  public boolean supports(String eventType) {
    return false;
  }

  @Override
  public CompletableFuture<Void> deliver(OutboxMessage message) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("Email ACL delivery is not implemented yet"));
  }
}
