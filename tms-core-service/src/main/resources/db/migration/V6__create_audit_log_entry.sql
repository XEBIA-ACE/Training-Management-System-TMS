-- US-003 (FR-US-003-009, -010): append-only audit trail. Rows can be inserted but never changed.
-- GAP: no audit schema is defined in the spec; details holds only non-sensitive fields (no documents).
CREATE TABLE audit_log_entry (
    id          UUID         PRIMARY KEY,
    actor       VARCHAR(200) NOT NULL,
    action      VARCHAR(64)  NOT NULL,
    entity_type VARCHAR(64)  NOT NULL,
    entity_id   VARCHAR(64)  NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL,
    details     JSONB        NOT NULL
);

CREATE INDEX idx_audit_log_entity ON audit_log_entry (entity_type, entity_id);

CREATE FUNCTION audit_log_entry_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_log_entry is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_entry_immutable
    BEFORE UPDATE OR DELETE ON audit_log_entry
    FOR EACH ROW EXECUTE FUNCTION audit_log_entry_immutable();
