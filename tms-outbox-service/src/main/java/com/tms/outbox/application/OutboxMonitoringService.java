package com.tms.outbox.application;

import com.tms.outbox.api.dto.OutboxStatistics;
import com.tms.outbox.api.dto.TrainerAllocationOutboxStatistics;
import com.tms.outbox.domain.OutboxEntryFilter;
import com.tms.outbox.domain.OutboxEntryNotFoundException;
import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.domain.OutboxRepository;
import com.tms.outbox.domain.OutboxStatus;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/** C4 {@code OutboxMonitoringService}: read-side queries, statistics and metrics. */
@Service
public class OutboxMonitoringService {

  private static final Instant MIN_DATE = Instant.EPOCH;
  private static final Instant MAX_DATE = Instant.parse("9999-12-31T23:59:59Z");

  private final OutboxRepository outboxRepository;
  private final TrainerAllocationEventClassifier trainerAllocationEventClassifier;

  public OutboxMonitoringService(
      OutboxRepository outboxRepository,
      TrainerAllocationEventClassifier trainerAllocationEventClassifier) {
    this.outboxRepository = outboxRepository;
    this.trainerAllocationEventClassifier = trainerAllocationEventClassifier;
  }

  public OutboxMetricsDTO getOutboxMetrics() {
    return new OutboxMetricsDTO(
        outboxRepository.countByStatus(OutboxStatus.PENDING),
        outboxRepository.countByStatus(OutboxStatus.PROCESSING),
        outboxRepository.countByStatus(OutboxStatus.COMPLETED),
        outboxRepository.countByStatus(OutboxStatus.FAILED),
        outboxRepository.countByStatus(OutboxStatus.DEAD_LETTER),
        outboxRepository.averageProcessingTimeMs(MIN_DATE, MAX_DATE));
  }

  public long getPendingCount() {
    return outboxRepository.countByStatus(OutboxStatus.PENDING);
  }

  public long getFailedCount() {
    return outboxRepository.countByStatus(OutboxStatus.FAILED);
  }

  /** TODO SCAFFOLD-SVC-002: the processing-rate window/definition is unspecified in the export. */
  public double getProcessingRate() {
    return 0.0;
  }

  public Page<OutboxMessage> findEntries(OutboxEntryFilter filter, int page, int size) {
    return outboxRepository.findEntries(filter, page, size);
  }

  public OutboxMessage getEntry(UUID id) {
    return outboxRepository.findById(id).orElseThrow(() -> new OutboxEntryNotFoundException(id));
  }

  public OutboxStatistics getStatistics(Instant fromDate, Instant toDate) {
    Instant from = fromDate != null ? fromDate : MIN_DATE;
    Instant to = toDate != null ? toDate : MAX_DATE;
    Map<OutboxStatus, Long> byStatus = outboxRepository.countByStatusBetween(from, to);
    long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
    // GAP: DEAD_LETTER is not part of the OutboxStatistics contract; it is counted in totalEntries
    // only.
    return new OutboxStatistics(
        total,
        byStatus.getOrDefault(OutboxStatus.PENDING, 0L),
        byStatus.getOrDefault(OutboxStatus.PROCESSING, 0L),
        byStatus.getOrDefault(OutboxStatus.COMPLETED, 0L),
        byStatus.getOrDefault(OutboxStatus.FAILED, 0L),
        byStatus.getOrDefault(OutboxStatus.SKIPPED, 0L),
        outboxRepository.averageProcessingTimeMs(from, to),
        outboxRepository.countByEventTypeBetween(from, to),
        fromDate,
        toDate,
        toStatistics(getTrainerAllocationSnapshot(from, to)));
  }

  /** US-001 (FR-US-001-010): current allocation/deallocation counts from the outbox table. */
  public TrainerAllocationOutboxSnapshot getTrainerAllocationSnapshot() {
    return getTrainerAllocationSnapshot(MIN_DATE, MAX_DATE);
  }

  private TrainerAllocationOutboxSnapshot getTrainerAllocationSnapshot(Instant from, Instant to) {
    Map<TrainerAllocationEventType, Map<OutboxStatus, Long>> counts =
        new EnumMap<>(TrainerAllocationEventType.class);
    outboxRepository
        .countByEventTypeAndStatusBetween(trainerAllocationEventClassifier.eventTypes(), from, to)
        .forEach(
            (eventType, byStatus) ->
                trainerAllocationEventClassifier
                    .classify(eventType)
                    .ifPresent(
                        type -> {
                          Map<OutboxStatus, Long> target =
                              counts.computeIfAbsent(
                                  type, key -> new EnumMap<>(OutboxStatus.class));
                          byStatus.forEach(
                              (status, total) -> target.merge(status, total, Long::sum));
                        }));
    return new TrainerAllocationOutboxSnapshot(counts);
  }

  private static TrainerAllocationOutboxStatistics toStatistics(
      TrainerAllocationOutboxSnapshot snapshot) {
    Map<String, Long> byStatus = new LinkedHashMap<>();
    for (OutboxStatus status : OutboxStatus.values()) {
      byStatus.put(status.name(), snapshot.countByStatus(status));
    }
    Map<String, Long> byEventType = new LinkedHashMap<>();
    Map<String, Map<String, Long>> byEventTypeAndStatus = new LinkedHashMap<>();
    long backlog = 0;
    for (TrainerAllocationEventType type : TrainerAllocationEventType.values()) {
      byEventType.put(type.key(), snapshot.countByType(type));
      Map<String, Long> statuses = new LinkedHashMap<>();
      for (OutboxStatus status : OutboxStatus.values()) {
        statuses.put(status.name(), snapshot.count(type, status));
      }
      byEventTypeAndStatus.put(type.key(), statuses);
      backlog += snapshot.backlog(type);
    }
    return new TrainerAllocationOutboxStatistics(
        snapshot.total(), backlog, byStatus, byEventType, byEventTypeAndStatus);
  }
}
