package com.tms.crosscutting.audit;

/** Kinds of {@link AuditLogEntry} records. */
public enum AuditLogEntryType {
  /** A search/filter query over a directory (e.g. {@code GET /api/trainers}). */
  SEARCH,
  /**
   * An UnauthorizedAccessEntry: a caller was denied access (HTTP 403), or a request without valid
   * authentication was rejected (HTTP 401, {@code details.reason=AUTHENTICATION_REQUIRED}).
   */
  UNAUTHORIZED_ACCESS,
  /**
   * A create operation (US-003 {@code POST /api/v1/trainers}); {@code details.outcome} is {@code
   * SUCCESS} or {@code REJECTED}.
   */
  CREATE,
  /** An update operation (US-003 {@code PUT /api/v1/trainers/{trainerId}}). */
  UPDATE,
  /**
   * A trainer status change attempt (US-004 {@code PATCH /api/v1/trainers/{trainerId}/status});
   * {@code details.outcome} is {@code SUCCESS}, {@code DENIED} or {@code REJECTED}.
   */
  STATUS_CHANGE
}
