# Tasks: US-001

## Implementation Tasks
- [ ] T001 FR-US-001-001 (US-001-ac-1): The system SHALL send an HTTP POST request to the /customers endpoint of the customer-service REST API when creating a new customer resource, and upon receiving an HTTP 201 Created response, SHALL display in the UI at least the returned customer ID, name, and city. This requirement assumes, but does not define, the structure of the POST payload and the full response body from the customer-service API.
- [ ] T002 FR-US-001-002 (US-001-ac-2): The system SHALL send an HTTP GET request to the /customers endpoint of the customer-service REST API to retrieve all customer records, and when an HTTP 200 OK response is received, SHALL present all customer records returned by that response in the UI. This requirement does not specify how pagination, filtering, or partial responses (if any) are handled by the API or UI.
- [ ] T003 FR-US-001-003 (US-001-ac-3): The system SHALL use the /customers/:id route of the customer-service REST API to send an HTTP GET request for a specific customer when a valid customer ID is provided, SHALL display that customer’s information in the UI when an HTTP 200 OK response is received, and SHALL correctly handle an HTTP 404 Not Found response by indicating in the UI that the customer does not exist (exact wording and visual treatment are not specified here).
- [ ] T004 FR-US-001-004 (US-001-ac-4): The system SHALL use the /customers/:id endpoint of the customer-service REST API to send HTTP requests to update an existing customer’s name and/or city, and upon receiving an HTTP 200 OK response, SHALL reflect the updated name and/or city in the UI. This requirement does not specify whether other customer fields may also be updated or how concurrent updates are resolved.
- [ ] T005 FR-US-001-005 (US-001-ac-5): The system SHALL send HTTP DELETE requests to the /customers/:id endpoint of the customer-service REST API to remove customers, and when an HTTP 200 OK or 204 No Content response is received, SHALL update the UI to indicate successful removal of the customer. If an HTTP 404 Not Found response is received, the system SHALL indicate in the UI that the customer was not found; this requirement does not define the exact UI messaging or behavior in other error cases.

## Verification Tasks
- [ ] T006 Verify US-001-ac-1: Ensure that POST requests to the /customers endpoint create a new customer resource by consuming the customer-service REST API, then display the customer ID, name, and city in the UI upon receiving a HTTP 201 response.
- [ ] T007 Verify US-001-ac-2: Present all customer records in the UI when a GET request is sent to the /customers endpoint by consuming the customer-service REST API and receiving a HTTP 200 response.
- [ ] T008 Verify US-001-ac-3: Use the /customers/:id route to fetch and display specific customer information by consuming the customer-service REST API when a GET request is sent with a valid customer ID, and correctly handle a 404 response if the customer does not exist.
- [ ] T009 Verify US-001-ac-4: Utilize the /customers/:id endpoint to send customer update requests for name and/or city changes, reflecting these changes in the UI after receiving a HTTP 200 response from the customer-service REST API.
- [ ] T010 Verify US-001-ac-5: Implement DELETE requests using the /customers/:id endpoint to remove customers, ensuring the UI reflects success upon receiving a 200 or 204 response, and handle a 404 response correctly if the customer is not found.

## Test Obligations & Evidence
- Test obligations (CI, required before release):
  - e51336cf-d749-4a4a-9e59-b6322b3b4237:test:acceptance:US-001-ac-1
  - e51336cf-d749-4a4a-9e59-b6322b3b4237:test:acceptance:US-001-ac-2
  - e51336cf-d749-4a4a-9e59-b6322b3b4237:test:acceptance:US-001-ac-3
  - e51336cf-d749-4a4a-9e59-b6322b3b4237:test:acceptance:US-001-ac-4
  - e51336cf-d749-4a4a-9e59-b6322b3b4237:test:acceptance:US-001-ac-5
- Expected evidence:
  - test result for US-001-ac-1
  - test result for US-001-ac-2
  - test result for US-001-ac-3
  - test result for US-001-ac-4
  - test result for US-001-ac-5
