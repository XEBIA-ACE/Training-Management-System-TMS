package com.tms.admin.domain;

/** Audit Log Repository Interface (US-003, FR-US-003-010): append-only. */
public interface AuditLogRepository {

  /** Appends one audit event in the caller's transaction. */
  AuditLogEntry appendAuditEvent(AuditLogEntry event);
}
