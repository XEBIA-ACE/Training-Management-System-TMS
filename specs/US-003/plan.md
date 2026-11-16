# Implementation Plan: US-003

## Technical Context
- unresolved reference (artifact-SVC-005-api_spec)
- unresolved reference (artifact-SVC-005-c3)
- unresolved reference (artifact-SVC-005-c4)
- unresolved reference (service:SVC-005)
- unresolved reference (service:SVC-009)
- unresolved reference (service:SVC-010)
- unresolved reference (service:SVC-011)

## Contract Changes
- No material API, event, or data contract change is evidenced.

## Security and Quality
- Validate credential and personal-data handling against the story's acceptance criteria
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **US-003-ac-1**: Trainer Management micro frontend defines and wires a Create Trainer Profile screen at route /trainers/new that captures trainer type, required fields, employee identifier for internal trainers, email for external trainers, and location, using components from the shared design system for input validation feedback.
- **US-003-ac-2**: Form-level validation logic in trainer-mgmt-web ensures that required fields for the selected trainer type are enforced on the client side, preventing submission and displaying inline error messages when required fields are empty or in an invalid format before calling trainer-service create APIs.
- **US-003-ac-3**: Before submitting a new trainer profile, the Trainer Management module invokes a trainer-service endpoint that checks for existing profiles with the same employee identifier or email (across active and inactive records) and surfaces any duplicates as field-level error messages without navigating away from /trainers/new.
- **US-003-ac-4**: On successful response from trainer-service create profile, the frontend reads the returned trainer status field and confirms it is Active, then updates the client-side state so that the newly created trainer is immediately available in trainer search and allocation UIs that consume trainer-service data.
- **US-003-ac-5**: When trainer-service returns the created trainer resource, the frontend displays or stores the derived time zone value that was computed server side from the chosen location so that subsequent scheduling and notification UIs can rely on it without additional user input.
- **US-003-ac-6**: Upon successful profile creation, the Trainer Management micro frontend triggers an audit-service compatible event or navigates to the trainer audit view route /trainers/:trainerId/audit so that users can confirm that an audit entry was recorded for the create action, including actor identity and timestamp but excluding any sensitive document content.

## Provenance
- **Policy version**: `ace-spec-format/v1`
- **Repository baseline commit**: `ed7662cf093095208753963f475fec3e5d21d70c`
- **Accepted impact revision**: `null`
