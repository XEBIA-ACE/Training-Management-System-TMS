# ADR: POST /api/v1/orders – locally chosen behavior for spec gaps (US-002)

Source of the gaps: `specs/US-002/spec.md` (FR-US-002-001, -002, -004 mark these behaviors unspecified). The spec files are unchanged and do not require any choice below.

## Decisions (implementation choices, not requirements)
- Unknown/inactive customer: 404 `CUSTOMER_NOT_FOUND`, reusing the existing customer-module error code. Unknown/inactive product: 404 `PRODUCT_NOT_FOUND`.
- Success: `201 Created` with `Location` and a body of `id`, `customerId`, `productId`, `quantity`, `createdAt`.
- Missing/invalid body fields (including `quantity <= 0` or absent) → 400 `VALIDATION_FAILED`, as for customers.
- No persistence on failure: validation and both lookups precede the only `save`.

## Design notes / conflicts
- Story title ("Search and Filter Trainer Profiles...") does not match the defined orders behavior; the API behavior was implemented as given.
- The repo had no Order or Product module; minimal `com.tms.order` and `com.tms.product` modules (tables `orders`, `product`) were added. Product attributes beyond ID/name are undefined.
- `service:SVC-006` maps to `tms-core-service`. The tech design lists `/api/...` paths without `v1`; the repo uses `/api/v1/...` (as the customer API does).
