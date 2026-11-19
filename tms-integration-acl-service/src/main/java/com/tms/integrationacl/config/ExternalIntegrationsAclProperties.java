package com.tms.integrationacl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalized configuration of SVC-003 ({@code tms.integration-acl.*}), bound from environment
 * variables via {@code application.yml}.
 *
 * <p>TODO SCAFFOLD-SVC-003: {@code defaultValidity} and provider aliasing are named in the C4
 * insights but their types and semantics are not specified; add them with the ACL domain story.
 */
@ConfigurationProperties(prefix = "tms.integration-acl")
public record ExternalIntegrationsAclProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue Persistence persistence,
    @DefaultValue Messaging messaging,
    @DefaultValue Security security) {

  public record Persistence(@DefaultValue("false") boolean enabled) {}

  public record Messaging(@DefaultValue("false") boolean enabled) {}

  public record Security(@DefaultValue Jwt jwt) {}

  public record Jwt(String issuerUri, String jwkSetUri) {}
}
