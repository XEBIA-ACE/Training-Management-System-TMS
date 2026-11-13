package com.tms.outbox.health;

import com.tms.outbox.config.OutboxProperties;
import com.tms.outbox.config.OutboxProperties.PublisherType;
import com.tms.outbox.domain.MessagePublisher;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Actuator component {@code messageBroker}: UNKNOWN while {@code publisher-type=none} (processing
 * is disabled), otherwise UP/DOWN from {@link MessagePublisher#isConnected()} (US-001,
 * FR-US-001-011). TODO SCAFFOLD-SVC-002: the RabbitMQ/Kafka adapters are stubs without a broker
 * client, so they report DOWN until a client is bound.
 */
@Component("messageBrokerHealthIndicator")
public class MessageBrokerHealthIndicator implements HealthIndicator {

  private final OutboxProperties properties;
  private final MessagePublisher messagePublisher;

  public MessageBrokerHealthIndicator(
      OutboxProperties properties, MessagePublisher messagePublisher) {
    this.properties = properties;
    this.messagePublisher = messagePublisher;
  }

  @Override
  public Health health() {
    String publisherType = properties.publisherType().name().toLowerCase();
    if (properties.publisherType() == PublisherType.NONE) {
      return Health.unknown().withDetail("publisherType", publisherType).build();
    }
    Health.Builder builder = messagePublisher.isConnected() ? Health.up() : Health.down();
    return builder.withDetail("publisherType", publisherType).build();
  }
}
