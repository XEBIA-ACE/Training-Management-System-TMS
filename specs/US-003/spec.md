# Feature Specification: US-003

## Story Overview
- **Story ID**: US-003
- **Title**: Create Trainer Profile (with Required Fields & Validation)

## Behavior Summary
When an authorized HR/admin user opens the Create Trainer Profile screen at route /trainers/new, they can create an internal or external trainer profile by entering the required identity/contact details, employee identifier (for internal trainers), email (for external trainers), location, and other mandatory attributes. The screen uses shared design system components to provide inline validation and feedback, performs client-side required-field and format validation based on the selected trainer type, and calls a trainer-service endpoint to check for duplicate employee identifiers or emails before creating the trainer. If duplicates or validation issues are found, the screen shows field-level errors without navigating away. On successful profile creation, the frontend confirms the returned trainer status is Active, stores the server-derived time zone value from the selected location, updates client-side state so the new trainer is immediately available in trainer search and allocation UIs, and either emits an audit-service-compatible event or navigates to /trainers/:trainerId/audit so that an audit record of the create action can be verified.

## Acceptance Criteria & User Scenarios
- **US-003-ac-1**: Trainer Management micro frontend defines and wires a Create Trainer Profile screen at route /trainers/new that captures trainer type, required fields, employee identifier for internal trainers, email for external trainers, and location, using components from the shared design system for input validation feedback.
- **US-003-ac-2**: Form-level validation logic in trainer-mgmt-web ensures that required fields for the selected trainer type are enforced on the client side, preventing submission and displaying inline error messages when required fields are empty or in an invalid format before calling trainer-service create APIs.
- **US-003-ac-3**: Before submitting a new trainer profile, the Trainer Management module invokes a trainer-service endpoint that checks for existing profiles with the same employee identifier or email (across active and inactive records) and surfaces any duplicates as field-level error messages without navigating away from /trainers/new.
- **US-003-ac-4**: On successful response from trainer-service create profile, the frontend reads the returned trainer status field and confirms it is Active, then updates the client-side state so that the newly created trainer is immediately available in trainer search and allocation UIs that consume trainer-service data.
- **US-003-ac-5**: When trainer-service returns the created trainer resource, the frontend displays or stores the derived time zone value that was computed server side from the chosen location so that subsequent scheduling and notification UIs can rely on it without additional user input.
- **US-003-ac-6**: Upon successful profile creation, the Trainer Management micro frontend triggers an audit-service compatible event or navigates to the trainer audit view route /trainers/:trainerId/audit so that users can confirm that an audit entry was recorded for the create action, including actor identity and timestamp but excluding any sensitive document content.

## Functional Requirements
- FR-US-003-001 (US-003-ac-1): The Trainer Management micro frontend SHALL expose a Create Trainer Profile screen at route /trainers/new that allows the user to input at minimum: trainer type (internal/external), all required base fields for a trainer profile, employee identifier when trainer type = internal, email address when trainer type = external, and a location selection, and SHALL implement these inputs using shared design system components that support displaying input validation feedback inline.
- FR-US-003-002 (US-003-ac-1): The /trainers/new screen in the Trainer Management micro frontend SHALL be reachable only to authorized users via the application navigation or direct URL and, once loaded, SHALL render the form in a ready-to-edit state without requiring additional navigation steps.
- FR-US-003-003 (US-003-ac-2): The trainer-mgmt-web frontend SHALL implement client-side form-level validation that enforces all required fields for the currently selected trainer type (internal or external) such that clicking the submit/save action when any required field is empty or in an invalid format prevents the create API from being called and instead displays inline error messages adjacent to the offending fields.
- FR-US-003-004 (US-003-ac-2): Client-side validation on /trainers/new SHALL include, at a minimum, format validation for email fields (for external trainers) and any other data types that the UI can validate locally (e.g., non-empty text, allowed character sets), and SHALL clear the inline error message for a field once the user corrects the value and it passes validation.
- FR-US-003-005 (US-003-ac-3): Before issuing the trainer-service create profile call for a submitted form that has passed client-side validation, the Trainer Management module SHALL invoke a trainer-service endpoint specifically intended to check for duplicate trainer identities by employee identifier and/or email across both active and inactive trainer records.
- FR-US-003-006 (US-003-ac-3): If the duplicate-check endpoint indicates that one or more existing trainer profiles share the same employee identifier or email as the values entered on /trainers/new, the frontend SHALL not proceed with creating the trainer and instead SHALL display field-level error messages on the conflicting employee identifier and/or email fields, while keeping the user on the /trainers/new route with all other form data preserved.
- FR-US-003-007 (US-003-ac-3): When no duplicates are reported by the trainer-service duplicate-check endpoint, the Trainer Management module SHALL proceed to call the trainer-service create profile API using the values entered on the form, without requiring additional user confirmation beyond the original submit action.
- FR-US-003-008 (US-003-ac-4): Upon receiving a successful response from the trainer-service create profile API, the frontend SHALL read the trainer status field in the returned payload and verify that its value corresponds to an "Active" state as defined by trainer-service; if the status is not Active, this requirement does not define behavior and such cases MUST be treated as out of scope for this story.
- FR-US-003-009 (US-003-ac-4): When the create response returns an Active trainer, the Trainer Management frontend SHALL update its client-side trainer data store or cache so that the newly created trainer record is immediately included in any in-memory collections that back trainer search and trainer allocation UIs which consume trainer-service data, without requiring a full page reload by the user.
- FR-US-003-010 (US-003-ac-5): When the trainer-service create profile API response includes a derived time zone value that was computed server side from the submitted location, the frontend SHALL persist that derived time zone value in its trainer record representation and/or local state so that future scheduling and notification UIs can retrieve it without asking the user to re-enter a time zone.
- FR-US-003-011 (US-003-ac-5): The Create Trainer Profile screen SHALL either display the derived time zone value to the user in a read-only field or badge after successful creation, or silently store it without additional user input; this requirement deliberately leaves the visual presentation ambiguous and only constrains that the value is available to downstream UIs.
- FR-US-003-012 (US-003-ac-6): After the trainer-service create profile call completes successfully, the Trainer Management micro frontend SHALL either (a) emit or call an audit-service-compatible endpoint/event with sufficient data for the audit-service to record a "trainer create" action, including actor identity and timestamp but excluding any sensitive document content, or (b) navigate the browser to the trainer audit view route /trainers/:trainerId/audit, where :trainerId is the identifier of the newly created trainer, in order to allow the user to verify an audit entry was recorded; this requirement intentionally allows either approach without prescribing which is primary.
- FR-US-003-013 (US-003-ac-6): If the implementation chooses the navigation-based approach, on navigating to /trainers/:trainerId/audit the Trainer Management frontend SHALL pass or resolve the correct :trainerId for the newly created trainer so that the audit view can display the entry corresponding to that create action, assuming the audit-service has recorded it.

