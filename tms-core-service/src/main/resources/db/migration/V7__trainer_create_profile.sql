-- US-003 (FR-US-003-001..007): trainer creation. Adds the trainer type and derived timezone, and a
-- minimal location lookup used to derive Trainer.timezone.
-- GAP(FR-US-003-004): the core database has no CustomerLocation/location master data yet, so a
-- minimal read-only lookup (id, timezone) is introduced. It is not populated by this story; a
-- locationId without a row (or without a timezone) leaves Trainer.timezone NULL.
ALTER TABLE trainer ADD COLUMN type VARCHAR(20);
ALTER TABLE trainer ADD COLUMN timezone VARCHAR(64);

CREATE TABLE location (
    id       UUID        PRIMARY KEY,
    timezone VARCHAR(64)
);

CREATE INDEX idx_trainer_employee_id ON trainer (employee_id);
CREATE INDEX idx_trainer_email_lower ON trainer (lower(email));
