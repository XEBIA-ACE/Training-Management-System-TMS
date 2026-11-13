package com.tms.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Transactional outbox record (C4 {@code OutboxMessage}; API model {@code OutboxEntry}). Table
 * {@code outbox_message} is created by {@code db/migration/V1__create_outbox_message.sql}.
 */
@Entity
@Table(name = "outbox_message")
public class OutboxMessage {

  @Id private UUID id;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private String aggregateId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false)
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OutboxStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "max_retries", nullable = false)
  private int maxRetries;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

  @Embedded private OutboxEventMetadata metadata;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected OutboxMessage() {}

  OutboxMessage(
      UUID id,
      String aggregateType,
      String aggregateId,
      String eventType,
      String payload,
      int maxRetries,
      OutboxEventMetadata metadata) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
    this.maxRetries = maxRetries;
    this.metadata = metadata;
    this.status = OutboxStatus.PENDING;
    this.createdAt = metadata.getTimestamp();
  }

  public void markAsProcessing() {
    this.status = OutboxStatus.PROCESSING;
  }

  public void markAsProcessed() {
    this.status = OutboxStatus.COMPLETED;
    this.processedAt = Instant.now();
    this.nextRetryAt = null;
    this.lastError = null;
  }

  public void markAsFailed(String error) {
    this.status = OutboxStatus.FAILED;
    this.lastError = error;
  }

  public void incrementRetry() {
    this.retryCount++;
  }

  public boolean isRetryable() {
    return retryCount < maxRetries;
  }

  public void scheduleRetry(Instant at) {
    this.nextRetryAt = at;
  }

  /** FAILED or DEAD_LETTER back to PENDING for reprocessing (manual recovery). */
  public void resetForRetry(Instant at) {
    if (status != OutboxStatus.FAILED && status != OutboxStatus.DEAD_LETTER) {
      throw new InvalidOutboxStateException(
          "Outbox entry " + id + " is not in a retryable state (status " + status + ")");
    }
    this.status = OutboxStatus.PENDING;
    this.nextRetryAt = at;
    this.processedAt = null;
  }

  /**
   * Terminal failure (retries exhausted or rejected). US-001 (FR-US-001-006): {@code processedAt}
   * records when the terminal outcome was reached; {@code retryCount} and {@code lastError} keep
   * the final attempt's values.
   */
  public void moveToDeadLetter() {
    this.status = OutboxStatus.DEAD_LETTER;
    this.nextRetryAt = null;
    this.processedAt = Instant.now();
  }

  /**
   * FAILED to SKIPPED. TODO SCAFFOLD-SVC-002: the export requires the skip reason to be audited but
   * defines no storage for it; persisting the reason is deferred.
   */
  public void skip() {
    if (status != OutboxStatus.FAILED) {
      throw new InvalidOutboxStateException(
          "Outbox entry " + id + " cannot be skipped (status " + status + ")");
    }
    this.status = OutboxStatus.SKIPPED;
    this.nextRetryAt = null;
  }

  public UUID getId() {
    return id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public Instant getNextRetryAt() {
    return nextRetryAt;
  }

  public String getLastError() {
    return lastError;
  }

  public String getCorrelationId() {
    return metadata != null ? metadata.getCorrelationId() : null;
  }

  public OutboxEventMetadata getMetadata() {
    return metadata;
  }
}
