# Implementation Plan: US-004

## Technical Context
- unresolved reference (artifact-SVC-001-api_spec)
- unresolved reference (artifact-SVC-001-c3)
- unresolved reference (artifact-SVC-001-c4)
- unresolved reference (service:SVC-001)
- unresolved reference (story:US-004)
- unresolved reference (FR-US-004-AC1-001)
- unresolved reference (FR-US-004-AC1-002)
- unresolved reference (FR-US-004-AC1-003)
- unresolved reference (FR-US-004-AC2-001)
- unresolved reference (FR-US-004-AC2-002)
- unresolved reference (FR-US-004-AC2-003)
- unresolved reference (FR-US-004-AC2-004)
- unresolved reference (FR-US-004-AC3-001)
- unresolved reference (FR-US-004-AC3-002)
- unresolved reference (FR-US-004-AC4-001)
- unresolved reference (FR-US-004-AC4-002)
- unresolved reference (FR-US-004-AC4-003)
- unresolved reference (FR-US-004-AC5-001)
- unresolved reference (FR-US-004-AC5-002)
- unresolved reference (FR-US-004-AC5-003)
- unresolved reference (FR-US-004-AC6-001)
- unresolved reference (FR-US-004-AC6-002)
- unresolved reference (FR-US-004-AC6-003)
- unresolved reference (FR-US-004-AC7-001)

## Contract Changes
- Trainer lifecycle and allocation APIs impacted by introduction of trainer activation/inactivation semantics, exclusion of INACTIVE trainers from default search and eligibility, and enforcement at allocation time.

## Security and Quality
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **US-004-ac-1**: When an HR_ADMIN or SUPER_ADMIN calls PATCH /api/v1/trainers/{trainerId}/status with a valid status value such as ACTIVE or INACTIVE, the service updates only the Trainer.status field and related status metadata while preserving all other Trainer profile fields unchanged, returning the updated Trainer representation.
- **US-004-ac-2**: When a trainer’s status is set to INACTIVE via PATCH /api/v1/trainers/{trainerId}/status, subsequent calls to GET /api/v1/trainers and GET /api/v1/allocations/eligible-trainers must exclude that trainer from default search and eligibility results unless the caller explicitly filters for INACTIVE or ALL, ensuring they cannot be selected for new allocations.
- **US-004-ac-3**: When a client attempts to allocate a trainer whose Trainer.status is INACTIVE using POST /api/v1/allocations, the service must reject the request with HTTP 400 or 403 using ErrorResponse, include a clear business message indicating the trainer is inactive and cannot be allocated, and must not create an Allocation record.
- **US-004-ac-4**: When a previously inactive trainer is reactivated to ACTIVE via PATCH /api/v1/trainers/{trainerId}/status, subsequent GET /api/v1/trainers and GET /api/v1/allocations/eligible-trainers calls must again consider that trainer as eligible for search and allocation results provided other filters such as certifications and availability match, using existing Trainer.certifications and availability logic.
- **US-004-ac-5**: Every successful or failed attempt to change a trainer’s status via PATCH /api/v1/trainers/{trainerId}/status must append an immutable audit entry, accessible via GET /api/v1/trainers/{trainerId}/audit-logs and or /api/v1/admin/audit-logs, that records the trainer identifier, previous status, new status, acting user, timestamp, and outcome such as SUCCESS or DENIED.
- **US-004-ac-6**: If a caller without HR_ADMIN or SUPER_ADMIN role invokes PATCH /api/v1/trainers/{trainerId}/status, the service must deny the request with HTTP 403 using ErrorResponse and record the denied attempt in the unauthorized access logs exposed via GET /api/v1/admin/unauthorized-access-logs, without changing the trainer’s status.'],'rationale':'The story is about activating and inactivating trainer profiles and their impact on allocation and audit, which is directly implemented by SVC-007 endpoints such as PATCH /api/v1/trainers/{trainerId}/status for status toggling, GET /api/v1/allocations/eligible-trainers and POST /api/v1/allocations for allocation behavior, and trainer specific and global audit endpoints for lifecycle and access logging.'}]}
- **US-004-ac-7**: rationale