-- US-002 (FR-US-002-006/007): immutable audit trail (data model AuditLogEntry) for trainer search
-- requests and UnauthorizedAccessEntry records. details holds a non-sensitive summary only.
CREATE TABLE audit_log_entry (
    id            UUID         PRIMARY KEY,
    occurred_at   TIMESTAMPTZ  NOT NULL,
    entry_type    VARCHAR(40)  NOT NULL,
    actor         VARCHAR(200) NOT NULL,
    action        VARCHAR(120) NOT NULL,
    resource_type VARCHAR(60)  NOT NULL,
    resource_id   VARCHAR(120),
    trace_id      VARCHAR(128),
    details       JSONB        NOT NULL
);

CREATE INDEX idx_audit_log_entry_occurred ON audit_log_entry (occurred_at);
CREATE INDEX idx_audit_log_entry_actor ON audit_log_entry (actor, occurred_at);
