package com.tms.trainer.domain;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Location to time zone mapping used to derive a trainer's {@code timeZone} (FR-US-003-004).
 *
 * <p>GAP(FR-US-003-004, artifact-SVC-005-c3): no location master data or mapping rules exist in the
 * spec bundle or this repository. The rules are therefore configuration ({@code
 * tms.trainer.management.location-time-zones}, entries {@code <location>=<IANA zone id>}), matched
 * case-insensitively on the trimmed location. Unmapped locations yield no time zone; the story
 * explicitly leaves that behaviour as a separate design decision.
 */
public final class LocationTimeZoneRules {

  private final Map<String, ZoneId> zones;

  private LocationTimeZoneRules(Map<String, ZoneId> zones) {
    this.zones = Map.copyOf(zones);
  }

  /**
   * Parses {@code <location>=<zone id>} entries. Blank entries are ignored; malformed entries,
   * unknown zone ids and conflicting mappings for the same location fail fast.
   */
  public static LocationTimeZoneRules parse(Collection<String> entries) {
    Map<String, ZoneId> zones = new HashMap<>();
    for (String entry : entries == null ? List.<String>of() : entries) {
      if (entry == null || entry.isBlank()) {
        continue;
      }
      int separator = entry.indexOf('=');
      String location = separator > 0 ? normalise(entry.substring(0, separator)) : "";
      String zoneId = separator > 0 ? entry.substring(separator + 1).trim() : "";
      if (location.isEmpty() || zoneId.isEmpty()) {
        throw new IllegalArgumentException(
            "Invalid location time zone rule '" + entry + "', expected <location>=<zone id>");
      }
      ZoneId zone;
      try {
        zone = ZoneId.of(zoneId);
      } catch (DateTimeException ex) {
        throw new IllegalArgumentException(
            "Unknown time zone '" + zoneId + "' for location '" + location + "'", ex);
      }
      ZoneId previous = zones.put(location, zone);
      if (previous != null && !previous.equals(zone)) {
        throw new IllegalArgumentException(
            "Ambiguous time zone rules for location '" + location + "'");
      }
    }
    return new LocationTimeZoneRules(zones);
  }

  public Optional<ZoneId> resolve(String location) {
    return location == null
        ? Optional.empty()
        : Optional.ofNullable(zones.get(normalise(location)));
  }

  private static String normalise(String location) {
    return location.trim().toLowerCase(Locale.ROOT);
  }
}
