package com.tms.outbox.application;

import com.tms.outbox.config.OutboxProperties;
import com.tms.outbox.config.OutboxProperties.PublisherType;
import com.tms.outbox.domain.EventFilter;
import com.tms.outbox.domain.IdempotencyChecker;
import com.tms.outbox.domain.OutboxEntryNotFoundException;
import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.domain.OutboxRepository;
import com.tms.outbox.infrastructure.OutboxTransactionManager;
import com.tms.outbox.infrastructure.messaging.DeadLetterHandler;
import com.tms.outbox.infrastructure.messaging.EventDispatcher;
import com.tms.outbox.infrastructure.messaging.RetryHandler;
import com.tms.outbox.infrastructure.observability.MetricsCollector;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * C4 {@code OutboxProcessingService} / C3 Batch Processor: claims due PENDING messages in batches,
 * filters, dispatches and records the delivery result (COMPLETED, FAILED with backoff, or
 * DEAD_LETTER).
 *
 * <p>Processing is skipped while {@code tms.outbox.publisher-type=none} so that no message is
 * failed or dead-lettered before a broker adapter exists. TODO SCAFFOLD-SVC-002: the publish
 * timeout and partition-aware polling are unspecified in the export.
 */
@Service
public class OutboxProcessingService {

  private static final Logger log = LoggerFactory.getLogger(OutboxProcessingService.class);
  private static final long PUBLISH_TIMEOUT_SECONDS = 30;

  private final OutboxRepository outboxRepository;
  private final OutboxProperties properties;
  private final OutboxTransactionManager transactionManager;
  private final EventFilter eventFilter;
  private final EventDispatcher eventDispatcher;
  private final IdempotencyChecker idempotencyChecker;
  private final RetryHandler retryHandler;
  private final DeadLetterHandler deadLetterHandler;
  private final MetricsCollector metricsCollector;

  public OutboxProcessingService(
      OutboxRepository outboxRepository,
      OutboxProperties properties,
      OutboxTransactionManager transactionManager,
      EventFilter eventFilter,
      EventDispatcher eventDispatcher,
      IdempotencyChecker idempotencyChecker,
      RetryHandler retryHandler,
      DeadLetterHandler deadLetterHandler,
      MetricsCollector metricsCollector) {
    this.outboxRepository = outboxRepository;
    this.properties = properties;
    this.transactionManager = transactionManager;
    this.eventFilter = eventFilter;
    this.eventDispatcher = eventDispatcher;
    this.idempotencyChecker = idempotencyChecker;
    this.retryHandler = retryHandler;
    this.deadLetterHandler = deadLetterHandler;
    this.metricsCollector = metricsCollector;
  }

  /** Processes one batch; returns the number of successfully published messages. */
  public int processOutboxMessages() {
    if (properties.publisherType() == PublisherType.NONE) {
      log.debug("Outbox processing skipped: no message publisher configured");
      return 0;
    }
    long start = System.nanoTime();
    List<OutboxMessage> batch =
        transactionManager.inTransaction(
            () -> {
              List<OutboxMessage> claimed =
                  outboxRepository.findPendingMessages(properties.batchSize());
              claimed.forEach(
                  message -> {
                    message.markAsProcessing();
                    outboxRepository.save(message);
                  });
              return claimed;
            });
    int processed = 0;
    for (OutboxMessage message : batch) {
      if (process(message)) {
        processed++;
      }
    }
    metricsCollector.recordBatch(batch.size(), Duration.ofNanos(System.nanoTime() - start));
    return processed;
  }

  public boolean processSingleMessage(UUID id) {
    OutboxMessage message =
        outboxRepository.findById(id).orElseThrow(() -> new OutboxEntryNotFoundException(id));
    return process(message);
  }

  /** Moves FAILED messages whose backoff elapsed back to PENDING; returns how many. */
  public int retryFailedMessages() {
    Instant now = Instant.now();
    return transactionManager.inTransaction(
        () -> {
          int count = 0;
          for (OutboxMessage message : outboxRepository.findFailedMessages()) {
            if (message.isRetryable()
                && (message.getNextRetryAt() == null || !message.getNextRetryAt().isAfter(now))) {
              message.resetForRetry(message.getNextRetryAt());
              outboxRepository.save(message);
              count++;
            }
          }
          return count;
        });
  }

  /** Manual retry of a FAILED or DEAD_LETTER entry (POST /entries/{entryId}/retry). */
  public OutboxMessage retryMessage(UUID id) {
    return transactionManager.inTransaction(
        () -> {
          OutboxMessage message =
              outboxRepository.findById(id).orElseThrow(() -> new OutboxEntryNotFoundException(id));
          message.resetForRetry(Instant.now());
          return outboxRepository.save(message);
        });
  }

  /** Marks a FAILED entry as SKIPPED (POST /entries/{entryId}/skip). */
  public OutboxMessage skipMessage(UUID id, String reason) {
    return transactionManager.inTransaction(
        () -> {
          OutboxMessage message =
              outboxRepository.findById(id).orElseThrow(() -> new OutboxEntryNotFoundException(id));
          message.skip();
          // TODO SCAFFOLD-SVC-002: persist the skip reason once an audit store is specified.
          log.info("Outbox entry {} skipped: {}", id, reason);
          return outboxRepository.save(message);
        });
  }

  private boolean process(OutboxMessage message) {
    Optional<String> rejection = eventFilter.validate(message);
    if (rejection.isPresent()) {
      message.markAsFailed("Rejected by event filter: " + rejection.get());
      deadLetterHandler.moveToDeadLetter(message);
      transactionManager.inTransaction(() -> outboxRepository.save(message));
      return false;
    }
    boolean published = publishWithRetry(message);
    transactionManager.inTransaction(() -> outboxRepository.save(message));
    return published;
  }

  private boolean publishWithRetry(OutboxMessage message) {
    try {
      eventDispatcher.dispatch(message).get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      message.markAsProcessed();
      idempotencyChecker.markProcessed(message);
      metricsCollector.recordPublished(message.getEventType());
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      handlePublishFailure(message, e);
      return false;
    } catch (Exception e) {
      handlePublishFailure(message, e);
      return false;
    }
  }

  private void handlePublishFailure(OutboxMessage message, Exception ex) {
    metricsCollector.recordFailed(message.getEventType());
    boolean willRetry = retryHandler.onFailure(message, ex);
    log.warn(
        "Publishing outbox message {} ({}) failed, attempt {}/{}, {}",
        message.getId(),
        message.getEventType(),
        message.getRetryCount(),
        message.getMaxRetries(),
        willRetry ? "retry scheduled" : "dead-lettered");
  }
}
