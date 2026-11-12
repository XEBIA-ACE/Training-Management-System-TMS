-- SCAFFOLD-SVC-002: transactional outbox table (C4 OutboxMessage / API OutboxEntry).
-- Status values: PENDING, PROCESSING, COMPLETED, FAILED, SKIPPED (API) + DEAD_LETTER (C4).
CREATE TABLE outbox_message (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(255)  NOT NULL,
    aggregate_id    VARCHAR(255)  NOT NULL,
    event_type      VARCHAR(255)  NOT NULL,
    payload         JSONB         NOT NULL,
    status          VARCHAR(32)   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    processed_at    TIMESTAMPTZ,
    retry_count     INTEGER       NOT NULL DEFAULT 0,
    max_retries     INTEGER       NOT NULL,
    next_retry_at   TIMESTAMPTZ,
    last_error      TEXT,
    event_id        UUID          NOT NULL,
    event_timestamp TIMESTAMPTZ   NOT NULL,
    source          VARCHAR(255),
    correlation_id  VARCHAR(128),
    causation_id    VARCHAR(128),
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_outbox_message_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'SKIPPED', 'DEAD_LETTER'))
);

CREATE INDEX ix_outbox_message_status_due ON outbox_message (status, next_retry_at, created_at);
CREATE INDEX ix_outbox_message_aggregate ON outbox_message (aggregate_type, aggregate_id);
CREATE INDEX ix_outbox_message_event_type ON outbox_message (event_type, created_at);
CREATE UNIQUE INDEX ux_outbox_message_event_id ON outbox_message (event_id);