## Success Criteria (Traceable to Acceptance Criteria)
- **US-003-ac-1**: Trainer Management micro frontend defines and wires a Create Trainer Profile screen at route /trainers/new that captures trainer type, required fields, employee identifier for internal trainers, email for external trainers, and location, using components from the shared design system for input validation feedback.
- **US-003-ac-2**: Form-level validation logic in trainer-mgmt-web ensures that required fields for the selected trainer type are enforced on the client side, preventing submission and displaying inline error messages when required fields are empty or in an invalid format before calling trainer-service create APIs.
- **US-003-ac-3**: Before submitting a new trainer profile, the Trainer Management module invokes a trainer-service endpoint that checks for existing profiles with the same employee identifier or email (across active and inactive records) and surfaces any duplicates as field-level error messages without navigating away from /trainers/new.
- **US-003-ac-4**: On successful response from trainer-service create profile, the frontend reads the returned trainer status field and confirms it is Active, then updates the client-side state so that the newly created trainer is immediately available in trainer search and allocation UIs that consume trainer-service data.
- **US-003-ac-5**: When trainer-service returns the created trainer resource, the frontend displays or stores the derived time zone value that was computed server side from the chosen location so that subsequent scheduling and notification UIs can rely on it without additional user input.
- **US-003-ac-6**: Upon successful profile creation, the Trainer Management micro frontend triggers an audit-service compatible event or navigates to the trainer audit view route /trainers/:trainerId/audit so that users can confirm that an audit entry was recorded for the create action, including actor identity and timestamp but excluding any sensitive document content.

## Technical Design and Constraints
- No additional structured technical design content is available beyond unresolved references; see below.

### Evidence References and Unresolved Dependencies
- unresolved reference (artifact-SVC-005-api_spec)
- unresolved reference (artifact-SVC-005-c3)
- unresolved reference (artifact-SVC-005-c4)
- unresolved reference (service:SVC-005)
- unresolved reference (service:SVC-009)
- unresolved reference (service:SVC-010)
- unresolved reference (service:SVC-011)

These references indicate expected grounding artifacts for API specs and C3/C4 models that are not present in this export.

## API and Integration Impact
- From spec_api_impact: `contract_change_required = false`.
- No impacted_service_ids are recorded.
- No contract_change_reasons, touchpoints, or unresolved_items are defined.

## Test Obligations
- d91b94da-057d-4059-9efb-97a7e7a20ddf:test:acceptance:US-003-ac-1 → **US-003-ac-1**
- d91b94da-057d-4059-9efb-97a7e7a20ddf:test:acceptance:US-003-ac-2 → **US-003-ac-2**
- d91b94da-057d-4059-9efb-97a7e7a20ddf:test:acceptance:US-003-ac-3 → **US-003-ac-3**
- d91b94da-057d-4059-9efb-97a7e7a20ddf:test:acceptance:US-003-ac-4 → **US-003-ac-4**
- d91b94da-057d-4059-9efb-97a7e7a20ddf:test:acceptance:US-003-ac-5 → **US-003-ac-5**
- d91b94da-057d-4059-9efb-97a7e7a20ddf:test:acceptance:US-003-ac-6 → **US-003-ac-6**

All test obligations are required_before `release` and target environment `ci`.

## Quality and Security Obligations
- Validate credential and personal-data handling against the story's acceptance criteria.
- Verify the changed user flow against the stated accessibility criteria.

## Traceability
- Each acceptance criterion US-003-ac-1 through US-003-ac-6 is mapped to the functional specification (`spec_functional`) and to its corresponding clause and test obligation in the implementation specification.
- Expected evidence listed in the implementation specification:
  - test result for US-003-ac-1
  - test result for US-003-ac-2
  - test result for US-003-ac-3
  - test result for US-003-ac-4
  - test result for US-003-ac-5
  - test result for US-003-ac-6

## Provenance
- **Policy version**: `ace-spec-format/v1`
- **Repository baseline commit**: `ed7662cf093095208753963f475fec3e5d21d70c`
- **Accepted impact revision**: `null`
