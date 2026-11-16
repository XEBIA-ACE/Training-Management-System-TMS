package com.tms.trainer.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Trainer directory policy bound from {@code tms.trainer.directory.*}.
 *
 * <p>GAP(FR-US-002-006/007): neither the RBAC role mapping nor the auditing policy is defined by
 * the story. They are configuration so the external security policy can be applied without a code
 * change. The defaults fail closed (only {@code SUPER_ADMIN}) and audit every request.
 *
 * @param roles roles (without {@code ROLE_}) allowed to search trainers and view trainer profiles
 * @param audit which trainer directory operations write audit entries
 */
@ConfigurationProperties("tms.trainer.directory")
public record TrainerDirectoryProperties(
    @DefaultValue("SUPER_ADMIN") List<String> roles, @DefaultValue Audit audit) {

  /**
   * @param searchEnabled write an audit entry for every {@code GET /api/trainers} request
   * @param unauthorizedAccessEnabled write an UnauthorizedAccessEntry for every 403
   */
  public record Audit(
      @DefaultValue("true") boolean searchEnabled,
      @DefaultValue("true") boolean unauthorizedAccessEnabled) {}
}
