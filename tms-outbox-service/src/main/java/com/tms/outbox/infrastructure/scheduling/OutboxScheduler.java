package com.tms.outbox.infrastructure.scheduling;

import com.tms.outbox.api.dto.ProcessorStatus;
import com.tms.outbox.application.OutboxCleanupService;
import com.tms.outbox.application.OutboxProcessingService;
import com.tms.outbox.config.OutboxProperties;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * C4 {@code OutboxScheduler} / C3 Outbox Poller (Spring Scheduler). Polls every {@code
 * tms.outbox.polling-interval-ms}; housekeeping runs on {@code tms.outbox.scheduler-cron}. All
 * scheduled work pauses while the processor is stopped.
 *
 * <p>TODO SCAFFOLD-SVC-002: C4 calls for distributed locking (ShedLock) for multi-instance
 * deployments; the lock provider is not part of the approved stack yet. Row claiming already uses
 * {@code FOR UPDATE SKIP LOCKED}. The running flag is per instance.
 */
@Component
public class OutboxScheduler {

  private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

  private final OutboxProcessingService outboxProcessingService;
  private final OutboxCleanupService outboxCleanupService;
  private final OutboxProperties properties;
  private final AtomicBoolean running;
  private final AtomicReference<Instant> lastRunAt = new AtomicReference<>();
  private final AtomicInteger entriesProcessedLastRun = new AtomicInteger();

  public OutboxScheduler(
      OutboxProcessingService outboxProcessingService,
      OutboxCleanupService outboxCleanupService,
      OutboxProperties properties) {
    this.outboxProcessingService = outboxProcessingService;
    this.outboxCleanupService = outboxCleanupService;
    this.properties = properties;
    this.running = new AtomicBoolean(properties.autoStart());
  }

  @Scheduled(
      fixedDelayString = "${tms.outbox.polling-interval-ms:5000}",
      initialDelayString = "${tms.outbox.polling-interval-ms:5000}")
  public void processOutboxScheduled() {
    if (!running.get()) {
      return;
    }
    try {
      outboxProcessingService.retryFailedMessages();
      entriesProcessedLastRun.set(outboxProcessingService.processOutboxMessages());
    } catch (RuntimeException e) {
      log.error("Outbox processing run failed", e);
    } finally {
      lastRunAt.set(Instant.now());
    }
  }

  public void retryFailedScheduled() {
    if (running.get()) {
      outboxProcessingService.retryFailedMessages();
    }
  }

  @Scheduled(cron = "${tms.outbox.scheduler-cron:0 0 * * * *}")
  public void cleanupScheduled() {
    if (!running.get()) {
      return;
    }
    try {
      outboxCleanupService.moveToDeadLetter();
      outboxCleanupService.cleanupProcessedMessages();
      outboxCleanupService.archiveOldMessages();
    } catch (RuntimeException e) {
      log.error("Outbox cleanup run failed", e);
    }
  }

  /** Returns {@code false} if the processor was already running. */
  public boolean start() {
    return running.compareAndSet(false, true);
  }

  /** Returns {@code false} if the processor was already stopped. */
  public boolean stop() {
    return running.compareAndSet(true, false);
  }

  public boolean isRunning() {
    return running.get();
  }

  public ProcessorStatus status() {
    Instant last = lastRunAt.get();
    Instant next = null;
    if (running.get()) {
      next = (last != null ? last : Instant.now()).plusMillis(properties.pollingIntervalMs());
    }
    return new ProcessorStatus(
        running.get(),
        last,
        next,
        properties.batchSize(),
        properties.pollingIntervalMs(),
        entriesProcessedLastRun.get());
  }
}
