package com.tms.trainer.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainerRepository extends JpaRepository<Trainer, UUID> {

  /** Spans active and inactive trainers (FR-US-003-003). Emails are stored lower-cased. */
  boolean existsByEmail(String email);

  /** Spans active and inactive trainers (FR-US-003-003). */
  boolean existsByEmployeeId(String employeeId);

  List<Trainer> findAllByStatus(TrainerStatus status);
}
