# Implementation Plan: US-003

## Technical Context
- unresolved reference (artifact-SVC-005-api_spec)
- unresolved reference (artifact-SVC-005-c3)
- unresolved reference (artifact-SVC-005-c4)
- unresolved reference (service:SVC-005)
- unresolved reference (service:SVC-012)
- unresolved reference (service:SVC-013)
- unresolved reference (service:SVC-014)

## Contract Changes
- No material API, event, or data contract change is evidenced.

## Security and Quality
- Validate credential and personal-data handling against the story's acceptance criteria
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **US-003-ac-1**: When the frontend submits POST /trainers with trainerType INTERNAL, the TrainerCreateRequest validation must enforce that employeeId is present, email is well formed, and all other required fields in the schema name, location are provided; on any missing or malformed field the API must return HTTP 400 with an ErrorResponse that includes field level ValidationError entries highlighting the invalid fields.
- **US-003-ac-2**: When the frontend submits POST /trainers with trainerType EXTERNAL, the TrainerCreateRequest validation must enforce that email and location and name are present and well formed and that any optional fields such as msaNdaStatus follow their enum constraints; on validation failure the API must return HTTP 400 with appropriate ValidationError entries and must not create a trainer record.
- **US-003-ac-3**: When POST /trainers is called with an employeeId already present on any existing trainer for INTERNAL trainers or an email already present on any existing trainer for either INTERNAL or EXTERNAL trainers, including trainers that are currently inactive, the Trainer and Certification Repository lookup must detect the duplicate and the controller must return HTTP 409 Conflict with an ErrorResponse whose errorCode clearly indicates a duplicate employeeId or email and no new trainer record is persisted.
- **US-003-ac-4**: When POST /trainers is called with a valid location string that maps to a known location or time zone rule, the Trainer and Certification Application Service must derive a timeZone value based on that location and persist it to the trainer record so that TrainerResponse includes the populated timeZone field on success.
- **US-003-ac-5**: When a trainer is successfully created via POST /trainers the Trainer and Certification Application Service must default the trainer status field to ACTIVE in persistence and ensure that the new trainer appears in subsequent GET /trainers queries that filter by status ACTIVE and can be returned as a TrainerResponse used by downstream allocation and search flows.
- **US-003-ac-6**: On every successful invocation of POST /trainers that results in a new trainer profile, the Trainer and Certification Application Service must create an immutable AuditEntry via the Audit and Logging Component with module set to trainer, recordId set to the new trainerId, actor information from the authenticated user, timestamp of creation, and non sensitive field values such as name, email, employeeId, trainerType and location while excluding any document file contents.
- **US-003-ac-7**: If POST /trainers is attempted without authentication or with insufficient TRAINER.MANAGE style permissions as enforced by the RBAC and Authorization Component, the API must reject the request with 401 or 403 and must not create or modify any trainer records, while still logging denied access through the security events and audit mechanisms as described in the shared ErrorResponse and AuditEntry models.