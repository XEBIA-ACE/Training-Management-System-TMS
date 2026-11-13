package com.tms.outbox.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * US-001 settings for the pre-send call to {@code POST /api/v1/integration-acl/evaluate}, bound
 * from {@code tms.outbox.acl-evaluation.*}.
 *
 * <p>ASSUMPTION (FR-US-001-001): {@code providerType} defaults to {@code EMAIL} for both email and
 * SMS gateways, as the spec mandates; how SMS gateways map onto it is unresolved (OPEN QUESTION).
 * {@code operation} defaults to {@code SEND_EMAIL} ("or equivalent" in the spec).
 *
 * <p>LATENCY TARGET (FR-US-001-005): the spec leaves the number to the implementation team. We
 * target p95 &lt;= {@code latencyTargetMs} (default 200 ms) per evaluate call, applied as the HTTP
 * connect/read timeout; a call over target fails closed (no send) and is retried by the outbox
 * retry policy. Measured via the {@code tms.outbox.acl.evaluate.latency} timer.
 *
 * @param baseUrl base URL of the ACL service; blank means not configured (evaluation fails closed)
 * @param providerType providerType sent in the evaluate request
 * @param providerId identifier of the configured email/SMS gateway
 * @param internalModule notifications module identifier sent as internalModule
 * @param operation operation sent in the evaluate request
 * @param latencyTargetMs per-call latency target and timeout in milliseconds
 */
@Validated
@ConfigurationProperties("tms.outbox.acl-evaluation")
public record AclEvaluationProperties(
    @DefaultValue("") String baseUrl,
    @DefaultValue("EMAIL") String providerType,
    @DefaultValue("email-gateway") String providerId,
    @DefaultValue("notifications") String internalModule,
    @DefaultValue("SEND_EMAIL") String operation,
    @DefaultValue("200") @Min(1) long latencyTargetMs) {}
