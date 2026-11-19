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

## Frontend scope gap (specs/US-004 T001..T018, US-004-ac-1..6)

The bundle targets `trainer-mgmt-web` and `trainer-allocation-web`. Neither exists in this repo. The
only frontend is `tms-frontend-app` (one SPA), and it has none of the surfaces US-004 attaches to:

| Spec reference | Found in repo | Consequence |
| --- | --- | --- |
| `/trainers/:trainerId`, `/trainers/:trainerId/edit` (ac-1, ac-2) | Only `/master-data/trainers` (SCR-010, mock data; detail/edit declared out of scaffold) | No form state or status field to wire. Adding screens/routes is forbidden by the spec. |
| `/allocation`, `/allocation/customer/:id`, `/allocation/workshop/:id` (ac-3..5) | `/allocation` and `/allocation/workspace` are `scaffolded: false` placeholders; the other two routes are absent | No trainer selection controls to filter or block. |
| Trainer API client (ac-2, ac-3, ac-5) | None (`src/api` has only `customerApi.ts`) | Backend endpoints exist (`PATCH /api/v1/trainers/{id}/status` body `{status}`, `GET /api/v1/trainers?status=`) but nothing calls them. |
| Frontend audit integration (ac-6) | None. Audit is written server-side by `TrainerCommandService`, actor taken from the JWT | Nothing for the SPAs to propagate. The SPA has no token acquisition (AUTH-02), so no identity is sent. |
| Allocation-service error mapping (ac-4) | `scheduling` module is an empty placeholder | No endpoint to return or map `TRAINER_INACTIVE`. |

Frontend work was **not** started, so no screens, routes or contracts were invented. Human decisions
needed: (1) whether US-004 UI should add detail/edit routes to `tms-frontend-app` or wait for F-01
detail stories; (2) whether the allocation screens (SCR-022/023) get built first; (3) token
acquisition (AUTH-02).
