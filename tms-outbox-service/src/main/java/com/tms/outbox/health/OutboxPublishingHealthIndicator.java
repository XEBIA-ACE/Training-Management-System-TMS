package com.tms.outbox.health;

import com.tms.outbox.application.OutboxMonitoringService;
import com.tms.outbox.application.TrainerAllocationEventType;
import com.tms.outbox.application.TrainerAllocationOutboxSnapshot;
import com.tms.outbox.config.OutboxProperties;
import com.tms.outbox.config.OutboxProperties.PublisherType;
import com.tms.outbox.domain.MessagePublisher;
import com.tms.outbox.domain.OutboxStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * US-001 (FR-US-001-011): Actuator component {@code outboxPublishing}, aggregated into {@code
 * /health}. Reflects whether trainer allocation/deallocation events can be published:
 *
 * <ul>
 *   <li>DOWN when the transactional outbox table cannot be read, or a publisher is configured but
 *       the message bus is not reachable ({@link MessagePublisher#isConnected()});
 *   <li>UNKNOWN when {@code publisher-type=none} (processing is disabled, see {@code
 *       OutboxProcessingService});
 *   <li>UP otherwise.
 * </ul>
 *
 * <p>Details report the backlog (PENDING + PROCESSING + FAILED) and exhausted (DEAD_LETTER) counts
 * per event type. OPEN QUESTION (US-001): the spec defines no backlog/exhausted thresholds that
 * should turn the status DOWN, so these counts are reported as details only.
 */
@Component
public class OutboxPublishingHealthIndicator implements HealthIndicator {

  private final OutboxMonitoringService outboxMonitoringService;
  private final MessagePublisher messagePublisher;
  private final OutboxProperties properties;

  public OutboxPublishingHealthIndicator(
      OutboxMonitoringService outboxMonitoringService,
      MessagePublisher messagePublisher,
      OutboxProperties properties) {
    this.outboxMonitoringService = outboxMonitoringService;
    this.messagePublisher = messagePublisher;
    this.properties = properties;
  }

  @Override
  public Health health() {
    String publisherType = properties.publisherType().name().toLowerCase();
    TrainerAllocationOutboxSnapshot snapshot;
    try {
      snapshot = outboxMonitoringService.getTrainerAllocationSnapshot();
    } catch (RuntimeException e) {
      return Health.down()
          .withDetail("outboxReadable", false)
          .withDetail("publisherType", publisherType)
          .withDetail("error", e.getClass().getSimpleName())
          .build();
    }
    boolean configured = properties.publisherType() != PublisherType.NONE;
    boolean connected = configured && messagePublisher.isConnected();
    Health.Builder builder;
    if (!configured) {
      builder = Health.unknown();
    } else if (connected) {
      builder = Health.up();
    } else {
      builder = Health.down();
    }
    Map<String, Long> backlog = new LinkedHashMap<>();
    Map<String, Long> exhausted = new LinkedHashMap<>();
    for (TrainerAllocationEventType type : TrainerAllocationEventType.values()) {
      backlog.put(type.key(), snapshot.backlog(type));
      exhausted.put(type.key(), snapshot.count(type, OutboxStatus.DEAD_LETTER));
    }
    return builder
        .withDetail("outboxReadable", true)
        .withDetail("publisherType", publisherType)
        .withDetail("messageBusConnected", connected)
        .withDetail("backlog", backlog)
        .withDetail("exhausted", exhausted)
        .build();
  }
}
