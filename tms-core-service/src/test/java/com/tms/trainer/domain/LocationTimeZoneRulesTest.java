package com.tms.trainer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-003-ac-4 / FR-US-003-004 / T011: location to time zone rules. */
class LocationTimeZoneRulesTest {

  @Test
  @DisplayName("[US-003-ac-4][FR-US-003-004][T011] maps a recognized location case-insensitively")
  void resolvesConfiguredLocation() {
    LocationTimeZoneRules rules =
        LocationTimeZoneRules.parse(List.of("Pune=Asia/Kolkata", " London = Europe/London ", ""));

    assertThat(rules.resolve("Pune")).contains(ZoneId.of("Asia/Kolkata"));
    assertThat(rules.resolve("  pune ")).contains(ZoneId.of("Asia/Kolkata"));
    assertThat(rules.resolve("LONDON")).contains(ZoneId.of("Europe/London"));
    assertThat(rules.resolve("Atlantis")).isEmpty();
    assertThat(rules.resolve(null)).isEmpty();
  }

  @Test
  @DisplayName("[US-003-ac-4][FR-US-003-004][T011] rejects ambiguous, unknown or malformed rules")
  void rejectsInvalidRules() {
    assertThatThrownBy(
            () -> LocationTimeZoneRules.parse(List.of("Pune=Asia/Kolkata", "pune=Europe/London")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Ambiguous");
    assertThatThrownBy(() -> LocationTimeZoneRules.parse(List.of("Pune=Mars/Olympus")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> LocationTimeZoneRules.parse(List.of("Pune")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> LocationTimeZoneRules.parse(List.of("=Asia/Kolkata")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
