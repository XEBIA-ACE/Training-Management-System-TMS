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

## Trainer allocation/deallocation notifications (US-001)

Outbox entries whose `eventType` is listed in `TMS_OUTBOX_TRAINER_ALLOCATION_EVENT_TYPES`
(default `TrainerAllocated`) or `TMS_OUTBOX_TRAINER_DEALLOCATION_EVENT_TYPES` (default
`TrainerDeallocated`) are reported as `allocation` / `deallocation`:

- `GET /api/v1/outbox/stats` → `trainerAllocation` with counts by status, by event type and by
  event type + status, plus `backlogCount` (PENDING + PROCESSING + FAILED). `DEAD_LETTER` = retries
  exhausted.
- `/health` → `outboxPublishing` component: DOWN when the outbox table cannot be read or the message
  bus is unreachable, UNKNOWN when `TMS_OUTBOX_PUBLISHER_TYPE=none`.
- `/metrics` → `outbox_trainer_notification_publish_attempts_total{eventCategory,outcome}`,
  `outbox_trainer_notification_exhausted_total{eventCategory}`,
  `outbox_trainer_notification_backlog{eventCategory}` and
  `outbox_trainer_notification_entries{eventCategory,status}` (DB gauges cached for
  `TMS_OUTBOX_TRAINER_METRICS_CACHE_TTL_MS`, default 5000).

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
