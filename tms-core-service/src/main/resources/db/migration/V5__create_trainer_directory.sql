-- US-002 (FR-US-002-001..FR-US-002-006): trainer directory search (GET /api/v1/trainers) and the
-- workshop/allocation data it derives allocation flags from.
-- GAP(FR-US-002-005/006): the exact trainer/availability field set is not enumerated by the story;
-- a single availability identifier per trainer is a provisional choice.
CREATE TABLE trainer (
    id                 UUID         PRIMARY KEY,
    employee_id        VARCHAR(60)  NOT NULL UNIQUE,
    name               VARCHAR(200) NOT NULL,
    email              VARCHAR(254) NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    location           VARCHAR(120),
    weekend_available  BOOLEAN      NOT NULL DEFAULT FALSE,
    availability_id    UUID         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE trainer_certification (
    trainer_id       UUID NOT NULL REFERENCES trainer (id) ON DELETE CASCADE,
    certification_id UUID NOT NULL,
    PRIMARY KEY (trainer_id, certification_id)
);

CREATE TABLE workshop (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL CHECK (end_date >= start_date),
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE allocation (
    id          UUID        PRIMARY KEY,
    workshop_id UUID        NOT NULL REFERENCES workshop (id),
    trainer_id  UUID        NOT NULL REFERENCES trainer (id),
    status      VARCHAR(20) NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);

-- FR-US-002-003: support the structured filters and ordering of SearchTrainers, and the
-- per-trainer allocation lookups. Free-text matching is an unanchored case-insensitive contains
-- (FR-US-002-002 gap) and is not index-backed; TODO(FR-US-002-003): revisit (e.g. pg_trgm) once
-- the production-scale performance threshold is agreed.
CREATE INDEX idx_trainer_status_name ON trainer (status, name, id);
CREATE INDEX idx_trainer_location ON trainer (lower(location));
CREATE INDEX idx_trainer_certification_cert ON trainer_certification (certification_id);
CREATE INDEX idx_allocation_trainer_status ON allocation (trainer_id, status, end_date);
CREATE INDEX idx_allocation_status ON allocation (status, end_date);
