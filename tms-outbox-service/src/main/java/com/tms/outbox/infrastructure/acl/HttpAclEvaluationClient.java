package com.tms.outbox.infrastructure.acl;

import com.tms.outbox.config.AclEvaluationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * HTTP adapter for the Integration ACL evaluate endpoint. Fails closed: a missing base URL, a
 * timeout beyond the latency target, a transport error or an empty body all yield {@code
 * allowed=false} with a reason, so no notification is sent without an explicit allow (US-001-ac-1).
 *
 * <p>TODO US-001-ac-1: service-account authentication for server-to-server calls is not specified
 * in the export and is not implemented here.
 */
@Component
public class HttpAclEvaluationClient implements AclEvaluationClient {

  static final String EVALUATE_PATH = "/api/v1/integration-acl/evaluate";
  static final String LATENCY_TIMER = "tms.outbox.acl.evaluate.latency";

  private static final Logger log = LoggerFactory.getLogger(HttpAclEvaluationClient.class);

  private final MeterRegistry registry;
  private final RestClient restClient;

  public HttpAclEvaluationClient(AclEvaluationProperties properties, MeterRegistry registry) {
    this.registry = registry;
    if (StringUtils.hasText(properties.baseUrl())) {
      SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
      Duration timeout = Duration.ofMillis(properties.latencyTargetMs());
      factory.setConnectTimeout(timeout);
      factory.setReadTimeout(timeout);
      this.restClient =
          RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
    } else {
      this.restClient = null;
    }
  }

  @Override
  public AclEvaluationResult evaluate(AclEvaluationRequest request) {
    if (restClient == null) {
      return new AclEvaluationResult(
          false, "ACL evaluation is not configured (tms.outbox.acl-evaluation.base-url is blank)");
    }
    Timer.Sample sample = Timer.start(registry);
    String outcome = "error";
    try {
      AclEvaluationResult result =
          restClient
              .post()
              .uri(EVALUATE_PATH)
              .body(request)
              .retrieve()
              .body(AclEvaluationResult.class);
      if (result == null) {
        return new AclEvaluationResult(false, "ACL evaluation returned an empty response");
      }
      outcome = result.allowed() ? "allowed" : "denied";
      return result;
    } catch (RuntimeException e) {
      log.warn("ACL evaluation call failed: {}", e.getClass().getSimpleName());
      return new AclEvaluationResult(
          false, "ACL evaluation unavailable: " + e.getClass().getSimpleName());
    } finally {
      sample.stop(Timer.builder(LATENCY_TIMER).tag("outcome", outcome).register(registry));
    }
  }
}
