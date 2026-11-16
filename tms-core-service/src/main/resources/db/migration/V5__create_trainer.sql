-- US-003 (FR-US-003-003, -005, -007): trainer profiles created via POST /api/v1/trainers.
-- GAP (specs/US-003/spec.md): the profile field list is not defined by the story; the columns below
-- are an implementation choice. time_zone is NULL when the location cannot be resolved (FR-US-003-006).
CREATE TABLE trainer (
    id                UUID          PRIMARY KEY,
    trainer_type      VARCHAR(16)   NOT NULL CHECK (trainer_type IN ('INTERNAL', 'EXTERNAL')),
    status            VARCHAR(16)   NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    name              VARCHAR(200)  NOT NULL,
    email             VARCHAR(254)  NOT NULL,
    employee_id       VARCHAR(64),
    location          VARCHAR(120)  NOT NULL,
    time_zone         VARCHAR(64),
    weekend_available BOOLEAN       NOT NULL,
    rate_per_day      NUMERIC(12,2) NOT NULL CHECK (rate_per_day > 0),
    msa_reference     VARCHAR(120),
    nda_reference     VARCHAR(120),
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    version           BIGINT        NOT NULL DEFAULT 0
);

-- Uniqueness spans active and inactive records (FR-US-003-003); emails are stored lower-cased.
CREATE UNIQUE INDEX uq_trainer_email ON trainer (email);
CREATE UNIQUE INDEX uq_trainer_employee_id ON trainer (employee_id) WHERE employee_id IS NOT NULL;
CREATE INDEX idx_trainer_status ON trainer (status);

CREATE TABLE trainer_certification (
    trainer_id    UUID         NOT NULL REFERENCES trainer (id),
    certification VARCHAR(200) NOT NULL
);

CREATE INDEX idx_trainer_certification_trainer ON trainer_certification (trainer_id);
