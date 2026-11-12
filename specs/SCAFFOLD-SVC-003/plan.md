# Implementation Plan: SCAFFOLD-SVC-003

## Technical Context
- Technology stack: Java 21, Spring Boot 3.x, Spring WebClient or RestTemplate, JavaMail or email provider SDK (SES, SendGrid, etc.), Microsoft Graph or Google Calendar SDKs, Survey provider REST SDKs or HTTP clients, Micrometer, OpenTelemetry.
- Platform: backend.
- Set up the project skeleton for this stack: dependency manifest, entrypoint, and linting/formatting config.
- Add environment-based configuration loading (no hardcoded secrets or endpoints).
- Add a health-check endpoint so the service can be verified as running locally.
- Existing class (C4): ExternalIntegrationAclAggregate (see [references/SVC-003.md](./references/SVC-003.md))
- Existing class (C4): PrincipalRef (see [references/SVC-003.md](./references/SVC-003.md))
- Existing class (C4): AclCondition (see [references/SVC-003.md](./references/SVC-003.md))
- Existing class (C4): AuditMetadata (see [references/SVC-003.md](./references/SVC-003.md))
- Existing class (C4): ExternalIntegrationAclEntity (see [references/SVC-003.md](./references/SVC-003.md))
- Existing class (C4): SpringDataExternalIntegrationAclRepository (see [references/SVC-003.md](./references/SVC-003.md))
- Existing class (C4): ExternalIntegrationAclRepositoryAdapter (see [references/SVC-003.md](./references/SVC-003.md))
- Existing class (C4): [REDACTED-AWS_SECRET_KEY] (see [references/SVC-003.md](./references/SVC-003.md))
- Existing class (C4): AclValidationService (see [references/SVC-003.md](./references/SVC-003.md))
- Existing class (C4): AclEvaluationService (see [references/SVC-003.md](./references/SVC-003.md))
- Existing API endpoint: GET /api/v1/integration-acl -- Returns a paginated list of External Integration ACL rules controlling which internal modules/roles may use which external providers and operations. (see [references/SVC-003.md](./references/SVC-003.md))
- Existing API endpoint: GET /api/v1/integration-acl/{id} -- Retrieve a single ACL rule by its identifier. (see [references/SVC-003.md](./references/SVC-003.md))
- Existing API endpoint: POST /api/v1/integration-acl -- Create a new External Integration ACL rule. (see [references/SVC-003.md](./references/SVC-003.md))
- Existing API endpoint: PUT /api/v1/integration-acl/{id} -- Replace an existing ACL rule. Uses optimistic locking via version field. (see [references/SVC-003.md](./references/SVC-003.md))
- Existing API endpoint: PATCH /api/v1/integration-acl/{id} -- Partially update mutable fields of an ACL entry, typically used for enabling/disabling or updating limits. (see [references/SVC-003.md](./references/SVC-003.md))
- Existing API endpoint: DELETE /api/v1/integration-acl/{id} -- Delete (or soft-delete, depending on implementation) an ACL rule. Typically allowed only for integration admins and system admins. (see [references/SVC-003.md](./references/SVC-003.md))
- Existing API endpoint: POST /api/v1/integration-acl/evaluate -- Internal/business operation endpoint for other backend modules (e.g., NOTIF-01, SURV-INT-01, FX-01) to check whether a given internal module, role, and context are permitted to call a specific external provider/operation right now. Intended for server-to-server calls via service accounts. (see [references/SVC-003.md](./references/SVC-003.md))
- Existing API endpoint: GET /api/v1/integration-acl/export -- Export all ACL entries as CSV or JSON for security review and audits. Potentially scheduled via JOB-01. Returns synchronous export for moderate volumes. (see [references/SVC-003.md](./references/SVC-003.md))
- Existing API endpoint: GET /api/v1/integration-acl/info -- Basic informational endpoint exposing build info, supported provider types, and max limits as understood by the ACL service. (see [references/SVC-003.md](./references/SVC-003.md))
- Existing API endpoint: GET /actuator/health -- Liveness/readiness health endpoint exposed by Spring Boot Actuator. (see [references/SVC-003.md](./references/SVC-003.md))

## Contract Changes
- No material API, event, or data contract change is evidenced.

## Security and Quality
- Validate credential and personal-data handling against the story's acceptance criteria
- The scaffolded service must start successfully and its health-check endpoint must respond, using only environment-based configuration.

## Verification Plan
- **AC-1**: Scaffold the External Integrations ACL Service (integration) per its approved architecture. Technology stack: Java 21, Spring Boot 3.x, Spring WebClient or RestTemplate, JavaMail or email provider SDK (SES, SendGrid, etc.), Microsoft Graph or Google Calendar SDKs, Survey provider REST SDKs or HTTP clients, Micrometer, OpenTelemetry. Responsibilities: Provide a unified EmailNotificationService abstraction used by the Outbox Processing Service and core domain to send templated, localized emails through the configured email provider.; Implement a CalendarService abstraction that maps internal Workshop and TrainerAllocation data into provider-specific calendar events, handling time zones, recurrence, and idempotent updates or cancellations.; Implement a SurveyService abstraction that creates and distributes survey instances for workshops, generates per-participant and per-trainer links, and fetches or receives responses for collation into internal feedback models.; Expose BI integration functions used by reporting flows to publish curated datasets or refresh signals to external BI tools using governed views or exports.; Hide external provider authentication, including API keys, OAuth tokens, and secrets, behind configuration and secret management.; Handle provider-specific throttling, rate limiting, retries, and error mapping, surfacing uniform error semantics back to the Outbox Processing and Core services.; Support provider replacement and evolution by keeping internal interfaces stable and encapsulating vendor-specific DTOs and quirks within adapter implementations.; Log and trace all outbound calls and critical failures for audit and observability purposes without leaking sensitive payloads.. Ground the generated spec in this service's own C3/C4 diagrams and API spec already on file in Architecture, not just this summary.