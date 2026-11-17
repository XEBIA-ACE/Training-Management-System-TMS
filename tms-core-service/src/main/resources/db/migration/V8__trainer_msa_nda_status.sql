-- US-003 (FR-US-003-002): optional MSA/NDA status of EXTERNAL trainers.
-- GAP(artifact-SVC-005-api_spec): allowed values are provisional (MsaNdaStatus enum), stored as text.
ALTER TABLE trainer ADD COLUMN msa_nda_status VARCHAR(20);
