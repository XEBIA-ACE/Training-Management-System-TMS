package com.tms.trainer.domain;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves a location to an IANA zone by matching either the full zone ID ("Asia/Kolkata") or its
 * city part ("Kolkata", "New York") case-insensitively.
 *
 * <p>GAP(FR-US-003-005): the repo has no CustomerLocation/region data, so the JDK tz database is
 * the only region source; locations it does not name (e.g. "Pune") are unresolved. GAP
 * (FR-US-003-006): unresolved locations yield no time zone; no default is guessed.
 */
@Component
class IanaLocationTimeZoneResolver implements TimeZoneResolver {

  @Override
  public Optional<String> resolve(String location) {
    if (location == null || location.isBlank()) {
      return Optional.empty();
    }
    String wanted = location.trim().toLowerCase(Locale.ROOT);
    return ZoneId.getAvailableZoneIds().stream()
        .filter(id -> id.contains("/"))
        .sorted()
        .filter(id -> matches(id, wanted))
        .findFirst();
  }

  private static boolean matches(String zoneId, String wanted) {
    String id = zoneId.toLowerCase(Locale.ROOT);
    String city = id.substring(id.lastIndexOf('/') + 1).replace('_', ' ');
    return id.equals(wanted) || city.equals(wanted);
  }
}
