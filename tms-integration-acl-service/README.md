# TMS External Integrations ACL Service (SVC-003)

Scaffold for **SCAFFOLD-SVC-003**: a standalone Spring Boot 3 / Java 21 service that will manage
and evaluate access-control rules for outbound integrations (email, survey, FX, ...). Design:
`../specs/SCAFFOLD-SVC-003/references/SVC-003.md`.

Stack: Spring Web, Spring Data JPA + PostgreSQL (disabled by default), Spring Security (OAuth2
resource server), Actuator + Micrometer, Micrometer Tracing + OpenTelemetry (OTLP), Spotless
(google-java-format).

## Run locally

No environment variables are required; persistence, messaging and JWT validation are disabled
by default.

```bash
mvn spring-boot:run
curl http://localhost:8082/actuator/health   # {"status":"UP",...}
```

Optional settings are read from `TMS_ACL_*` environment variables (see `.env.example`). Set
`SPRING_PROFILES_ACTIVE=persistence` plus `TMS_ACL_DB_URL`, `TMS_ACL_DB_USERNAME` and
`TMS_ACL_DB_PASSWORD` to enable the PostgreSQL datasource.

## Build, format, test

```bash
mvn spotless:apply   # Google Java Format
mvn verify           # spotless:check + tests
```

## Endpoints

| Method | Path | Access |
| --- | --- | --- |
| GET | `/actuator/health`, `/actuator/health/{liveness,readiness}` | public |
| GET | `/actuator/metrics` | authenticated |
| * | `/api/v1/integration-acl/**` | authenticated (not implemented yet) |

## Package layout (C4 names)

- `api` – `ExternalIntegrationAclController`
- `application` – `AclValidationService`
- `domain` – `ExternalIntegrationAclAggregate`, `PrincipalRef`, `AclCondition`, `AuditMetadata`
- `infrastructure.persistence` – `ExternalIntegrationAclEntity`,
  `SpringDataExternalIntegrationAclRepository`, `ExternalIntegrationAclRepositoryAdapter`
- `security` – `SecurityConfig`, `AclEvaluationService`
- `config` – `ExternalIntegrationsAclProperties`, `ExternalIntegrationsAclConfiguration`

All C4 types are skeletons; ACL logic belongs to later stories.

## Open gaps (TODO SCAFFOLD-SVC-003)

- The C4 application service class name is redacted in the spec export; it is not declared.
- AC-1 names EmailNotificationService / CalendarService / SurveyService / BI abstractions, while
  C3/C4 name Email/Survey/FX client adapters; provider SDKs and WebClient vs RestTemplate are
  undecided, so none are added.
- OpenTelemetry exporter/collector setup, message broker, DB schema/migrations, scope
  enforcement (`tms.acl.*`), audience validation and rate limits are unspecified.
