package com.tms.outbox.infrastructure.messaging;

import com.tms.outbox.config.OutboxProperties;
import com.tms.outbox.domain.OutboxMessage;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * C3 Retry Handler: records a failed delivery attempt and schedules the next one with exponential
 * backoff ({@code retryBackoffMs * 2^(retryCount-1)}).
 *
 * <p>TODO SCAFFOLD-SVC-002: jitter, max backoff and the transient/permanent error classification
 * are unspecified in the export; the exponent is capped only to avoid overflow.
 */
@Component
public class RetryHandler {

  private static final int MAX_EXPONENT = 20;

  private final OutboxProperties properties;
  private final DeadLetterHandler deadLetterHandler;

  public RetryHandler(OutboxProperties properties, DeadLetterHandler deadLetterHandler) {
    this.properties = properties;
    this.deadLetterHandler = deadLetterHandler;
  }

  /** Returns {@code true} when another attempt was scheduled, {@code false} if dead-lettered. */
  public boolean onFailure(OutboxMessage message, Throwable error) {
    message.incrementRetry();
    message.markAsFailed(describe(error));
    if (!message.isRetryable()) {
      deadLetterHandler.moveToDeadLetter(message);
      return false;
    }
    message.scheduleRetry(Instant.now().plus(backoff(message.getRetryCount())));
    return true;
  }

  public Duration backoff(int retryCount) {
    int exponent = Math.min(Math.max(retryCount - 1, 0), MAX_EXPONENT);
    return Duration.ofMillis(properties.retryBackoffMs() * (1L << exponent));
  }

  private static String describe(Throwable error) {
    Throwable root = error;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    String message = root.getClass().getSimpleName() + ": " + root.getMessage();
    return message.length() > 2000 ? message.substring(0, 2000) : message;
  }
}
