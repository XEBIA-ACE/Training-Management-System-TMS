package com.tms.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.trainer.application.TrainerAppService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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
 * US-002 acceptance tests for the trainer directory ({@code GET /api/trainers}, {@code GET
 * /api/trainers/{id}}), FR-US-002-001..FR-US-002-007, tasks T008..T014. Test obligations:
 * dec87be6-a28a-4161-8950-606db714bf63:test:acceptance:US-002-ac-1..ac-7.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class TrainerDirectoryAcceptanceTest {

  private static final String TRAINERS = "/api/trainers";
  private static final String ACTOR = "coordinator-42";

  private static final UUID PUNE = UUID.randomUUID();
  private static final UUID DELHI = UUID.randomUUID();
  private static final UUID AWS = UUID.randomUUID();
  private static final UUID AZURE = UUID.randomUUID();

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired MeterRegistry meterRegistry;

  private UUID alice;
  private UUID bob;
  private UUID carol;
  private UUID dave;

  @BeforeEach
  void seed() {
    jdbc.update("delete from tms.audit_log_entry");
    jdbc.update("delete from tms.trainer_certification");
    jdbc.update("delete from tms.trainer");
    alice =
        trainer("E100", "Alice Smith", "alice@corp.com", "ACTIVE", "AVAILABLE", PUNE, true, AWS);
    bob = trainer("E200", "Bob Jones", "bob@corp.com", "ACTIVE", "AVAILABLE", PUNE, false, AWS);
    carol =
        trainer(
            "E300",
            "Carol White",
            "carol@corp.com",
            "INACTIVE",
            "AVAILABLE",
            PUNE,
            true,
            AWS,
            AZURE);
    dave =
        trainer("E400", "Dave Brown", "dave@corp.com", "ACTIVE", "ALLOCATED", DELHI, true, AZURE);
  }

  // --- US-002-ac-1 -------------------------------------------------------------------------------

  @Test
  @DisplayName(
      "[US-002-ac-1][FR-US-002-001][T008] each filter alone returns only trainers matching it")
  void us002Ac1_eachFilterRestrictsResults() throws Exception {
    assertThat(ids(Map.of("status", "ACTIVE"))).containsExactly(alice, bob, dave);
    assertThat(ids(Map.of("certificationId", AZURE.toString(), "status", "ALL")))
        .containsExactly(carol, dave);
    assertThat(ids(Map.of("availabilityState", "ALLOCATED"))).containsExactly(dave);
    assertThat(ids(Map.of("locationId", PUNE.toString(), "status", "ALL")))
        .containsExactly(alice, bob, carol);
    assertThat(ids(Map.of("weekendAvailable", "false"))).containsExactly(bob);
    // US-004 (FR-US-004-AC2-001/003): INACTIVE carol is hidden unless status=ALL is requested.
    assertThat(ids(Map.of())).containsExactly(alice, bob, dave);
    assertThat(ids(Map.of("status", "ALL"))).containsExactly(alice, bob, carol, dave);
  }

  @Test
  @DisplayName(
      "[US-002-ac-1][FR-US-002-001][T008] combined filters return only trainers matching all of"
          + " them in PagedTrainerList.items")
  void us002Ac1_combinedFiltersAreAndCombined() throws Exception {
    Map<String, String> all =
        Map.of(
            "status", "ACTIVE",
            "certificationId", AWS.toString(),
            "availabilityState", "AVAILABLE",
            "locationId", PUNE.toString(),
            "weekendAvailable", "true",
            "search", "smith");
    mockMvc
        .perform(withParams(authorized(get(TRAINERS)), all))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(alice.toString()))
        .andExpect(jsonPath("$.totalElements").value(1));

    // Carol matches everything except status; Dave everything except location/cert/availability.
    assertThat(ids(Map.of("status", "ACTIVE", "certificationId", AZURE.toString())))
        .containsExactly(dave);
    assertThat(
            ids(
                Map.of(
                    "status",
                    "INACTIVE",
                    "locationId",
                    DELHI.toString(),
                    "weekendAvailable",
                    "true")))
        .isEmpty();
  }

  @Test
  @DisplayName("[US-002-ac-1][FR-US-002-001][T008] malformed filter values are rejected with 400")
  void us002Ac1_invalidFilterValuesRejected() throws Exception {
    mockMvc
        .perform(authorized(get(TRAINERS)).param("certificationId", "not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    mockMvc
        .perform(authorized(get(TRAINERS)).param("weekendAvailable", "maybe"))
        .andExpect(status().isBadRequest());
  }

  // --- US-002-ac-2 -------------------------------------------------------------------------------

  @Test
  @DisplayName(
      "[US-002-ac-2][FR-US-002-002][T009] search matches trainer name, email and employeeId"
          + " (case-insensitive)")
  void us002Ac2_searchMatchesNameEmailAndEmployeeId() throws Exception {
    assertThat(ids(Map.of("search", "JONES"))).containsExactly(bob);
    assertThat(ids(Map.of("search", "carol@corp", "status", "ALL"))).containsExactly(carol);
    assertThat(ids(Map.of("search", "e400"))).containsExactly(dave);
    assertThat(ids(Map.of("search", "corp.com", "status", "ALL")))
        .containsExactly(alice, bob, carol, dave);
    assertThat(ids(Map.of("search", "nobody"))).isEmpty();
  }

  @Test
  @DisplayName(
      "[US-002-ac-2][FR-US-002-002][T009] search is AND-combined with every other active filter")
  void us002Ac2_searchCombinedWithFilters() throws Exception {
    assertThat(ids(Map.of("search", "corp.com", "weekendAvailable", "true", "status", "ACTIVE")))
        .containsExactly(alice, dave);
    assertThat(ids(Map.of("search", "carol", "status", "ACTIVE"))).isEmpty();
    assertThat(ids(Map.of("search", "E", "locationId", DELHI.toString()))).containsExactly(dave);
  }

  @Test
  @DisplayName("[US-002-ac-2][FR-US-002-002][T009] LIKE wildcards in search are matched literally")
  void us002Ac2_wildcardsAreEscaped() throws Exception {
    assertThat(ids(Map.of("search", "%"))).isEmpty();
    assertThat(ids(Map.of("search", "_"))).isEmpty();
  }

  // --- US-002-ac-3 -------------------------------------------------------------------------------

  @Test
  @DisplayName(
      "[US-002-ac-3][FR-US-002-003][T010] GET /api/trainers latency is recorded as a Micrometer"
          + " timer for LoggingMonitoring")
  void us002Ac3_searchLatencyIsObservable(CapturedOutput output) throws Exception {
    long before = searchCount("true");
    ids(Map.of("search", "secret-term-xyz", "status", "ACTIVE"));
    assertThat(searchCount("true")).isEqualTo(before + 1);
    assertThat(output.getOut())
        .contains("Trainer search completed")
        .contains("freeTextPresent=true")
        .doesNotContain("secret-term-xyz");
  }

  @Test
  @DisplayName(
      "[US-002-ac-3][FR-US-002-003][T010] page size is bounded to keep query cost predictable")
  void us002Ac3_pageSizeBounded() throws Exception {
    mockMvc
        .perform(authorized(get(TRAINERS)).param("size", "101"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(authorized(get(TRAINERS)).param("search", "x".repeat(201)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            authorized(get(TRAINERS)).param("size", "2").param("page", "1").param("status", "ALL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].id").value(carol.toString()))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(4))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  @DisplayName(
      "[US-002-ac-3][FR-US-002-003][T010] a page of trainers with certifications loads within a"
          + " bounded number of SQL statements")
  void us002Ac3_noPerTrainerQueries() throws Exception {
    for (int i = 0; i < 60; i++) {
      trainer(
          "B" + i, "Bulk " + i, "bulk" + i + "@corp.com", "ACTIVE", "AVAILABLE", PUNE, true, AWS);
    }
    long start = System.nanoTime();
    JsonNode page = list(Map.of("size", "100", "certificationId", AWS.toString()));
    assertThat(page.get("items")).hasSize(63);
    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(5_000);
  }

  // --- US-002-ac-4 -------------------------------------------------------------------------------

  @Test
  @DisplayName(
      "[US-002-ac-4][FR-US-002-004][T011] GET /api/trainers/{id} returns the trainer for an id"
          + " taken from PagedTrainerList")
  void us002Ac4_profileForIdFromList() throws Exception {
    JsonNode item = list(Map.of("search", "dave")).get("items").get(0);
    String id = item.get("id").asText();

    mockMvc
        .perform(authorized(get(TRAINERS + "/" + id)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value("Dave Brown"))
        .andExpect(jsonPath("$.email").value("dave@corp.com"));
  }

  @Test
  @DisplayName(
      "[US-002-ac-4][FR-US-002-004][T011] an unauthorized caller gets 403 without trainer data,"
          + " and cannot probe trainer existence")
  void us002Ac4_noLeakageToUnauthorizedCaller() throws Exception {
    String body =
        mockMvc
            .perform(unprivileged(get(TRAINERS + "/" + alice)))
            .andExpect(status().isForbidden())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).doesNotContain("Alice").doesNotContain("alice@corp.com");

    mockMvc
        .perform(unprivileged(get(TRAINERS + "/" + UUID.randomUUID())))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(authorized(get(TRAINERS + "/" + UUID.randomUUID())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TRAINER_NOT_FOUND"));
  }

  // --- US-002-ac-5 -------------------------------------------------------------------------------

  @Test
  @DisplayName(
      "[US-002-ac-5][FR-US-002-005][T012] directory rows and profiles expose the same trainer id"
          + " and basic attributes for allocation APIs")
  void us002Ac5_identityAttributesAvailableWithoutExtraLookup() throws Exception {
    JsonNode item = list(Map.of("search", "carol", "status", "ALL")).get("items").get(0);
    assertThat(item.get("id").asText()).isEqualTo(carol.toString());
    assertThat(item.get("employeeId").asText()).isEqualTo("E300");
    assertThat(item.get("name").asText()).isEqualTo("Carol White");
    assertThat(item.get("email").asText()).isEqualTo("carol@corp.com");
    assertThat(item.get("status").asText()).isEqualTo("INACTIVE");
    assertThat(item.get("availabilityState").asText()).isEqualTo("AVAILABLE");
    assertThat(item.get("locationId").asText()).isEqualTo(PUNE.toString());
    assertThat(item.get("weekendAvailable").asBoolean()).isTrue();
    List<String> certs = new ArrayList<>();
    item.get("certificationIds").forEach(c -> certs.add(c.asText()));
    assertThat(certs).containsExactlyInAnyOrder(AWS.toString(), AZURE.toString());

    String profile =
        mockMvc
            .perform(authorized(get(TRAINERS + "/" + carol)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(objectMapper.readTree(profile)).isEqualTo(item);
  }

  // --- US-002-ac-6 -------------------------------------------------------------------------------

  @Test
  @DisplayName(
      "[US-002-ac-6][FR-US-002-006][T013] each search writes an audit entry with actor, timestamp"
          + " and criteria summary but never the raw free text")
  void us002Ac6_searchIsAuditedWithoutRawText() throws Exception {
    Instant before = Instant.now().minusSeconds(1);
    ids(Map.of("search", "Smith-Secret", "status", "ACTIVE", "locationId", PUNE.toString()));

    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select entry_type, actor, action, resource_type, occurred_at, details::text as details"
                + " from tms.audit_log_entry");
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat(row.get("entry_type")).isEqualTo("SEARCH");
    assertThat(row.get("actor")).isEqualTo(ACTOR);
    assertThat(row.get("action")).isEqualTo("GET /api/trainers");
    assertThat(row.get("resource_type")).isEqualTo("Trainer");
    assertThat(((Timestamp) row.get("occurred_at")).toInstant()).isAfter(before);

    String details = (String) row.get("details");
    assertThat(details).doesNotContainIgnoringCase("smith-secret").doesNotContain(PUNE.toString());
    JsonNode summary = objectMapper.readTree(details);
    assertThat(summary.get("freeTextPresent").asBoolean()).isTrue();
    List<String> filters = new ArrayList<>();
    summary.get("filters").forEach(f -> filters.add(f.asText()));
    assertThat(filters).containsExactly("status", "locationId");
  }

  @Test
  @DisplayName(
      "[US-002-ac-6][FR-US-002-006][T013] a search without free text is audited with"
          + " freeTextPresent=false")
  void us002Ac6_searchWithoutFreeText() throws Exception {
    ids(Map.of("weekendAvailable", "true"));
    String details =
        jdbc.queryForObject(
            "select details::text from tms.audit_log_entry where entry_type = 'SEARCH'",
            String.class);
    JsonNode summary = objectMapper.readTree(details);
    assertThat(summary.get("freeTextPresent").asBoolean()).isFalse();
    assertThat(summary.get("filters").get(0).asText()).isEqualTo("weekendAvailable");
  }

  // --- US-002-ac-7 -------------------------------------------------------------------------------

  @Test
  @DisplayName(
      "[US-002-ac-7][FR-US-002-007][T014] trainer endpoints require a bearer token (401 without"
          + " one)")
  void us002Ac7_bearerTokenRequired() throws Exception {
    mockMvc.perform(get(TRAINERS)).andExpect(status().isUnauthorized());
    mockMvc.perform(get(TRAINERS + "/" + alice)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName(
      "[US-002-ac-7][FR-US-002-007][T014] callers without a trainer directory role get 403 and an"
          + " UnauthorizedAccessEntry is written")
  void us002Ac7_forbiddenWritesUnauthorizedAccessEntry() throws Exception {
    mockMvc
        .perform(unprivileged(get(TRAINERS)).param("search", "alice"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    mockMvc
        .perform(unprivileged(get(TRAINERS + "/" + alice)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select entry_type, actor, action, resource_id, details::text as details"
                + " from tms.audit_log_entry order by occurred_at");
    assertThat(rows).hasSize(2);
    assertThat(rows)
        .allSatisfy(r -> assertThat(r.get("entry_type")).isEqualTo("UNAUTHORIZED_ACCESS"));
    assertThat(rows).allSatisfy(r -> assertThat(r.get("actor")).isEqualTo("intruder"));
    assertThat(rows.get(0).get("action")).isEqualTo("GET /api/trainers");
    assertThat(rows.get(0).get("details").toString()).doesNotContain("alice");
    assertThat(rows.get(1).get("action")).isEqualTo("GET /api/trainers/{id}");
    assertThat(rows.get(1).get("resource_id")).isEqualTo(alice.toString());
  }

  // --- helpers -----------------------------------------------------------------------------------

  private UUID trainer(
      String employeeId,
      String name,
      String email,
      String status,
      String availability,
      UUID location,
      boolean weekend,
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
        location,
        weekend,
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

  private List<UUID> ids(Map<String, String> params) throws Exception {
    List<UUID> ids = new ArrayList<>();
    list(params).get("items").forEach(item -> ids.add(UUID.fromString(item.get("id").asText())));
    return ids;
  }

  private JsonNode list(Map<String, String> params) throws Exception {
    String body =
        mockMvc
            .perform(withParams(authorized(get(TRAINERS)), params))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body);
  }

  private long searchCount(String freeText) {
    Timer timer =
        meterRegistry.find(TrainerAppService.SEARCH_TIMER).tag("freeText", freeText).timer();
    return timer == null ? 0 : timer.count();
  }

  private static MockHttpServletRequestBuilder withParams(
      MockHttpServletRequestBuilder builder, Map<String, String> params) {
    params.forEach(builder::param);
    return builder;
  }

  private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder) {
    return builder.with(token(ACTOR, "ROLE_SUPER_ADMIN"));
  }

  private static MockHttpServletRequestBuilder unprivileged(MockHttpServletRequestBuilder builder) {
    return builder.with(token("intruder", "ROLE_OPS_MONITOR"));
  }

  private static RequestPostProcessor token(String subject, String authority) {
    return jwt().jwt(j -> j.subject(subject)).authorities(new SimpleGrantedAuthority(authority));
  }
}
