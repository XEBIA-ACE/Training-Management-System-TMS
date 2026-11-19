# Implementation Plan: US-004

## Technical Context
- unresolved reference (artifact-SVC-001-api_spec)
- unresolved reference (artifact-SVC-001-c3)
- unresolved reference (artifact-SVC-001-c4)
- unresolved reference (service:SVC-001)
- unresolved reference (spec-functional:US-004)

## Contract Changes
- POST — Add POST /api/v1/trainers/{trainerId}/status to toggle a trainer's status between ACTIVE and INACTIVE based on TrainerStatusChangeRequest, enforcing RBAC and emitting audit events.
- POST — Refine POST /api/v1/allocations behavior to reject allocation attempts for trainers whose status is INACTIVE with a 4xx ErrorResponse and clear message, without persisting an allocation record.
- GET — Adjust default GET /api/v1/trainers listing/search semantics to exclude INACTIVE trainers by default unless an explicit status filter or INACTIVE_DATA.VIEW-style permission is present.
- GET — Ensure trainer search backing allocation eligibility excludes INACTIVE trainers by default, only including them when explicit filters or INACTIVE_DATA.VIEW apply.
- GET — Confirm audit events for trainer status changes are exposed via existing GET /api/v1/audit/events endpoint without schema changes, relying on TRAINER module events.
- GET — Ensure existing trainer-specific audit listing surfaces status change events with previous/new status and optional reason, without requiring response schema changes.
- GET — Ensure denied trainer status changes and blocked inactive-trainer allocations are logged as denied-access audit events consumable via GET /api/v1/security/denied-access, without changing its response schema.

## Security and Quality
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **US-004-ac-1**: When a client calls POST /trainers/{trainerId}/status with a valid TrainerStatusChangeRequest and the authenticated user has the necessary trainer lifecycle permission, the Training Management Core Service updates only the TrainerResponse.status field between ACTIVE and INACTIVE and preserves all other trainer profile fields unchanged in the underlying trainer repository.
- **US-004-ac-2**: When a trainer is set to INACTIVE via POST /trainers/{trainerId}/status, subsequent calls to GET /trainers and any trainer search used internally by POST /allocations must, by default, exclude that trainer from eligibility and list results unless an explicit status or INACTIVE_DATA.VIEW style permission is applied in accordance with centralized visibility rules.
- **US-004-ac-3**: When POST /allocations is invoked to allocate a trainer whose TrainerResponse.status is INACTIVE, the allocation application service must reject the request with a 4xx ErrorResponse, not create a TrainerAllocationResponse record, and include a human readable message that clearly indicates the trainer is inactive and cannot be allocated.
- **US-004-ac-4**: When an inactive trainer is reactivated through POST /trainers/{trainerId}/status with status ACTIVE, subsequent GET /trainers results and internal allocation eligibility checks backing POST /allocations must again consider that trainer, subject to other eligibility constraints such as certificationIds and availability encoded in domainAllocationRules and related models.
- **US-004-ac-5**: Each successful trainer status change performed through POST /trainers/{trainerId}/status must result in an append only audit event written through the Audit Logging Component and exposed via GET /audit/events and GET /trainers/{trainerId}/audit-events, capturing at minimum module TRAINER, the trainer recordId, previous and new status, actorUserId derived from the JWT, timestamp, and any provided reason from TrainerStatusChangeRequest.
- **US-004-ac-6**: When a status change or blocked allocation is denied due to missing permissions or inactive visibility rules, the service must log a denied access audit event via the unauthorized access logging endpoint semantics, so that GET /security/denied-access surfaces these attempts for monitoring.