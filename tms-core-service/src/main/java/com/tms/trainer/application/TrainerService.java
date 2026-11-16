package com.tms.trainer.application;

import com.tms.admin.domain.AuditLogEntry;
import com.tms.admin.domain.AuditLogRepository;
import com.tms.trainer.domain.DuplicateTrainerException;
import com.tms.trainer.domain.Trainer;
import com.tms.trainer.domain.TrainerDomainService;
import com.tms.trainer.domain.TrainerRepository;
import com.tms.trainer.domain.TrainerStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Trainer use cases for US-003. */
@Service
@Transactional
public class TrainerService {

  static final String AUDIT_ACTION_CREATED = "TRAINER_CREATED";
  static final String AUDIT_ENTITY_TYPE = "Trainer";

  private final TrainerRepository repository;
  private final TrainerDomainService domainService;
  private final AuditLogRepository auditLog;

  public TrainerService(
      TrainerRepository repository,
      TrainerDomainService domainService,
      AuditLogRepository auditLog) {
    this.repository = repository;
    this.domainService = domainService;
    this.auditLog = auditLog;
  }

  /**
   * CreateTrainer (FR-US-003-003, -007, -009, -010, -012). The trainer and its audit entry are
   * written in one transaction, so any failure leaves neither behind.
   */
  public Trainer create(CreateTrainerCommand command, String actor) {
    Instant now = Instant.now();
    Trainer trainer = domainService.newTrainer(command, now);
    try {
      repository.saveAndFlush(trainer);
    } catch (DataIntegrityViolationException e) {
      // Concurrent create that passed the existence checks; the unique indexes are the backstop.
      throw new DuplicateTrainerException(
          command.employeeId() != null && String.valueOf(e.getMessage()).contains("employee_id")
              ? "employeeId"
              : "email");
    }
    auditLog.appendAuditEvent(
        new AuditLogEntry(
            actor,
            AUDIT_ACTION_CREATED,
            AUDIT_ENTITY_TYPE,
            trainer.getId().toString(),
            now,
            auditDetails(trainer)));
    return trainer;
  }

  /**
   * SearchTrainers (FR-US-003-008): Active trainers only. GAP: no SearchTrainers/eligibility rules
   * exist in the repo yet, so this is the minimal status-based view new trainers flow into.
   */
  @Transactional(readOnly = true)
  public List<Trainer> searchTrainers() {
    return repository.findAllByStatus(TrainerStatus.ACTIVE);
  }

  /**
   * GAP(FR-US-003-009): the non-sensitive field list is not specified and the repo has no privacy
   * convention; only non-personal profile attributes are recorded (no name, email, employee ID,
   * rate, agreement references or documents).
   */
  private static Map<String, String> auditDetails(Trainer trainer) {
    Map<String, String> details = new HashMap<>();
    details.put("trainerType", trainer.getTrainerType().name());
    details.put("status", trainer.getStatus().name());
    details.put("location", trainer.getLocation());
    if (trainer.getTimeZone() != null) {
      details.put("timeZone", trainer.getTimeZone());
    }
    return details;
  }
}
