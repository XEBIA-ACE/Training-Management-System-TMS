# Implementation Plan: US-003

## Technical Context
- unresolved reference (artifact-SVC-001-api_spec)
- unresolved reference (artifact-SVC-001-c3)
- unresolved reference (artifact-SVC-001-c4)
- unresolved reference (service:SVC-001)
- unresolved reference (spec_functional.US-003)

## Contract Changes
- No material API, event, or data contract change is evidenced.

## Security and Quality
- Validate credential and personal-data handling against the story's acceptance criteria
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **US-003-ac-1**: When the client calls POST /trainers with a TrainerCreateRequest, the REST controller and Request Validation component must enforce required fields based on the type field such that INTERNAL trainers require a non empty employeeId while EXTERNAL trainers do not, and email, name, location, and type are always present and correctly formatted, returning an ErrorResponse with field level details on any violation.
- **US-003-ac-2**: When handling POST /trainers, the Trainer Application Service and Trainer Repository must enforce uniqueness of email across all trainer records regardless of status ACTIVE or INACTIVE and additionally enforce uniqueness of employeeId for INTERNAL trainers, returning a 400 response with an ErrorResponse that identifies the conflicting field when a duplicate is detected.
- **US-003-ac-3**: Upon successful creation of a trainer via POST /trainers, the Trainer Application Service must persist the trainer with status set to ACTIVE in the underlying TrainerResponse.status field so that subsequent GET /trainers queries with status ACTIVE include the new trainer without requiring a separate status change call.
- **US-003-ac-4**: When a valid location value is supplied in TrainerCreateRequest and POST /trainers succeeds, the Trainer Application Service and underlying domain must derive and persist an appropriate timeZone value in the TrainerResponse.timeZone field, using platform configuration or scheduling domain rules, so that downstream scheduling and notification components can rely on this stored timezone without additional derivation at read time.
- **US-003-ac-5**: For each successful trainer creation through POST /trainers, the REST controller or Trainer Application Service must invoke the Audit Logging Component so that an immutable AuditEvent with module set to TRAINER, actionType indicating create, actorUserId equal to the authenticated user, timestamp, and non sensitive key fields such as trainerId, type, employeeId when present, email, location, and status is appended to the audit log via /audit/events or the AuditLogRepository.
- **US-003-ac-6**: All validation and duplicate constraint failures during POST /trainers must be returned using the standard ErrorResponse schema with code values such as VALIDATION_ERROR or a duplicate specific code and populated details entries for each invalid field, consistent with the shared error handling rules in the Training Management Core Service.