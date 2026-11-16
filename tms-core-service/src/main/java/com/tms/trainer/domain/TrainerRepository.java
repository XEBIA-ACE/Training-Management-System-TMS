package com.tms.trainer.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainerRepository
    extends JpaRepository<Trainer, UUID>, JpaSpecificationExecutor<Trainer> {

  /**
   * TrainerRepository.Search (FR-US-002-001/002): trainers matching every supplied filter in {@code
   * criteria}, including the free-text predicate, as one page.
   */
  default Page<Trainer> search(TrainerSearchCriteria criteria, Pageable pageable) {
    return findAll(TrainerSpecifications.matching(criteria), pageable);
  }

  /** FR-US-003-003: any trainer (any status) with exactly this employeeId. */
  boolean existsByEmployeeId(String employeeId);

  /**
   * FR-US-003-003: any trainer (any status) with this email, compared case-insensitively (backed by
   * the {@code uq_trainer_email_lower} index).
   */
  @Query("select count(t) > 0 from Trainer t where lower(t.email) = lower(:email)")
  boolean existsByEmailIgnoreCase(@Param("email") String email);
}
