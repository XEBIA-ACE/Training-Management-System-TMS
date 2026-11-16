package com.tms.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * US-003 acceptance tests for trainer creation ({@code POST /api/v1/trainers}),
 * FR-US-003-001..FR-US-003-007, tasks T008..T014, acceptance criteria US-003-ac-1..US-003-ac-7.
 */
@SpringBootTest(
    properties =
        "tms.trainer.management.location-time-zones=Pune=Asia/Kolkata,London=Europe/London")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class TrainerCreateAcceptanceTest {

  private static final String TRAINERS_V1 = "/api/v1/trainers";
  private static final String ACTOR = "trainer-admin-7";
  private static final String INACTIVE_EMPLOYEE_ID = "E900";
  private static final String INACTIVE_EMAIL = "inactive.trainer@corp.com";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void seed() {
    jdbc.update("delete from tms.audit_log_entry");
    jdbc.update("delete from tms.trainer_certification");
    jdbc.update("delete from tms.trainer");
    Timestamp now = Timestamp.from(Instant.now());
    jdbc.update(
        "insert into tms.trainer (id, employee_id, name, email, status, trainer_type, location,"
            + " weekend_available, created_at, updated_at) values (?, ?, ?, ?, 'INACTIVE',"
            + " 'INTERNAL', 'Pune', false, ?, ?)",
        UUID.randomUUID(),
        INACTIVE_EMPLOYEE_ID,
        "Ivan Inactive",
        INACTIVE_EMAIL,
        now,
        now);
  }

  // --- US-003-ac-1 / FR-US-003-001 / T008 ------------------------------------------------------

  @Test
  @DisplayName(
      "[US-003-ac-1][FR-US-003-001][T008] INTERNAL with missing/malformed fields -> 400 with"
          + " field-level errors, nothing persisted")
  void internalRequiresEmployeeIdEmailNameLocation() throws Exception {
    Map<String, Object> body = internal("ignored", "ignored@corp.com");
    body.remove("employeeId");
    body.put("email", "not-an-email");
    body.put("name", " ");
    body.remove("location");

    JsonNode error = json(create(body, manager()).andExpect(status().isBadRequest()));

    assertThat(error.path("code").asText()).isEqualTo("VALIDATION_FAILED");
    assertThat(fields(error)).containsExactly("email", "employeeId", "location", "name");
    assertThat(trainerCount()).isEqualTo(1);
    assertThat(auditCount("CREATE")).isZero();
  }

  @Test
  @DisplayName(
      "[US-003-ac-1][FR-US-003-001][T008] INTERNAL with a blank employeeId -> 400 on employeeId")
  void internalRejectsBlankEmployeeId() throws Exception {
    Map<String, Object> body = internal("  ", "blank.employee@corp.com");

    JsonNode error = json(create(body, manager()).andExpect(status().isBadRequest()));

    assertThat(fields(error)).containsExactly("employeeId");
    assertThat(trainerCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("[US-003-ac-1][FR-US-003-001][T008] valid INTERNAL request -> 201 Created")
  void internalValidIsCreated() throws Exception {
    JsonNode created =
        json(
            create(internal("E100", "alice@corp.com"), manager())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location")));

    assertThat(created.path("trainerType").asText()).isEqualTo("INTERNAL");
    assertThat(created.path("employeeId").asText()).isEqualTo("E100");
    assertThat(created.path("email").asText()).isEqualTo("alice@corp.com");
    assertThat(trainerCount()).isEqualTo(2);
  }

  // --- US-003-ac-2 / FR-US-003-002 / T009 ------------------------------------------------------

  @Test
  @DisplayName(
      "[US-003-ac-2][FR-US-003-002][T009] EXTERNAL with missing name/email/location -> 400 with"
          + " field-level errors, nothing persisted")
  void externalRequiresNameEmailLocation() throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("trainerType", "EXTERNAL");

    JsonNode error = json(create(body, manager()).andExpect(status().isBadRequest()));

    assertThat(fields(error)).containsExactly("email", "location", "name");
    assertThat(trainerCount()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "[US-003-ac-2][FR-US-003-002][T009] EXTERNAL with an invalid msaNdaStatus enum -> 400")
  void externalRejectsInvalidMsaNdaStatus() throws Exception {
    Map<String, Object> body = external("ext@vendor.com");
    body.put("msaNdaStatus", "MAYBE");

    JsonNode error = json(create(body, manager()).andExpect(status().isBadRequest()));

    assertThat(fields(error)).containsExactly("msaNdaStatus");
    assertThat(trainerCount()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "[US-003-ac-2][FR-US-003-002][T009] unknown trainerType or malformed field type -> 400")
  void rejectsUnknownTrainerTypeAndMalformedValues() throws Exception {
    Map<String, Object> unknownType = external("ext@vendor.com");
    unknownType.put("trainerType", "FREELANCE");
    assertThat(fields(json(create(unknownType, manager()).andExpect(status().isBadRequest()))))
        .containsExactly("trainerType");

    Map<String, Object> badUuid = external("ext@vendor.com");
    badUuid.put("certificationIds", List.of("not-a-uuid"));
    assertThat(fields(json(create(badUuid, manager()).andExpect(status().isBadRequest()))))
        .containsExactly("certificationIds[0]");

    assertThat(trainerCount()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "[US-003-ac-2][FR-US-003-002][T009] valid EXTERNAL without employeeId -> 201 with"
          + " msaNdaStatus")
  void externalValidIsCreated() throws Exception {
    Map<String, Object> body = external("ext@vendor.com");
    body.put("msaNdaStatus", "SIGNED");

    JsonNode created = json(create(body, manager()).andExpect(status().isCreated()));

    assertThat(created.path("trainerType").asText()).isEqualTo("EXTERNAL");
    assertThat(created.path("employeeId").isNull()).isTrue();
    assertThat(created.path("msaNdaStatus").asText()).isEqualTo("SIGNED");
    assertThat(trainerCount()).isEqualTo(2);
  }

  // --- US-003-ac-3 / FR-US-003-003 / T010 ------------------------------------------------------

  @Test
  @DisplayName(
      "[US-003-ac-3][FR-US-003-003][T010] INTERNAL employeeId of an inactive trainer -> 409"
          + " TRAINER_DUPLICATE_EMPLOYEE_ID, nothing persisted")
  void duplicateEmployeeIdOfInactiveTrainer() throws Exception {
    JsonNode error =
        json(
            create(internal(INACTIVE_EMPLOYEE_ID, "fresh@corp.com"), manager())
                .andExpect(status().isConflict()));

    assertThat(error.path("code").asText()).isEqualTo("TRAINER_DUPLICATE_EMPLOYEE_ID");
    assertThat(fields(error)).containsExactly("employeeId");
    assertThat(trainerCount()).isEqualTo(1);
    assertThat(auditCount("CREATE")).isZero();
  }

  @Test
  @DisplayName(
      "[US-003-ac-3][FR-US-003-003][T010] email of an inactive trainer (any case, any type) ->"
          + " 409 TRAINER_DUPLICATE_EMAIL, nothing persisted")
  void duplicateEmailOfInactiveTrainer() throws Exception {
    JsonNode external =
        json(
            create(external(INACTIVE_EMAIL.toUpperCase()), manager())
                .andExpect(status().isConflict()));
    JsonNode internal =
        json(create(internal("E901", INACTIVE_EMAIL), manager()).andExpect(status().isConflict()));

    assertThat(external.path("code").asText()).isEqualTo("TRAINER_DUPLICATE_EMAIL");
    assertThat(internal.path("code").asText()).isEqualTo("TRAINER_DUPLICATE_EMAIL");
    assertThat(fields(internal)).containsExactly("email");
    assertThat(trainerCount()).isEqualTo(1);
    assertThat(auditCount("CREATE")).isZero();
  }

  @Test
  @DisplayName(
      "[US-003-ac-3][FR-US-003-003][T010] duplicate of an active trainer created via the API ->"
          + " 409")
  void duplicateOfActiveTrainer() throws Exception {
    create(internal("E300", "carol@corp.com"), manager()).andExpect(status().isCreated());

    json(create(internal("E300", "other@corp.com"), manager()).andExpect(status().isConflict()));
    json(create(internal("E301", "carol@corp.com"), manager()).andExpect(status().isConflict()));

    assertThat(trainerCount()).isEqualTo(2);
    assertThat(auditCount("CREATE")).isEqualTo(1);
  }

  // --- US-003-ac-4 / FR-US-003-004 / T011 ------------------------------------------------------

  @Test
  @DisplayName(
      "[US-003-ac-4][FR-US-003-004][T011] recognized location -> derived timeZone persisted and"
          + " returned")
  void derivesTimeZoneFromLocation() throws Exception {
    Map<String, Object> body = internal("E400", "dan@corp.com");
    body.put("location", "  pune ");

    JsonNode created = json(create(body, manager()).andExpect(status().isCreated()));

    assertThat(created.path("location").asText()).isEqualTo("pune");
    assertThat(created.path("timeZone").asText()).isEqualTo("Asia/Kolkata");
    assertThat(column(created, "time_zone")).isEqualTo("Asia/Kolkata");

    JsonNode london =
        json(create(external("eve@vendor.com"), manager()).andExpect(status().isCreated()));
    assertThat(london.path("timeZone").asText()).isEqualTo("Europe/London");
  }

  @Test
  @DisplayName(
      "[US-003-ac-4][FR-US-003-004][T011] unmapped location -> created without a timeZone"
          + " (documented gap)")
  void unmappedLocationHasNoTimeZone() throws Exception {
    Map<String, Object> body = internal("E401", "frank@corp.com");
    body.put("location", "Atlantis");

    JsonNode created = json(create(body, manager()).andExpect(status().isCreated()));

    assertThat(created.path("timeZone").isNull()).isTrue();
    assertThat(column(created, "time_zone")).isNull();
  }

  // --- US-003-ac-5 / FR-US-003-005 / T012 ------------------------------------------------------

  @Test
  @DisplayName(
      "[US-003-ac-5][FR-US-003-005][T012] new trainer is ACTIVE and returned by GET"
          + " ?status=ACTIVE")
  void createdTrainerIsActiveAndListed() throws Exception {
    JsonNode created =
        json(create(internal("E500", "grace@corp.com"), manager()).andExpect(status().isCreated()));
    String id = created.path("id").asText();

    assertThat(created.path("status").asText()).isEqualTo("ACTIVE");
    assertThat(column(created, "status")).isEqualTo("ACTIVE");

    JsonNode active =
        json(
            mockMvc
                .perform(get(TRAINERS_V1).param("status", "ACTIVE").with(manager()))
                .andExpect(status().isOk()));
    assertThat(ids(active)).containsExactly(id);
    assertThat(active.path("items").get(0)).isEqualTo(created);

    JsonNode inactive =
        json(
            mockMvc
                .perform(get(TRAINERS_V1).param("status", "INACTIVE").with(manager()))
                .andExpect(status().isOk()));
    assertThat(ids(inactive)).doesNotContain(id);

    json(
        mockMvc
            .perform(get(TRAINERS_V1 + "/" + id).with(manager()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE")));
  }

  @Test
  @DisplayName("[US-003-ac-5][FR-US-003-005][T012] the legacy /api/trainers path stays read-only")
  void legacyPathDoesNotCreate() throws Exception {
    mockMvc
        .perform(
            post("/api/trainers")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(internal("E501", "legacy@corp.com"))))
        .andExpect(status().isMethodNotAllowed());

    assertThat(trainerCount()).isEqualTo(1);
  }

  // --- US-003-ac-6 / FR-US-003-006 / T013 ------------------------------------------------------

  @Test
  @DisplayName(
      "[US-003-ac-6][FR-US-003-006][T013] successful create writes one CREATE audit entry with"
          + " actor, timestamp and non-sensitive fields only")
  void auditsCreation() throws Exception {
    Instant before = Instant.now();
    Map<String, Object> body = internal("E600", "heidi@corp.com");
    body.put("msaNdaStatus", "SIGNED");
    body.put("msaNdaReference", "https://docs.example.com/msa/heidi.pdf");
    JsonNode created = json(create(body, manager()).andExpect(status().isCreated()));

    List<Map<String, Object>> entries =
        jdbc.queryForList(
            "select actor, action, resource_type, resource_id, occurred_at, details::text as"
                + " details from tms.audit_log_entry where entry_type = 'CREATE'");
    assertThat(entries).hasSize(1);
    Map<String, Object> entry = entries.get(0);
    assertThat(entry.get("actor")).isEqualTo(ACTOR);
    assertThat(entry.get("action")).isEqualTo("POST /api/v1/trainers");
    assertThat(entry.get("resource_type")).isEqualTo("Trainer");
    assertThat(entry.get("resource_id")).isEqualTo(created.path("id").asText());
    assertThat(((Timestamp) entry.get("occurred_at")).toInstant())
        .isAfterOrEqualTo(before.minusSeconds(1));

    JsonNode details = objectMapper.readTree((String) entry.get("details"));
    assertThat(details.path("module").asText()).isEqualTo("trainer");
    assertThat(details.path("recordId").asText()).isEqualTo(created.path("id").asText());
    assertThat(details.path("name").asText()).isEqualTo("Heidi Example");
    assertThat(details.path("email").asText()).isEqualTo("heidi@corp.com");
    assertThat(details.path("employeeId").asText()).isEqualTo("E600");
    assertThat(details.path("trainerType").asText()).isEqualTo("INTERNAL");
    assertThat(details.path("location").asText()).isEqualTo("Pune");
    assertThat(details.has("msaNdaReference")).isFalse();
    assertThat((String) entry.get("details")).doesNotContain("heidi.pdf");
  }

  // --- US-003-ac-7 / FR-US-003-007 / T014 ------------------------------------------------------

  @Test
  @DisplayName(
      "[US-003-ac-7][FR-US-003-007][T014] unauthenticated create -> 401, nothing persisted,"
          + " security event logged")
  void unauthenticatedIsRejected(CapturedOutput output) throws Exception {
    JsonNode error =
        json(
            mockMvc
                .perform(
                    post(TRAINERS_V1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(internal("E700", "ivy@corp.com"))))
                .andExpect(status().isUnauthorized()));

    assertThat(error.path("code").asText()).isEqualTo("AUTHENTICATION_FAILED");
    assertThat(trainerCount()).isEqualTo(1);
    assertThat(auditCount("CREATE")).isZero();
    assertThat(output.getOut())
        .contains("Security event: request rejected code=AUTHENTICATION_FAILED")
        .contains("path=/api/v1/trainers");
  }

  @Test
  @DisplayName(
      "[US-003-ac-7][FR-US-003-007][T014] authenticated without TRAINER.MANAGE -> 403 before"
          + " validation, nothing persisted, UnauthorizedAccessEntry written")
  void unauthorizedIsRejectedAndAudited(CapturedOutput output) throws Exception {
    JsonNode valid =
        json(
            create(internal("E701", "judy@corp.com"), token("viewer-1", "ROLE_OPS_MONITOR"))
                .andExpect(status().isForbidden()));
    Map<String, Object> invalid = new LinkedHashMap<>();
    invalid.put("trainerType", "INTERNAL");
    json(create(invalid, token("viewer-1", "ROLE_OPS_MONITOR")).andExpect(status().isForbidden()));

    assertThat(valid.path("code").asText()).isEqualTo("ACCESS_DENIED");
    assertThat(trainerCount()).isEqualTo(1);
    assertThat(auditCount("CREATE")).isZero();
    List<Map<String, Object>> denied =
        jdbc.queryForList(
            "select actor, action, details::text as details from tms.audit_log_entry where"
                + " entry_type = 'UNAUTHORIZED_ACCESS'");
    assertThat(denied).hasSize(2);
    assertThat(denied)
        .allSatisfy(
            row -> {
              assertThat(row.get("actor")).isEqualTo("viewer-1");
              assertThat(row.get("action")).isEqualTo("POST /api/v1/trainers");
              assertThat((String) row.get("details"))
                  .contains("MISSING_TRAINER_MANAGE_PERMISSION")
                  .doesNotContain("judy@corp.com");
            });
    assertThat(output.getOut()).contains("Trainer management access denied");
  }

  // --- helpers ---------------------------------------------------------------------------------

  private static Map<String, Object> internal(String employeeId, String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("trainerType", "INTERNAL");
    body.put("employeeId", employeeId);
    body.put("name", "Heidi Example");
    body.put("email", email);
    body.put("location", "Pune");
    body.put("weekendAvailable", true);
    return body;
  }

  private static Map<String, Object> external(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("trainerType", "EXTERNAL");
    body.put("name", "Eve Vendor");
    body.put("email", email);
    body.put("location", "London");
    return body;
  }

  private ResultActions create(Map<String, Object> body, RequestPostProcessor auth)
      throws Exception {
    return mockMvc.perform(
        post(TRAINERS_V1)
            .with(auth)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
  }

  private JsonNode json(ResultActions actions) throws Exception {
    MvcResult result = actions.andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private static List<String> fields(JsonNode error) {
    List<String> fields = new ArrayList<>();
    error.path("details").forEach(d -> fields.add(d.path("field").asText()));
    return fields;
  }

  private static List<String> ids(JsonNode page) {
    List<String> ids = new ArrayList<>();
    page.path("items").forEach(i -> ids.add(i.path("id").asText()));
    return ids;
  }

  private String column(JsonNode created, String column) {
    return jdbc.queryForObject(
        "select " + column + " from tms.trainer where id = ?",
        String.class,
        UUID.fromString(created.path("id").asText()));
  }

  private int trainerCount() {
    return jdbc.queryForObject("select count(*) from tms.trainer", Integer.class);
  }

  private int auditCount(String type) {
    return jdbc.queryForObject(
        "select count(*) from tms.audit_log_entry where entry_type = ?", Integer.class, type);
  }

  private static RequestPostProcessor manager() {
    return token(ACTOR, "ROLE_SUPER_ADMIN");
  }

  private static RequestPostProcessor token(String subject, String authority) {
    return jwt().jwt(j -> j.subject(subject)).authorities(new SimpleGrantedAuthority(authority));
  }
}
