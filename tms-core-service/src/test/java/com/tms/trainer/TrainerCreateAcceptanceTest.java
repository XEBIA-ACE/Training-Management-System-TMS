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
 * 2d9e1c7c-6d1c-46cc-9803-1edeb7c26a64:test:acceptance:US-003-ac-1..ac-7 (tasks T008..T014).
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

  // --- US-003-ac-1 (FR-US-003-001, T008) ------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-1][FR-US-003-001][T008] INTERNAL without employeeId/location is a 400")
  void us003Ac1_internalRequiresEmployeeIdAndLocation() throws Exception {
    JsonNode body =
        createExpect400(hr(), Map.of("type", "INTERNAL", "name", "Asha", "email", "asha@corp.com"));
    assertThat(fields(body)).contains("employeeId", "locationId");
    assertThat(body.get("code").asText()).isEqualTo("VALIDATION_FAILED");
    assertThat(count("select count(*) from tms.trainer")).isZero();
  }

  @Test
  @DisplayName(
      "[US-003-ac-1][FR-US-003-001][T008] every invalid field is listed; nothing persisted")
  void us003Ac1_formatErrorsListedPerField() throws Exception {
    JsonNode body =
        createExpect400(
            hr(),
            Map.of(
                "type", "INTERNAL",
                "employeeId", "E1",
                "name", " ",
                "email", "nope",
                "locationId", PUNE.toString()));
    assertThat(fields(body)).containsExactlyInAnyOrder("name", "email");
    assertThat(count("select count(*) from tms.trainer")).isZero();
  }

  @Test
  @DisplayName("[US-003-ac-1][FR-US-003-001][T008] valid INTERNAL profile is created (201)")
  void us003Ac1_validInternalCreated() throws Exception {
    mockMvc
        .perform(json(post(TRAINERS), validInternal("E1", "a@corp.com"), hr()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("INTERNAL"))
        .andExpect(jsonPath("$.employeeId").value("E1"));
  }

  // --- US-003-ac-2 (FR-US-003-002, T009) ------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-2][FR-US-003-002][T009] EXTERNAL requires name, email and location")
  void us003Ac2_externalRequiresNameEmailAndLocation() throws Exception {
    JsonNode body = createExpect400(hr(), Map.of("type", "EXTERNAL"));
    assertThat(fields(body)).containsExactlyInAnyOrder("name", "email", "locationId");
    assertThat(count("select count(*) from tms.trainer")).isZero();
  }

  @Test
  @DisplayName("[US-003-ac-2][FR-US-003-002][T009] malformed email / locationId / type are 400")
  void us003Ac2_malformedValues() throws Exception {
    JsonNode body =
        createExpect400(
            hr(),
            Map.of("type", "EXTERNAL", "name", "Ext", "email", "bad", "locationId", "not-a-uuid"));
    assertThat(fields(body)).contains("email", "locationId");
    assertThat(
            fields(createExpect400(hr(), Map.of("type", "OTHER", "name", "x", "email", "a@b.co"))))
        .contains("type");
    assertThat(count("select count(*) from tms.trainer")).isZero();
  }

  @Test
  @DisplayName("[US-003-ac-2][FR-US-003-002][T009] msaNdaStatus outside its enum is a 400")
  void us003Ac2_msaNdaStatusEnumEnforced() throws Exception {
    Map<String, Object> req = validExternal("ext@vendor.com", PUNE);
    req.put("msaNdaStatus", "MAYBE");
    assertThat(fields(createExpect400(hr(), req))).containsExactly("msaNdaStatus");
    assertThat(count("select count(*) from tms.trainer")).isZero();
  }

  @Test
  @DisplayName("[US-003-ac-2][FR-US-003-002][T009] valid EXTERNAL profile (no employeeId) is 201")
  void us003Ac2_validExternalCreated() throws Exception {
    Map<String, Object> req = validExternal("ext@vendor.com", PUNE);
    req.put("msaNdaStatus", "SIGNED");
    mockMvc
        .perform(json(post(TRAINERS), req, hr()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("EXTERNAL"))
        .andExpect(jsonPath("$.msaNdaStatus").value("SIGNED"));
    mockMvc
        .perform(json(post(TRAINERS), validExternal("ext2@vendor.com", PUNE), hr()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.msaNdaStatus").doesNotExist());
  }

  // --- US-003-ac-3 (FR-US-003-003, T010) ------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-3][FR-US-003-003][T010] duplicate employeeId (inactive trainer) is 409")
  void us003Ac3_duplicateEmployeeId() throws Exception {
    UUID id = createOk(validInternal("E1", "first@corp.com"));
    jdbc.update("update tms.trainer set status='INACTIVE' where id=?", id);
    JsonNode body = createExpect409(validInternal("E1", "second@corp.com"));
    assertThat(body.get("code").asText()).isEqualTo("DUPLICATE_EMPLOYEE_ID");
    assertThat(fields(body)).containsExactly("employeeId");
    assertThat(count("select count(*) from tms.trainer")).isEqualTo(1);
  }

  @Test
  @DisplayName("[US-003-ac-3][FR-US-003-003][T010] duplicate email (any type/status) is 409")
  void us003Ac3_duplicateEmail() throws Exception {
    UUID id = createOk(validInternal("E1", "same@corp.com"));
    jdbc.update("update tms.trainer set status='INACTIVE' where id=?", id);
    JsonNode internal = createExpect409(validInternal("E2", "SAME@corp.com"));
    assertThat(internal.get("code").asText()).isEqualTo("DUPLICATE_EMAIL");
    assertThat(fields(internal)).containsExactly("email");
    JsonNode external = createExpect409(validExternal("same@corp.com", PUNE));
    assertThat(external.get("code").asText()).isEqualTo("DUPLICATE_EMAIL");
    assertThat(count("select count(*) from tms.trainer")).isEqualTo(1);
  }

  @Test
  @DisplayName("[US-003-ac-3][FR-US-003-003][T010] duplicate rejection is visible via audit-logs")
  void us003Ac3_duplicateRejectionAudited() throws Exception {
    UUID existing = createOk(validInternal("E1", "a@corp.com"));
    createExpect409(validInternal("E1", "other@corp.com"));
    JsonNode logs = auditLogs(existing);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(1).get("details").get("outcome").asText()).isEqualTo("REJECTED");
    assertThat(logs.get(1).get("details").get("fields").toString()).contains("employeeId");
    assertThat(logs.get(1).toString()).doesNotContain("other@corp.com");
  }

  // --- US-003-ac-4 (FR-US-003-004, T011) ------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-4][FR-US-003-004][T011] create derives, persists and returns timezone")
  void us003Ac4_createDerivesTimezone() throws Exception {
    MvcResult result =
        mockMvc
            .perform(json(post(TRAINERS), validInternal("E1", "a@corp.com"), hr()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
            .andReturn();
    UUID id = idOf(result);
    assertThat(jdbc.queryForObject("select timezone from tms.trainer where id=?", String.class, id))
        .isEqualTo("Asia/Kolkata");
  }

  @Test
  @DisplayName("[US-003-ac-4][FR-US-003-004][T011] PUT with a new locationId recalculates timezone")
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
  @DisplayName("[US-003-ac-4][FR-US-003-004][T011] unknown location leaves timezone unset (GAP)")
  void us003Ac4_unknownLocationHasNoTimezone() throws Exception {
    Map<String, Object> req = validInternal("E1", "a@corp.com");
    req.put("locationId", NOWHERE.toString());
    UUID id = createOk(req);
    assertThat(jdbc.queryForObject("select timezone from tms.trainer where id=?", String.class, id))
        .isNull();
  }

  // --- US-003-ac-5 (FR-US-003-005, T012) ------------------------------------------------------

  @Test
  @DisplayName(
      "[US-003-ac-5][FR-US-003-005][T012] new trainer is ACTIVE and listed by status=ACTIVE")
  void us003Ac5_activeByDefaultAndListed() throws Exception {
    mockMvc
        .perform(json(post(TRAINERS), validInternal("E1", "a@corp.com"), hr()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    UUID id = jdbc.queryForObject("select id from tms.trainer where employee_id='E1'", UUID.class);
    assertThat(jdbc.queryForObject("select status from tms.trainer where id=?", String.class, id))
        .isEqualTo("ACTIVE");
    mockMvc
        .perform(authorizedGet(TRAINERS).param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(id.toString()))
        .andExpect(jsonPath("$.items[0].status").value("ACTIVE"));
    mockMvc
        .perform(authorizedGet(TRAINERS).param("status", "INACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  // --- US-003-ac-6 (FR-US-003-006, T013) ------------------------------------------------------

  @Test
  @DisplayName("[US-003-ac-6][FR-US-003-006][T013] creation appends a module=trainer audit entry")
  void us003Ac6_successAudited() throws Exception {
    UUID id = createOk(validInternal("E1", "a@corp.com"));
    JsonNode logs = auditLogs(id);
    assertThat(logs).hasSize(1);
    JsonNode e = logs.get(0);
    assertThat(e.get("entryType").asText()).isEqualTo("CREATE");
    assertThat(e.get("actor").asText()).isEqualTo(HR);
    assertThat(e.get("occurredAt").asText()).isNotBlank();
    assertThat(e.get("resourceId").asText()).isEqualTo(id.toString());
    JsonNode d = e.get("details");
    assertThat(d.get("module").asText()).isEqualTo("trainer");
    assertThat(d.get("outcome").asText()).isEqualTo("SUCCESS");
    assertThat(d.get("trainerId").asText()).isEqualTo(id.toString());
    assertThat(d.get("name").asText()).isEqualTo("Asha Rao");
    assertThat(d.get("email").asText()).isEqualTo("a@corp.com");
    assertThat(d.get("employeeId").asText()).isEqualTo("E1");
    assertThat(d.get("type").asText()).isEqualTo("INTERNAL");
    assertThat(d.get("locationId").asText()).isEqualTo(PUNE.toString());
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
  @DisplayName("[US-003-ac-6][FR-US-003-006][T013] audit entries are immutable")
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

  @Test
  @DisplayName(
      "[US-003-ac-6][FR-US-003-006][T013] validation failure audit has no submitted values")
  void us003Ac6_validationFailureAuditHasNoValues() throws Exception {
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

  // --- US-003-ac-7 (FR-US-003-007, T014) ------------------------------------------------------

  @Test
  @DisplayName(
      "[US-003-ac-7][FR-US-003-007][T014] no token is 401, nothing persisted, denial audited")
  void us003Ac7_unauthenticatedIs401() throws Exception {
    mockMvc
        .perform(
            post(TRAINERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validInternal("E1", "a@corp.com"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    assertThat(count("select count(*) from tms.trainer")).isZero();
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select entry_type, actor, action, details::text as details from tms.audit_log_entry");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("entry_type")).isEqualTo("UNAUTHORIZED_ACCESS");
    assertThat(rows.get(0).get("actor")).isEqualTo("anonymous");
    assertThat(rows.get(0).get("action")).isEqualTo("POST " + TRAINERS);
    assertThat((String) rows.get(0).get("details"))
        .contains("AUTHENTICATION_REQUIRED")
        .doesNotContain("a@corp.com");
  }

  @Test
  @DisplayName(
      "[US-003-ac-7][FR-US-003-007][T014] no trainer create role is 403, nothing persisted")
  void us003Ac7_forbiddenWithoutCreateRole() throws Exception {
    mockMvc
        .perform(json(post(TRAINERS), validInternal("E1", "a@corp.com"), token("u", "ROLE_VIEWER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    assertThat(count("select count(*) from tms.trainer")).isZero();
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select entry_type, actor, action, details::text as details from tms.audit_log_entry");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("entry_type")).isEqualTo("UNAUTHORIZED_ACCESS");
    assertThat(rows.get(0).get("actor")).isEqualTo("u");
    assertThat(rows.get(0).get("action")).isEqualTo("POST /api/v1/trainers");
    assertThat((String) rows.get(0).get("details")).contains("trainer");
  }

  @Test
  @DisplayName("[US-003-ac-7][FR-US-003-007][T014] HR admin and super admin may create (201)")
  void us003Ac7_createRolesAllowed() throws Exception {
    mockMvc
        .perform(
            json(post(TRAINERS), validInternal("E1", "a@corp.com"), token("u", "ROLE_HR_ADMIN")))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            json(post(TRAINERS), validInternal("E2", "b@corp.com"), token("u", "ROLE_SUPER_ADMIN")))
        .andExpect(status().isCreated());
    assertThat(
            count(
                "select count(*) from tms.audit_log_entry where entry_type='UNAUTHORIZED_ACCESS'"))
        .isZero();
  }

  // --- helpers -----------------------------------------------------------------------------------

  private UUID createOk(Map<String, Object> body) throws Exception {
    return idOf(
        mockMvc
            .perform(json(post(TRAINERS), body, hr()))
            .andExpect(status().isCreated())
            .andReturn());
  }

  private UUID idOf(MvcResult result) throws Exception {
    return UUID.fromString(
        objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
  }

  private JsonNode createExpect409(Map<String, Object> body) throws Exception {
    MvcResult result =
        mockMvc
            .perform(json(post(TRAINERS), body, hr()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.details").isArray())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
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
