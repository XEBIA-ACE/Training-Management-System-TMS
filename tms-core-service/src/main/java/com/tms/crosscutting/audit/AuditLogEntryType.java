package com.tms.crosscutting.audit;

/** Kinds of {@link AuditLogEntry} records. */
public enum AuditLogEntryType {
  /** A search/filter query over a directory (e.g. {@code GET /api/trainers}). */
  SEARCH,
  /** An UnauthorizedAccessEntry: a caller was denied access (HTTP 403). */
  UNAUTHORIZED_ACCESS,
  /** A record was created (e.g. {@code POST /api/v1/trainers}, FR-US-003-006). */
  CREATE
}
