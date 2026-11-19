package com.tms.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.trainer.application.TrainerAppService;
import com.tms.trainer.domain.TrainerInactiveException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * US-004 acceptance tests for trainer activation/inactivation ({@code PATCH
 * /api/v1/trainers/{trainerId}/status}), the status filter of {@code GET /api/v1/trainers}, the
 * allocation guard and the trainer/admin audit views. Test obligations:
 * aa728f09-dcd2-4e0c-9c1a-27c6053543a9:test:acceptance:US-004-ac-1..ac-7.
 *
 * <p>GAP: GET /api/v1/allocations/eligible-trainers and POST /api/v1/allocations do not exist in
 * this repository; the eligibility and allocation clauses are verified through the shared trainer
 * search and {@link TrainerAppService#requireAllocatable(UUID)} (see
 * docs/adr/US-004-trainer-status-gaps.md).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TrainerStatusAcceptanceTest {

  private static final String TRAINERS = "/api/v1/trainers";
  private static final String ADMIN = "/api/v1/admin";
  private static final UUID PUNE = UUID.randomUUID();
  private static final UUID AWS = UUID.randomUUID();
  private static final UUID AZURE = UUID.randomUUID();

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired TrainerAppService trainerService;

  private UUID alice;
  private UUID bob;

  @BeforeEach
  void seed() {
    jdbc.update("delete from tms.audit_log_entry");
    jdbc.update("delete from tms.trainer_certification");
    jdbc.update("delete from tms.trainer");
    alice = trainer("E1", "Alice Smith", "alice@corp.com", "ACTIVE", "AVAILABLE", AWS, AZURE);
    bob = trainer("E2", "Bob Jones", "bob@corp.com", "ACTIVE", "AVAILABLE", AWS);
  }

  // --- US-004-ac-1 (FR-US-004-AC1-001/002/003) -------------------------------------------------

  @Test
  @DisplayName("[US-004-ac-1][US-004-AC1][FR-US-004-AC1-002/003] HR_ADMIN changes only status + metadata")
  void us004Ac1_onlyStatusAndMetadataChange() throws Exception {
    Map<String, Object> before = row(alice);

    mockMvc
        .perform(patchStatus(alice, "INACTIVE", token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(alice.toString()))
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.name").value("Alice Smith"))
        .andExpect(jsonPath("$.email").value("alice@corp.com"))
        .andExpect(jsonPath("$.certificationIds.length()").value(2))
        .andExpect(jsonPath("$.statusChangedBy").value("hr-1"))
        .andExpect(jsonPath("$.statusChangedAt").isNotEmpty());

    Map<String, Object> after = row(alice);
    assertThat(after.get("status")).isEqualTo("INACTIVE");
    assertThat(after.get("status_changed_by")).isEqualTo("hr-1");
    assertThat(after.get("status_changed_at")).isNotNull();
    for (String column :
        List.of(
            "employee_id",
            "name",
            "email",
            "availability_state",
            "location_id",
            "weekend_available",
            "created_at",
            "updated_at")) {
      assertThat(after.get(column)).as(column).isEqualTo(before.get(column));
    }
    assertThat(count("select count(*) from tms.trainer_certification where trainer_id = ?", alice))
        .isEqualTo(2);
  }

  @Test
  @DisplayName("[US-004-ac-1][US-004-AC1][FR-US-004-AC1-001] SUPER_ADMIN may change status; bad value is 400")
  void us004Ac1_superAdminAllowedAndValueValidated() throws Exception {
    mockMvc
        .perform(patchStatus(bob, "INACTIVE", token("root", "ROLE_SUPER_ADMIN")))
        .andExpect(status().isOk());
    mockMvc
        .perform(patchStatus(bob, "SUSPENDED", token("root", "ROLE_SUPER_ADMIN")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    assertThat(statusOf(bob)).isEqualTo("INACTIVE");
    mockMvc
        .perform(patchStatus(UUID.randomUUID(), "ACTIVE", token("root", "ROLE_SUPER_ADMIN")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TRAINER_NOT_FOUND"));
  }

  // --- US-004-ac-2 (FR-US-004-AC2-001/002/003/004) ---------------------------------------------

  @Test
  @DisplayName("[US-004-ac-2][US-004-AC2][FR-US-004-AC2-001/003] INACTIVE hidden by default, shown on filter")
  void us004Ac2_inactiveExcludedUnlessRequested() throws Exception {
    deactivate(alice);

    assertThat(listIds(Map.of())).containsExactly(bob);
    assertThat(listIds(Map.of("status", "ACTIVE"))).containsExactly(bob);
    assertThat(listIds(Map.of("status", "INACTIVE"))).containsExactly(alice);
    assertThat(listIds(Map.of("status", "ALL"))).containsExactlyInAnyOrder(alice, bob);
    // Other filters keep applying to the default (status-less) search.
    assertThat(listIds(Map.of("certificationId", AZURE.toString()))).isEmpty();
    assertThat(listIds(Map.of("certificationId", AZURE.toString(), "status", "ALL")))
        .containsExactly(alice);
  }

  @Test
  @DisplayName("[US-004-ac-2][US-004-AC2][FR-US-004-AC2-004] an INACTIVE trainer cannot be selected")
  void us004Ac2_inactiveTrainerNotAllocatable() throws Exception {
    deactivate(alice);
    assertThatThrownBy(() -> trainerService.requireAllocatable(alice))
        .isInstanceOf(TrainerInactiveException.class);
  }

  // --- US-004-ac-3 (FR-US-004-AC3-001/002) -----------------------------------------------------

  @Test
  @DisplayName("[US-004-ac-3][US-004-AC3][FR-US-004-AC3-002] inactive guard has a business-level message")
  void us004Ac3_inactiveGuardMessageAndActiveUnaffected() throws Exception {
    deactivate(alice);
    assertThatThrownBy(() -> trainerService.requireAllocatable(alice))
        .isInstanceOf(TrainerInactiveException.class)
        .hasMessageContaining("inactive and cannot be allocated");
    assertThat(TrainerInactiveException.CODE).isEqualTo("TRAINER_INACTIVE");
    trainerService.requireAllocatable(bob); // ACTIVE trainers are unaffected
  }

  // --- US-004-ac-4 (FR-US-004-AC4-001/002/003) -------------------------------------------------

  @Test
  @DisplayName("[US-004-ac-4][US-004-AC4][FR-US-004-AC4-001/003] reactivation restores search eligibility")
  void us004Ac4_reactivationRestoresEligibility() throws Exception {
    deactivate(alice);
    assertThat(listIds(Map.of("certificationId", AZURE.toString()))).isEmpty();

    mockMvc
        .perform(patchStatus(alice, "ACTIVE", token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    assertThat(listIds(Map.of())).containsExactlyInAnyOrder(alice, bob);
    // Existing certification/availability filters still decide eligibility.
    assertThat(listIds(Map.of("certificationId", AZURE.toString()))).containsExactly(alice);
    assertThat(listIds(Map.of("availabilityState", "ALLOCATED"))).isEmpty();
    trainerService.requireAllocatable(alice);
  }

  // --- US-004-ac-5 (FR-US-004-AC5-001/002/003) -------------------------------------------------

  @Test
  @DisplayName("[US-004-ac-5][US-004-AC5][FR-US-004-AC5-002/003] SUCCESS and DENIED attempts are audited")
  void us004Ac5_everyAttemptAuditedAndRetrievable() throws Exception {
    Instant before = Instant.now().minusSeconds(1);
    mockMvc
        .perform(patchStatus(alice, "INACTIVE", token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk());
    mockMvc
        .perform(patchStatus(alice, "ACTIVE", token("viewer-9", "ROLE_VIEWER")))
        .andExpect(status().isForbidden());

    JsonNode trainerLog = json(mockMvc.perform(auditLogs(alice)).andReturn().getResponse());
    List<JsonNode> changes = statusChanges(trainerLog);
    assertThat(changes).hasSize(2);
    JsonNode success = changes.get(0);
    assertThat(success.get("actor").asText()).isEqualTo("hr-1");
    assertThat(success.get("resourceId").asText()).isEqualTo(alice.toString());
    assertThat(Instant.parse(success.get("occurredAt").asText())).isAfter(before);
    assertThat(success.get("details").get("previousStatus").asText()).isEqualTo("ACTIVE");
    assertThat(success.get("details").get("newStatus").asText()).isEqualTo("INACTIVE");
    assertThat(success.get("details").get("outcome").asText()).isEqualTo("SUCCESS");
    JsonNode denied = changes.get(1);
    assertThat(denied.get("actor").asText()).isEqualTo("viewer-9");
    assertThat(denied.get("details").get("previousStatus").asText()).isEqualTo("INACTIVE");
    assertThat(denied.get("details").get("newStatus").asText()).isEqualTo("ACTIVE");
    assertThat(denied.get("details").get("outcome").asText()).isEqualTo("DENIED");

    // The same entries are visible through the admin audit log (SUPER_ADMIN only).
    JsonNode admin =
        json(
            mockMvc
                .perform(get(ADMIN + "/audit-logs").with(token("root", "ROLE_SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse());
    assertThat(statusChanges(admin.get("items"))).hasSize(2);
    assertThat(admin.get("items").toString()).doesNotContain("UNAUTHORIZED_ACCESS");
  }

  @Test
  @DisplayName("[US-004-AC5][FR-US-004-011] GET /admin/audit-logs filters status-change events")
  void us004Ac5_adminAuditLogFilters() throws Exception {
    mockMvc
        .perform(patchStatus(alice, "INACTIVE", token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk());
    mockMvc
        .perform(patchStatus(bob, "INACTIVE", token("hr-2", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk());
    mockMvc
        .perform(patchStatus(alice, "ACTIVE", token("hr-2", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk());

    assertThat(adminAudit("trainerId", alice.toString())).hasSize(2);
    assertThat(adminAudit("actor", "hr-2")).hasSize(2);
    assertThat(adminAudit("newStatus", "INACTIVE")).hasSize(2);
    assertThat(adminAudit("previousStatus", "INACTIVE")).hasSize(1);
    assertThat(adminAudit("trainerId", alice.toString(), "newStatus", "ACTIVE", "actor", "hr-2"))
        .hasSize(1);
    assertThat(adminAudit("from", Instant.now().plusSeconds(60).toString())).isEmpty();
    assertThat(adminAudit("from", Instant.now().minusSeconds(60).toString())).hasSize(3);
    // Non-SUPER_ADMIN callers stay forbidden (ADMIN.AUDIT.VIEW equivalent).
    mockMvc
        .perform(get(ADMIN + "/audit-logs").with(token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isForbidden());
  }

  private List<JsonNode> adminAudit(String... kv) throws Exception {
    MockHttpServletRequestBuilder request =
        get(ADMIN + "/audit-logs").with(token("root", "ROLE_SUPER_ADMIN"));
    for (int i = 0; i < kv.length; i += 2) {
      request.param(kv[i], kv[i + 1]);
    }
    JsonNode body =
        json(mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse());
    List<JsonNode> items = new ArrayList<>();
    body.get("items").forEach(items::add);
    return items;
  }

  @Test
  @DisplayName("[US-004-ac-5][US-004-AC5][FR-US-004-AC5-001] audit entries are append-only")
  void us004Ac5_entriesAreImmutable() throws Exception {
    mockMvc
        .perform(patchStatus(alice, "INACTIVE", token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk());
    assertThat(
            com.tms.crosscutting.audit.AuditLogEntry.class.isAnnotationPresent(
                org.hibernate.annotations.Immutable.class))
        .isTrue();
    // A second change appends; it never rewrites the first entry.
    mockMvc
        .perform(patchStatus(alice, "ACTIVE", token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk());
    assertThat(
            count(
                "select count(*) from tms.audit_log_entry where entry_type='STATUS_CHANGE'"
                    + " and resource_id = ?",
                alice))
        .isEqualTo(2);
  }

  // --- US-004-ac-6 (FR-US-004-AC6-001/002/003) -------------------------------------------------

  @Test
  @DisplayName("[US-004-ac-6][FR-US-004-AC6-001/002/003] non-admin gets 403 and is logged")
  void us004Ac6_deniedCallerLoggedAsUnauthorized() throws Exception {
    mockMvc
        .perform(patchStatus(alice, "INACTIVE", token("viewer-9", "ROLE_VIEWER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
        .andExpect(jsonPath("$.status").value(403));
    assertThat(statusOf(alice)).isEqualTo("ACTIVE");
    assertThat(row(alice).get("status_changed_by")).isNull();

    JsonNode logs =
        json(
            mockMvc
                .perform(
                    get(ADMIN + "/unauthorized-access-logs")
                        .with(token("root", "ROLE_SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse());
    assertThat(logs.get("totalElements").asLong()).isEqualTo(1);
    JsonNode entry = logs.get("items").get(0);
    assertThat(entry.get("entryType").asText()).isEqualTo("UNAUTHORIZED_ACCESS");
    assertThat(entry.get("actor").asText()).isEqualTo("viewer-9");
    assertThat(entry.get("action").asText()).contains("PATCH").contains("/status");
    assertThat(entry.get("resourceId").asText()).isEqualTo(alice.toString());
  }

  @Test
  @DisplayName("[US-004-ac-6][FR-US-004-AC6-001] 401 unauthenticated; admin views SUPER_ADMIN")
  void us004Ac6_unauthenticatedAndAdminViewsProtected() throws Exception {
    mockMvc
        .perform(
            patch(TRAINERS + "/" + alice + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get(ADMIN + "/audit-logs").with(token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get(ADMIN + "/unauthorized-access-logs").with(token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isForbidden());
  }

  // --- US-004-ac-7 (FR-US-004-AC7-001) ---------------------------------------------------------

  @Test
  @DisplayName("[US-004-ac-7][FR-US-004-AC7-001] endpoints stay consistent across the lifecycle")
  void us004Ac7_crossEndpointConsistency() throws Exception {
    long certsBefore = count("select count(*) from tms.trainer_certification");

    mockMvc
        .perform(patchStatus(alice, "INACTIVE", token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk());
    assertThat(listIds(Map.of())).containsExactly(bob);
    assertThatThrownBy(() -> trainerService.requireAllocatable(alice))
        .isInstanceOf(TrainerInactiveException.class);
    // Historical/profile data is preserved and the profile is still readable by id.
    assertThat(count("select count(*) from tms.trainer_certification")).isEqualTo(certsBefore);
    mockMvc
        .perform(get(TRAINERS + "/" + alice).with(token("root", "ROLE_SUPER_ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));

    mockMvc
        .perform(patchStatus(alice, "ACTIVE", token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk());
    assertThat(listIds(Map.of())).containsExactlyInAnyOrder(alice, bob);
    trainerService.requireAllocatable(alice);
    assertThat(statusChanges(json(mockMvc.perform(auditLogs(alice)).andReturn().getResponse())))
        .hasSize(2);
  }

  // --- helpers ---------------------------------------------------------------------------------

  private void deactivate(UUID id) throws Exception {
    mockMvc
        .perform(patchStatus(id, "INACTIVE", token("hr-1", "ROLE_HR_ADMIN")))
        .andExpect(status().isOk());
  }

  private MockHttpServletRequestBuilder patchStatus(
      UUID id, String newStatus, RequestPostProcessor auth) throws Exception {
    return patch(TRAINERS + "/" + id + "/status")
        .with(auth)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("status", newStatus)));
  }

  private MockHttpServletRequestBuilder auditLogs(UUID id) {
    return get(TRAINERS + "/" + id + "/audit-logs").with(token("root", "ROLE_SUPER_ADMIN"));
  }

  private List<JsonNode> statusChanges(JsonNode entries) {
    List<JsonNode> result = new ArrayList<>();
    entries.forEach(
        e -> {
          if ("STATUS_CHANGE".equals(e.get("entryType").asText())) {
            result.add(e);
          }
        });
    return result;
  }

  private List<UUID> listIds(Map<String, String> params) throws Exception {
    MockHttpServletRequestBuilder request =
        get(TRAINERS).with(token("root", "ROLE_SUPER_ADMIN")).param("size", "100");
    params.forEach(request::param);
    JsonNode body =
        json(mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse());
    List<UUID> ids = new ArrayList<>();
    body.get("items").forEach(i -> ids.add(UUID.fromString(i.get("id").asText())));
    return ids;
  }

  private JsonNode json(org.springframework.mock.web.MockHttpServletResponse response)
      throws Exception {
    return objectMapper.readTree(response.getContentAsString());
  }

  private Map<String, Object> row(UUID id) {
    return jdbc.queryForMap("select * from tms.trainer where id = ?", id);
  }

  private String statusOf(UUID id) {
    return jdbc.queryForObject("select status from tms.trainer where id = ?", String.class, id);
  }

  private long count(String sql, Object... args) {
    Long n = jdbc.queryForObject(sql, Long.class, args);
    return n == null ? 0 : n;
  }

  private UUID trainer(
      String employeeId,
      String name,
      String email,
      String status,
      String availability,
      UUID... certifications) {
    UUID id = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());
    jdbc.update(
        "insert into tms.trainer (id, employee_id, name, email, status, availability_state,"
            + " location_id, weekend_available, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        employeeId,
        name,
        email,
        status,
        availability,
        PUNE,
        true,
        now,
        now);
    for (UUID cert : certifications) {
      jdbc.update(
          "insert into tms.trainer_certification (trainer_id, certification_id) values (?, ?)",
          id,
          cert);
    }
    return id;
  }

  private static RequestPostProcessor token(String subject, String authority) {
    return jwt().jwt(j -> j.subject(subject)).authorities(new SimpleGrantedAuthority(authority));
  }
}
