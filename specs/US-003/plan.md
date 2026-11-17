# Implementation Plan: US-003

## Technical Context
- unresolved reference (service:SVC-015)
- unresolved reference (story:US-003)
- unresolved reference (FR-US-003-001)
- unresolved reference (FR-US-003-002)
- unresolved reference (FR-US-003-003)
- unresolved reference (FR-US-003-004)
- unresolved reference (FR-US-003-005)
- unresolved reference (FR-US-003-006)
- unresolved reference (FR-US-003-007)

## Contract Changes
- No material API, event, or data contract change is evidenced.

## Security and Quality
- Validate credential and personal-data handling against the story's acceptance criteria
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **US-003-ac-1**: When an HR admin or super admin calls POST /api/v1/trainers with a CreateTrainerRequest whose type is INTERNAL, the service must enforce required fields including employeeId and validate field formats according to the Trainer and CreateTrainerRequest schemas, rejecting invalid payloads with HTTP 400 and a structured ErrorResponse listing field level errors via FieldError entries.
- **US-003-ac-2**: When an HR admin or super admin calls POST /api/v1/trainers with a CreateTrainerRequest whose type is EXTERNAL, the service must enforce required fields including email and locationId and validate field formats as defined in the Trainer and CreateTrainerRequest schemas, rejecting any missing or malformed required data with HTTP 400 and ErrorResponse.
- **US-003-ac-3**: When processing POST /api/v1/trainers, the service must check for uniqueness of trainer identifiers such that if an INTERNAL trainer is being created and any existing Trainer record, regardless of its ACTIVE or INACTIVE status, already has the same employeeId, the request is rejected with HTTP 400 ErrorResponse indicating the conflicting employeeId field, and similarly if any existing Trainer has the same email as the requested profile, the request is rejected with HTTP 400 highlighting the email conflict.
- **US-003-ac-4**: When processing POST /api/v1/trainers with a locationId that corresponds to a known timezone in the core database, the service must derive and persist the Trainer.timezone field based on that location so that subsequent scheduling and reporting endpoints that use Trainer.timezone behave consistently; if the trainer is updated via PUT /api/v1/trainers/{trainerId} with a new locationId, the timezone must be recalculated and stored accordingly.
- **US-003-ac-5**: Upon successful creation of a trainer via POST /api/v1/trainers, the service must persist the Trainer aggregate with status set to ACTIVE by default and ensure that this trainer is returned in eligible states by GET /api/v1/trainers and considered by GET /api/v1/allocations/eligible-trainers, subject to their certifications and availability, without requiring an explicit status change call.
- **US-003-ac-6**: For every successful trainer creation through POST /api/v1/trainers, the service must append an immutable audit event recording the actor, timestamp, trainerId, and non document trainer profile fields to the trainer specific audit trail exposed via GET /api/v1/trainers/{trainerId}/audit-logs, and must ensure that no sensitive document file contents from /api/v1/trainers/{trainerId}/documents are included in the audit payload.
- **US-003-ac-7**: If validation or duplicate identifier checks fail during POST /api/v1/trainers, the service must still record an appropriate audit or unauthorized access log entry according to its global audit model so that attempted but blocked profile creations are visible via the relevant audit log endpoints.

## Provenance
- **Implementation specification source hash**: `sha256:dde6f06528d7e8f836848b6c011156f769c87dbf9d4084a0821f702b38223cec`
- **Accepted impact revision**: _none; no accepted impact hash provided_
- **Policy version**: `ace-spec-format/v1`
- **Repository baseline commit**: `1e24775171e7dc2026e6fa760087c1df1ab0091f`
