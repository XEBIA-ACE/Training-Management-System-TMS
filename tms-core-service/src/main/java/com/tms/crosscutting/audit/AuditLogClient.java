package com.tms.crosscutting.audit;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  /** Optional audit-log filters (FR-US-004-011); null members are not applied. */
  public record Filter(
      String trainerId,
      String actor,
      String entryType,
      String previousStatus,
      String newStatus,
      Instant from,
      Instant to) {
    public static Filter none() {
      return new Filter(null, null, null, null, null, null, null);
    }
  }

  public Page<AuditLogEntry> findPage(boolean unauthorizedOnly, int page, int size) {
    return findPage(unauthorizedOnly, Filter.none(), page, size);
  }

  /**
   * One page of entries, newest first. {@code unauthorizedOnly} selects only UNAUTHORIZED_ACCESS
   * entries; otherwise every other entry type is returned (the two admin log views are disjoint).
   * Filters are AND-combined; status filters read the {@code previousStatus}/{@code newStatus}
   * keys of the JSON details.
   */
  @Transactional(readOnly = true)
  public Page<AuditLogEntry> findPage(boolean unauthorizedOnly, Filter f, int page, int size) {
    StringBuilder where =
        new StringBuilder(" where e.entryType " + (unauthorizedOnly ? "=" : "<>") + " :unauth");
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("unauth", AuditLogEntryType.UNAUTHORIZED_ACCESS);
    if (f.trainerId() != null) {
      where.append(" and e.resourceType = 'Trainer' and e.resourceId = :trainerId");
      params.put("trainerId", f.trainerId());
    }
    if (f.actor() != null) {
      where.append(" and e.actor = :actor");
      params.put("actor", f.actor());
    }
    if (f.entryType() != null) {
      where.append(" and e.entryType = :entryType");
      params.put("entryType", AuditLogEntryType.valueOf(f.entryType()));
    }
    if (f.previousStatus() != null) {
      where.append(" and function('jsonb_extract_path_text', e.details, 'previousStatus') = :prev");
      params.put("prev", f.previousStatus());
    }
    if (f.newStatus() != null) {
      where.append(" and function('jsonb_extract_path_text', e.details, 'newStatus') = :next");
      params.put("next", f.newStatus());
    }
    if (f.from() != null) {
      where.append(" and e.occurredAt >= :from");
      params.put("from", f.from());
    }
    if (f.to() != null) {
      where.append(" and e.occurredAt <= :to");
      params.put("to", f.to());
    }
    var query =
        entityManager.createQuery(
            "select e from AuditLogEntry e" + where + " order by e.occurredAt desc, e.id",
            AuditLogEntry.class);
    var count =
        entityManager.createQuery("select count(e) from AuditLogEntry e" + where, Long.class);
    params.forEach(
        (k, v) -> {
          query.setParameter(k, v);
          count.setParameter(k, v);
        });
    List<AuditLogEntry> items =
        query.setFirstResult(page * size).setMaxResults(size).getResultList();
    return new PageImpl<>(items, PageRequest.of(page, size), count.getSingleResult());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AuditLogEntry appendAuditEntry(AuditLogEntry entry) {
    entityManager.persist(entry);
    return entry;
  }
}
