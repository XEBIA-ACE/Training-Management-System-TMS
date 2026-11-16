# Implementation Plan: US-002

## Technical Context
- unresolved reference (service:SVC-008)
- unresolved reference (story:US-002)
- unresolved reference (FR-US-002-001)
- unresolved reference (FR-US-002-002)
- unresolved reference (FR-US-002-003)
- unresolved reference (FR-US-002-004)
- unresolved reference (FR-US-002-005)
- unresolved reference (FR-US-002-006)
- unresolved reference (FR-US-002-007)
- unresolved reference (dependency:CACHE)
- unresolved reference (dependency:OBS-LOG)
- unresolved reference (dependency:OIDC-PROVIDER)
- unresolved reference (dependency:AUTHZ-SVC)
- unresolved reference (data_model:AuditLogEntry)

## Contract Changes
- No material API, event, or data contract change is evidenced.

## Security and Quality
- Validate credential and personal-data handling against the story's acceptance criteria
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **US-002-ac-1**: When the frontend trainer directory invokes GET /trainers with combinations of query parameters status, certificationId, availabilityState, locationId, weekendAvailable, and search, the TrainerController and TrainerAppService must ensure that only Trainer entities matching all provided criteria are returned in the PagedTrainerList items collection, with no records that violate any of the filters.
- **US-002-ac-2**: When the search query parameter on GET /trainers contains a free text term representing trainer name, email, or employeeId, the TrainerRepository.Search implementation must match on those fields and combine the text predicate with all other active filters so that the effective result set respects both the search term and filter parameters together.
- **US-002-ac-3**: Under expected production-scale volumes of Trainer records, the TrainerAppService, TrainerRepository, and any use of CacheAdapter for trainer lookups must keep the end to end GET /trainers response time within the agreed performance SLO and avoid HTTP timeouts, as observed via LoggingMonitoring metrics for this operation.
- **US-002-ac-4**: When an authorized user selects an individual trainer from the filtered results and navigates to that profile, the GET /trainers/{trainerId} endpoint must return the corresponding Trainer details for the id present in the PagedTrainerList without leaking trainers the caller is not permitted to view, enforcing bearerAuth and RBAC checks as configured.
- **US-002-ac-5**: When an allocation workflow initiates from an action on a trainer in the directory, the system must be able to use the trainer id and basic attributes from the Trainer aggregate (as returned in PagedTrainerList or Trainer) to call downstream allocation APIs, without requiring additional round trips to resolve core trainer identity data.
- **US-002-ac-6**: For each GET /trainers request that satisfies internal auditing policy for search and filter operations, TrainerAppService must invoke AuditLogClient.AppendAuditEntry or equivalent so that an audit record is written identifying the actor, timestamp, and a high level summary of the applied criteria (such as which filter fields were used and whether free text was present), without persisting the raw free text content itself.
- **US-002-ac-7**: All trainer search and profile retrieval operations exposed via GET /trainers and GET /trainers/{trainerId} must enforce security through bearerAuth and AuthContextProvider so that users lacking appropriate trainer directory permissions receive a 403 response and an UnauthorizedAccessEntry is written if required by policy.