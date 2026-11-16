# Tasks: US-003

Implementation tasks are ordered and independently reviewable; each maps to one or more functional requirements or acceptance criteria.

## Functional Implementation
- [ ] T001 — Implement FR-US-003-001 (US-003-ac-1): Create Trainer Profile screen at route /trainers/new with inputs for trainer type (internal/external), required base fields, employee identifier (internal), email (external), and location, using shared design system components that support inline validation feedback.
- [ ] T002 — Implement FR-US-003-002 (US-003-ac-1): Ensure /trainers/new is reachable only to authorized users and renders the form in a ready-to-edit state without extra navigation.
- [ ] T003 — Implement FR-US-003-003 (US-003-ac-2): Add client-side form-level validation enforcing required fields per trainer type and blocking API submission when invalid, with inline error messages.
- [ ] T004 — Implement FR-US-003-004 (US-003-ac-2): Implement specific client-side format validation (e.g., email, non-empty text, allowed characters) and clearing of inline errors once values are corrected.
- [ ] T005 — Implement FR-US-003-005 (US-003-ac-3): Integrate with trainer-service duplicate-check endpoint using employee identifier and/or email across active and inactive trainer records.
- [ ] T006 — Implement FR-US-003-006 (US-003-ac-3): Handle duplicate-check responses by surfacing field-level errors on conflicting employee identifier/email fields while preserving form state on /trainers/new.
- [ ] T007 — Implement FR-US-003-007 (US-003-ac-3): Proceed to trainer-service create profile API when duplicate-check passes, without extra user confirmation.
- [ ] T008 — Implement FR-US-003-008 (US-003-ac-4): On successful create response, verify trainer status is "Active" as defined by trainer-service; behavior for non-Active is out of scope for this story.
- [ ] T009 — Implement FR-US-003-009 (US-003-ac-4): Update client-side trainer store/cache so the new Active trainer appears immediately in trainer search and allocation UIs without full page reload.
- [ ] T010 — Implement FR-US-003-010 (US-003-ac-5): Persist server-derived time zone from the create response in the trainer record/local state for use by future scheduling/notification UIs.
- [ ] T011 — Implement FR-US-003-011 (US-003-ac-5): Either display the derived time zone (read-only) after creation or store it silently while ensuring it is accessible to downstream UIs.
- [ ] T012 — Implement FR-US-003-012 (US-003-ac-6): After successful create, either emit/call an audit-service-compatible endpoint/event with actor identity and timestamp (excluding sensitive document content) or navigate to /trainers/:trainerId/audit for the newly created trainer.
- [ ] T013 — Implement FR-US-003-013 (US-003-ac-6): If using navigation-based approach, ensure the correct :trainerId is resolved/passed so the audit view displays the create-action entry.

## Verification Tasks
- [ ] T014 — Verify US-003-ac-1: Trainer Management micro frontend defines and wires a Create Trainer Profile screen at /trainers/new capturing trainer type, required fields, internal employee identifier, external email, and location, using shared design system components for inline validation feedback.
- [ ] T015 — Verify US-003-ac-2: Client-side form-level validation enforces required fields by trainer type, blocks submission when invalid, and shows inline error messages before any trainer-service create API call.
- [ ] T016 — Verify US-003-ac-3: Duplicate-check endpoint is invoked before create; duplicates by employee identifier/email produce field-level errors without leaving /trainers/new and preserve form data.
- [ ] T017 — Verify US-003-ac-4: On create success, frontend confirms trainer status is Active and updates client-side state so the new trainer is immediately available in trainer search and allocation UIs.
- [ ] T018 — Verify US-003-ac-5: Derived time zone from trainer-service create response is stored and/or displayed so downstream scheduling/notification UIs can use it without additional user input.
- [ ] T019 — Verify US-003-ac-6: After profile creation, either an audit-service compatible event/endpoint call is made or navigation to /trainers/:trainerId/audit occurs, allowing confirmation of an audit entry including actor identity and timestamp but excluding sensitive document content.

## Security and Quality Tasks
- [ ] T020 — Validate credential and personal-data handling against the story's acceptance criteria, with particular attention to employee identifiers, email addresses, and any stored location/time zone data.
- [ ] T021 — Verify the changed user flow against the stated accessibility criteria (e.g., focus management, error message semantics, screen-reader compatibility, and keyboard-only navigation on /trainers/new).

## Provenance
- **Policy version**: `ace-spec-format/v1`
- **Repository baseline commit**: `ed7662cf093095208753963f475fec3e5d21d70c`
- **Accepted impact revision**: `null`
