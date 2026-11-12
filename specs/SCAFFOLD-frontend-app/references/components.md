# Reusable UI components

## Primary Button
High-emphasis button used for main actions such as Login, Save, Confirm Allocation, and Create records.

- Usage: Use for the primary action on a screen or modal (e.g., 'Save', 'Confirm Allocation', 'Create requirement'). Only one primary button per context.
- States: default, hover, active, disabled, focus
- Cross-platform notes: On web, render as a standard <button> element with keyboard focus ring and ARIA labels where relevant. On iOS/Android, map to high-emphasis filled button components using system touch feedback while preserving color and contrast.

## Secondary Button
Medium-emphasis button used for secondary actions like 'Cancel', 'Back', and neutral options.

- Usage: Use alongside a primary button or alone when the action is not the main call-to-action on the screen.
- States: default, hover, active, disabled, focus
- Cross-platform notes: Maintain the outlined style on all platforms to clearly differentiate from primary actions; ensure sufficient hit area on touch devices.

## Text Input
Single-line text input used for fields such as Email, Employee ID, Trainer Name, and search filters.

- Usage: Use for any free-form single-line text entry. Ensure labels clearly indicate required fields and formats (e.g., email).
- States: default, hover, focus, error, disabled
- Cross-platform notes: On mobile, ensure the appropriate keyboard type (e.g., email, numeric) is used. Support full keyboard navigation on web with visible focus outlines.

## Dropdown / Select
Select component for choosing values such as Location, Customer, OEM Certification, Delivery Mode, and Trainer.

- Usage: Use when users must pick one value from a constrained list (single-select). If multi-select is needed (e.g., locations), apply chip-based multi-select variant.
- States: default, hover, focus, disabled, error
- Cross-platform notes: Use native picker controls on mobile where possible while preserving label, helper text, and error messages consistent with web.

## Radio Group
Set of radio buttons for mutually exclusive choices such as Internal/External, Delivery Mode, and travel cost handling.

- Usage: Use when only one option can be selected and all options should be visible at once.
- States: default, selected, focus, disabled
- Cross-platform notes: Ensure radio groups are announced as groups to screen readers and that selection changes are keyboard accessible.

## Slider Toggle (Yes/No)
Two-state toggle control used for Yes/No and enabled/disabled flags such as weekend availability, incentive applicability, exam-oriented, renewal required, customization required, and PO confirmation.

- Usage: Use when representing a binary setting that users may switch frequently. Label clearly indicates what 'on' means.
- States: on, off, focus, disabled
- Cross-platform notes: On mobile, align with platform-native switch patterns; on web, ensure the toggle is keyboard-operable and labeled with aria-checked state.

## Table / Data Grid
Data table used extensively for lists and reports such as Trainer List, Certification List, Customer List, Requirement List, Workshop Dashboard, and all report views.

- Usage: Use for dense, sortable, filterable data views, supporting row actions, status chips, and pagination or infinite scroll.
- States: default, row_hover, row_selected, header
- Cross-platform notes: On smaller screens, consider responsive layouts (stacked rows or horizontal scroll). Ensure headers, sort controls, and row actions are keyboard accessible and announced correctly.

## Status Chip / Tag
Small pill-shaped label used to indicate statuses such as active/inactive, confirmed/unconfirmed/pending sessions, MSA/NDA expiring, trainer internal/external.

- Usage: Use near key entities (trainers, customers, requirements, workshops) to convey state at a glance.
- States: confirmed, unconfirmed, pending, error
- Cross-platform notes: Do not rely on color alone; include text labels like 'Confirmed', 'Potential', 'Inactive' for clarity.

## Modal / Dialog
Overlay panel for critical confirmations and forms such as Allocation Change Confirmation, Sign-out Confirmation, and permission errors when shown inline.

- Usage: Use for blocking tasks that require explicit confirmation or when editing context must be isolated from underlying content.
- States: default, backdrop
- Cross-platform notes: Trap focus within the modal on web; ensure Escape and appropriate mobile gestures close it, and that role=dialog is set with aria-modal=true.

## Slide-over Panel
Side panel used for Workshop Detail Slide-over and possibly document viewers, allowing users to review details without leaving the current context.

- Usage: Use for read-oriented detail views such as workshop summaries, trainer documents, customer details, and checklists where context from the originating list must be preserved.
- States: default
- Cross-platform notes: On mobile, convert to full-screen overlay but preserve header, close control, and scroll behavior.

## Filter Panel / Chip Filters
Set of controls for filtering lists, dashboards, and reports (e.g., date ranges, status, customer, trainer, certification).

- Usage: Use above tables and dashboards to refine data sets. Represent active filters clearly with chips or labels.
- States: filter_chip_default, filter_chip_active
- Cross-platform notes: On touch devices, ensure filter controls are large enough and do not rely solely on hover effects.

## Global Error Banner
System-level banner used for partial failures, API errors, and degraded states as described in Global Error Banner screen.

- Usage: Appear at the top of the content area when operations fail or partial data is shown (e.g., dashboard load partially complete, survey integration errors).
- States: error, warning, info
- Cross-platform notes: Use role='alert' or role='status' on web to announce to assistive technologies. Include retry buttons where relevant.

## Empty State Pattern
Reusable block for communicating when no data is available (e.g., no workshops in selected range, no allocations, no report results).

- Usage: Embed inside list or table areas when there are no matching records for current filters or context.
- States: default
- Cross-platform notes: Include clear text guidance and actionable links where possible (e.g., 'Create requirement'). Avoid relying on illustration alone.
