package com.tms.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.trainer.application.TrainerService;
import com.tms.trainer.domain.Trainer;
import com.tms.trainer.domain.TrainerRepository;
import com.tms.trainer.domain.TrainerStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * US-003 acceptance tests for {@code POST /api/v1/trainers}. Test obligations:
 * 32e8e9f4-af14-4ee8-86ca-4175e165dea1:test:acceptance:US-003-ac-1..ac-6.
 *
 * <p>Status codes/codes follow the existing error envelope (400 VALIDATION_FAILED); 409
 * TRAINER_DUPLICATE and the profile field set are local choices (see GAP notes in the sources).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TrainerApiAcceptanceTest {

  private static final String TRAINERS = "/api/v1/trainers";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired TrainerRepository trainers;
  @Autowired TrainerService trainerService;

  @BeforeEach
  void clean() {
    jdbc.update("alter table tms.audit_log_entry disable trigger trg_audit_log_entry_immutable");
    jdbc.update("delete from tms.audit_log_entry");
    jdbc.update("alter table tms.audit_log_entry enable trigger trg_audit_log_entry_immutable");
    jdbc.update("delete from tms.trainer_certification");
    jdbc.update("delete from tms.trainer");
  }

  @Test
  @DisplayName("[US-003-ac-1] FR-US-003-001/002: invalid payloads return 400 with field errors, nothing persisted")
  void us003Ac1_invalidPayloadReturnsStructuredErrorAndPersistsNothing() throws Exception {
    // Internal trainer without employeeId, bad email, no certifications.
    create(Map.of("trainerType", "INTERNAL", "name", "A", "email", "nope", "location", "Kolkata",
            "weekendAvailable", true, "ratePerDay", 100))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.traceId").exists())
        .andExpect(jsonPath("$.details[?(@.field=='employeeId')]").exists())
        .andExpect(jsonPath("$.details[?(@.field=='email')]").exists())
        .andExpect(jsonPath("$.details[?(@.field=='certifications')]").exists());

    // External trainer requires MSA/NDA instead of employeeId.
    create(Map.of("trainerType", "EXTERNAL", "name", "B", "email", "b@x.com", "location", "Kolkata",
            "weekendAvailable", false, "certifications", List.of("AWS"), "ratePerDay", 100))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[?(@.field=='msaReference')]").exists())
        .andExpect(jsonPath("$.details[?(@.field=='ndaReference')]").exists())
        .andExpect(jsonPath("$.details[?(@.field=='employeeId')]").doesNotExist());

    create(Map.of()).andExpect(status().isBadRequest());
    mockMvc
        .perform(post(TRAINERS).with(jwt()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

    assertThat(trainers.count()).isZero();
    assertThat(auditCount()).isZero();
  }

  @Test
  @DisplayName("[US-003-ac-1] valid internal and external payloads are created (201)")
  void us003Ac1_validPayloadsCreated() throws Exception {
    create(internal("e1@x.com", "EMP-1", "Kolkata")).andExpect(status().isCreated());
    create(external("e2@x.com", "Kolkata")).andExpect(status().isCreated());
    assertThat(trainers.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("[US-003-ac-2] FR-US-003-003/004: duplicate Employee ID or Email returns 409 duplicate")
  void us003Ac2_duplicatesRejected() throws Exception {
    create(internal("dup@x.com", "EMP-9", "Kolkata")).andExpect(status().isCreated());

    create(internal("other@x.com", "EMP-9", "Kolkata"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TRAINER_DUPLICATE"))
        .andExpect(jsonPath("$.details[0].field").value("employeeId"));
    // Email is unique for every trainer type and case-insensitively.
    create(external("DUP@x.com", "Kolkata"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TRAINER_DUPLICATE"))
        .andExpect(jsonPath("$.details[0].field").value("email"));

    // Inactive records still count.
    jdbc.update("update tms.trainer set status = 'INACTIVE'");
    create(internal("dup@x.com", "EMP-10", "Kolkata")).andExpect(status().isConflict());
    create(internal("fresh@x.com", "EMP-9", "Kolkata")).andExpect(status().isConflict());

    assertThat(trainers.count()).isEqualTo(1);
    assertThat(auditCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("[US-003-ac-3] FR-US-003-005/006: time zone derived from a resolvable location")
  void us003Ac3_timeZoneDerivedFromLocation() throws Exception {
    create(internal("tz@x.com", "EMP-2", "Kolkata")).andExpect(status().isCreated())
        .andExpect(jsonPath("$.timeZone").value("Asia/Kolkata"));
    assertThat(jdbc.queryForObject("select time_zone from tms.trainer", String.class))
        .isEqualTo("Asia/Kolkata");

    // FR-US-003-006 gap: unresolvable location -> no time zone is guessed.
    create(internal("tz2@x.com", "EMP-3", "Nowhereville")).andExpect(status().isCreated())
        .andExpect(jsonPath("$.timeZone").doesNotExist());
  }

  @Test
  @DisplayName("[US-003-ac-4] FR-US-003-007/008: new trainer is Active regardless of client status and is searchable")
  void us003Ac4_defaultsToActiveAndIsSearchable() throws Exception {
    Map<String, Object> body = new java.util.HashMap<>(internal("act@x.com", "EMP-4", "Kolkata"));
    body.put("status", "INACTIVE");
    create(body).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"));

    assertThat(trainers.findAll()).extracting(Trainer::getStatus).containsOnly(TrainerStatus.ACTIVE);
    assertThat(trainerService.searchTrainers()).extracting(Trainer::getEmail).contains("act@x.com");
  }

  @Test
  @DisplayName("[US-003-ac-5] FR-US-003-009/010: successful create appends one immutable audit entry")
  void us003Ac5_auditEntryAppended() throws Exception {
    mockMvc
        .perform(
            post(TRAINERS)
                .with(jwt().jwt(j -> j.subject("hr-user-1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(internal("aud@x.com", "EMP-5", "Kolkata"))))
        .andExpect(status().isCreated());

    Map<String, Object> row = jdbc.queryForMap("select * from tms.audit_log_entry");
    String trainerId = jdbc.queryForObject("select id::text from tms.trainer", String.class);
    assertThat(row.get("actor")).isEqualTo("hr-user-1");
    assertThat(row.get("action")).isEqualTo("TRAINER_CREATED");
    assertThat(row.get("entity_id")).isEqualTo(trainerId);
    assertThat(row.get("occurred_at")).isNotNull();
    String details = row.get("details").toString();
    assertThat(details).contains("INTERNAL", "Asia/Kolkata");
    assertThat(details).doesNotContain("aud@x.com", "EMP-5", "Test Trainer");

    assertThatThrownBy(() -> jdbc.update("update tms.audit_log_entry set actor = 'x'"))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(() -> jdbc.update("delete from tms.audit_log_entry"))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  @DisplayName("[US-003-ac-6] FR-US-003-011/012: failed creates yield consistent errors and no trainer/audit rows")
  void us003Ac6_failuresLeaveNoTrainerOrAudit() throws Exception {
    create(Map.of("trainerType", "BOGUS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.path").value(TRAINERS))
        .andExpect(jsonPath("$.message").isNotEmpty());
    assertThat(trainers.count()).isZero();
    assertThat(auditCount()).isZero();

    create(internal("f@x.com", "EMP-6", "Kolkata")).andExpect(status().isCreated());
    create(internal("f@x.com", "EMP-7", "Kolkata"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(jsonPath("$.path").value(TRAINERS))
        .andExpect(jsonPath("$.traceId").exists());

    // Only the one successful create left a trainer and an audit entry.
    assertThat(trainers.count()).isEqualTo(1);
    assertThat(auditCount()).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from tms.trainer_certification", Integer.class))
        .isEqualTo(1);
  }

  private long auditCount() {
    return jdbc.queryForObject("select count(*) from tms.audit_log_entry", Long.class);
  }

  private static Map<String, Object> internal(String email, String employeeId, String location) {
    return Map.of(
        "trainerType", "INTERNAL",
        "name", "Test Trainer",
        "email", email,
        "employeeId", employeeId,
        "location", location,
        "weekendAvailable", true,
        "certifications", List.of("AWS-SAA"),
        "ratePerDay", 150.5);
  }

  private static Map<String, Object> external(String email, String location) {
    return Map.of(
        "trainerType", "EXTERNAL",
        "name", "Ext Trainer",
        "email", email,
        "location", location,
        "weekendAvailable", false,
        "certifications", List.of("CKA"),
        "ratePerDay", 200,
        "msaReference", "MSA-1",
        "ndaReference", "NDA-1");
  }

  private ResultActions create(Map<String, Object> body) throws Exception {
    return mockMvc.perform(
        post(TRAINERS).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(json(body)));
  }

  private static String json(Map<String, Object> body) throws Exception {
    return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
  }
}
