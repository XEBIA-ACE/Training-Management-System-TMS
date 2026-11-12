package com.tms.integrationacl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * AC-1 smoke test (SCAFFOLD-SVC-003): the context loads with no environment variables or profile
 * set, {@code /actuator/health} answers 200 UP and everything else is protected.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationAclServiceApplicationTest {

  @Autowired TestRestTemplate restTemplate;

  @Test
  void healthIsPublicAndUp() {
    ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
  }

  @Test
  void livenessAndReadinessProbesAreUp() {
    assertThat(restTemplate.getForEntity("/actuator/health/liveness", String.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(
            restTemplate.getForEntity("/actuator/health/readiness", String.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  void apiAndMetricsRequireAuthentication() {
    assertThat(restTemplate.getForEntity("/api/v1/integration-acl", String.class).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(restTemplate.getForEntity("/actuator/metrics", String.class).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
