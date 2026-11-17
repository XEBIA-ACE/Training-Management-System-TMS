package com.tms.trainer.domain;

/**
 * {@code CreateTrainerRequest.msaNdaStatus} (FR-US-003-002): MSA/NDA status of an EXTERNAL trainer.
 *
 * <p>GAP(artifact-SVC-005-api_spec, FR-US-003-002): the enum values are not defined anywhere in the
 * repository or the spec export. These values are provisional and must be confirmed (see
 * docs/adr/US-003-trainer-create-gaps.md).
 */
public enum MsaNdaStatus {
  NOT_STARTED,
  PENDING,
  SIGNED,
  EXPIRED
}
