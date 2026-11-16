-- US-002 (FR-US-002-001..005): trainer aggregate read by the trainer directory
-- (GET /api/trainers, GET /api/trainers/{id}).
-- GAP(FR-US-002-001): allowed status / availability_state values are not defined by the story and
-- are stored as opaque strings. Location master data does not exist yet, so location_id has no FK.
CREATE TABLE trainer (
    id                 UUID         PRIMARY KEY,
    employee_id        VARCHAR(60),
    name               VARCHAR(200) NOT NULL,
    email              VARCHAR(254) NOT NULL,
    status             VARCHAR(40)  NOT NULL,
    availability_state VARCHAR(40),
    location_id        UUID,
    weekend_available  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE trainer_certification (
    trainer_id       UUID NOT NULL REFERENCES trainer (id),
    certification_id UUID NOT NULL,
    PRIMARY KEY (trainer_id, certification_id)
);

-- FR-US-002-003: back the structured filters and the (name, id) ordering of TrainerRepository.Search.
-- The free-text "contains" predicate is not index-backed (see docs/adr/US-002-trainer-directory-gaps.md).
CREATE INDEX idx_trainer_name_id ON trainer (name, id);
CREATE INDEX idx_trainer_status ON trainer (status);
CREATE INDEX idx_trainer_location ON trainer (location_id);
CREATE INDEX idx_trainer_availability ON trainer (availability_state);
CREATE INDEX idx_trainer_certification_cert ON trainer_certification (certification_id, trainer_id);
