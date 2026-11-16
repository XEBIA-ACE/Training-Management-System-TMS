# US-002 trainer directory: unresolved gaps and provisional choices

Status: open — needs product / security confirmation.

`specs/US-002/spec.md` leaves several behaviours undefined. The implementation of
`GET /api/trainers` and `GET /api/trainers/{id}` (SVC-008, `com.tms.trainer`) makes the minimal
provisional choices below and marks them with `GAP(...)` comments in code. None of them should be
treated as approved contract.

| Ref | Gap | Provisional choice | Where |
| --- | --- | --- | --- |
| FR-US-002-001 | Allowed `status` / `availabilityState` values | Opaque strings, exact match | `Trainer`, `TrainerSpecifications` |
| FR-US-002-001 | Types of `certificationId`, `locationId` | UUID (malformed value -> 400 `VALIDATION_FAILED`) | `TrainerController` |
| FR-US-002-002 | Search matching algorithm | Case-insensitive *contains* over `name`, `email`, `employeeId`; LIKE wildcards escaped; max 200 chars | `TrainerSpecifications` |
| FR-US-002-003 | SLO values, production volumes | No threshold asserted. Latency exposed as Micrometer timer `tms.trainer.search` (tag `freeText`) and `tms.trainer.profile` (tag `outcome`) with percentile histograms; structured completion log; page size capped at 100; indexes on filter columns; certifications batch-loaded (no N+1). The contains predicate is not index-backed — a `pg_trgm` index may be needed once volumes are known. | `TrainerAppService`, `V5__create_trainer.sql` |
| FR-US-002-003 | `CacheAdapter` usage | **Not cached.** Results are per-actor-authorized, every request must be audited, and no invalidation source (trainer writes) exists in this story; caching would risk stale/leaked data and skipped audit entries. Revisit with an agreed SLO and invalidation strategy. | — |
| FR-US-002-004 | Error semantics on denial | 403 `ACCESS_DENIED` (shared envelope), decided **before** any lookup so trainer existence is not leaked; 404 `TRAINER_NOT_FOUND` only for authorized callers. No per-trainer (row-level) visibility rules are defined, so authorization is directory-wide. | `TrainerAppService` |
| FR-US-002-005 | "Basic" trainer attributes | `id, employeeId, name, email, status, availabilityState, locationId, weekendAvailable, certificationIds`; same shape for list items and profile | `TrainerResponse` |
| FR-US-002-006 | Audit trigger policy | Configurable `tms.trainer.directory.audit.search-enabled` (default `true` = every request). Details: filter field names, `freeTextPresent`, page, size, resultCount; never the raw search term or filter values. | `TrainerAppService`, `AuditLogEntry` |
| FR-US-002-007 | RBAC role mapping | Configurable `tms.trainer.directory.roles` (default `SUPER_ADMIN`, fail-closed). UnauthorizedAccessEntry written when `tms.trainer.directory.audit.unauthorized-access-enabled` (default `true`). | `TrainerDirectoryProperties`, `AuthContextProvider` |
| Contract | Path prefix | Spec text says `GET /trainers`; implemented as `/api/trainers` per the work item and the SVC-001 scaffold. | `TrainerController` |
| Contract | `PagedTrainerList` paging fields | Only `items` is named; `page, size, totalElements, totalPages` follow `CustomerPageResponse`. | `PagedTrainerList` |

## Proposed (not implemented) data-model additions

The story names `AuditLogEntry` and `UnauthorizedAccessEntry` but no persistence schema existed in
this repository. The minimum needed to satisfy US-002-ac-6/ac-7 was added: the `audit_log_entry`
table (`V6`) with `entry_type` `SEARCH` / `UNAUTHORIZED_ACCESS`, and the `trainer` /
`trainer_certification` tables (`V5`) the directory reads. No write/admin API for trainers or audit
entries was introduced. Please confirm these schemas or point to the authoritative model.
