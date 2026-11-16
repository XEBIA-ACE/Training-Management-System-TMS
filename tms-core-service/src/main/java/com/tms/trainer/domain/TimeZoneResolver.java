package com.tms.trainer.domain;

import java.util.Optional;

/** Resolves a selected location to a time zone ID (FR-US-003-005). */
public interface TimeZoneResolver {

  /** Empty when the location cannot be resolved (behavior undefined by FR-US-003-006). */
  Optional<String> resolve(String location);
}
