package com.tms.trainer.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrainerRepository
    extends JpaRepository<Trainer, UUID>, JpaSpecificationExecutor<Trainer> {

  /**
   * TrainerRepository.Search (FR-US-002-001/002): trainers matching every supplied filter in {@code
   * criteria}, including the free-text predicate, as one page.
   */
  default Page<Trainer> search(TrainerSearchCriteria criteria, Pageable pageable) {
    return findAll(TrainerSpecifications.matching(criteria), pageable);
  }
}
