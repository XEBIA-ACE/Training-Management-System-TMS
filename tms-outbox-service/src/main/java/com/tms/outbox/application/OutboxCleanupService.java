package com.tms.outbox.application;

import com.tms.outbox.config.OutboxProperties;
import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.domain.OutboxRepository;
import com.tms.outbox.infrastructure.OutboxTransactionManager;
import com.tms.outbox.infrastructure.messaging.DeadLetterHandler;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** C4 {@code OutboxCleanupService}: periodic housekeeping of the outbox table. */
@Service
public class OutboxCleanupService {

  private final OutboxRepository outboxRepository;
  private final OutboxTransactionManager transactionManager;
  private final DeadLetterHandler deadLetterHandler;
  private final int retentionDays;

  public OutboxCleanupService(
      OutboxRepository outboxRepository,
      OutboxTransactionManager transactionManager,
      DeadLetterHandler deadLetterHandler,
      OutboxProperties properties) {
    this.outboxRepository = outboxRepository;
    this.transactionManager = transactionManager;
    this.deadLetterHandler = deadLetterHandler;
    this.retentionDays = properties.cleanupRetentionDays();
  }

  /** Deletes COMPLETED messages older than the retention period; returns the deleted count. */
  public int cleanupProcessedMessages() {
    return outboxRepository.deleteCompletedBefore(
        Instant.now().minus(Duration.ofDays(retentionDays)));
  }

  /** TODO SCAFFOLD-SVC-002: archive target and policy are unspecified in the export. */
  public int archiveOldMessages() {
    return 0;
  }

  /** Dead-letters FAILED messages that have no retry attempts left; returns how many. */
  public int moveToDeadLetter() {
    return transactionManager.inTransaction(
        () -> {
          int count = 0;
          for (OutboxMessage message : outboxRepository.findFailedMessages()) {
            if (!message.isRetryable()) {
              deadLetterHandler.moveToDeadLetter(message);
              outboxRepository.save(message);
              count++;
            }
          }
          return count;
        });
  }
}
