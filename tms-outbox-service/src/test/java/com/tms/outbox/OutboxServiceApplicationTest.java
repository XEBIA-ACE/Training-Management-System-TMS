package com.tms.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.domain.OutboxMessageFactory;
import com.tms.outbox.domain.OutboxRepository;
import com.tms.outbox.infrastructure.persistence.DatabaseConnectionHelper;
import com.tms.outbox.infrastructure.persistence.DatabaseConnectionHelper.DatabaseConnectionInfo;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * AC-1 smoke test (SCAFFOLD-SVC-002): the service starts against PostgreSQL using only
 * environment-style configuration, applies the outbox migration, answers 200 on /health and wires
 * the /api/v1/outbox API with scopes, ErrorResponse and correlation ids.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
@ActiveProfiles("test")
@Testcontainers
class OutboxServiceApplicationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired TestRestTemplate restTemplate;
  @Autowired MockMvc mockMvc;
  @Autowired DatabaseConnectionHelper databaseConnectionHelper;
  @Autowired OutboxRepository outboxRepository;

  @Value("${TMS_OUTBOX_DB_SCHEMA}")
  String configuredSchema;

  private static RequestPostProcessor scope(String scope) {
    return jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + scope));
  }

  private OutboxMessage saveFailedEntry() {
    OutboxMessage message =
        OutboxMessageFactory.create(
            "Workshop",
            UUID.randomUUID().toString(),
            "WorkshopCompleted",
            "{\"workshopId\":\"w-1\"}",
            5,
            "tms-core-service",
            "corr-1",
            null);
    message.markAsFailed("broker unavailable");
    return outboxRepository.save(message);
  }

  @Test
  void healthIsPublicAndUp() {
    ResponseEntity<String> response = restTemplate.getForEntity("/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"").contains("\"database\"");
    assertThat(response.getHeaders().getFirst("X-Correlation-ID")).isNotBlank();
  }

  @Test
  void readinessLivenessAndMetricsArePublic() throws Exception {
    mockMvc
        .perform(get("/health/ready"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mockMvc
        .perform(get("/health/live"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mockMvc
        .perform(get("/metrics"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("jvm_memory_used_bytes")));
  }

  @Test
  void connectsToPostgresUsingConfiguredSchema() {
    DatabaseConnectionInfo info = databaseConnectionHelper.verifyConnection();

    assertThat(info.productName()).isEqualTo("PostgreSQL");
    assertThat(info.schema()).isEqualTo(configuredSchema);
  }

  @Test
  void apiRequiresBearerTokenWithErrorResponseAndCorrelationId() throws Exception {
    mockMvc
        .perform(get("/api/v1/outbox/entries").header("X-Request-ID", "req-123"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("X-Correlation-ID", "req-123"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/v1/outbox/entries"))
        .andExpect(jsonPath("$.correlationId").value("req-123"));
  }

  @Test
  void scopesAreEnforced() throws Exception {
    mockMvc
        .perform(get("/api/v1/outbox/entries").with(scope("other")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
    mockMvc
        .perform(
            post("/api/v1/outbox/entries/" + UUID.randomUUID() + "/retry")
                .with(scope("outbox:read")))
        .andExpect(status().isForbidden());
  }

  @Test
  void listAndGetEntries() throws Exception {
    OutboxMessage saved = saveFailedEntry();

    mockMvc
        .perform(
            get("/api/v1/outbox/entries")
                .param("status", "FAILED")
                .param("size", "100")
                .with(scope("outbox:read")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.size").value(100))
        .andExpect(jsonPath("$.first").value(true));
    mockMvc
        .perform(get("/api/v1/outbox/entries/" + saved.getId()).with(scope("outbox:read")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(saved.getId().toString()))
        .andExpect(jsonPath("$.payload.workshopId").value("w-1"))
        .andExpect(jsonPath("$.status").value("FAILED"));
  }

  @Test
  void pageSizeAbove100IsRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/outbox/entries").param("size", "101").with(scope("outbox:read")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.correlationId").isNotEmpty());
  }

  @Test
  void unknownEntryReturns404() throws Exception {
    mockMvc
        .perform(get("/api/v1/outbox/entries/" + UUID.randomUUID()).with(scope("outbox:read")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"));
  }

  @Test
  void retryAndSkipFailedEntries() throws Exception {
    OutboxMessage toRetry = saveFailedEntry();
    mockMvc
        .perform(
            post("/api/v1/outbox/entries/" + toRetry.getId() + "/retry")
                .with(scope("outbox:manage")))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.entryId").value(toRetry.getId().toString()))
        .andExpect(jsonPath("$.status").value("PENDING"));
    mockMvc
        .perform(
            post("/api/v1/outbox/entries/" + toRetry.getId() + "/retry")
                .with(scope("outbox:manage")))
        .andExpect(status().isBadRequest());

    OutboxMessage toSkip = saveFailedEntry();
    mockMvc
        .perform(
            post("/api/v1/outbox/entries/" + toSkip.getId() + "/skip")
                .with(scope("outbox:admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"duplicate event\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SKIPPED"));
    mockMvc
        .perform(
            post("/api/v1/outbox/entries/" + saveFailedEntry().getId() + "/skip")
                .with(scope("outbox:admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));
  }

  @Test
  void statisticsAndProcessorLifecycle() throws Exception {
    mockMvc
        .perform(get("/api/v1/outbox/stats").with(scope("outbox:read")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalEntries").isNumber())
        .andExpect(jsonPath("$.entriesByEventType").isMap());
    mockMvc
        .perform(get("/api/v1/outbox/processor/status").with(scope("outbox:read")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.running").value(false))
        .andExpect(jsonPath("$.currentBatchSize").value(100));
    mockMvc
        .perform(post("/api/v1/outbox/processor/start").with(scope("outbox:admin")))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.running").value(true));
    mockMvc
        .perform(post("/api/v1/outbox/processor/start").with(scope("outbox:admin")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
    mockMvc
        .perform(post("/api/v1/outbox/processor/stop").with(scope("outbox:admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.running").value(false));
  }
}
