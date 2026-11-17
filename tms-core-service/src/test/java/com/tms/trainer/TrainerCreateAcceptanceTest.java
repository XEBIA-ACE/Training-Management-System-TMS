package com.tms.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * US-003 acceptance tests for trainer creation ({@code POST /api/v1/trainers}), update ({@code PUT
 * /api/v1/trainers/{id}}) and the trainer audit trail. Test obligations:
 * 0363282d-7150-41e1-9994-7cfcdf2fd621:test:acceptance:US-003-ac-1..ac-6; verify tasks T007..T012.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TrainerCreateAcceptanceTest {

  private static final String TRAINERS = "/api/v1/trainers";
  private static final String HR = "hr-admin-1";

  private static final UUID PUNE = UUID.randomUUID();
  private static final UUID LONDON = UUID.randomUUID();
  private static final UUID NOWHERE = UUID.randomUUID();

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void reset() {
    jdbc.update("delete from tms.audit_log_entry");
    jdbc.update("delete from tms.trainer_certification");
    jdbc.update("delete from tms.trainer");
    jdbc.update("delete from tms.location");
    jdbc.update("insert into tms.location (id, timezone) values (?, ?)", PUNE, "Asia/Kolkata");
    jdbc.update("insert into tms.location (id, timezone) values (?, ?)", LONDON, "Europe/London");
  }

  // --- US-003-ac-1 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-1][FR-US-003-001][T007] INTERNAL without employeeId is a 400 with details")
  void us003Ac1_internalRequiresEmployeeId() throws Exception {
    Map<String, Object> req = validInternal("E1", "asha@corp.com");
    req.remove("employeeId");
    assertThat(fields(createExpect400(hr(), req))).containsExactly("employeeId");
  }

  @Test
  @DisplayName("[US-003-ac-1][FR-US-003-001][T007] EXTERNAL does not need employeeId")
  void us003Ac1_externalDoesNotRequireEmployeeId() throws Exception {
    mockMvc
        .perform(json(post(TRAINERS), validExternal("ext@vendor.com", PUNE), hr()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("EXTERNAL"));
  }

  @Test
  @DisplayName("[US-003-ac-1][FR-US-003-001][T007] email, name, location and type are always required")
  void us003Ac1_alwaysRequiredFields() throws Exception {
    JsonNode body = createExpect400(hr(), Map.of());
    assertThat(fields(body)).contains("type", "name", "email", "locationId");
    // location is required for INTERNAL as well as EXTERNAL
    Map<String, Object> internal = validInternal("E1", "a@corp.com");
    internal.remove("locationId");
    assertThat(fields(createExpect400(hr(), internal))).containsExactly("locationId");
    Map<String, Object> external = validExternal("e@vendor.com", PUNE);
    external.remove("locationId");
    assertThat(fields(createExpect400(hr(), external))).containsExactly("locationId");
  }

  @Test
  @DisplayName("[US-003-ac-1][FR-US-003-001][T007] malformed values are listed per field; nothing persisted")
  void us003Ac1_formatErrorsListedPerField() throws Exception {
    JsonNode body =
        createExpect400(
            hr(),
            Map.of(
                "type", "OTHER",
                "name", " ",
                "email", "nope",
                "locationId", "not-a-uuid"));
    assertThat(fields(body)).contains("type", "name", "email", "locationId");
    assertThat(count("select count(*) from tms.trainer")).isZero();
  }

  @Test
  @DisplayName("[US-003-ac-1][FR-US-003-001][T007] only HR admin / super admin may create (403)")
  void us003Ac1_rbac() throws Exception {
    mockMvc
        .perform(json(post(TRAINERS), validInternal("E1", "a@corp.com"), token("u", "ROLE_VIEWER")))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(json(post(TRAINERS), validInternal("E1", "a@corp.com"), token("u", "ROLE_HR_ADMIN")))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            json(post(TRAINERS), validInternal("E2", "b@corp.com"), token("u", "ROLE_SUPER_ADMIN")))
        .andExpect(status().isCreated());
    assertThat(count("select count(*) from tms.audit_log_entry where entry_type='UNAUTHORIZED_ACCESS'"))
        .isEqualTo(1);
  }

  // --- US-003-ac-2 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-2][FR-US-003-002][T008] duplicate employeeId is rejected for any status")
  void us003Ac2_duplicateEmployeeId() throws Exception {
    UUID id = createOk(validInternal("E1", "first@corp.com"));
    jdbc.update("update tms.trainer set status='INACTIVE' where id=?", id);
    JsonNode body = createExpect400(hr(), validInternal("E1", "second@corp.com"));
    assertThat(fields(body)).containsExactly("employeeId");
    assertThat(count("select count(*) from tms.trainer")).isEqualTo(1);
  }

  @Test
  @DisplayName("[US-003-ac-2][FR-US-003-002][T008] duplicate email is rejected for any type/status")
  void us003Ac2_duplicateEmail() throws Exception {
    UUID id = createOk(validInternal("E1", "same@corp.com"));
    assertThat(fields(createExpect400(hr(), validInternal("E2", "SAME@corp.com"))))
        .containsExactly("email");
    jdbc.update("update tms.trainer set status='INACTIVE' where id=?", id);
    assertThat(fields(createExpect400(hr(), validExternal("same@corp.com", PUNE))))
        .containsExactly("email");
  }

  @Test
  @DisplayName("[US-003-ac-2][FR-US-003-002][T008] both conflicting fields are reported together")
  void us003Ac2_bothFieldsReported() throws Exception {
    createOk(validInternal("E1", "same@corp.com"));
    assertThat(fields(createExpect400(hr(), validInternal("E1", "same@corp.com"))))
        .containsExactlyInAnyOrder("employeeId", "email");
  }

  // --- US-003-ac-3 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-3][FR-US-003-003][T009] new trainer is ACTIVE and listed with status=ACTIVE")
  void us003Ac3_activeByDefaultAndListed() throws Exception {
    UUID id = createOk(validInternal("E1", "a@corp.com"));
    assertThat(jdbc.queryForObject("select status from tms.trainer where id=?", String.class, id))
        .isEqualTo("ACTIVE");
    mockMvc
        .perform(authorizedGet(TRAINERS).param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(id.toString()))
        .andExpect(jsonPath("$.items[0].status").value("ACTIVE"));
  }

  // --- US-003-ac-4 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-4][FR-US-003-004][T010] create derives and persists timezone")
  void us003Ac4_createDerivesTimezone() throws Exception {
    UUID id = createOk(validInternal("E1", "a@corp.com"));
    assertThat(jdbc.queryForObject("select timezone from tms.trainer where id=?", String.class, id))
        .isEqualTo("Asia/Kolkata");
  }

  @Test
  @DisplayName("[US-003-ac-4][FR-US-003-004][T010] PUT with a new locationId recalculates timezone")
  void us003Ac4_updateRecalculatesTimezone() throws Exception {
    UUID id = createOk(validInternal("E1", "a@corp.com"));
    mockMvc
        .perform(json(put(TRAINERS + "/" + id), Map.of("locationId", LONDON.toString()), hr()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timezone").value("Europe/London"));
    assertThat(jdbc.queryForObject("select timezone from tms.trainer where id=?", String.class, id))
        .isEqualTo("Europe/London");
  }

  @Test
  @DisplayName("[US-003-ac-4][FR-US-003-004][T010] unknown location leaves timezone unset (GAP)")
  void us003Ac4_unknownLocationHasNoTimezone() throws Exception {
    Map<String, Object> req = validInternal("E1", "a@corp.com");
    req.put("locationId", NOWHERE.toString());
    UUID id = createOk(req);
    assertThat(jdbc.queryForObject("select timezone from tms.trainer where id=?", String.class, id))
        .isNull();
  }

  // --- US-003-ac-5 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-5][FR-US-003-005][T011] creation appends an audit event with profile data")
  void us003Ac5_successAudited() throws Exception {
    UUID id = createOk(validInternal("E1", "a@corp.com"));
    JsonNode logs = auditLogs(id);
    assertThat(logs).hasSize(1);
    JsonNode e = logs.get(0);
    assertThat(e.get("actor").asText()).isEqualTo(HR);
    assertThat(e.get("occurredAt").asText()).isNotBlank();
    assertThat(e.get("resourceId").asText()).isEqualTo(id.toString());
    assertThat(e.get("details").get("outcome").asText()).isEqualTo("SUCCESS");
    assertThat(e.get("details").get("trainerId").asText()).isEqualTo(id.toString());
    assertThat(e.get("details").get("type").asText()).isEqualTo("INTERNAL");
    assertThat(e.get("details").get("employeeId").asText()).isEqualTo("E1");
    assertThat(e.get("details").get("email").asText()).isEqualTo("a@corp.com");
    assertThat(e.get("details").get("locationId").asText()).isEqualTo(PUNE.toString());
    assertThat(e.get("details").get("status").asText()).isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("[US-003-ac-5][FR-US-003-005][T011] audit payload carries no document content")
  void us003Ac5_noDocumentContentInAudit() throws Exception {
    Map<String, Object> req = validInternal("E1", "a@corp.com");
    req.put("documents", "SECRET-DOCUMENT-BYTES");
    UUID id = createOk(req);
    assertThat(auditLogs(id).toString()).doesNotContain("SECRET-DOCUMENT-BYTES");
    assertThat(
            count(
                "select count(*) from tms.audit_log_entry where details::text like '%SECRET-DOC%'"))
        .isZero();
  }

  @Test
  @DisplayName("[US-003-ac-5][FR-US-003-005][T011] audit entries are immutable (entity is @Immutable)")
  void us003Ac5_entriesAreImmutable() throws Exception {
    UUID id = createOk(validInternal("E1", "a@corp.com"));
    String before = auditLogs(id).toString();
    mockMvc
        .perform(json(put(TRAINERS + "/" + id), Map.of("name", "Renamed"), hr()))
        .andExpect(status().isOk());
    JsonNode after = auditLogs(id);
    assertThat(after).hasSize(2);
    assertThat(after.get(0).toString()).isEqualTo(objectMapper.readTree(before).get(0).toString());
  }

  // --- US-003-ac-6 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-6][FR-US-003-006][T012] validation failure uses ErrorResponse with per-field details")
  void us003Ac6_validationErrorResponseShape() throws Exception {
    Map<String, Object> req = validInternal("E1", "bad");
    req.remove("name");
    JsonNode body = createExpect400(hr(), req);
    // GAP(FR-US-003-006): the shared code is VALIDATION_FAILED; the spec says "such as
    // VALIDATION_ERROR". Kept aligned with the existing Core Service ErrorResponse convention.
    assertThat(body.get("code").asText()).isEqualTo("VALIDATION_FAILED");
    assertThat(body.get("status").asInt()).isEqualTo(400);
    assertThat(fields(body)).containsExactlyInAnyOrder("name", "email");
    body.get("details").forEach(d -> assertThat(d.get("message").asText()).isNotBlank());
  }

  @Test
  @DisplayName("[US-003-ac-6][FR-US-003-006][T012] duplicate failure uses ErrorResponse naming the field")
  void us003Ac6_duplicateErrorResponseShape() throws Exception {
    createOk(validInternal("E1", "a@corp.com"));
    JsonNode body = createExpect400(hr(), validInternal("E1", "a@corp.com"));
    assertThat(body.get("code").asText()).isEqualTo("VALIDATION_FAILED");
    assertThat(fields(body)).containsExactlyInAnyOrder("employeeId", "email");
  }

  @Test
  @DisplayName("[US-003-ac-6][FR-US-003-006][T012] validation failure is audited without submitted values")
  void us003Ac6_validationFailureAudited() throws Exception {
    Map<String, Object> req = validInternal("E1", "asha@corp.com");
    req.remove("employeeId");
    createExpect400(hr(), req);
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select actor, details::text as details from tms.audit_log_entry"
                + " where entry_type='CREATE'");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("actor")).isEqualTo(HR);
    assertThat((String) rows.get(0).get("details"))
        .contains("REJECTED")
        .contains("employeeId")
        .doesNotContain("asha@corp.com");
  }

  @Test
  @DisplayName("[US-003-ac-6][FR-US-003-006][T012] duplicate rejection is visible via audit-logs")
  void us003Ac6_duplicateRejectionVisibleViaEndpoint() throws Exception {
    UUID existing = createOk(validInternal("E1", "a@corp.com"));
    createExpect400(hr(), validInternal("E1", "other@corp.com"));
    JsonNode logs = auditLogs(existing);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(1).get("details").get("outcome").asText()).isEqualTo("REJECTED");
    assertThat(logs.get(1).get("details").get("fields").toString()).contains("employeeId");
    assertThat(logs.get(1).get("details").has("email")).isFalse();
  }

  // --- helpers -----------------------------------------------------------------------------------

  private UUID createOk(Map<String, Object> body) throws Exception {
    MvcResult result =
        mockMvc
            .perform(json(post(TRAINERS), body, hr()))
            .andExpect(status().isCreated())
            .andReturn();
    return UUID.fromString(
        objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
  }

  private JsonNode createExpect400(RequestPostProcessor auth, Map<String, Object> body)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(json(post(TRAINERS), body, auth))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details").isArray())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode auditLogs(UUID id) throws Exception {
    MvcResult result =
        mockMvc
            .perform(authorizedGet(TRAINERS + "/" + id + "/audit-logs"))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private static List<String> fields(JsonNode errorResponse) {
    return errorResponse.get("details").findValuesAsText("field");
  }

  private long count(String sql) {
    Long n = jdbc.queryForObject(sql, Long.class);
    return n == null ? 0 : n;
  }

  private static Map<String, Object> validInternal(String employeeId, String email) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", "INTERNAL");
    m.put("employeeId", employeeId);
    m.put("name", "Asha Rao");
    m.put("email", email);
    m.put("locationId", PUNE.toString());
    return m;
  }

  private static Map<String, Object> validExternal(String email, UUID location) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", "EXTERNAL");
    m.put("name", "Ext Trainer");
    m.put("email", email);
    m.put("locationId", location.toString());
    return m;
  }

  private MockHttpServletRequestBuilder json(
      MockHttpServletRequestBuilder builder, Object body, RequestPostProcessor auth)
      throws Exception {
    return builder
        .with(auth)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body));
  }

  private static MockHttpServletRequestBuilder authorizedGet(String url) {
    return get(url).with(token("directory-admin", "ROLE_SUPER_ADMIN"));
  }

  private static RequestPostProcessor hr() {
    return token(HR, "ROLE_HR_ADMIN");
  }

  private static RequestPostProcessor token(String subject, String authority) {
    return jwt().jwt(j -> j.subject(subject)).authorities(new SimpleGrantedAuthority(authority));
  }
}
