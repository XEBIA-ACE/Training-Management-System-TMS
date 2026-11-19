# Implementation Plan: US-004

## Technical Context
- unresolved reference (artifact-SVC-001-api_spec)
- unresolved reference (artifact-SVC-001-c3)
- unresolved reference (artifact-SVC-001-c4)
- unresolved reference (service:SVC-001)

## Contract Changes
- No material API, event, or data contract change is evidenced.

## Security and Quality
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **US-004-ac-1**: In the Trainer Management micro frontend trainer-mgmt-web, the trainer detail and edit routes at path pattern /trainers/:trainerId and /trainers/:trainerId/edit must render and allow modification of a discrete status field that maps to the backend trainer status attribute, without clearing or overwriting any non status profile fields in the form state when toggled between active and inactive.
- **US-004-ac-2**: The Trainer Management micro frontend trainer-mgmt-web must call the trainer-service status update endpoint that persists status changes while leaving all other trainer profile data unchanged, and it must refresh its local view model from the updated trainer record after a successful response so that no profile information appears lost to the user.
- **US-004-ac-3**: On all allocation related UIs in the Trainer Allocation micro frontend trainer-allocation-web, the trainer selection controls used on routes matching /allocation, /allocation/customer/:customerId, and /allocation/workshop/:workshopId must only request and display trainers whose status is active according to trainer-service data, thereby excluding inactive trainers from selection lists for new allocations.
- **US-004-ac-4**: When a user attempts to allocate a trainer who is currently inactive, the Trainer Allocation micro frontend trainer-allocation-web must detect this either from the pre filtered list or from an allocation-service error response and must block completion of the allocation flow, displaying a clear inline error message explaining that inactive trainers cannot be allocated.
- **US-004-ac-5**: When a trainer whose status was inactive becomes active again, the Trainer Allocation micro frontend trainer-allocation-web must ensure that subsequent trainer search or lookup calls against trainer-service or trainer-allocation-service include that trainer in results when they satisfy other eligibility filters such as required certifications and availability, and the UI must reflect this by making the trainer selectable.
- **US-004-ac-6**: The Trainer Management and Trainer Allocation micro frontends must propagate the authenticated user identity to the audit-service when invoking any existing audit logging integration for trainer status changes, so that audit entries include the actor identifier along with the trainer identifier, previous status, new status, and timestamp as returned by the audit-service.