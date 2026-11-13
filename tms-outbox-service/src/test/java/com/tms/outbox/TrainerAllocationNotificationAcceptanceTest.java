package com.tms.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.outbox.application.OutboxProcessingService;
import com.tms.outbox.domain.MessagePublisher;
import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.domain.OutboxMessageFactory;
import com.tms.outbox.domain.OutboxRepository;
import com.tms.outbox.domain.OutboxStatus;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * US-001 acceptance tests (T013–T017): trainer allocation/deallocation outbox entries are published
 * through the EventDispatcher/MessagePublisher with retries, auditable outcomes, grouped
 * statistics, health and metrics. A controllable in-memory {@link MessagePublisher} stands in for
 * the message bus.
 */
@SpringBootTest(
    properties = {
      "tms.outbox.publisher-type=rabbitmq",
      "tms.outbox.retry-max-attempts=3",
      "tms.outbox.retry-backoff-ms=0",
      "tms.outbox.trainer-notifications.metrics-cache-ttl-ms=0"
    })
@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
@ActiveProfiles("test")
@Testcontainers
class TrainerAllocationNotificationAcceptanceTest {

  private static final String ALLOCATED = "TrainerAllocated";
  private static final String DEALLOCATED = "TrainerDeallocated";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired OutboxRepository outboxRepository;
  @Autowired OutboxProcessingService processingService;
  @Autowired ControllablePublisher publisher;
  @Autowired JdbcTemplate jdbcTemplate;

  @Value("${TMS_OUTBOX_DB_SCHEMA}")
  String schema;

  @TestConfiguration
  static class PublisherConfig {
    @Bean
    @Primary
    ControllablePublisher controllablePublisher() {
      return new ControllablePublisher();
    }
  }

  /** Records published entries; fails the next {@code failures} publishes; toggles connectivity. */
  static class ControllablePublisher implements MessagePublisher {
    final List<OutboxMessage> published = new CopyOnWriteArrayList<>();
    final AtomicInteger failures = new AtomicInteger();
    final AtomicBoolean connected = new AtomicBoolean(true);

    @Override
    public CompletableFuture<Void> publish(OutboxMessage message) {
      if (failures.getAndUpdate(n -> n > 0 ? n - 1 : n) > 0) {
        return CompletableFuture.failedFuture(new IllegalStateException("message bus unavailable"));
      }
      published.add(message);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isConnected() {
      return connected.get();
    }

    void reset() {
      published.clear();
      failures.set(0);
      connected.set(true);
    }
  }

  @BeforeEach
  void cleanState() {
    jdbcTemplate.update("delete from " + schema + ".outbox_message");
    publisher.reset();
  }

  private OutboxMessage saveEntry(String eventType, String correlationId) {
    return outboxRepository.save(
        OutboxMessageFactory.create(
            "TrainerAllocation",
            UUID.randomUUID().toString(),
            eventType,
            "{\"trainerId\":\"t-1\",\"workshopId\":\"w-1\"}",
            3,
            "tms-core-service",
            correlationId,
            null));
  }

  private OutboxMessage reload(OutboxMessage message) {
    return outboxRepository.findById(message.getId()).orElseThrow();
  }

  private void processUntilIdle() {
    for (int i = 0; i < 10; i++) {
      processingService.retryFailedMessages();
      processingService.processOutboxMessages();
    }
  }

  @Test
  void usAc1_allocationAndDeallocationEntriesArePublishedThroughDispatcherAndPublisher() {
    OutboxMessage allocation = saveEntry(ALLOCATED, "corr-alloc");
    OutboxMessage deallocation = saveEntry(DEALLOCATED, "corr-dealloc");

    int published = processingService.processOutboxMessages();

    // US-001-ac-1: each entry is handed to the message publisher with its event type and payload
    assertThat(published).isEqualTo(2);
    assertThat(publisher.published)
        .extracting(OutboxMessage::getId)
        .containsExactlyInAnyOrder(allocation.getId(), deallocation.getId());
    assertThat(publisher.published)
        .extracting(OutboxMessage::getEventType)
        .containsExactlyInAnyOrder(ALLOCATED, DEALLOCATED);
    assertThat(publisher.published)
        .allSatisfy(m -> assertThat(m.getPayload()).contains("trainerId").contains("t-1"));
    assertThat(reload(allocation).getStatus()).isEqualTo(OutboxStatus.COMPLETED);
    assertThat(reload(deallocation).getStatus()).isEqualTo(OutboxStatus.COMPLETED);
  }

