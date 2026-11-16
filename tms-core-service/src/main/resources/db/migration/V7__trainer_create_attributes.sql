-- US-003 (FR-US-003-001..004): attributes of a TrainerCreateRequest (POST /api/v1/trainers).
-- Columns are nullable because trainers created before US-003 (US-002 directory rows) have no values.
ALTER TABLE trainer ADD COLUMN trainer_type      VARCHAR(20);
ALTER TABLE trainer ADD COLUMN location          VARCHAR(200);
ALTER TABLE trainer ADD COLUMN time_zone         VARCHAR(64);
ALTER TABLE trainer ADD COLUMN msa_nda_status    VARCHAR(40);
ALTER TABLE trainer ADD COLUMN msa_nda_reference VARCHAR(500);

ALTER TABLE trainer ADD CONSTRAINT ck_trainer_type
    CHECK (trainer_type IS NULL OR trainer_type IN ('INTERNAL', 'EXTERNAL'));

-- FR-US-003-003: uniqueness across all trainers regardless of status (inactive included). The
-- application checks first (TrainerDomainService.ensureUnique); these indexes close the race between
-- concurrent creates. GAP: this fails if pre-existing rows already violate it (see
-- docs/adr/US-003-trainer-create-gaps.md).
CREATE UNIQUE INDEX uq_trainer_email_lower ON trainer (lower(email));
CREATE UNIQUE INDEX uq_trainer_internal_employee_id ON trainer (employee_id)
    WHERE trainer_type = 'INTERNAL';
CREATE INDEX idx_trainer_employee_id ON trainer (employee_id);
