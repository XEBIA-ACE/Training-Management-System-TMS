-- Baseline migration for the Training Management Core Service schema.
-- Flyway creates the configured schema (TMS_DB_SCHEMA); domain tables are added by later stories.
-- GAP (specs/SCAFFOLD-SVC-001/references/SVC-001.md, "C4 - Data structures" / "API specification -
-- Data models"): entity fields for Workshop, Trainer, UserAccount, outbox and audit tables are not
-- defined in the spec, so no domain tables are created here.
SELECT 1;
