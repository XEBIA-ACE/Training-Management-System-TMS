# TMS Outbox Processing Service (SVC-002)

Scaffold for **SCAFFOLD-SVC-002**: a standalone Spring Boot 3 / Java 21 service that polls the
transactional outbox table written by the Training Management Core Service (SVC-001) and hands
events to integration adapters. Design: `../specs/SCAFFOLD-SVC-002/references/SVC-002.md`.

Stack: Spring Web, Spring Scheduler, Spring Data JPA + Flyway, PostgreSQL, Jackson, Spring Security
(OAuth2 resource server), Actuator + Micrometer (Prometheus), Micrometer Tracing + OpenTelemetry
(OTLP).

## Run locally

```bash
cp .env.example .env          # set TMS_OUTBOX_DB_PASSWORD
docker compose up -d          # PostgreSQL on localhost:5433
set -a && . ./.env && set +a
mvn spring-boot:run           # http://localhost:8081/health
```

## Build, format, test

```bash
mvn spotless:apply   # Google Java Format
mvn verify           # spotless:check + tests (Testcontainers, needs Docker)
```

## Endpoints

| Method | Path | Scope |
| --- | --- | --- |
| GET | `/api/v1/outbox/entries` | `outbox:read` |
| GET | `/api/v1/outbox/entries/{entryId}` | `outbox:read` |
| POST | `/api/v1/outbox/entries/{entryId}/retry` | `outbox:manage` |
| POST | `/api/v1/outbox/entries/{entryId}/skip` | `outbox:admin` |
| GET | `/api/v1/outbox/stats` | `outbox:read` (gap: unspecified) |
| POST | `/api/v1/outbox/processor/start` | `outbox:admin` (gap: unspecified) |
| POST | `/api/v1/outbox/processor/stop` | `outbox:admin` (gap: unspecified) |
| GET | `/api/v1/outbox/processor/status` | `outbox:read` (gap: unspecified) |
| GET | `/health`, `/health/ready`, `/health/live` | public |
| GET | `/metrics` (Prometheus) | public |

Errors use the `ErrorResponse` envelope. The correlation id is read from `X-Correlation-ID` (or
`X-Request-ID`), generated when missing, echoed as `X-Correlation-ID` and logged as MDC
`correlationId`.

## Package layout

- `domain` – `OutboxMessage`, `OutboxStatus`, `OutboxEventMetadata`, `OutboxMessageFactory`,
  ports `OutboxRepository` / `MessagePublisher`, `EventFilter`, `IdempotencyChecker`
- `application` – `OutboxProcessingService`, `OutboxCleanupService`, `OutboxMonitoringService`
- `infrastructure` – `scheduling.OutboxScheduler`, `persistence.JpaOutboxRepository`,
  `publisher.*` (RabbitMQ/Kafka stubs), `messaging.*` (`EventSerializer`, `EventDispatcher`,
  `RetryHandler`, `DeadLetterHandler`), `acl.*` (Email/Calendar/Survey/BI stubs),
  `OutboxTransactionManager`, `observability.MetricsCollector`
- `api`, `health`, `security`, `web`, `config` – controllers/DTOs, health monitor, JWT + scopes,
  correlation id + error handling, `OutboxConfiguration` / `OutboxProperties`

## Open gaps (TODO SCAFFOLD-SVC-002)

Unspecified in the export and left as explicit TODOs: broker exchanges/topics and routing,
Email/Calendar/Survey/BI ACL endpoints and event-type routing, retry jitter, dead-letter storage
beyond the `DEAD_LETTER` status, skip-reason audit storage, distributed scheduler locking,
rate limits, scopes for stats/processor endpoints, and outbox table ownership between SVC-001 and
SVC-002.
