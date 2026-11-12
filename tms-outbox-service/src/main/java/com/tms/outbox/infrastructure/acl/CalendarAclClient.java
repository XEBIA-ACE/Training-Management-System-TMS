package com.tms.outbox.infrastructure.acl;

import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * Calendar ACL: create/update/cancel workshop and trainer calendar events.
 *
 * <p>TODO SCAFFOLD-SVC-002: endpoint, auth and payload mapping are unspecified in the export.
 */
@Component
public class CalendarAclClient implements IntegrationAclClient {

  @Override
  public String name() {
    return "calendar";
  }

  @Override
  public boolean supports(String eventType) {
    return false;
  }

  @Override
  public CompletableFuture<Void> deliver(OutboxMessage message) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("Calendar ACL delivery is not implemented yet"));
  }
}
