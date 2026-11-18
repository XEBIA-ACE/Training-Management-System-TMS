# Implementation Plan: US-003

## Technical Context
- unresolved reference (artifact-SVC-004-api_spec)
- unresolved reference (artifact-SVC-004-c3)
- unresolved reference (artifact-SVC-004-c4)
- unresolved reference (service:SVC-004)
- unresolved reference (US-003)
- unresolved reference (US-003-ac-1)
- unresolved reference (US-003-ac-2)
- unresolved reference (US-003-ac-3)
- unresolved reference (US-003-ac-4)
- unresolved reference (US-003-ac-5)
- unresolved reference (US-003-ac-6)
- unresolved reference (US-003-ac-7)
- unresolved reference (FR-US-003-001)
- unresolved reference (FR-US-003-002)
- unresolved reference (FR-US-003-003)
- unresolved reference (FR-US-003-004)
- unresolved reference (FR-US-003-005)
- unresolved reference (FR-US-003-006)
- unresolved reference (FR-US-003-007)

## Contract Changes
- POST — Introduce POST /api/trainers to create trainer profiles (internal and external) using TrainerCreateRequest with contextual validation, uniqueness checks, timezone derivation, default active status, and audit logging.
- GET — Ensure Trainer representation returned by GET /api/trainers/{id} (and list endpoints) includes derived timeZone and defaulted active status so that clients see these fields populated after creation.
- Extend Trainer aggregate/data model to include fields required for creation and subsequent reads: type (internal|external), employeeId (for internal), locationId, timeZone (derived), status (default active), weekend availability, certification links, commercial attributes, and external legal attributes such as MSA/NDA status and reference.
- Clarify that AuditEntry records must capture trainer creation events with actor identity, timestamp, resourceType=trainer, new trainer id, actionType=create, and key non-sensitive fields, and optionally policy-compliant entries for failed attempts.

## Security and Quality
- Validate credential and personal-data handling against the story's acceptance criteria
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **US-003-ac-1**: When a client calls POST /trainers with a TrainerCreateRequest whose type is internal, the TMS Core Application Service must invoke the ValidationEngine through TrainerAppService to enforce required fields and format validation specific to internal trainers, returning a 400 ErrorResponse with details if any mandatory field such as employeeId, name, or email is missing or malformed.
- **US-003-ac-2**: When a client calls POST /trainers with a TrainerCreateRequest whose type is external, the TMS Core Application Service must invoke the ValidationEngine through TrainerAppService to enforce required fields and format validation specific to external trainers, at minimum requiring name, email, and type, and must return a 400 ErrorResponse with details when these are missing or incorrectly formatted.
- **US-003-ac-3**: When handling POST /trainers, TrainerAppService must query TrainerRepository to ensure that the provided employeeId (for internal trainers) or email (for any trainer type) is unique across all existing trainer records, and must return HTTP 400 with an ErrorResponse indicating a validation or duplicate error if a conflict is found.
- **US-003-ac-4**: When handling a successfully validated POST /trainers request that includes a locationId, TrainerAppService together with TrainerDomainService must derive and persist a timeZone value on the Trainer entity before saving via TrainerRepository, so that subsequent GET /trainers and related allocation/reporting endpoints return this timeZone field populated.
- **US-003-ac-5**: When a trainer profile is successfully created via POST /trainers, the Trainer aggregate persisted through TrainerRepository must have its status field set to active by default, making the trainer immediately eligible to appear in results from GET /trainers when the status filter is active or all.
- **US-003-ac-6**: For every successful trainer creation transaction through POST /trainers, TrainerAppService must use AuditLogClient to append an audit entry that records the actor identity from AuthContextProvider, a timestamp, the resourceType representing trainer, the new trainer id, an actionType representing create, and a summary or payload of key non sensitive fields, with this audit visible through the existing admin and audit search interfaces such as GET /admin/audit.
- **US-003-ac-7**: If a POST /trainers request fails validation or uniqueness checks, the service must ensure via TransactionManager that no partial Trainer record is persisted in tmsDb, and that only audit entries appropriate for failed attempts, if any, are written without creating an inconsistent trainer profile.