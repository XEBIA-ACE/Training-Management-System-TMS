package com.tms.outbox.infrastructure.persistence;

import com.tms.outbox.domain.OutboxMessage;
import com.tms.outbox.domain.OutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataOutboxMessageRepository
    extends JpaRepository<OutboxMessage, UUID>, JpaSpecificationExecutor<OutboxMessage> {

  @Query(
      value =
          "select * from outbox_message where status = 'PENDING'"
              + " and (next_retry_at is null or next_retry_at <= now())"
              + " order by created_at limit :limit for update skip locked",
      nativeQuery = true)
  List<OutboxMessage> claimPending(@Param("limit") int limit);

  List<OutboxMessage> findByAggregateIdOrderByCreatedAtAsc(String aggregateId);

  List<OutboxMessage> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

  long countByStatus(OutboxStatus status);

  @Modifying
  @Query("update OutboxMessage m set m.status = :status where m.id = :id")
  int updateStatus(@Param("id") UUID id, @Param("status") OutboxStatus status);

  @Modifying
  @Query(
      "delete from OutboxMessage m where m.status = com.tms.outbox.domain.OutboxStatus.COMPLETED"
          + " and m.processedAt < :before")
  int deleteCompletedBefore(@Param("before") Instant before);

  @Query(
      "select m.status as status, count(m) as total from OutboxMessage m"
          + " where m.createdAt >= :from and m.createdAt <= :to group by m.status")
  List<StatusCount> countByStatusBetween(@Param("from") Instant from, @Param("to") Instant to);

  @Query(
      "select m.eventType as eventType, count(m) as total from OutboxMessage m"
          + " where m.createdAt >= :from and m.createdAt <= :to group by m.eventType")
  List<EventTypeCount> countByEventTypeBetween(
      @Param("from") Instant from, @Param("to") Instant to);

  @Query(
      value =
          "select coalesce(avg(extract(epoch from (processed_at - created_at)) * 1000), 0)"
              + " from outbox_message where status = 'COMPLETED' and processed_at is not null"
              + " and created_at >= :from and created_at <= :to",
      nativeQuery = true)
  Double averageProcessingTimeMs(@Param("from") Instant from, @Param("to") Instant to);

  interface StatusCount {
    OutboxStatus getStatus();

    long getTotal();
  }

  interface EventTypeCount {
    String getEventType();

    long getTotal();
  }
}