  @Test
  void usAc2_transientFailuresAreRetriedUntilSuccess() {
    OutboxMessage allocation = saveEntry(ALLOCATED, "corr-retry");
    publisher.failures.set(2);

    processingService.processOutboxMessages();
    // US-001-ac-2: a failed attempt updates status and retry counter and schedules a retry
    OutboxMessage afterFirst = reload(allocation);
    assertThat(afterFirst.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(afterFirst.getRetryCount()).isEqualTo(1);
    assertThat(afterFirst.getNextRetryAt()).isNotNull();

    assertThat(processingService.retryFailedMessages()).isEqualTo(1);
    assertThat(reload(allocation).getStatus()).isEqualTo(OutboxStatus.PENDING);
    processingService.processOutboxMessages();
    assertThat(reload(allocation).getRetryCount()).isEqualTo(2);

    processUntilIdle();
    OutboxMessage finalState = reload(allocation);
    assertThat(finalState.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
    assertThat(finalState.getRetryCount()).isEqualTo(2);
    assertThat(publisher.published).hasSize(1);
  }

  @Test
  void usAc2_retriesStopAtConfiguredMaximum() {
    OutboxMessage deallocation = saveEntry(DEALLOCATED, "corr-exhaust");
    publisher.failures.set(Integer.MAX_VALUE);

    processUntilIdle();

    // US-001-ac-2: no further attempts after the maximum retry count is reached
    OutboxMessage finalState = reload(deallocation);
    assertThat(finalState.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
    assertThat(finalState.getRetryCount()).isEqualTo(3);
    assertThat(processingService.retryFailedMessages()).isZero();
    assertThat(publisher.published).isEmpty();
  }

  @Test
  void usAc3_successOutcomeIsPersistedForAudit() {
    OutboxMessage allocation = saveEntry(ALLOCATED, "corr-audit-ok");
    publisher.failures.set(1);

    processUntilIdle();

    // US-001-ac-3: final status, retry count, empty error, processed timestamp and correlation id
    OutboxMessage stored = reload(allocation);
    assertThat(stored.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
    assertThat(stored.getRetryCount()).isEqualTo(1);
    assertThat(stored.getLastError()).isNull();
    assertThat(stored.getProcessedAt()).isNotNull();
    assertThat(stored.getCorrelationId()).isEqualTo("corr-audit-ok");
  }

  @Test
  void usAc3_exhaustedOutcomeIsPersistedForAudit() throws Exception {
    OutboxMessage deallocation = saveEntry(DEALLOCATED, "corr-audit-fail");
    publisher.failures.set(Integer.MAX_VALUE);

    processUntilIdle();

    // US-001-ac-3: exhausted status, final retry count, last error, processed timestamp, corr id
    OutboxMessage stored = reload(deallocation);
    assertThat(stored.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
    assertThat(stored.getRetryCount()).isEqualTo(3);
    assertThat(stored.getLastError()).contains("message bus unavailable");
    assertThat(stored.getProcessedAt()).isNotNull();
    assertThat(stored.getCorrelationId()).isEqualTo("corr-audit-fail");

    // US-001-ac-3: the persisted outcome is queryable by monitoring/audit tools via the API
    mockMvc
        .perform(
            get("/api/v1/outbox/entries/" + deallocation.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_outbox:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DEAD_LETTER"))
        .andExpect(jsonPath("$.retryCount").value(3))
        .andExpect(jsonPath("$.processedAt").isNotEmpty());
  }

  @Test
  void usAc4_statsGroupTrainerEntriesByEventTypeAndStatus() throws Exception {
    saveEntry(ALLOCATED, "corr-s1");
    saveEntry(ALLOCATED, "corr-s2");
    saveEntry(DEALLOCATED, "corr-s3");
    processingService.processOutboxMessages(); // 2 allocation + 1 deallocation COMPLETED
    saveEntry(ALLOCATED, "corr-s4"); // PENDING
    OutboxMessage exhausted = saveEntry(DEALLOCATED, "corr-s5");
    exhausted.markAsFailed("broker down");
    exhausted.moveToDeadLetter();
    outboxRepository.save(exhausted); // DEAD_LETTER
    OutboxMessage failed = saveEntry(DEALLOCATED, "corr-s6");
    failed.markAsFailed("broker down");
    outboxRepository.save(failed); // FAILED
    saveEntry("WorkshopCompleted", "corr-other"); // not a trainer allocation event

    // US-001-ac-4: GET /api/v1/outbox/stats returns counts derived from the outbox table
    mockMvc
        .perform(
            get("/api/v1/outbox/stats")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_outbox:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalEntries").value(7))
        .andExpect(jsonPath("$.trainerAllocation.totalEntries").value(6))
        .andExpect(jsonPath("$.trainerAllocation.backlogCount").value(2))
        .andExpect(jsonPath("$.trainerAllocation.countsByEventType.allocation").value(3))
        .andExpect(jsonPath("$.trainerAllocation.countsByEventType.deallocation").value(3))
        .andExpect(jsonPath("$.trainerAllocation.countsByStatus.COMPLETED").value(3))
        .andExpect(jsonPath("$.trainerAllocation.countsByStatus.PENDING").value(1))
        .andExpect(jsonPath("$.trainerAllocation.countsByStatus.FAILED").value(1))
        .andExpect(jsonPath("$.trainerAllocation.countsByStatus.DEAD_LETTER").value(1))
        .andExpect(jsonPath("$.trainerAllocation.countsByStatus.PROCESSING").value(0))
        .andExpect(
            jsonPath("$.trainerAllocation.countsByEventTypeAndStatus.allocation.COMPLETED")
                .value(2))
        .andExpect(
            jsonPath("$.trainerAllocation.countsByEventTypeAndStatus.allocation.PENDING").value(1))
        .andExpect(
            jsonPath("$.trainerAllocation.countsByEventTypeAndStatus.deallocation.COMPLETED")
                .value(1))
        .andExpect(
            jsonPath("$.trainerAllocation.countsByEventTypeAndStatus.deallocation.FAILED").value(1))
        .andExpect(
            jsonPath("$.trainerAllocation.countsByEventTypeAndStatus.deallocation.DEAD_LETTER")
                .value(1));
  }

  @Test
  void usAc5_healthReflectsOutboxReadabilityAndMessageBusConnectivity() throws Exception {
    saveEntry(ALLOCATED, "corr-h1");

    // US-001-ac-5: /health exposes the outbox publishing component with backlog details
    mockMvc
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components.outboxPublishing.status").value("UP"))
        .andExpect(jsonPath("$.components.messageBroker.status").value("UP"));

    publisher.connected.set(false);

    // US-001-ac-5: losing the message bus is reported as unhealthy
    mockMvc
        .perform(get("/health"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"))
        .andExpect(jsonPath("$.components.outboxPublishing.status").value("DOWN"))
        .andExpect(jsonPath("$.components.messageBroker.status").value("DOWN"));
  }

  @Test
  void usAc5_metricsExposePublishingOutcomesAndBacklog() throws Exception {
    saveEntry(ALLOCATED, "corr-m1");
    saveEntry(DEALLOCATED, "corr-m2");
    publisher.failures.set(1);
    processingService.processOutboxMessages(); // one failure + one success
    saveEntry(ALLOCATED, "corr-m3");
    saveEntry(ALLOCATED, "corr-m4");

    String body =
        mockMvc
            .perform(get("/metrics"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // US-001-ac-5: success/failure counters and backlog gauges per event type
    assertThat(body)
        .containsPattern(
            metric("outbox_trainer_notification_publish_attempts_total", "outcome=\"success\""))
        .containsPattern(
            metric("outbox_trainer_notification_publish_attempts_total", "outcome=\"failure\""));
    assertThat(countBacklog(ALLOCATED) + countBacklog(DEALLOCATED)).isEqualTo(3);
    assertThat(body)
        .containsPattern(
            Pattern.compile(
                "outbox_trainer_notification_backlog\\{[^}]*eventCategory=\"allocation\"[^}]*\\} "
                    + (double) countBacklog(ALLOCATED)))
        .containsPattern(
            Pattern.compile(
                "outbox_trainer_notification_backlog\\{[^}]*eventCategory=\"deallocation\"[^}]*\\} "
                    + (double) countBacklog(DEALLOCATED)));
  }

  private static Pattern metric(String name, String tag) {
    return Pattern.compile(Pattern.quote(name) + "\\{[^}]*" + Pattern.quote(tag) + "[^}]*\\} \\d");
  }

  private long countBacklog(String eventType) {
    Long count =
        jdbcTemplate.queryForObject(
            "select count(*) from "
                + schema
                + ".outbox_message where event_type = ? and status in"
                + " ('PENDING','PROCESSING','FAILED')",
            Long.class,
            eventType);
    return count == null ? 0 : count;
  }
}
