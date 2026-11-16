package com.tms.scheduling.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Workshop and Allocation Repository (FR-US-002-004). "Current" allocations are those that have
 * not ended before {@code today}.
 */
public interface AllocationRepository extends JpaRepository<Allocation, UUID> {

  @Query("select a from Allocation a where a.trainerId in :trainerIds and a.endDate >= :today")
  List<Allocation> findCurrentByTrainerIds(
      @Param("trainerIds") Collection<UUID> trainerIds, @Param("today") LocalDate today);

  @Query(
      "select distinct a.trainerId from Allocation a"
          + " where a.status = :status and a.endDate >= :today")
  List<UUID> findTrainerIdsWithStatus(
      @Param("status") AllocationStatus status, @Param("today") LocalDate today);

  @Query("select distinct a.trainerId from Allocation a where a.endDate >= :today")
  List<UUID> findTrainerIdsWithAnyCurrentAllocation(@Param("today") LocalDate today);

  /** True when the trainer already holds an allocation overlapping [start, end]. */
  boolean existsByTrainerIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
      UUID trainerId, LocalDate end, LocalDate start);
}
