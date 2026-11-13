# Implementation Plan: US-002

## Technical Context
- unresolved reference (service:SVC-007)
- unresolved reference (story:US-002)
- unresolved reference (FR-US-002-001)
- unresolved reference (FR-US-002-002)
- unresolved reference (FR-US-002-003)
- unresolved reference (FR-US-002-004)
- unresolved reference (FR-US-002-005)
- unresolved reference (FR-US-002-006)
- unresolved reference (FR-US-002-007)
- unresolved reference (AC:US-002-ac-1)
- unresolved reference (AC:US-002-ac-2)
- unresolved reference (AC:US-002-ac-3)
- unresolved reference (AC:US-002-ac-4)
- unresolved reference (AC:US-002-ac-5)
- unresolved reference (AC:US-002-ac-6)
- unresolved reference (AC:US-002-ac-7)

## Contract Changes
- change required (Introduce or standardize GET /api/trainers within SVC-007 (Trainer Management Core Application Service) as the trainer directory endpoint that supports structured filters (status, OEM certification identifiers, allocation state, location, weekend availability) and an optional free-text search term for name, email, or employee identifier.)
- change required (Extend the query parameter set of GET /api/trainers to include allocation-state filters, OEM certification identifiers, location, weekend-availability flag, and a free-text search term while preserving existing behavior for any previously supported parameters.)
- change required (Extend the response model for each trainer entry returned by GET /api/trainers to include: (a) per-trainer allocation state indicators (free, tentative, confirmed); (b) identifiers and routing information sufficient to navigate to trainer profile flows backed by CreateTrainer, UpdateTrainer, and GetTrainerById; and (c) trainer and availability identifiers required for downstream POST /api/v1/workshops/{id}/allocate-trainer calls.)

## Security and Quality
- Validate credential and personal-data handling against the story's acceptance criteria
- Verify the changed user flow against the stated accessibility criteria

## Implementation Steps
- Implement changes in grounded service SVC-007.
- Within SVC-007, define or extend the HTTP API Controller for GET /api/trainers to accept query parameters for trainer status, OEM certification identifiers, allocation state, location, weekend availability, and an optional free-text search term, routing requests to Trainer Application Service SearchTrainers. [evidence: service:SVC-007, story:US-002, FR-US-002-001, FR-US-002-002]
- Extend the Trainer Application Service SearchTrainers operation in SVC-007 to compose logical-AND filtering across all provided structured filters (status, OEM certifications, allocation state, location, weekend availability) and delegate to Trainer Domain Services for applying free-text search over trainer name, email, and employee identifier. [evidence: service:SVC-007, story:US-002, AC:US-002-ac-1, AC:US-002-ac-2, FR-US-002-001, FR-US-002-002]
- Enhance Trainer Domain Services in SVC-007 to intersect the free-text search result set with the structured filter result set before paging, without specifying matching semantics beyond what is defined in FR-US-002-002. [evidence: service:SVC-007, story:US-002, FR-US-002-002]
- Optimize the Trainer Repository SearchTrainers query in SVC-007 (Core-OLTP-DB access) for expected production-scale data, ensuring the initial page for GET /api/trainers meets externally defined performance thresholds without degrading other transactional workloads; capture relevant timing metrics via the Logging and Monitoring Component. [evidence: service:SVC-007, story:US-002, AC:US-002-ac-3, FR-US-002-003]
- Extend Workshop and Allocation Repository components and GetTrainerAllocations usage in SVC-007 so that Trainer Application Service can derive per-trainer allocation flags (free, tentative, confirmed) for inclusion in GET /api/trainers responses, without resolving representation ambiguities noted in FR-US-002-004. [evidence: service:SVC-007, story:US-002, AC:US-002-ac-4, FR-US-002-004]
- Adjust the GET /api/trainers response model in SVC-007 so each trainer record includes identifiers and routing information sufficient for the frontend to navigate to trainer profile flows backed by CreateTrainer, UpdateTrainer, and GetTrainerById, avoiding additional lookups for basic identity fields. [evidence: service:SVC-007, story:US-002, AC:US-002-ac-5, FR-US-002-005]
- Extend the GET /api/trainers response model in SVC-007 to include trainer and availability identifiers required by downstream POST /api/v1/workshops/{id}/allocate-trainer calls, ensuring the Workshop and Allocation Application Service AllocateTrainer enforces availability and conflict rules prior to confirming allocations. [evidence: service:SVC-007, story:US-002, AC:US-002-ac-6, FR-US-002-006]
- Update the HTTP API Controller Layer for GET /api/trainers in SVC-007 to emit structured search-completion logs via the Logging and Monitoring Component, including authenticated user, timestamp, and high-level filter criteria, while omitting or masking sensitive free-text content as required by external logging policy. [evidence: service:SVC-007, story:US-002, AC:US-002-ac-7, FR-US-002-007]

## Verification Plan
- **US-002-ac-1**: When the UI requests trainer directory results via GET /api/v1/trainers with query parameters for status, OEM certification identifiers, allocation state, location, and weekend availability, the Trainer Application Service SearchTrainers query combines all provided filters so that only trainers satisfying every specified criterion are returned from Trainer Repository SearchTrainers.
- **US-002-ac-2**: When the UI includes a free text search term (covering trainer name, email, or employee identifier) alongside structured filters in GET /api/v1/trainers, the Trainer Domain Services apply the text term against the appropriate trainer fields and intersect those matches with the structured filter set before returning the paged result to the client.
- **US-002-ac-3**: When the trainer dataset volume is at expected production scale, the Trainer Repository SearchTrainers operation executing behind GET /api/v1/trainers must return the initial page of results within the agreed performance threshold, as measured by Logging and Monitoring Component metrics, without timing out or degrading other transactional workloads.
- **US-002-ac-4**: When the client requests trainer directory data including allocation state indicators, Trainer Application Service uses GetTrainerAllocations together with Workshop and Allocation Repository data to derive and return per trainer allocation flags for free, tentative, and confirmed states in the GET /api/v1/trainers response model.
- **US-002-ac-5**: When a user selects an individual trainer from the directory results and chooses to view the profile, GET /api/v1/trainers responds with enough identifier and routing information for the frontend to navigate to the detailed trainer profile view backed by CreateTrainer, UpdateTrainer, and GetTrainerById flows without additional lookup calls for basic identity fields.
- **US-002-ac-6**: When a user selects an individual trainer from the directory results and chooses to initiate an allocation, GET /api/v1/trainers returns trainer and availability identifiers required for downstream POST /api/v1/workshops/{id}/allocate-trainer calls, and Workshop and Allocation Application Service AllocateTrainer enforces availability and conflict rules before confirming the allocation.
- **US-002-ac-7**: Whenever a trainer search or filter operation completes through GET /api/v1/trainers, the HTTP API Controller Layer invokes Logging and Monitoring Component to emit a structured log entry including the authenticated user, timestamp, and high level filter criteria used for the search, ensuring any sensitive free text is not logged in full but is either omitted or appropriately masked according to logging policy.