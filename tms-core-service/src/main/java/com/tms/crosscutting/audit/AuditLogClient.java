package com.tms.crosscutting.audit;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

  /** Entries for one resource, oldest first (read-only; entries are immutable). */
  @Transactional(readOnly = true)
  public List<AuditLogEntry> findByResource(String resourceType, String resourceId) {
    return entityManager
        .createQuery(
            "select e from AuditLogEntry e where e.resourceType = :type and e.resourceId = :id"
                + " order by e.occurredAt, e.id",
            AuditLogEntry.class)
        .setParameter("type", resourceType)
        .setParameter("id", resourceId)
        .getResultList();
  }

  /**
   * One page of entries, newest first. {@code unauthorizedOnly} selects only UNAUTHORIZED_ACCESS
   * entries; otherwise every other entry type is returned (the two admin log views are disjoint).
   */
  @Transactional(readOnly = true)
  public Page<AuditLogEntry> findPage(boolean unauthorizedOnly, int page, int size) {
    String op = unauthorizedOnly ? "=" : "<>";
    List<AuditLogEntry> items =
        entityManager
            .createQuery(
                "select e from AuditLogEntry e where e.entryType "
                    + op
                    + " :type order by e.occurredAt desc, e.id",
                AuditLogEntry.class)
            .setParameter("type", AuditLogEntryType.UNAUTHORIZED_ACCESS)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();
    long total =
        entityManager
            .createQuery(
                "select count(e) from AuditLogEntry e where e.entryType " + op + " :type",
                Long.class)
            .setParameter("type", AuditLogEntryType.UNAUTHORIZED_ACCESS)
            .getSingleResult();
    return new PageImpl<>(items, PageRequest.of(page, size), total);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AuditLogEntry appendAuditEntry(AuditLogEntry entry) {
    entityManager.persist(entry);
    return entry;
  }
}
