# US-004 trainer activation/inactivation: gaps and decisions

Stopgaps pending product/architect confirmation. Related: FR-US-004-AC1..AC7, US-004-ac-1..7.

## Missing inputs

- `specs/US-004/{spec,plan,tasks}.md` (and task ids T001..T026) are **not in this repository**. They
  were not recreated; tests reference the `US-004-ac-N` / `FR-US-004-ACn-nnn` ids from the bundle.
- The code under test could not be compiled or run in the authoring environment (no JDK/Maven/Docker);
  tests are written against the existing Testcontainers pattern and need a CI run.

## Decisions filling gaps

| Gap | Decision |
| --- | --- |
| Status filter syntax (FR-US-004-AC2-003) | The existing `status` query parameter is reused. Absent = INACTIVE excluded; `ACTIVE`/`INACTIVE` = exact; `ALL` (case-insensitive) = no restriction. Applies to `GET /api/trainers` and `/api/v1/trainers`. |
| Metadata fields (FR-US-004-AC1-002) | `status_changed_at`, `status_changed_by` (migration V9), exposed as `statusChangedAt`/`statusChangedBy` in the trainer representation (additive). |
| Who may change status | Reuses `tms.trainer.directory.create-roles` (default `HR_ADMIN`, `SUPER_ADMIN`). |
| Audit entry shape (FR-US-004-AC5-002) | `AuditLogEntry` type `STATUS_CHANGE`, resource `Trainer`/id; `details`: `previousStatus`, `newStatus`, `outcome` (`SUCCESS`, `DENIED`, or `REJECTED` for 400/404), actor and timestamp on the entry. `previousStatus` is absent when the trainer does not exist; `newStatus` only when the requested value is valid. |
| Denied attempts (FR-US-004-AC6-003) | Two entries: `STATUS_CHANGE`/`DENIED` and the existing `UNAUTHORIZED_ACCESS`. |
| Immutability (FR-US-004-AC5-001) | Application level only (`@Immutable` entity, append-only client). **No DB trigger**: existing tests clear the table with `delete`. Open gap. |
| Admin views (FR-US-004-AC5-003/AC6-003) | `GET /api/v1/admin/audit-logs` (all entries except `UNAUTHORIZED_ACCESS`) and `GET /api/v1/admin/unauthorized-access-logs`, `SUPER_ADMIN` only, newest first, `page`/`size` (max 100), no filters, no retention. |
| 400 vs 403 for inactive allocation (FR-US-004-AC3-001) | No codebase convention for business-rule failures; **400** with code `TRAINER_INACTIVE` was chosen (403 is used here for role denials). |

## Not implementable in this repository

`GET /api/v1/allocations/eligible-trainers` and `POST /api/v1/allocations` do not exist (the
`scheduling` module is an empty placeholder), so FR-US-004-AC2-002, AC3-001/002 and AC4-002 are
**not wired to HTTP endpoints**. Provided instead:

- `TrainerSearchCriteria`/`TrainerRepository.search` carry the default-exclude/`ALL` semantics for any
  future eligibility query to reuse (certification and availability filters are unchanged).
- `TrainerAppService.requireAllocatable(trainerId)` throws `TrainerInactiveException`
  (`TRAINER_INACTIVE`, message "...is inactive and cannot be allocated."). It has no caller yet and the
  `ErrorResponse` mapping (400) must be added with the allocation controller.

FR-US-004-AC2-004 (open question): any future direct-allocation path that skips both the search and
`requireAllocatable` could still reference an INACTIVE trainer. Nothing was blocked or relaxed.

## Behavior changes to existing tests

`GET /api/trainers` without `status` no longer returns INACTIVE trainers (FR-US-004-AC2-001), so the
US-002 directory test now passes `status=ALL` where it expects the INACTIVE trainer.
