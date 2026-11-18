-- US-004 (FR-US-004-AC1-002): status-change metadata. Only trainer.status and these two columns
-- are written by PATCH /api/v1/trainers/{trainerId}/status. Nullable: never-toggled trainers have none.
ALTER TABLE trainer ADD COLUMN status_changed_at TIMESTAMPTZ;
ALTER TABLE trainer ADD COLUMN status_changed_by VARCHAR(200);
