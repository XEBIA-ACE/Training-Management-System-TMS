package com.tms.outbox.infrastructure.acl;

import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * BI ACL: export / data-refresh notifications for reporting workflows.
 *
 * <p>TODO SCAFFOLD-SVC-002: endpoint, auth and payload mapping are unspecified in the export.
 */
@Component
public class BiAclClient implements IntegrationAclClient {

  @Override
  public String name() {
    return "bi";
  }

  @Override
  public boolean supports(String eventType) {
    return false;
  }

  @Override
  public CompletableFuture<Void> deliver(OutboxMessage message) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("Bi ACL delivery is not implemented yet"));
  }
}
