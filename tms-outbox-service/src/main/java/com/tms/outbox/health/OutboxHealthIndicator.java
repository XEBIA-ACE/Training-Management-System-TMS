package com.tms.outbox.health;

import com.tms.outbox.application.OutboxMonitoringService;
import com.tms.outbox.config.OutboxProperties;
import com.tms.outbox.infrastructure.scheduling.OutboxScheduler;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * C4 {@code OutboxHealthIndicator} (Actuator component {@code processor}): reports the processor
 * state and pending backlog.
 *
 * <p>TODO SCAFFOLD-SVC-002: the export does not define whether a backlog above {@code
 * pendingThreshold} should turn the service DOWN; it is only reported as a detail.
 */
@Component("processorHealthIndicator")
public class OutboxHealthIndicator implements HealthIndicator {

  private final OutboxMonitoringService outboxMonitoringService;
  private final OutboxScheduler outboxScheduler;
  private final long pendingThreshold;

  public OutboxHealthIndicator(
      OutboxMonitoringService outboxMonitoringService,
      OutboxScheduler outboxScheduler,
      OutboxProperties properties) {
    this.outboxMonitoringService = outboxMonitoringService;
    this.outboxScheduler = outboxScheduler;
    this.pendingThreshold = properties.pendingThreshold();
  }

  @Override
  public Health health() {
    long pending = outboxMonitoringService.getPendingCount();
    return Health.up()
        .withDetail("running", outboxScheduler.isRunning())
        .withDetail("pendingCount", pending)
        .withDetail("pendingThreshold", pendingThreshold)
        .withDetail("backlogExceeded", pending > pendingThreshold)
        .build();
  }
}
