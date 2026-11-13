package com.tms.outbox.application;

import com.tms.outbox.domain.OutboxStatus;
import java.util.EnumMap;
import java.util.Map;

/**
 * US-001: counts of trainer allocation/deallocation outbox entries per {@link
 * TrainerAllocationEventType} and {@link OutboxStatus}, read from the persisted outbox table
 * (FR-US-001-010). Shared by the stats API, the {@code outboxPublishing} health component and the
 * backlog gauges.
 *
 * <p>Status mapping to the spec vocabulary (FR-US-001-004): PENDING = pending, PROCESSING =
 * in-progress, COMPLETED = succeeded, FAILED = failed (retry scheduled), DEAD_LETTER = exhausted,
 * SKIPPED = manually skipped. OPEN QUESTION (US-001): the spec lists example values only; the
 * existing {@link OutboxStatus} contract is kept instead of introducing new values.
 */
public record TrainerAllocationOutboxSnapshot(
    Map<TrainerAllocationEventType, Map<OutboxStatus, Long>> counts) {

  /** Statuses that still need publishing work: pending, in-progress and failed-awaiting-retry. */
  public static final OutboxStatus[] BACKLOG_STATUSES = {
    OutboxStatus.PENDING, OutboxStatus.PROCESSING, OutboxStatus.FAILED
  };

  public TrainerAllocationOutboxSnapshot {
    Map<TrainerAllocationEventType, Map<OutboxStatus, Long>> filled =
        new EnumMap<>(TrainerAllocationEventType.class);
    for (TrainerAllocationEventType type : TrainerAllocationEventType.values()) {
      Map<OutboxStatus, Long> byStatus = new EnumMap<>(OutboxStatus.class);
      for (OutboxStatus status : OutboxStatus.values()) {
        Map<OutboxStatus, Long> source = counts != null ? counts.get(type) : null;
        byStatus.put(status, source != null ? source.getOrDefault(status, 0L) : 0L);
      }
      filled.put(type, byStatus);
    }
    counts = filled;
  }

  public long count(TrainerAllocationEventType type, OutboxStatus status) {
    return counts.get(type).get(status);
  }

  public long countByType(TrainerAllocationEventType type) {
    return counts.get(type).values().stream().mapToLong(Long::longValue).sum();
  }

  public long countByStatus(OutboxStatus status) {
    long total = 0;
    for (TrainerAllocationEventType type : TrainerAllocationEventType.values()) {
      total += count(type, status);
    }
    return total;
  }

  public long backlog(TrainerAllocationEventType type) {
    long total = 0;
    for (OutboxStatus status : BACKLOG_STATUSES) {
      total += count(type, status);
    }
    return total;
  }

  public long total() {
    long total = 0;
    for (TrainerAllocationEventType type : TrainerAllocationEventType.values()) {
      total += countByType(type);
    }
    return total;
  }
}
