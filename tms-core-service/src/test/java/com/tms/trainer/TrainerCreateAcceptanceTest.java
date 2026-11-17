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
 * 34a5ad90-698f-4343-9f53-b2abfb05b6c6:test:acceptance:US-003-ac-1..ac-7.
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
  @DisplayName("[US-003-ac-1][FR-US-003-001][T008] INTERNAL without employeeId is a 400 with details")
  void us003Ac1_internalRequiresEmployeeId() throws Exception {
    JsonNode body =
        createExpect400(hr(), Map.of("type", "INTERNAL", "name", "Asha", "email", "asha@corp.com"));
    assertThat(fields(body)).contains("employeeId");
    assertThat(body.get("code").asText()).isEqualTo("VALIDATION_FAILED");
  }

  @Test
  @DisplayName("[US-003-ac-1][FR-US-003-001][T008] every invalid field is listed; nothing persisted")
  void us003Ac1_formatErrorsListedPerField() throws Exception {
    JsonNode body =
        createExpect400(
            hr(), Map.of("type", "INTERNAL", "employeeId", "E1", "name", " ", "email", "nope"));
    assertThat(fields(body)).contains("name", "email");
    assertThat(count("select count(*) from tms.trainer")).isZero();
  }

  @Test
  @DisplayName("[US-003-ac-1][FR-US-003-001][T008] only HR admin / super admin may create (403)")
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
  @DisplayName("[US-003-ac-2][FR-US-003-002][T009] EXTERNAL requires email and locationId")
  void us003Ac2_externalRequiresEmailAndLocation() throws Exception {
    JsonNode body = createExpect400(hr(), Map.of("type", "EXTERNAL", "name", "Ext"));
    assertThat(fields(body)).contains("email", "locationId");
  }

  @Test
  @DisplayName("[US-003-ac-2][FR-US-003-002][T009] malformed email / locationId / type are 400")
  void us003Ac2_malformedValues() throws Exception {
    JsonNode body =
        createExpect400(
            hr(),
            Map.of("type", "EXTERNAL", "name", "Ext", "email", "bad", "locationId", "not-a-uuid"));
    assertThat(fields(body)).contains("email", "locationId");
    assertThat(fields(createExpect400(hr(), Map.of("type", "OTHER", "name", "x", "email", "a@b.co"))))
        .contains("type");
  }

  @Test
  @DisplayName("[US-003-ac-2][FR-US-003-002][T009] valid EXTERNAL profile is created (201)")
  void us003Ac2_validExternalCreated() throws Exception {
    mockMvc
        .perform(
            json(
                post(TRAINERS),
                Map.of(
                    "type", "EXTERNAL",
                    "name", "Ext",
                    "email", "ext@vendor.com",
                    "locationId", PUNE.toString()),
                hr()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("EXTERNAL"));
  }

  // --- US-003-ac-3 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-3][FR-US-003-003][T010] duplicate employeeId is rejected for any status")
  void us003Ac3_duplicateEmployeeId() throws Exception {
    UUID id = createOk(validInternal("E1", "first@corp.com"));
    jdbc.update("update tms.trainer set status='INACTIVE' where id=?", id);
    JsonNode body = createExpect400(hr(), validInternal("E1", "second@corp.com"));
    assertThat(fields(body)).containsExactly("employeeId");
    assertThat(count("select count(*) from tms.trainer")).isEqualTo(1);
  }

  @Test
  @DisplayName("[US-003-ac-3][FR-US-003-003][T010] duplicate email is rejected for any type")
  void us003Ac3_duplicateEmail() throws Exception {
    createOk(validInternal("E1", "same@corp.com"));
    JsonNode body = createExpect400(hr(), validInternal("E2", "SAME@corp.com"));
    assertThat(fields(body)).containsExactly("email");
    Map<String, Object> external = validExternal("same@corp.com", PUNE);
    assertThat(fields(createExpect400(hr(), external))).containsExactly("email");
  }

  // --- US-003-ac-4 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-4][FR-US-003-004][T011] create derives and persists timezone")
  void us003Ac4_createDerivesTimezone() throws Exception {
    Map<String, Object> req = validInternal("E1", "a@corp.com");
    req.put("locationId", PUNE.toString());
    UUID id = createOk(req);
    assertThat(jdbc.queryForObject("select timezone from tms.trainer where id=?", String.class, id))
        .isEqualTo("Asia/Kolkata");
  }

  @Test
  @DisplayName("[US-003-ac-4][FR-US-003-004][T011] PUT with a new locationId recalculates timezone")
  void us003Ac4_updateRecalculatesTimezone() throws Exception {
    Map<String, Object> req = validInternal("E1", "a@corp.com");
    req.put("locationId", PUNE.toString());
    UUID id = createOk(req);
    mockMvc
        .perform(json(put(TRAINERS + "/" + id), Map.of("locationId", LONDON.toString()), hr()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timezone").value("Europe/London"));
    assertThat(jdbc.queryForObject("select timezone from tms.trainer where id=?", String.class, id))
        .isEqualTo("Europe/London");
  }

  @Test
  @DisplayName("[US-003-ac-4][FR-US-003-004][T011] unknown location leaves timezone unset (GAP)")
  void us003Ac4_unknownLocationHasNoTimezone() throws Exception {
    Map<String, Object> req = validInternal("E1", "a@corp.com");
    req.put("locationId", NOWHERE.toString());
    UUID id = createOk(req);
    assertThat(jdbc.queryForObject("select timezone from tms.trainer where id=?", String.class, id))
        .isNull();
  }

  // --- US-003-ac-5 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-5][FR-US-003-005][T012] new trainer is ACTIVE and listed as eligible")
  void us003Ac5_activeByDefaultAndListed() throws Exception {
    UUID id = createOk(validInternal("E1", "a@corp.com"));
    assertThat(jdbc.queryForObject("select status from tms.trainer where id=?", String.class, id))
        .isEqualTo("ACTIVE");
    // GAP(FR-US-003-005): GET /api/allocations/eligible-trainers does not exist in this
    // repository; eligibility is asserted through the existing status=ACTIVE trainer listing.
    mockMvc
        .perform(authorizedGet(TRAINERS).param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(id.toString()))
        .andExpect(jsonPath("$.items[0].status").value("ACTIVE"));
  }

  // --- US-003-ac-6 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-6][FR-US-003-006][T013] creation appends an audit event with profile data")
  void us003Ac6_successAudited() throws Exception {
    UUID id = createOk(validInternal("E1", "a@corp.com"));
    JsonNode logs = auditLogs(id);
    assertThat(logs).hasSize(1);
    JsonNode e = logs.get(0);
    assertThat(e.get("actor").asText()).isEqualTo(HR);
    assertThat(e.get("occurredAt").asText()).isNotBlank();
    assertThat(e.get("resourceId").asText()).isEqualTo(id.toString());
    assertThat(e.get("details").get("outcome").asText()).isEqualTo("SUCCESS");
    assertThat(e.get("details").get("trainerId").asText()).isEqualTo(id.toString());
    assertThat(e.get("details").get("email").asText()).isEqualTo("a@corp.com");
    assertThat(e.get("details").get("employeeId").asText()).isEqualTo("E1");
  }

  @Test
  @DisplayName("[US-003-ac-6][FR-US-003-006][T013] audit payload carries no document content")
  void us003Ac6_noDocumentContentInAudit() throws Exception {
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
  @DisplayName("[US-003-ac-6][FR-US-003-006][T013] audit entries are immutable (entity is @Immutable)")
  void us003Ac6_entriesAreImmutable() throws Exception {
    UUID id = createOk(validInternal("E1", "a@corp.com"));
    String before = auditLogs(id).toString();
    mockMvc
        .perform(json(put(TRAINERS + "/" + id), Map.of("name", "Renamed"), hr()))
        .andExpect(status().isOk());
    JsonNode after = auditLogs(id);
    assertThat(after).hasSize(2);
    assertThat(after.get(0).toString()).isEqualTo(objectMapper.readTree(before).get(0).toString());
  }

  // --- US-003-ac-7 -------------------------------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-7][FR-US-003-007][T014] validation failure is audited in the audit log")
  void us003Ac7_validationFailureAudited() throws Exception {
    createExpect400(hr(), Map.of("type", "INTERNAL", "name", "Asha", "email", "asha@corp.com"));
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
  @DisplayName("[US-003-ac-7][FR-US-003-007][T014] duplicate rejection is visible via audit-logs")
  void us003Ac7_duplicateRejectionVisibleViaEndpoint() throws Exception {
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
