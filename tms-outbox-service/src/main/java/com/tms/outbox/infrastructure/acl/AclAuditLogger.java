package com.tms.outbox.infrastructure.acl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * US-001-ac-4: audit entry for every ACL evaluation made for a trainer notification. Records only
 * internalModule, providerType, providerId, operation and the allow/deny decision (plus the denial
 * reason); the notification content (subject, body, SMS text, recipient) is never passed in.
 *
 * <p>TODO US-001-ac-4: only the structured log sink is implemented; a persistent audit store is not
 * specified in the export.
 */
@Component
public class AclAuditLogger {

  private static final Logger audit = LoggerFactory.getLogger("com.tms.outbox.acl.audit");

  public void log(AclEvaluationRequest request, AclEvaluationResult result) {
    audit.info(
        "acl-evaluation internalModule={} providerType={} providerId={} operation={} allowed={}"
            + " reason={}",
        request.internalModule(),
        request.providerType(),
        request.providerId(),
        request.operation(),
        result.allowed(),
        result.allowed() ? "" : result.reason());
  }
}
