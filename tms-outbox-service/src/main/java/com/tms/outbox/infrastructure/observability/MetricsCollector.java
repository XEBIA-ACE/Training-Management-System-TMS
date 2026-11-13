package com.tms.outbox.infrastructure.observability;

import com.tms.outbox.application.TrainerAllocationEventClassifier;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * C3 Metrics Collector: Micrometer counters/timers for outbox processing (exported at /metrics).
 *
 * <p>US-001 (FR-US-001-012): trainer allocation/deallocation events additionally increment {@code
 * outbox.trainer_notification.publish.attempts} (tags {@code eventCategory}, {@code outcome} =
 * success|failure) and {@code outbox.trainer_notification.exhausted} (tag {@code eventCategory}) so
 * success/failure rates can be derived per event type. Backlog gauges are registered by {@link
 * TrainerAllocationOutboxMetrics}.
 */
@Component
public class MetricsCollector {

  static final String TRAINER_PUBLISH_ATTEMPTS = "outbox.trainer_notification.publish.attempts";
  static final String TRAINER_EXHAUSTED = "outbox.trainer_notification.exhausted";

  private final MeterRegistry registry;
  private final TrainerAllocationEventClassifier trainerAllocationEventClassifier;

  public MetricsCollector(
      MeterRegistry registry, TrainerAllocationEventClassifier trainerAllocationEventClassifier) {
    this.registry = registry;
    this.trainerAllocationEventClassifier = trainerAllocationEventClassifier;
  }

  public void recordPublished(String eventType) {
    registry.counter("outbox.messages.published", "eventType", eventType).increment();
    recordTrainerAttempt(eventType, "success");
  }

  public void recordFailed(String eventType) {
    registry.counter("outbox.messages.failed", "eventType", eventType).increment();
    recordTrainerAttempt(eventType, "failure");
  }

  public void recordDeadLettered(String eventType) {
    registry.counter("outbox.messages.dead_lettered", "eventType", eventType).increment();
    trainerAllocationEventClassifier
        .classify(eventType)
        .ifPresent(
            type -> registry.counter(TRAINER_EXHAUSTED, "eventCategory", type.key()).increment());
  }

  public void recordBatch(int size, Duration duration) {
    registry.timer("outbox.batch.duration").record(duration);
    registry.summary("outbox.batch.size").record(size);
  }

  private void recordTrainerAttempt(String eventType, String outcome) {
    trainerAllocationEventClassifier
        .classify(eventType)
        .ifPresent(
            type ->
                registry
                    .counter(
                        TRAINER_PUBLISH_ATTEMPTS, "eventCategory", type.key(), "outcome", outcome)
                    .increment());
  }
}
