package com.tms.outbox.infrastructure.acl;

/** Body of {@code POST /api/v1/integration-acl/evaluate} (US-001-ac-1). */
public record AclEvaluationRequest(
    String providerType, String providerId, String internalModule, String operation) {}
