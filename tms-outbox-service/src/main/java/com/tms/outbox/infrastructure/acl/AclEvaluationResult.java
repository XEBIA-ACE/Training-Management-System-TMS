package com.tms.outbox.infrastructure.acl;

/** Response of the evaluate endpoint; {@code reason} is a human-readable denial reason. */
public record AclEvaluationResult(boolean allowed, String reason) {}
