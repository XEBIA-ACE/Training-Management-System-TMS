package com.tms.outbox.infrastructure.acl;

import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * Survey ACL: feedback form creation, distribution and reminders for completed workshops.
 *
 * <p>TODO SCAFFOLD-SVC-002: endpoint, auth and payload mapping are unspecified in the export.
 */
@Component
public class SurveyAclClient implements IntegrationAclClient {

  @Override
  public String name() {
    return "survey";
  }

  @Override
  public boolean supports(String eventType) {
    return false;
  }

  @Override
  public CompletableFuture<Void> deliver(OutboxMessage message) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("Survey ACL delivery is not implemented yet"));
  }
}
