# US-003 create trainer: unresolved gaps and provisional choices

Status: open — needs product / security confirmation. Marked `GAP(...)` in code.

| Ref | Gap | Provisional choice | Where |
| --- | --- | --- | --- |
| Contract | Spec paths are `/api/v1/trainers`; US-002 reads are `/api/trainers` | Controller serves both prefixes; POST/PUT/audit-logs are new under `/api/v1/trainers` | `TrainerController` |
| FR-US-003-001/002 | Required fields and formats of `CreateTrainerRequest` are not in the repo | `type`, `name`, `email` always; `employeeId` for INTERNAL; `locationId` (UUID) for EXTERNAL; email shape check; lengths per column | `TrainerCommandService` |
| FR-US-003-001 | RBAC role names | Configurable `tms.trainer.directory.create-roles` (default `HR_ADMIN,SUPER_ADMIN`); denials write an UNAUTHORIZED_ACCESS entry | `TrainerDirectoryProperties` |
| FR-US-003-004 | No CustomerLocation / location master data exists | Minimal `location(id, timezone)` lookup (`V7`); unknown location or no timezone leaves `timezone` NULL (create) / unchanged (PUT) | `V7__trainer_create_profile.sql`, `Location` |
| FR-US-003-004 | PUT body is undefined | `{name?, locationId?}`; absent fields unchanged | `UpdateTrainerRequest` |
| FR-US-003-005 | `GET /api/allocations/eligible-trainers` does not exist in this repo | Not implemented (no new endpoints). ACTIVE trainers are returned by the existing `status=ACTIVE` listing | — |
| FR-US-003-006/007 | Audit entry types | Added `CREATE` and `UPDATE` to `AuditLogEntryType`; `details.outcome` is `SUCCESS` / `REJECTED`. Existing `AuditLogEntry` schema reused | `AuditLogEntryType` |
| FR-US-003-007 | A blocked create has no trainerId, so it cannot appear in a per-trainer trail | Duplicate rejections are attached to the conflicting trainer's trail (visible at `/audit-logs`); pure validation rejections are stored in the global `audit_log_entry` table only (no endpoint lists them) | `TrainerCommandService.rejectCreate` |
| FR-US-003-007 | PII in rejected-attempt entries | Only field names and messages are stored, never submitted values | `TrainerCommandService.rejectCreate` |
