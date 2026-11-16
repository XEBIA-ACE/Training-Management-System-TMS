package com.tms.trainer.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * MSA/NDA status of an (external) trainer (FR-US-003-002).
 *
 * <p>GAP(artifact-SVC-005-api_spec, FR-US-003-002): the story requires {@code msaNdaStatus} to
 * conform to "its defined enum constraints" but no enum values are defined anywhere in the spec
 * bundle or this repository. These values are provisional and must be confirmed (see
 * docs/adr/US-003-trainer-create-gaps.md).
 */
public enum MsaNdaStatus {
  PENDING,
  SIGNED,
  EXPIRED;

  /** Exact (case-sensitive) match on the enum name; empty for unknown values. */
  public static Optional<MsaNdaStatus> parse(String value) {
    return Arrays.stream(values()).filter(s -> s.name().equals(value)).findFirst();
  }
}
