package com.tms.outbox.domain;

/**
 * Lifecycle state of an {@link OutboxMessage}.
 *
 * <p>GAP (specs/SCAFFOLD-SVC-002/references/SVC-002.md): the API data model {@code OutboxEntry}
 * defines PENDING, PROCESSING, COMPLETED, FAILED, SKIPPED; the C4 diagram adds DEAD_LETTER and the
 * story summary uses NEW/IN_PROGRESS/DONE/FAILED. The API values are kept as the contract and
 * DEAD_LETTER is added for the C4 dead-letter flow.
 */
public enum OutboxStatus {
  PENDING,
  PROCESSING,
  COMPLETED,
  FAILED,
  SKIPPED,
  DEAD_LETTER
}
