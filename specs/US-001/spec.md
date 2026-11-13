# Feature Specification: US-001

## Story Overview
- **Story ID**: US-001
- **Title**: Send Notifications for Trainer Allocation and Deallocation

## Behavior Summary
When a trainer allocation change (allocation, reallocation, or deallocation) is performed for a workshop, the system sends automated, template-based notifications to the affected primary trainer and co-trainer via email, and via SMS only when SMS notifications are configured. Notifications are dispatched asynchronously so that allocation workflows are not delayed. Each notification attempt and its outcome, including retries and failures, is recorded for traceability.

## User Scenarios & Acceptance Criteria
- **US-001-ac-1**: Ensure that POST requests to the /customers endpoint create a new customer resource by consuming the customer-service REST API, then display the customer ID, name, and city in the UI upon receiving a HTTP 201 response.
- **US-001-ac-2**: Present all customer records in the UI when a GET request is sent to the /customers endpoint by consuming the customer-service REST API and receiving a HTTP 200 response.
- **US-001-ac-3**: Use the /customers/:id route to fetch and display specific customer information by consuming the customer-service REST API when a GET request is sent with a valid customer ID, and correctly handle a 404 response if the customer does not exist.
- **US-001-ac-4**: Utilize the /customers/:id endpoint to send customer update requests for name and/or city changes, reflecting these changes in the UI after receiving a HTTP 200 response from the customer-service REST API.
- **US-001-ac-5**: Implement DELETE requests using the /customers/:id endpoint to remove customers, ensuring the UI reflects success upon receiving a 200 or 204 response, and handle a 404 response correctly if the customer is not found.

## Functional Requirements
- FR-US-001-001 (US-001-ac-1): The system SHALL send an HTTP POST request to the /customers endpoint of the customer-service REST API when creating a new customer resource, and upon receiving an HTTP 201 Created response, SHALL display in the UI at least the returned customer ID, name, and city. This requirement assumes, but does not define, the structure of the POST payload and the full response body from the customer-service API.
- FR-US-001-002 (US-001-ac-2): The system SHALL send an HTTP GET request to the /customers endpoint of the customer-service REST API to retrieve all customer records, and when an HTTP 200 OK response is received, SHALL present all customer records returned by that response in the UI. This requirement does not specify how pagination, filtering, or partial responses (if any) are handled by the API or UI.
- FR-US-001-003 (US-001-ac-3): The system SHALL use the /customers/:id route of the customer-service REST API to send an HTTP GET request for a specific customer when a valid customer ID is provided, SHALL display that customer’s information in the UI when an HTTP 200 OK response is received, and SHALL correctly handle an HTTP 404 Not Found response by indicating in the UI that the customer does not exist (exact wording and visual treatment are not specified here).
- FR-US-001-004 (US-001-ac-4): The system SHALL use the /customers/:id endpoint of the customer-service REST API to send HTTP requests to update an existing customer’s name and/or city, and upon receiving an HTTP 200 OK response, SHALL reflect the updated name and/or city in the UI. This requirement does not specify whether other customer fields may also be updated or how concurrent updates are resolved.
- FR-US-001-005 (US-001-ac-5): The system SHALL send HTTP DELETE requests to the /customers/:id endpoint of the customer-service REST API to remove customers, and when an HTTP 200 OK or 204 No Content response is received, SHALL update the UI to indicate successful removal of the customer. If an HTTP 404 Not Found response is received, the system SHALL indicate in the UI that the customer was not found; this requirement does not define the exact UI messaging or behavior in other error cases.

## Success Criteria (per Acceptance Criterion)
- **US-001-ac-1**: Ensure that POST requests to the /customers endpoint create a new customer resource by consuming the customer-service REST API, then display the customer ID, name, and city in the UI upon receiving a HTTP 201 response.
- **US-001-ac-2**: Present all customer records in the UI when a GET request is sent to the /customers endpoint by consuming the customer-service REST API and receiving a HTTP 200 response.
- **US-001-ac-3**: Use the /customers/:id route to fetch and display specific customer information by consuming the customer-service REST API when a GET request is sent with a valid customer ID, and correctly handle a 404 response if the customer does not exist.
- **US-001-ac-4**: Utilize the /customers/:id endpoint to send customer update requests for name and/or city changes, reflecting these changes in the UI after receiving a HTTP 200 response from the customer-service REST API.
- **US-001-ac-5**: Implement DELETE requests using the /customers/:id endpoint to remove customers, ensuring the UI reflects success upon receiving a 200 or 204 response, and handle a 404 response correctly if the customer is not found.

## Contract & API Impact
- Contract change required: **false**
- No material API, event, or data contract change is evidenced.
- Impacted services: none identified in API impact analysis.

## Architectural & Technical Design References
- Primary service: **SVC-004 – Training Operations Core Service**
  - Evidence ref: `service:SVC-004`
  - Design is degraded and must be cross-checked with the referenced service documentation.
  - Implement changes in grounded service SVC-004.

## Security & Quality Obligations
- Validate credential and personal-data handling against the story's acceptance criteria.

## Traceability
- Clause-to-criterion mapping:
  - Clause US-001-ac-1 → Criterion US-001-ac-1
  - Clause US-001-ac-2 → Criterion US-001-ac-2
  - Clause US-001-ac-3 → Criterion US-001-ac-3
  - Clause US-001-ac-4 → Criterion US-001-ac-4
  - Clause US-001-ac-5 → Criterion US-001-ac-5
- Traceability matrix entries:
  - US-001-ac-1 → functional spec ref: `spec_functional`, accepted_impact_hash: `null`
  - US-001-ac-2 → functional spec ref: `spec_functional`, accepted_impact_hash: `null`
  - US-001-ac-3 → functional spec ref: `spec_functional`, accepted_impact_hash: `null`
  - US-001-ac-4 → functional spec ref: `spec_functional`, accepted_impact_hash: `null`
  - US-001-ac-5 → functional spec ref: `spec_functional`, accepted_impact_hash: `null`

## Readiness & Gaps
- API impact checkpoint status: ready; no contract changes required.
- Technical design marked as **degraded**; implementers MUST consult the referenced service documentation `service:SVC-004` before final design decisions.
- No explicit migration concerns, state transitions, or screen component behaviors are defined in the current specification.

## Provenance
- **Policy version**: `ace-spec-format/v1`
- **Specification source hash**: `sha256:7039ff482cd88051bab1ccc60add57999b22d8e772db879c131f88fdc437aa1a`
- **Repository baseline commit**: `94164df170bc95931c327ee27955ff2fa9fc3194`
- **Generation mode**: greenfield
- **Requirement source**: ace_context
- **Impact revision**: none accepted (accepted_impact_hash = null)
