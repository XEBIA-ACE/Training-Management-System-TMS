package com.tms.crosscutting.audit;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Appends immutable {@link AuditLogEntry} records. Each entry is written in its own transaction so
 * it survives a rollback of the caller (e.g. an UnauthorizedAccessEntry written just before a 403).
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
}
