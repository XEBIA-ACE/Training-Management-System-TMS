# Training Management Core Service

Spring Boot 3 / Java 21 modular monolith backing the Training Management System (TMS).
Scaffolded by story `SCAFFOLD-SVC-001` (see `../specs/SCAFFOLD-SVC-001`).

## Stack

Spring MVC, Spring Data JPA (Hibernate) + Envers, PostgreSQL, Flyway, Spring Security OAuth2
Resource Server (bearer JWT), Bean Validation, Spring Cache + Redis, Actuator, springdoc OpenAPI,
ArchUnit, Spotless (google-java-format).

## Layout

| Package | Purpose |
| --- | --- |
| `com.tms` | `TmsCoreServiceApplication` entrypoint |
| `com.tms.crosscutting.security` | `SecurityConfig`, JWT role/tenant mapping, `RbacPermissionEvaluator` |
| `com.tms.crosscutting.web` | `ErrorResponse` envelope, `CorrelationIdFilter` (`X-Request-Id` -> `traceId`) |
| `com.tms.crosscutting.persistence` | `DatabaseConnectionHelper` |
| `com.tms.crosscutting.cache` / `openapi` | Spring Cache and OpenAPI configuration |
| `com.tms.{scheduling,trainer,customer,certification,feedback,reporting,admin}` | Module boundaries (C4 `mod-*`), empty until later stories |

## Configuration

All configuration comes from environment variables (`TMS_*`); see `.env.example`. Nothing
environment-specific or secret is hardcoded. `application-local.yml` (profile `local`) adds
localhost defaults for the services in `docker-compose.yml`.

## Run locally

```bash
cp .env.example .env              # set TMS_DB_PASSWORD
docker compose up -d postgres
set -a && . ./.env && set +a
mvn spring-boot:run
curl http://localhost:8080/actuator/health   # {"status":"UP",...}
```

## Security

| Path | Access |
| --- | --- |
| `GET /actuator/health/**` | public (rate limited at the gateway) |
| `/actuator/metrics/**` | `SUPER_ADMIN`, `OPS_MONITOR` |
| other `/actuator/**` | denied / not exposed |
| `POST /api/auth/login`, `POST /api/auth/refresh` | public only when `TMS_LEGACY_AUTH_ENABLED=true` |
| `/api/admin/**` | `SUPER_ADMIN` |
| everything else | authenticated bearer JWT |

Roles are read from the `roles`, `groups` and `app_roles` claims (`ROLE_<value>` authorities);
the tenant from `tid`. 401/403 responses use the `ErrorResponse` envelope
(`AUTHENTICATION_FAILED` / `ACCESS_DENIED`) with `traceId` = `X-Request-Id`.

## Build and test

```bash
mvn verify            # tests (Testcontainers PostgreSQL; requires Docker) + spotless:check
mvn spotless:apply    # format
```
