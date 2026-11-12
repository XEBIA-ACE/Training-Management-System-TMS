package com.tms.outbox.infrastructure.messaging;

import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.infrastructure.observability.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * C3 Dead Letter Handler: parks messages that exceeded the retry limit (status DEAD_LETTER).
 *
 * <p>TODO SCAFFOLD-SVC-002: separate dead-letter storage/queue routing is unspecified in the
 * export; dead-lettered rows stay in {@code outbox_message} and can be retried manually.
 */
@Component
public class DeadLetterHandler {

  private static final Logger log = LoggerFactory.getLogger(DeadLetterHandler.class);

  private final MetricsCollector metricsCollector;

  public DeadLetterHandler(MetricsCollector metricsCollector) {
    this.metricsCollector = metricsCollector;
  }

  public void moveToDeadLetter(OutboxMessage message) {
    message.moveToDeadLetter();
    metricsCollector.recordDeadLettered(message.getEventType());
    log.warn(
        "Outbox message {} ({}) moved to dead letter after {} attempts",
        message.getId(),
        message.getEventType(),
        message.getRetryCount());
  }
}
