# Implementation Plan: SCAFFOLD-SVC-001

## Technical Context
- Technology stack: Java 21, Spring Boot 3.x, Spring MVC or WebFlux, Spring Data JPA (Hibernate), PostgreSQL, Spring Security (RBAC/ABAC, OAuth2 Resource Server), Spring Validation (Bean Validation/Jakarta Validation), Hibernate Envers or custom audit tables, Spring Cache with Redis, Spring Scheduler / Quartz (for in-process jobs), ArchUnit for modular boundaries, Flyway or Liquibase for DB migrations.
- Platform: backend.
- Set up the project skeleton for this stack: dependency manifest, entrypoint, and linting/formatting config.
- Add environment-based configuration loading (no hardcoded secrets or endpoints).
- Add a health-check endpoint so the service can be verified as running locally.
- Add a database connection helper for the stack's datastore, configured via environment variables.
- Existing class (C4): WorkshopDashboardController (see [references/SVC-001.md](./references/SVC-001.md))
- Existing class (C4): WorkshopDashboardQueryService (see [references/SVC-001.md](./references/SVC-001.md))
- Existing class (C4): Workshop (see [references/SVC-001.md](./references/SVC-001.md))
- Existing class (C4): WorkshopRepository (see [references/SVC-001.md](./references/SVC-001.md))
- Existing class (C4): JpaWorkshopRepository (see [references/SVC-001.md](./references/SVC-001.md))
- Existing class (C4): WorkshopAllocationApplicationService (see [references/SVC-001.md](./references/SVC-001.md))
- Existing class (C4): WorkshopManagementApplicationService (see [references/SVC-001.md](./references/SVC-001.md))
- Existing class (C4): Trainer (see [references/SVC-001.md](./references/SVC-001.md))
- Existing class (C4): TrainerRepository (see [references/SVC-001.md](./references/SVC-001.md))
- Existing class (C4): WorkshopDashboardReadRepository (see [references/SVC-001.md](./references/SVC-001.md))
- Existing API endpoint: POST /api/auth/login -- Authenticate a user with username/password for non-SSO environments (e.g., local, test) and issue a JWT access token plus optional refresh token. In production, primary auth is via OIDC; this endpoint is typically disabled or restricted. (see [references/SVC-001.md](./references/SVC-001.md))
- Existing API endpoint: POST /api/auth/refresh -- Exchange a valid refresh token for a new access token. (see [references/SVC-001.md](./references/SVC-001.md))
- Existing API endpoint: GET /api/auth/me -- Return the authenticated user profile and effective roles/permissions as understood by the TMS, based on the current access token and internal role mappings. (see [references/SVC-001.md](./references/SVC-001.md))
- Existing API endpoint: GET /actuator/health -- Liveness/readiness probe for the service. (see [references/SVC-001.md](./references/SVC-001.md))
- Existing API endpoint: GET /actuator/metrics -- Application metrics for Prometheus/similar. Locked to infra/ops roles. (see [references/SVC-001.md](./references/SVC-001.md))
- Existing API endpoint: GET /api/workshops -- Timeline-filtered, scrollable list of workshop sessions for the Workshop Dashboard with server-side filtering, sorting, and pagination. (see [references/SVC-001.md](./references/SVC-001.md))
- Existing API endpoint: GET /api/workshops/dashboard-kpis -- Compute KPI summary aligned with the Workshop Dashboard filters (timeline preset + confirmation filter + RBAC). (see [references/SVC-001.md](./references/SVC-001.md))
- Existing API endpoint: GET /api/workshops/stream -- Server-Sent Events (SSE) stream that pushes real-time updates for workshop sessions visible to the current user (status changes, timing changes, etc.). (see [references/SVC-001.md](./references/SVC-001.md))
- Existing API endpoint: GET /api/workshops/{id} -- Detailed workshop view used when drilling down from dashboard or allocation view. (see [references/SVC-001.md](./references/SVC-001.md))
- Existing API endpoint: GET /api/requirements -- Paged list of customer training requirements with status filters (Active, Inactive, Delivered/Closed). (see [references/SVC-001.md](./references/SVC-001.md))

## Contract Changes
- No material API, event, or data contract change is evidenced.

## Security and Quality
- Validate credential and personal-data handling against the story's acceptance criteria
- Verify the changed user flow against the stated accessibility criteria

## Verification Plan
- **AC-1**: Scaffold the Store Management API Service (business) per its approved architecture. Technology stack: Java, Spring Boot, Spring MVC (REST Controllers), Spring Data JPA or equivalent ORM, PostgreSQL or compatible relational database, Bean Validation (Jakarta Validation), Spring Security with OAuth2/OIDC JWT, OpenAPI/Swagger, Docker/Kubernetes or equivalent container platform. Responsibilities: Expose REST endpoints for creating, listing, retrieving, updating, and deleting customers.; Expose REST endpoints for creating, listing, retrieving, updating, and deleting products.; Apply validation and domain rules such as required customer name/city and positive product price.; Orchestrate transactional persistence of customers and products using repositories and a relational database.; Handle authentication and authorization via integration with the enterprise IdP and enforce admin-only access.; Provide health check endpoints and basic observability hooks like structured logging and metrics.; Map between external API DTOs and internal domain models while keeping the domain persistence-agnostic.; Implement pagination for list endpoints to protect performance and support large datasets.. Ground the generated spec in this service's own C3/C4 diagrams and API spec already on file in Architecture, not just this summary.