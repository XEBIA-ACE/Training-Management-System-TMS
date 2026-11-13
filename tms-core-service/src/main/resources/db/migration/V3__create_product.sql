-- US-002: product master data referenced by orders.
-- GAP (specs/US-002/spec.md): the story defines no product attributes; only ID and name are modelled.
CREATE TABLE product (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0
);
