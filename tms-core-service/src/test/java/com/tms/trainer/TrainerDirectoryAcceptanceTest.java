package com.tms.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tms.scheduling.domain.Allocation;
import com.tms.scheduling.domain.AllocationRepository;
import com.tms.scheduling.domain.AllocationStatus;
import com.tms.scheduling.domain.Workshop;
import com.tms.scheduling.domain.WorkshopRepository;
import com.tms.trainer.application.TrainerSearchService;
import com.tms.trainer.domain.Trainer;
import com.tms.trainer.domain.TrainerRepository;
import com.tms.trainer.domain.TrainerStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * US-002 acceptance tests for {@code GET /api/v1/trainers} (AC1-AC6; AC7 is covered by {@link
 * TrainerSearchLoggingTest}). Test obligations:
 * 4652d90e-4c32-4e54-8607-6b46f4aa1425:test:acceptance:US-002-ac-1..ac-7.
 *
 * <p>Omitted-filter, free-text matching, allocation-flag representation and field-set behaviors are
 * provisional defaults (see TODOs referencing FR-US-002-001/002/004/005/006), not spec requirements.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TrainerDirectoryAcceptanceTest {

  private static final String TRAINERS = "/api/v1/trainers";
  private static final UUID CERT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
  private static final UUID CERT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired TrainerRepository trainers;
  @Autowired WorkshopRepository workshops;
  @Autowired AllocationRepository allocations;
  @Autowired MeterRegistry meters;

  private Trainer asha; // ACTIVE, Pune, weekends, A+B
  private Trainer ben; // ACTIVE, Pune, no weekends, A
  private Trainer chen; // INACTIVE, Delhi, weekends, B

  @BeforeEach
  void seed() {
    jdbcTemplate.update("delete from tms.allocation");
    jdbcTemplate.update("delete from tms.workshop");
    jdbcTemplate.update("delete from tms.trainer");
    asha = trainer("E001", "Asha Rao", "asha@example.com", TrainerStatus.ACTIVE, "Pune", true, CERT_A, CERT_B);
    ben = trainer("E002", "Ben Cole", "ben@example.com", TrainerStatus.ACTIVE, "Pune", false, CERT_A);
    chen = trainer("E003", "Chen Wu", "chen@example.com", TrainerStatus.INACTIVE, "Delhi", true, CERT_B);
  }

  @Test
  @DisplayName("[US-002-ac-1] all supplied filters are combined with AND")
  void us002Ac1_filtersAreCombinedWithAnd() throws Exception {
    search("status=ACTIVE&location=pune&weekendAvailable=true&certificationIds=" + CERT_A + "," + CERT_B)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(asha.getId().toString()));

    search("status=ACTIVE&certificationIds=" + CERT_A).andExpect(jsonPath("$.totalElements").value(2));
    search("status=INACTIVE&location=Pune").andExpect(jsonPath("$.totalElements").value(0));

    // Allocation state is one of the ANDed filters.
    Workshop w = workshop(10, 12);
    allocations.save(new Allocation(w, ben.getId(), AllocationStatus.CONFIRMED, Instant.now()));
    search("status=ACTIVE&allocationState=CONFIRMED")
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(ben.getId().toString()));
    search("status=ACTIVE&allocationState=FREE")
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(asha.getId().toString()));

    // TODO(FR-US-002-001): omitted filters currently mean "no constraint".
    search("").andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  @DisplayName("[US-002-ac-2] free text matches name/email/employeeId and is intersected with filters")
  void us002Ac2_freeTextIsIntersectedWithStructuredFilters() throws Exception {
    search("q=asha").andExpect(jsonPath("$.totalElements").value(1));
    search("q=BEN@EXAMPLE").andExpect(jsonPath("$.totalElements").value(1));
    search("q=E003").andExpect(jsonPath("$.content[0].id").value(chen.getId().toString()));
    // "example.com" matches all three trainers by email; the structured filter narrows it.
    search("q=example.com").andExpect(jsonPath("$.totalElements").value(3));
    search("q=example.com&status=INACTIVE")
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(chen.getId().toString()));
    // Text matches Chen but the structured filter excludes him: empty intersection.
    search("q=chen&status=ACTIVE").andExpect(jsonPath("$.totalElements").value(0));
    // LIKE wildcards in the term are treated literally.
    search("q=%25").andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  @DisplayName("[US-002-ac-2] free text is applied before paging")
  void us002Ac2_pagingAppliesToIntersectedResult() throws Exception {
    search("q=example.com&size=2&page=0")
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.content.length()").value(2));
    search("q=example.com&size=2&page=1").andExpect(jsonPath("$.content.length()").value(1));
  }

  @Test
  @DisplayName("[US-002-ac-3] repository timing is recorded as a metric; no threshold is enforced")
  void us002Ac3_searchTimingIsRecorded() throws Exception {
    search("status=ACTIVE").andExpect(status().isOk());
    // TODO(FR-US-002-003): the numeric performance threshold is undefined, so only the metric is
    // asserted; production-scale latency must be verified against the agreed threshold elsewhere.
    assertThat(meters.find(TrainerSearchService.SEARCH_TIMER).timer()).isNotNull();
    assertThat(meters.find(TrainerSearchService.SEARCH_TIMER).timer().count()).isPositive();
  }

  @Test
  @DisplayName("[US-002-ac-4] allocation flags free/tentative/confirmed are derived per trainer")
  void us002Ac4_allocationFlagsAreDerived() throws Exception {
    allocations.save(new Allocation(workshop(10, 12), ben.getId(), AllocationStatus.TENTATIVE, Instant.now()));
    allocations.save(new Allocation(workshop(20, 22), chen.getId(), AllocationStatus.CONFIRMED, Instant.now()));
    // Overlapping states: both flags set (representation is provisional, FR-US-002-004).
    allocations.save(new Allocation(workshop(30, 31), chen.getId(), AllocationStatus.TENTATIVE, Instant.now()));
    // An allocation that already ended does not count.
    allocations.save(
        new Allocation(workshop(-20, -18), asha.getId(), AllocationStatus.CONFIRMED, Instant.now()));

    search("size=10")
        .andExpect(jsonPath("$.content[?(@.employeeId=='E001')].allocation.free").value(true))
        .andExpect(jsonPath("$.content[?(@.employeeId=='E001')].allocation.confirmed").value(false))
        .andExpect(jsonPath("$.content[?(@.employeeId=='E002')].allocation.free").value(false))
        .andExpect(jsonPath("$.content[?(@.employeeId=='E002')].allocation.tentative").value(true))
        .andExpect(jsonPath("$.content[?(@.employeeId=='E003')].allocation.tentative").value(true))
        .andExpect(jsonPath("$.content[?(@.employeeId=='E003')].allocation.confirmed").value(true))
        .andExpect(jsonPath("$.content[?(@.employeeId=='E003')].allocation.free").value(false));
  }

  @Test
  @DisplayName("[US-002-ac-5] each trainer carries identity and profile routing information")
  void us002Ac5_identityAndRoutingAreIncluded() throws Exception {
    search("q=asha")
        .andExpect(jsonPath("$.content[0].id").value(asha.getId().toString()))
        .andExpect(jsonPath("$.content[0].employeeId").value("E001"))
        .andExpect(jsonPath("$.content[0].name").value("Asha Rao"))
        .andExpect(jsonPath("$.content[0].email").value("asha@example.com"))
        .andExpect(jsonPath("$.content[0].profileRoute").value(TRAINERS + "/" + asha.getId()));
  }

  @Test
  @DisplayName("[US-002-ac-6] directory ids drive allocate-trainer, which enforces availability and conflicts")
  void us002Ac6_allocateTrainerEnforcesAvailabilityAndConflicts() throws Exception {
    String body = search("q=asha").andReturn().getResponse().getContentAsString();
    String trainerId = JsonPath.read(body, "$.content[0].id");
    String availabilityId = JsonPath.read(body, "$.content[0].availabilityId");
    Workshop first = workshop(10, 12);
    Workshop overlapping = workshop(11, 13);

    allocate(first, trainerId, availabilityId).andExpect(status().isCreated());
    assertThat(allocations.count()).isEqualTo(1);

    // Conflict: overlaps an existing allocation of the same trainer.
    allocate(overlapping, trainerId, availabilityId)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ALLOCATION_CONFLICT"));

    // Availability: wrong availability id, and inactive trainer.
    allocate(workshop(40, 41), trainerId, UUID.randomUUID().toString())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TRAINER_UNAVAILABLE"));
    allocate(workshop(40, 41), chen.getId().toString(), chen.getAvailabilityId().toString())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TRAINER_UNAVAILABLE"));

    assertThat(allocations.count()).isEqualTo(1);
  }

  // [US-002-ac-7] is verified in TrainerSearchLoggingTest (no database needed).

  private ResultActions search(String query) throws Exception {
    return mockMvc.perform(get(TRAINERS + "?" + query).with(jwt()));
  }

  private ResultActions allocate(Workshop workshop, String trainerId, String availabilityId)
      throws Exception {
    String json = "{\"trainerId\":\"%s\",\"availabilityId\":\"%s\"}".formatted(trainerId, availabilityId);
    return mockMvc.perform(
        post("/api/v1/workshops/" + workshop.getId() + "/allocate-trainer")
            .with(jwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
  }

  private Trainer trainer(
      String employeeId,
      String name,
      String email,
      TrainerStatus status,
      String location,
      boolean weekend,
      UUID... certifications) {
    return trainers.save(
        new Trainer(employeeId, name, email, status, location, weekend, Set.of(certifications), Instant.now()));
  }

  private Workshop workshop(int startOffsetDays, int endOffsetDays) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    return workshops.save(
        new Workshop("Workshop", today.plusDays(startOffsetDays), today.plusDays(endOffsetDays), Instant.now()));
  }
}
