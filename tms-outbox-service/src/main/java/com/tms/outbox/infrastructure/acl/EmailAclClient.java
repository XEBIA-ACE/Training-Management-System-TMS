package com.tms.outbox.infrastructure.acl;

import com.tms.outbox.application.TrainerAllocationEventClassifier;
import com.tms.outbox.config.AclEvaluationProperties;
import com.tms.outbox.domain.OutboxMessage;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * Email ACL: allocation, deallocation, confirmation, reminder, feedback-invitation and admin-alert
 * notifications.
 *
 * <p>US-001: trainer allocation/deallocation notifications are gated by {@code POST
 * /api/v1/integration-acl/evaluate}. The send happens only when {@code allowed == true}
 * (FR-US-001-001); on deny the denial reason is surfaced and the provider is never invoked
 * (FR-US-001-002); every evaluation is audit-logged without content (FR-US-001-004).
 *
 * <p>TODO SCAFFOLD-SVC-002: other event types and the provider payload mapping are unspecified in
 * the export. Server-side items (rule CRUD, rateLimitPerMinute/dailyQuota enforcement, reason
 * population, evaluator caching) belong to the ACL service (SVC-003), which is not in this repo.
 */
@Component
public class EmailAclClient implements IntegrationAclClient {

  private final TrainerAllocationEventClassifier classifier;
  private final AclEvaluationClient aclEvaluationClient;
  private final AclAuditLogger auditLogger;
  private final TrainerNotificationSender sender;
  private final AclEvaluationProperties properties;

  public EmailAclClient(
      TrainerAllocationEventClassifier classifier,
      AclEvaluationClient aclEvaluationClient,
      AclAuditLogger auditLogger,
      TrainerNotificationSender sender,
      AclEvaluationProperties properties) {
    this.classifier = classifier;
    this.aclEvaluationClient = aclEvaluationClient;
    this.auditLogger = auditLogger;
    this.sender = sender;
    this.properties = properties;
  }

  @Override
  public String name() {
    return "email";
  }

  @Override
  public boolean supports(String eventType) {
    return classifier.classify(eventType).isPresent();
  }

  @Override
  public CompletableFuture<Void> deliver(OutboxMessage message) {
    if (!supports(message.getEventType())) {
      return CompletableFuture.failedFuture(
          new UnsupportedOperationException("Email ACL delivery is not implemented yet"));
    }
    AclEvaluationRequest request =
        new AclEvaluationRequest(
            properties.providerType(),
            properties.providerId(),
            properties.internalModule(),
            properties.operation());
    AclEvaluationResult result = aclEvaluationClient.evaluate(request);
    auditLogger.log(request, result);
    if (!result.allowed()) {
      return CompletableFuture.failedFuture(new AclDeniedException(result.reason()));
    }
    return sender.send(message);
  }
}
