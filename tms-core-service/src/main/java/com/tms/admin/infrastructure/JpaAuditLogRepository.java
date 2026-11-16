package com.tms.admin.infrastructure;

import com.tms.admin.domain.AuditLogEntry;
import com.tms.admin.domain.AuditLogRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaAuditLogRepository implements AuditLogRepository {

  private final EntityManager entityManager;

  JpaAuditLogRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public AuditLogEntry appendAuditEvent(AuditLogEntry event) {
    entityManager.persist(event);
    return event;
  }
}
