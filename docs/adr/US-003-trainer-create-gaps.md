# US-003 create trainer: unresolved gaps and provisional choices

Status: open — needs product / security confirmation. Marked `GAP(...)` in code.

| Ref | Gap | Provisional choice | Where |
| --- | --- | --- | --- |
| Contract | Spec paths are `/api/v1/trainers`; US-002 reads are `/api/trainers` | Controller serves both prefixes; POST/PUT/audit-logs are new under `/api/v1/trainers` | `TrainerController` |
| FR-US-003-001/002 | Required fields and formats of `CreateTrainerRequest` are not in the repo | `type`, `name`, `email` always; `employeeId` for INTERNAL; `locationId` (UUID) for both types (FR-US-003-001 lists location as required for INTERNAL too). The spec says `location`; the existing `locationId` contract is kept to avoid a breaking change; email shape check; lengths per column | `TrainerCommandService` |
| FR-US-003-001 | RBAC role names | Configurable `tms.trainer.directory.create-roles` (default `HR_ADMIN,SUPER_ADMIN`); denials write an UNAUTHORIZED_ACCESS entry | `TrainerDirectoryProperties` |
| FR-US-003-004 | No CustomerLocation / location master data exists | Minimal `location(id, timezone)` lookup (`V7`); unknown location or no timezone leaves `timezone` NULL (create) / unchanged (PUT) | `V7__trainer_create_profile.sql`, `Location` |
| FR-US-003-004 | PUT body is undefined | `{name?, locationId?}`; absent fields unchanged | `UpdateTrainerRequest` |
| FR-US-003-005 | `GET /api/allocations/eligible-trainers` does not exist in this repo | Not implemented (no new endpoints). ACTIVE trainers are returned by the existing `status=ACTIVE` listing | — |
| FR-US-003-006/007 | Audit entry types | Added `CREATE` and `UPDATE` to `AuditLogEntryType`; `details.outcome` is `SUCCESS` / `REJECTED`. Existing `AuditLogEntry` schema reused | `AuditLogEntryType` |
| FR-US-003-007 | A blocked create has no trainerId, so it cannot appear in a per-trainer trail | Duplicate rejections are attached to the conflicting trainer's trail (visible at `/audit-logs`); pure validation rejections are stored in the global `audit_log_entry` table only (no endpoint lists them) | `TrainerCommandService.rejectCreate` |
| FR-US-003-007 | PII in rejected-attempt entries | Only field names and messages are stored, never submitted values | `TrainerCommandService.rejectCreate` |
| FR-US-003-002 | `msaNdaStatus` enum values are not defined in the repo or spec export | Optional `msaNdaStatus` with provisional values `NOT_STARTED, PENDING, SIGNED, EXPIRED`; other values are a 400; stored as text (`V8`) | `MsaNdaStatus`, `V8__trainer_msa_nda_status.sql` |
| FR-US-003-003 | Exact 409 `errorCode` values are not specified (artifact-SVC-005-api_spec) | `DUPLICATE_EMPLOYEE_ID` / `DUPLICATE_EMAIL`, `details[]` names the field; checks ignore status (inactive included) | `TrainerConflictException`, `TrainerApiExceptionHandler` |
| FR-US-003-006 | AuditEntry has no `module` column (artifact-SVC-005-c3/c4) | `details.module = "trainer"`, `resourceId` = trainerId; existing `@Immutable` `AuditLogEntry` reused | `TrainerCommandService.profileFields` |
| FR-US-003-007 | Security-event sink for 401s is not specified (SVC-012/013/014) | 401/403 logged on logger `com.tms.security.events`; 401s also written as `UNAUTHORIZED_ACCESS` (actor `anonymous`, method+path only; `tms.security.audit-authentication-failures`, default true); 403s audited by the service making the RBAC decision. TRAINER.MANAGE is mapped to the configured create roles | `SecurityErrorHandler` |
