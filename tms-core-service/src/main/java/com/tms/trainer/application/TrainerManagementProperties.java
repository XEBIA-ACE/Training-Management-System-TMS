package com.tms.trainer.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Trainer management (create) policy bound from {@code tms.trainer.management.*} (US-003).
 *
 * <p>GAP(service:SVC-012, FR-US-003-007): the TRAINER.MANAGE permission and its role mapping are
 * owned by the Admin & User Rights service, which does not exist in this repository. Mirroring the
 * US-002 directory policy, the roles holding TRAINER.MANAGE are configuration with a fail-closed
 * default of {@code SUPER_ADMIN}.
 *
 * <p>GAP(FR-US-003-004): {@code locationTimeZones} holds the location to time zone rules ({@code
 * <location>=<IANA zone id>}); none are defined by the story, so the default is empty.
 *
 * @param roles roles (without {@code ROLE_}) holding TRAINER.MANAGE, i.e. allowed to create
 *     trainers
 * @param locationTimeZones location to time zone rules, see {@code LocationTimeZoneRules}
 * @param audit auditing of trainer management requests
 */
@ConfigurationProperties("tms.trainer.management")
public record TrainerManagementProperties(
    @DefaultValue("SUPER_ADMIN") List<String> roles,
    List<String> locationTimeZones,
    @DefaultValue Audit audit) {

  public TrainerManagementProperties {
    locationTimeZones = locationTimeZones == null ? List.of() : List.copyOf(locationTimeZones);
  }

  /**
   * @param unauthorizedAccessEnabled write an UnauthorizedAccessEntry for every 403
   */
  public record Audit(@DefaultValue("true") boolean unauthorizedAccessEnabled) {}
}
