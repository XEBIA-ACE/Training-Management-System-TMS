package com.tms.outbox.infrastructure.acl;

/** Port for {@code POST /api/v1/integration-acl/evaluate}. */
public interface AclEvaluationClient {

  /** Never throws: transport/contract failures are returned as a denial (fail closed). */
  AclEvaluationResult evaluate(AclEvaluationRequest request);
}
