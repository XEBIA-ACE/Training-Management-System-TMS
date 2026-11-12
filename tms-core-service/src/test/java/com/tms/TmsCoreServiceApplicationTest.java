package com.tms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.crosscutting.persistence.DatabaseConnectionHelper;
import com.tms.crosscutting.persistence.DatabaseConnectionHelper.DatabaseConnectionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * AC-1 smoke test: the service starts against PostgreSQL, binds configuration from
 * environment-style properties, applies Flyway migrations, exposes a public /actuator/health and
 * enforces the bearer-JWT RBAC rules.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "TMS_DB_SCHEMA=tms_smoke")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TmsCoreServiceApplicationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired TestRestTemplate restTemplate;
  @Autowired MockMvc mockMvc;
  @Autowired DatabaseConnectionHelper databaseConnectionHelper;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void healthIsPublicAndUp() {
    ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
    assertThat(response.getHeaders().getFirst("X-Request-Id")).isNotBlank();
  }

  @Test
  void livenessAndReadinessProbesArePublic() throws Exception {
    mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
  }

  @Test
  void connectsToPostgresUsingConfiguredSchemaAndAppliesMigrations() {
    DatabaseConnectionInfo info = databaseConnectionHelper.verifyConnection();

    assertThat(info.productName()).isEqualTo("PostgreSQL");
    assertThat(info.schema()).isEqualTo("tms_smoke");
    Integer applied =
        jdbcTemplate.queryForObject(
            "select count(*) from tms_smoke.flyway_schema_history where success", Integer.class);
    assertThat(applied).isGreaterThanOrEqualTo(1);
  }

  @Test
  void metricsRequireAuthenticationWithErrorResponseEnvelope() throws Exception {
    mockMvc
        .perform(get("/actuator/metrics").header("X-Request-Id", "trace-abc-123"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("X-Request-Id", "trace-abc-123"))
        .andExpect(header().exists("WWW-Authenticate"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
        .andExpect(jsonPath("$.traceId").value("trace-abc-123"))
        .andExpect(jsonPath("$.path").value("/actuator/metrics"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void metricsDeniedWithoutOpsRole() throws Exception {
    mockMvc
        .perform(get("/actuator/metrics").with(jwt()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void metricsAllowedForOpsMonitorAndSuperAdmin() throws Exception {
    mockMvc
        .perform(
            get("/actuator/metrics")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPS_MONITOR"))))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/actuator/metrics")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
        .andExpect(status().isOk());
  }

  @Test
  void adminEndpointsRequireSuperAdmin() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/users")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPS_MONITOR"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void apiEndpointsRequireBearerToken() throws Exception {
    mockMvc.perform(get("/api/workshops")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/requirements")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void legacyLoginIsClosedWhenDisabled() throws Exception {
    mockMvc.perform(post("/api/auth/login")).andExpect(status().isUnauthorized());
    mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
  }

  @Test
  void otherActuatorEndpointsAreNotExposed() throws Exception {
    mockMvc
        .perform(
            get("/actuator/env")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
        .andExpect(status().isForbidden());
  }
}
