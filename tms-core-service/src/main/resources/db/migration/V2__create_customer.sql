-- US-001 (FR-US-001-001..FR-US-001-005): customer master data consumed by the customer REST API.
-- GAP (specs/US-001/spec.md): only customer ID, name and city are defined by the story; locations,
-- SPOCs, agreements and status lifecycle are left to later stories. Removal is a soft delete
-- (active = false) in line with the PRD non-deletion policy.
CREATE TABLE customer (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    city       VARCHAR(120) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_customer_active_name ON customer (active, name);
