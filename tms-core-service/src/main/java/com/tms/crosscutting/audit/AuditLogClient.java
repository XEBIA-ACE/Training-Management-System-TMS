package com.tms.crosscutting.audit;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Appends immutable {@link AuditLogEntry} records. Each entry is written in its own transaction so
 * it survives a rollback of the caller (e.g. an UnauthorizedAccessEntry written just before a 403),
 * unless it must commit atomically with a domain change ({@link #appendInCurrentTransaction}).
 */
@Component
public class AuditLogClient {

  private final EntityManager entityManager;

  AuditLogClient(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AuditLogEntry appendAuditEntry(AuditLogEntry entry) {
    entityManager.persist(entry);
    return entry;
  }

  /**
   * Appends {@code entry} in the caller's transaction, so the audit record and the audited change
   * commit or roll back together (e.g. a trainer creation, FR-US-003-006).
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public AuditLogEntry appendInCurrentTransaction(AuditLogEntry entry) {
    entityManager.persist(entry);
    return entry;
  }
}
