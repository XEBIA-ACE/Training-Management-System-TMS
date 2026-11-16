package com.tms.crosscutting.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Immutable audit record (data model {@code AuditLogEntry}). {@code details} carries a high-level,
 * non-sensitive summary of the operation; callers must never put raw user input (e.g. free-text
 * search terms) or personal data into it.
 */
@Entity
@Immutable
@Table(name = "audit_log_entry")
public class AuditLogEntry {

  @Id private UUID id;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "entry_type", nullable = false, updatable = false, length = 40)
  private AuditLogEntryType entryType;

  @Column(nullable = false, updatable = false, length = 200)
  private String actor;

  @Column(nullable = false, updatable = false, length = 120)
  private String action;

  @Column(name = "resource_type", nullable = false, updatable = false, length = 60)
  private String resourceType;

  @Column(name = "resource_id", updatable = false, length = 120)
  private String resourceId;

  @Column(name = "trace_id", updatable = false, length = 128)
  private String traceId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
  private Map<String, Object> details;

  protected AuditLogEntry() {}

  public AuditLogEntry(
      AuditLogEntryType entryType,
      String actor,
      String action,
      String resourceType,
      String resourceId,
      String traceId,
      Map<String, Object> details,
      Instant occurredAt) {
    this.id = UUID.randomUUID();
    this.entryType = entryType;
    this.actor = actor;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.traceId = traceId;
    this.details = Map.copyOf(details);
    this.occurredAt = occurredAt;
  }

  public UUID getId() {
    return id;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public AuditLogEntryType getEntryType() {
    return entryType;
  }

  public String getActor() {
    return actor;
  }

  public String getAction() {
    return action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getTraceId() {
    return traceId;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
