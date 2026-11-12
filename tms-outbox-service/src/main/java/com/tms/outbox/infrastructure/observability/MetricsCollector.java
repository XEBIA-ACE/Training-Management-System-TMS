package com.tms.outbox.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * C3 Metrics Collector: Micrometer counters/timers for outbox processing (exported at /metrics).
 */
@Component
public class MetricsCollector {

  private final MeterRegistry registry;

  public MetricsCollector(MeterRegistry registry) {
    this.registry = registry;
  }

  public void recordPublished(String eventType) {
    registry.counter("outbox.messages.published", "eventType", eventType).increment();
  }

  public void recordFailed(String eventType) {
    registry.counter("outbox.messages.failed", "eventType", eventType).increment();
  }

  public void recordDeadLettered(String eventType) {
    registry.counter("outbox.messages.dead_lettered", "eventType", eventType).increment();
  }

  public void recordBatch(int size, Duration duration) {
    registry.timer("outbox.batch.duration").record(duration);
    registry.summary("outbox.batch.size").record(size);
  }
}
