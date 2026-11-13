package com.tms.outbox.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tms.outbox.config.OutboxProperties;
import com.tms.outbox.config.OutboxProperties.PublisherType;
import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.domain.OutboxMessageFactory;
import com.tms.outbox.domain.OutboxStatus;
import com.tms.outbox.infrastructure.observability.MetricsCollector;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** US-001-ac-2 (T003/T004/T014): retry handling with exponential backoff for trainer events. */
class RetryHandlerTest {

  private final MetricsCollector metricsCollector = mock(MetricsCollector.class);
  private final RetryHandler retryHandler =
      new RetryHandler(
          new OutboxProperties(
              100, 5000, "0 0 * * * *", 3, 1000, 7, PublisherType.RABBITMQ, 1000, false),
          new DeadLetterHandler(metricsCollector));

  private static OutboxMessage allocationEntry(int maxRetries) {
    return OutboxMessageFactory.create(
        "TrainerAllocation",
        "allocation-1",
        "TrainerAllocated",
        "{\"trainerId\":\"t-1\"}",
        maxRetries,
        "tms-core-service",
        "corr-ac2",
        null);
  }

  @Test
  void usAc2_backoffDoublesWithEachRetry() {
    // US-001-ac-2: retryBackoffMs * 2^(retryCount - 1)
    assertThat(retryHandler.backoff(1)).isEqualTo(Duration.ofSeconds(1));
    assertThat(retryHandler.backoff(2)).isEqualTo(Duration.ofSeconds(2));
    assertThat(retryHandler.backoff(3)).isEqualTo(Duration.ofSeconds(4));
    assertThat(retryHandler.backoff(4)).isEqualTo(Duration.ofSeconds(8));
  }

  @Test
  void usAc2_transientFailureUpdatesStatusRetryCountAndSchedulesBackoff() {
    OutboxMessage message = allocationEntry(3);
    Instant before = Instant.now();

    boolean willRetry = retryHandler.onFailure(message, new IllegalStateException("broker down"));

    // US-001-ac-2: status and retry counter are updated on each attempt
    assertThat(willRetry).isTrue();
    assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(message.getRetryCount()).isEqualTo(1);
    assertThat(message.getLastError()).contains("broker down");
    assertThat(message.getNextRetryAt()).isAfterOrEqualTo(before.plusSeconds(1));

    retryHandler.onFailure(message, new IllegalStateException("broker down again"));
    assertThat(message.getRetryCount()).isEqualTo(2);
    assertThat(message.getNextRetryAt()).isAfterOrEqualTo(before.plusSeconds(2));
    verifyNoInteractions(metricsCollector);
  }

  @Test
  void usAc2_exhaustedRetriesMoveEntryToTerminalState() {
    OutboxMessage message = allocationEntry(2);

    retryHandler.onFailure(message, new IllegalStateException("first"));
    boolean willRetry = retryHandler.onFailure(message, new IllegalStateException("last"));

    // US-001-ac-2: retrying stops once the configured maximum is reached
    assertThat(willRetry).isFalse();
    assertThat(message.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
    assertThat(message.getRetryCount()).isEqualTo(2);
    assertThat(message.getLastError()).contains("last");
    assertThat(message.getNextRetryAt()).isNull();
    assertThat(message.getProcessedAt()).isNotNull();
    verify(metricsCollector).recordDeadLettered("TrainerAllocated");
  }
}
