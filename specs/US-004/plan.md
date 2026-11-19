# Implementation Plan: US-004

## Technical Context
- unresolved reference (artifact-SVC-001-api_spec)
- unresolved reference (artifact-SVC-001-c3)
- unresolved reference (artifact-SVC-001-c4)
- unresolved reference (service:SVC-001)

## Contract Changes
- Trainer status change endpoint must enforce RBAC, only update the Trainer.status field, and trigger audit logging for each successful change per US-004 acceptance criteria.
- Trainer candidate search must exclude inactive trainers from allocation-eligible results by default and support an includeInactive filter for view-only scenarios.
- Trainer allocation operation must check trainer status and fail with a clear, localized error if the trainer is inactive, without creating a WorkshopAllocation record.
- Audit logs API must support filtering by trainer identifier, status transition, actor, and timestamp to retrieve trainer status change events, enforcing ADMIN.AUDIT.VIEW permission.

## Security and Quality
- Validate credential and personal-data handling against the story's acceptance criteria
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **US-004-ac-1**: When an authorized caller invokes an API endpoint that maps to the trainer-app-api operation changeTrainerStatus(trainerId, newStatus), the AppTrainerService must validate the caller's permission via the Authorization and RBAC Component and update the Trainer entity's status field in the Trainer and Allocation Repository without modifying or deleting any other profile attributes.
- **US-004-ac-2**: When a trainer's status is set to an inactive value through changeTrainerStatus, the Domain Services Layer and AppTrainerService must ensure that subsequent calls to searchTrainers(criteria) exclude inactive trainers from general allocation-eligible result sets that are used by POST /api/workshops/{id}/allocate-trainer, unless a dedicated includeInactive filter explicitly allows viewing but not allocation.
- **US-004-ac-3**: When AppTrainerService processes allocateTrainerToWorkshop(workshopId, trainerId, role), it must consult the Trainer and Allocation Repository and Domain Services Layer so that if the target trainer has an inactive status the operation fails without creating a WorkshopAllocation record, and the API and Controller Layer returns a clear, localized error message indicating that inactive trainers cannot be allocated.
- **US-004-ac-4**: When changeTrainerStatus transitions a trainer from inactive back to active, AppTrainerService together with TrainerRepository and CatalogRepository must ensure that subsequent searchTrainers(criteria) and allocation eligibility checks once again include the trainer only if they meet other eligibility conditions such as required certifications and availability, using getTrainerConflicts and certification lookups as needed.
- **US-004-ac-5**: For every successful trainer status change executed via changeTrainerStatus, the Domain Services Layer and Audit Log Repository must append an immutable audit event that records the trainer identifier, previous status, new status, acting user, and timestamp, and this event must be queryable via GET /api/admin/audit-logs with appropriate filters.