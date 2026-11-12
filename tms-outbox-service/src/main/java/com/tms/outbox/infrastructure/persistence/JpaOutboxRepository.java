package com.tms.outbox.infrastructure.persistence;

import com.tms.outbox.domain.OutboxEntryFilter;
import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.domain.OutboxRepository;
import com.tms.outbox.domain.OutboxStatus;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Spring Data JPA adapter of the {@link OutboxRepository} port (C4 {@code JpaOutboxRepository}).
 */
@Repository
@Transactional(readOnly = true)
public class JpaOutboxRepository implements OutboxRepository {

  private final SpringDataOutboxMessageRepository jpa;

  public JpaOutboxRepository(SpringDataOutboxMessageRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  @Transactional
  public List<OutboxMessage> findPendingMessages(int limit) {
    return jpa.claimPending(limit);
  }

  @Override
  public List<OutboxMessage> findByAggregateId(String aggregateId) {
    return jpa.findByAggregateIdOrderByCreatedAtAsc(aggregateId);
  }

  @Override
  public Optional<OutboxMessage> findById(UUID id) {
    return jpa.findById(id);
  }

  @Override
  @Transactional
  public OutboxMessage save(OutboxMessage message) {
    return jpa.save(message);
  }

  @Override
  @Transactional
  public void updateStatus(UUID id, OutboxStatus status) {
    jpa.updateStatus(id, status);
  }

  @Override
  public List<OutboxMessage> findFailedMessages() {
    return jpa.findByStatusOrderByCreatedAtAsc(OutboxStatus.FAILED);
  }

  @Override
  @Transactional
  public void moveToDeadLetter(UUID id) {
    jpa.updateStatus(id, OutboxStatus.DEAD_LETTER);
  }

  @Override
  public long countByStatus(OutboxStatus status) {
    return jpa.countByStatus(status);
  }

  @Override
  public Page<OutboxMessage> findEntries(OutboxEntryFilter filter, int page, int size) {
    Specification<OutboxMessage> spec = (root, query, cb) -> cb.conjunction();
    if (filter.status() != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
    }
    if (StringUtils.hasText(filter.eventType())) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), filter.eventType()));
    }
    if (filter.fromDate() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), filter.fromDate()));
    }
    if (filter.toDate() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.lessThanOrEqualTo(root.<Instant>get("createdAt"), filter.toDate()));
    }
    return jpa.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
  }

  @Override
  public Map<OutboxStatus, Long> countByStatusBetween(Instant from, Instant to) {
    Map<OutboxStatus, Long> counts = new EnumMap<>(OutboxStatus.class);
    jpa.countByStatusBetween(from, to).forEach(row -> counts.put(row.getStatus(), row.getTotal()));
    return counts;
  }

  @Override
  public Map<String, Long> countByEventTypeBetween(Instant from, Instant to) {
    Map<String, Long> counts = new LinkedHashMap<>();
    jpa.countByEventTypeBetween(from, to)
        .forEach(row -> counts.put(row.getEventType(), row.getTotal()));
    return counts;
  }

  @Override
  public double averageProcessingTimeMs(Instant from, Instant to) {
    Double average = jpa.averageProcessingTimeMs(from, to);
    return average != null ? average : 0.0;
  }

  @Override
  @Transactional
  public int deleteCompletedBefore(Instant before) {
    return jpa.deleteCompletedBefore(before);
  }
}
