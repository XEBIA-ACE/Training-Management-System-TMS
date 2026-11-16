package com.tms.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Immutable audit log record (US-003, FR-US-003-010). Append-only: no mutators, Hibernate {@link
 * Immutable}, and a database trigger rejects UPDATE/DELETE. {@code details} must only carry
 * non-sensitive fields; never document contents.
 */
@Entity
@Immutable
@Table(name = "audit_log_entry")
public class AuditLogEntry {

  @Id private UUID id;

  @Column(nullable = false, updatable = false, length = 200)
  private String actor;

  @Column(nullable = false, updatable = false, length = 64)
  private String action;

  @Column(name = "entity_type", nullable = false, updatable = false, length = 64)
  private String entityType;

  @Column(name = "entity_id", nullable = false, updatable = false, length = 64)
  private String entityId;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
  private Map<String, String> details;

  protected AuditLogEntry() {}

  public AuditLogEntry(
      String actor,
      String action,
      String entityType,
      String entityId,
      Instant occurredAt,
      Map<String, String> details) {
    this.id = UUID.randomUUID();
    this.actor = actor;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.occurredAt = occurredAt;
    this.details = Map.copyOf(details);
  }

  public UUID getId() {
    return id;
  }

  public String getActor() {
    return actor;
  }

  public String getAction() {
    return action;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public Map<String, String> getDetails() {
    return details;
  }
}
