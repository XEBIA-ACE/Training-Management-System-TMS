package com.tms.outbox.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;

/** Persistence port for outbox records (C4 {@code OutboxRepository}). */
public interface OutboxRepository {

  /** Claims up to {@code limit} due PENDING messages (row-locked, skipping locked rows). */
  List<OutboxMessage> findPendingMessages(int limit);

  List<OutboxMessage> findByAggregateId(String aggregateId);

  Optional<OutboxMessage> findById(UUID id);

  OutboxMessage save(OutboxMessage message);

  void updateStatus(UUID id, OutboxStatus status);

  List<OutboxMessage> findFailedMessages();

  void moveToDeadLetter(UUID id);

  long countByStatus(OutboxStatus status);

  Page<OutboxMessage> findEntries(OutboxEntryFilter filter, int page, int size);

  Map<OutboxStatus, Long> countByStatusBetween(Instant from, Instant to);

  Map<String, Long> countByEventTypeBetween(Instant from, Instant to);

  /**
   * Counts entries of the given event types, grouped by event type then status (US-001,
   * FR-US-001-010). Event types without entries are absent from the result.
   */
  Map<String, Map<OutboxStatus, Long>> countByEventTypeAndStatusBetween(
      Collection<String> eventTypes, Instant from, Instant to);

  double averageProcessingTimeMs(Instant from, Instant to);

  int deleteCompletedBefore(Instant before);
}
