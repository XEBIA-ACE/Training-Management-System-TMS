# Tasks: US-002

## Implementation Tasks (Functional Requirements)
- [ ] T001 FR-US-002-001 (traces to US-002-ac-1): The system SHALL provide a POST '/orders' endpoint that, when invoked with a request body containing a valid existing customer ID, a valid existing product ID, and a quantity value strictly greater than zero, responds with an HTTP 2xx status code and a response payload that includes the details of the newly created order.
- [ ] T002 FR-US-002-002 (traces to US-002-ac-1, ambiguity preserved): The definition of "new order details" in the 2xx response payload (e.g., which fields are included beyond at least an order identifier, customer ID, product ID, and quantity) is NOT specified by this story and MAY vary; tests for this requirement SHALL only assert that some order representation is returned, not a specific schema beyond what other specifications define.
- [ ] T003 FR-US-002-003 (traces to US-002-ac-2): When a POST '/orders' request references a customer ID that does not correspond to any existing customer in the system, the endpoint SHALL NOT create an order and SHALL respond with HTTP 400 Bad Request and an error indication that the customer does not exist (exact error message text and structure are unspecified by this story).
- [ ] T004 FR-US-002-004 (traces to US-002-ac-3): When a POST '/orders' request references a product ID that does not correspond to any existing product in the system, the endpoint SHALL NOT create an order and SHALL respond with HTTP 400 Bad Request and an error indication that the product does not exist (exact error message text and structure are unspecified by this story).
- [ ] T005 FR-US-002-005 (traces to US-002-ac-4): When a POST '/orders' request includes a quantity value that is less than or equal to zero, the endpoint SHALL NOT create an order and SHALL respond with HTTP 400 Bad Request and an error indication that the quantity is invalid (exact error message text and structure are unspecified by this story).
- [ ] T006 FR-US-002-006 (traces to US-002-ac-5): For any POST '/orders' request that fails due to validation errors covered by this story (non-existent customer ID, non-existent product ID, or quantity less than or equal to zero), the system SHALL ensure that no corresponding order record is persisted in the system (i.e., after the failed request completes, there MUST be no new order that can be retrieved which matches the rejected input).
- [ ] T007 FR-US-002-007 (traces to US-002-ac-2, US-002-ac-3, US-002-ac-4, ambiguity preserved): If multiple validation errors (e.g., both unknown customer and invalid quantity) are present in a single POST '/orders' request, this story does NOT specify whether the '/orders' endpoint SHALL report one or multiple error reasons; the only mandated behavior is that it returns HTTP 400 Bad Request and does not create an order.

## Verification Tasks (Acceptance Criteria)
- [ ] T008 Verify US-002-ac-1: Ensure that the POST request to '/orders' endpoint correctly creates an order when given valid customer ID, product ID, and quantity greater than zero, by returning a 2xx status and the new order details.
- [ ] T009 Verify US-002-ac-2: Validate that the '/orders' endpoint returns a 400 Bad Request status when an order creation attempt is made with a non-existent customer ID, correctly indicating that the customer does not exist.
- [ ] T010 Verify US-002-ac-3: Validate that the '/orders' endpoint returns a 400 Bad Request status when an order creation attempt is made with a non-existent product ID, correctly indicating that the product does not exist.
- [ ] T011 Verify US-002-ac-4: Implement validation at the '/orders' endpoint to return a 400 Bad Request status when an order creation attempt is made with a quantity less than or equal to zero, correctly indicating that the quantity is invalid.
- [ ] T012 Verify US-002-ac-5: Ensure that no orders exist in the system that correspond to failed order creation attempts due to validation errors such as unknown customer, unknown product, or non-positive quantity.

## Evidence and Traceability Notes
- Each verification task T008–T012 corresponds directly to an acceptance-criterion clause and an associated test obligation ID in the implementation specification.
- Test execution results for these tasks MUST be captured as evidence artifacts and linked back to:
  - bb21c484-7d07-4825-a021-6cb339f07906:test:acceptance:US-002-ac-1
  - bb21c484-7d07-4825-a021-6cb339f07906:test:acceptance:US-002-ac-2
  - bb21c484-7d07-4825-a021-6cb339f07906:test:acceptance:US-002-ac-3
  - bb21c484-7d07-4825-a021-6cb339f07906:test:acceptance:US-002-ac-4
  - bb21c484-7d07-4825-a021-6cb339f07906:test:acceptance:US-002-ac-5
- No additional implementation or verification tasks are defined beyond what appears in the approved artifacts; any new tasks MUST be managed outside this export to preserve traceability to the original specification.