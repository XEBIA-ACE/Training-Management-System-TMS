# ADR: POST /api/v1/trainers – locally chosen behavior for spec gaps (US-003)

Source of the gaps: `specs/US-003/spec.md` (FR-US-003-002, -004, -006, -009, -011). These are implementation choices, not requirements.

## Decisions
- Validation failure: 400 `VALIDATION_FAILED` with `details[]` (same envelope and code as the customer/order APIs; SVC-001 reference text says `VALIDATION_ERROR`, neighbouring code wins). FR-US-003-002/011.
- Duplicate Employee ID (internal only) or Email (all types, case-insensitive): 409 `TRAINER_DUPLICATE`, `details[0].field` = `employeeId` | `email`. Per SVC-001 "409 for business conflicts"; the code name is local. FR-US-003-004. Unique indexes are a backstop for races.
- Required fields (not defined by the story): all types – `trainerType`, `name`, `email`, `location`, `weekendAvailable`, `certifications` (≥1), `ratePerDay` (>0); INTERNAL – `employeeId`; EXTERNAL – `msaReference`, `ndaReference`.
- Time zone (FR-US-003-005): no CustomerLocation/region data exists in the repo; `IanaLocationTimeZoneResolver` matches the location to an IANA zone ID or city name from the JDK tz database.
- **Unresolved gap FR-US-003-006**: an unresolvable location is accepted and `time_zone` stays NULL; no default is guessed. Needs a product decision.
- Status: always `ACTIVE` on create; a client-sent `status` is ignored.
- SearchTrainers (FR-US-003-008): no SearchTrainers/TrainerAllocationView/eligibility rules exist yet; `TrainerService.searchTrainers()` returns Active trainers as the minimal view. Existing rules were not touched because none exist.
- **Unresolved gap FR-US-003-009**: no privacy convention for audit fields exists. Audit details record only `trainerType`, `status`, `location`, `timeZone` (no name, email, employee ID, rate, agreement references or documents), plus actor (JWT subject), timestamp and trainer ID.
- Audit storage (FR-US-003-010): `audit_log_entry` table with a DB trigger rejecting UPDATE/DELETE, Hibernate `@Immutable` entity, and `AuditLogRepository.appendAuditEvent` requiring an existing transaction. Trainer and audit rows are written in one transaction, so a failure leaves neither (FR-US-003-012).
- Authorization: authenticated bearer JWT only; the story names no roles for "HR/admin users".
- Paths: `/api/v1/trainers` (repo convention) rather than `/api/trainers`.
