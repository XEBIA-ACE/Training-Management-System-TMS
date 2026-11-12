package com.tms.outbox.health;

import com.tms.outbox.config.OutboxProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Actuator component {@code messageBroker}. TODO SCAFFOLD-SVC-002: no broker client is bound yet
 * (see publisher adapters), so the status is UNKNOWN and does not affect the aggregate status.
 */
@Component("messageBrokerHealthIndicator")
public class MessageBrokerHealthIndicator implements HealthIndicator {

  private final OutboxProperties properties;

  public MessageBrokerHealthIndicator(OutboxProperties properties) {
    this.properties = properties;
  }

  @Override
  public Health health() {
    return Health.unknown()
        .withDetail("publisherType", properties.publisherType().name().toLowerCase())
        .build();
  }
}
