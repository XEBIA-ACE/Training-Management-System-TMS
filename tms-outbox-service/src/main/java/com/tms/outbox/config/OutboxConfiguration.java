package com.tms.outbox.config;

import com.tms.outbox.domain.MessagePublisher;
import com.tms.outbox.infrastructure.publisher.KafkaMessagePublisher;
import com.tms.outbox.infrastructure.publisher.RabbitMQMessagePublisher;
import com.tms.outbox.infrastructure.publisher.UnconfiguredMessagePublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the {@link MessagePublisher} adapter selected by {@code tms.outbox.publisher-type}. */
@Configuration
public class OutboxConfiguration {

  @Bean
  public MessagePublisher messagePublisher(OutboxProperties properties) {
    return switch (properties.publisherType()) {
      case RABBITMQ -> new RabbitMQMessagePublisher();
      case KAFKA -> new KafkaMessagePublisher();
      case NONE -> new UnconfiguredMessagePublisher();
    };
  }
}
