package com.tms.trainer.application;

import com.tms.trainer.domain.LocationTimeZoneRules;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TrainerModuleConfiguration {

  /** FR-US-003-004: fails startup on malformed, unknown or ambiguous time zone rules. */
  @Bean
  LocationTimeZoneRules locationTimeZoneRules(TrainerManagementProperties properties) {
    return LocationTimeZoneRules.parse(properties.locationTimeZones());
  }
}
