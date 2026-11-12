package com.tms.outbox.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Outbox tuning bound from {@code tms.outbox.*} (C4 {@code OutboxProperties}; values come from
 * {@code TMS_OUTBOX_*} environment variables, see application.yml).
 *
 * @param batchSize messages claimed per processing run
 * @param pollingIntervalMs delay between polling runs (ProcessorStatus.pollingIntervalMs)
 * @param schedulerCron cron for housekeeping (cleanup / dead-letter sweep). GAP: C4 names a single
 *     {@code schedulerCron}; polling uses {@code pollingIntervalMs} from the ProcessorStatus
 *     contract
 * @param retryMaxAttempts max delivery attempts before dead-lettering (OutboxEntry.maxRetries)
 * @param retryBackoffMs base delay of the exponential backoff
 * @param cleanupRetentionDays retention of COMPLETED messages
 * @param publisherType broker adapter: {@code none}, {@code rabbitmq} or {@code kafka}
 * @param pendingThreshold backlog size above which the processor health reports a backlog
 * @param autoStart whether the processor is running at startup
 */
@Validated
@ConfigurationProperties("tms.outbox")
public record OutboxProperties(
    @DefaultValue("100") @Min(1) int batchSize,
    @DefaultValue("5000") @Min(100) long pollingIntervalMs,
    @DefaultValue("0 0 * * * *") @NotBlank String schedulerCron,
    @DefaultValue("5") @Min(0) int retryMaxAttempts,
    @DefaultValue("1000") @Min(0) long retryBackoffMs,
    @DefaultValue("7") @Min(1) int cleanupRetentionDays,
    @DefaultValue("none") PublisherType publisherType,
    @DefaultValue("1000") @Min(0) long pendingThreshold,
    @DefaultValue("true") boolean autoStart) {

  public enum PublisherType {
    NONE,
    RABBITMQ,
    KAFKA
  }
}
