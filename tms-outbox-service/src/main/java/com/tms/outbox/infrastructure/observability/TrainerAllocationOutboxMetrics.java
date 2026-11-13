package com.tms.outbox.infrastructure.observability;

import com.tms.outbox.application.OutboxMonitoringService;
import com.tms.outbox.application.TrainerAllocationEventType;
import com.tms.outbox.application.TrainerAllocationOutboxSnapshot;
import com.tms.outbox.config.TrainerNotificationProperties;
import com.tms.outbox.domain.OutboxStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * US-001 (FR-US-001-012): DB-derived gauges for trainer allocation/deallocation outbox entries,
 * exported at {@code /metrics}:
 *
 * <ul>
 *   <li>{@code outbox.trainer_notification.entries} (tags {@code eventCategory}, {@code status})
 *   <li>{@code outbox.trainer_notification.backlog} (tag {@code eventCategory}): PENDING +
 *       PROCESSING + FAILED
 * </ul>
 *
 * <p>Values come from {@link OutboxMonitoringService#getTrainerAllocationSnapshot()}, cached for
 * {@code tms.outbox.trainer-notifications.metrics-cache-ttl-ms} so one scrape runs one query. When
 * the outbox table cannot be read the gauges report NaN.
 */
@Component
public class TrainerAllocationOutboxMetrics {

  static final String ENTRIES = "outbox.trainer_notification.entries";
  static final String BACKLOG = "outbox.trainer_notification.backlog";

  private static final Logger log = LoggerFactory.getLogger(TrainerAllocationOutboxMetrics.class);

  private final OutboxMonitoringService outboxMonitoringService;
  private final long cacheTtlNanos;
  private TrainerAllocationOutboxSnapshot snapshot;
  private long snapshotAtNanos;

  public TrainerAllocationOutboxMetrics(
      OutboxMonitoringService outboxMonitoringService,
      TrainerNotificationProperties properties,
      MeterRegistry registry) {
    this.outboxMonitoringService = outboxMonitoringService;
    this.cacheTtlNanos = properties.metricsCacheTtlMs() * 1_000_000L;
    for (TrainerAllocationEventType type : TrainerAllocationEventType.values()) {
      for (OutboxStatus status : OutboxStatus.values()) {
        register(
            registry,
            ENTRIES,
            "Trainer allocation/deallocation outbox entries by status",
            s -> s.count(type, status),
            "eventCategory",
            type.key(),
            "status",
            status.name().toLowerCase(Locale.ROOT));
      }
      register(
          registry,
          BACKLOG,
          "Trainer allocation/deallocation outbox entries awaiting publication",
          s -> s.backlog(type),
          "eventCategory",
          type.key());
    }
  }

  private void register(
      MeterRegistry registry,
      String name,
      String description,
      ToDoubleFunction<TrainerAllocationOutboxSnapshot> value,
      String... tags) {
    Gauge.builder(
            name,
            this,
            metrics -> {
              TrainerAllocationOutboxSnapshot current = metrics.currentSnapshot();
              return current != null ? value.applyAsDouble(current) : Double.NaN;
            })
        .description(description)
        .tags(tags)
        .register(registry);
  }

  synchronized TrainerAllocationOutboxSnapshot currentSnapshot() {
    long now = System.nanoTime();
    if (snapshot == null || now - snapshotAtNanos >= cacheTtlNanos) {
      try {
        snapshot = outboxMonitoringService.getTrainerAllocationSnapshot();
      } catch (RuntimeException e) {
        log.warn("Unable to read trainer allocation outbox counts for metrics", e);
        snapshot = null;
      }
      snapshotAtNanos = now;
    }
    return snapshot;
  }
}
