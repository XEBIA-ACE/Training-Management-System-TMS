package com.tms.outbox.infrastructure.acl;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.tms.outbox.config.AclEvaluationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** US-001 (T006, T007, T010): HTTP contract, fail-closed behaviour and latency instrumentation. */
class HttpAclEvaluationClientTest {

  private static final AclEvaluationRequest REQUEST =
      new AclEvaluationRequest("EMAIL", "smtp-gw-1", "notifications", "SEND_EMAIL");

  private HttpServer server;
  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final AtomicReference<String> path = new AtomicReference<>();
  private final AtomicReference<String> body = new AtomicReference<>();

  @AfterEach
  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  private HttpAclEvaluationClient start(String responseJson, long delayMs, long targetMs)
      throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          path.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          try {
            Thread.sleep(delayMs);
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
          byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    String url = "http://127.0.0.1:" + server.getAddress().getPort();
    return new HttpAclEvaluationClient(
        new AclEvaluationProperties(url, "EMAIL", "smtp-gw-1", "notifications", "SEND_EMAIL", targetMs),
        registry);
  }

  /** [US-001-ac-1] POSTs the four specified fields to the evaluate path and returns allowed. */
  @Test
  void us001Ac1_postsSpecifiedContractAndReturnsAllowed() throws Exception {
    HttpAclEvaluationClient client = start("{\"allowed\":true}", 0, 2000);

    AclEvaluationResult result = client.evaluate(REQUEST);

    assertThat(result.allowed()).isTrue();
    assertThat(path.get()).isEqualTo("POST /api/v1/integration-acl/evaluate");
    assertThat(body.get())
        .contains("\"providerType\":\"EMAIL\"", "\"providerId\":\"smtp-gw-1\"")
        .contains("\"internalModule\":\"notifications\"", "\"operation\":\"SEND_EMAIL\"");
  }

  /** [US-001-ac-2] a denial carries the ACL's human-readable reason through unchanged. */
  @Test
  void us001Ac2_deniedResponseCarriesReason() throws Exception {
    HttpAclEvaluationClient client =
        start("{\"allowed\":false,\"reason\":\"Module not permitted for provider\"}", 0, 2000);

    AclEvaluationResult result = client.evaluate(REQUEST);

    assertThat(result.allowed()).isFalse();
    assertThat(result.reason()).isEqualTo("Module not permitted for provider");
  }

  /** [US-001-ac-2] missing configuration fails closed with a reason (no send without an allow). */
  @Test
  void us001Ac2_unconfiguredBaseUrlFailsClosed() {
    HttpAclEvaluationClient client =
        new HttpAclEvaluationClient(
            new AclEvaluationProperties("", "EMAIL", "g", "notifications", "SEND_EMAIL", 200),
            registry);

    AclEvaluationResult result = client.evaluate(REQUEST);

    assertThat(result.allowed()).isFalse();
    assertThat(result.reason()).contains("not configured");
  }

  /** [US-001-ac-5] latency is recorded per call, and a call over the target fails closed. */
  @Test
  void us001Ac5_latencyIsRecordedAndSlowCallsFailClosed() throws Exception {
    HttpAclEvaluationClient fast = start("{\"allowed\":true}", 0, 2000);
    fast.evaluate(REQUEST);
    assertThat(registry.get(HttpAclEvaluationClient.LATENCY_TIMER).tag("outcome", "allowed").timer().count())
        .isEqualTo(1);
    server.stop(0);

    HttpAclEvaluationClient slow = start("{\"allowed\":true}", 600, 100);
    AclEvaluationResult result = slow.evaluate(REQUEST);

    assertThat(result.allowed()).isFalse();
    assertThat(result.reason()).contains("unavailable");
    assertThat(registry.get(HttpAclEvaluationClient.LATENCY_TIMER).tag("outcome", "error").timer().count())
        .isEqualTo(1);
  }
}
