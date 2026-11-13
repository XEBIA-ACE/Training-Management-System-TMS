package com.tms.outbox.infrastructure.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tms.outbox.application.TrainerAllocationEventClassifier;
import com.tms.outbox.config.AclEvaluationProperties;
import com.tms.outbox.config.TrainerNotificationProperties;
import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.domain.OutboxMessageFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * US-001 (T006, T007, T009): ACL gate in front of trainer allocation/deallocation sends. The ACL
 * service itself (SVC-003) is not part of this repository, so it is replaced by a stub client.
 */
class EmailAclClientTest {

  private static final String SECRET_BODY = "Dear Trainer, you were allocated to workshop W-42";

  private final List<AclEvaluationRequest> requests = new ArrayList<>();
  private final List<OutboxMessage> sent = new ArrayList<>();
  private AclEvaluationResult decision;
  private ListAppender<ILoggingEvent> auditEvents;
  private EmailAclClient client;

  @BeforeEach
  void setUp() {
    AclEvaluationProperties props =
        new AclEvaluationProperties("http://acl", "EMAIL", "smtp-gw-1", "notifications", "SEND_EMAIL", 200);
    TrainerAllocationEventClassifier classifier =
        new TrainerAllocationEventClassifier(
            new TrainerNotificationProperties(
                List.of("TrainerAllocated"), List.of("TrainerDeallocated"), 0));
    client =
        new EmailAclClient(
            classifier,
            request -> {
              requests.add(request);
              return decision;
            },
            new AclAuditLogger(),
            message -> {
              sent.add(message);
              return CompletableFuture.completedFuture(null);
            },
            props);
    auditEvents = new ListAppender<>();
    auditEvents.start();
    ((Logger) LoggerFactory.getLogger("com.tms.outbox.acl.audit")).addAppender(auditEvents);
  }

  @AfterEach
  void tearDown() {
    ((Logger) LoggerFactory.getLogger("com.tms.outbox.acl.audit")).detachAppender(auditEvents);
  }

  private static OutboxMessage message(String eventType) {
    return OutboxMessageFactory.create(
        "TrainerAllocation",
        "alloc-1",
        eventType,
        "{\"body\":\"" + SECRET_BODY + "\"}",
        3,
        "tms-core",
        "corr-1",
        null);
  }

  /** [US-001-ac-1] evaluate is called with the specified contract before send; allow proceeds. */
  @Test
  void us001Ac1_allowedEvaluationSendsAfterEvaluateWithSpecifiedFields() throws Exception {
    decision = new AclEvaluationResult(true, null);

    client.deliver(message("TrainerAllocated")).get();
    client.deliver(message("TrainerDeallocated")).get();

    assertThat(requests)
        .hasSize(2)
        .allSatisfy(
            r ->
                assertThat(r)
                    .isEqualTo(
                        new AclEvaluationRequest(
                            "EMAIL", "smtp-gw-1", "notifications", "SEND_EMAIL")));
    assertThat(sent).hasSize(2);
  }

  /** [US-001-ac-1] only trainer allocation/deallocation events are handled by the gate. */
  @Test
  void us001Ac1_supportsOnlyTrainerAllocationAndDeallocationEvents() {
    assertThat(client.supports("TrainerAllocated")).isTrue();
    assertThat(client.supports("TrainerDeallocated")).isTrue();
    assertThat(client.supports("WorkshopCreated")).isFalse();
  }

  /** [US-001-ac-2] deny surfaces the reason and never reaches the provider. */
  @Test
  void us001Ac2_deniedEvaluationFailsWithReasonAndDoesNotSend() {
    decision = new AclEvaluationResult(false, "Daily quota exceeded for EMAIL/SEND_EMAIL");

    assertThatThrownBy(() -> client.deliver(message("TrainerAllocated")).get())
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(AclDeniedException.class)
        .hasMessageContaining("Daily quota exceeded for EMAIL/SEND_EMAIL");
    assertThat(sent).isEmpty();
  }

  /** [US-001-ac-3] limits are enforced server-side; a quota/rate denial blocks every retry. */
  @Test
  void us001Ac3_quotaDenialIsReEvaluatedOnEachRetryAndNeverSends() {
    decision = new AclEvaluationResult(false, "Rate limit of 60/min exceeded");

    for (int attempt = 0; attempt < 3; attempt++) {
      assertThatThrownBy(() -> client.deliver(message("TrainerDeallocated")).get())
          .hasCauseInstanceOf(AclDeniedException.class);
    }

    assertThat(requests).hasSize(3);
    assertThat(sent).isEmpty();
  }

  /** [US-001-ac-4] every evaluation (allow and deny) is audited without notification content. */
  @Test
  void us001Ac4_everyEvaluationIsAuditedWithoutNotificationContent() throws Exception {
    decision = new AclEvaluationResult(true, null);
    client.deliver(message("TrainerAllocated")).get();
    decision = new AclEvaluationResult(false, "Not permitted");
    assertThatThrownBy(() -> client.deliver(message("TrainerAllocated")).get())
        .hasCauseInstanceOf(AclDeniedException.class);

    List<String> lines = auditEvents.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    assertThat(lines).hasSize(2);
    assertThat(lines.get(0))
        .contains("internalModule=notifications", "providerType=EMAIL", "providerId=smtp-gw-1")
        .contains("operation=SEND_EMAIL", "allowed=true");
    assertThat(lines.get(1)).contains("allowed=false", "reason=Not permitted");
    assertThat(lines).noneMatch(l -> l.contains(SECRET_BODY) || l.contains("alloc-1"));
  }
}
