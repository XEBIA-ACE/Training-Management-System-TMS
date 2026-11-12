package com.tms.outbox.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Security settings bound from {@code tms.outbox.security.*} ({@code TMS_OUTBOX_JWT_*} env). */
@ConfigurationProperties("tms.outbox.security")
public record OutboxSecurityProperties(@DefaultValue Jwt jwt) {

  /**
   * Bearer JWT validation (same pattern as tms-core-service).
   *
   * @param issuerUri OIDC issuer of the central IdP; used for discovery and {@code iss} validation
   * @param jwkSetUri explicit JWK set URI; takes precedence over discovery for key lookup
   * @param audiences accepted {@code aud} values; empty disables audience validation
   * @param required fail startup when neither issuer nor JWK set URI is configured
   */
  public record Jwt(
      String issuerUri,
      String jwkSetUri,
      @DefaultValue List<String> audiences,
      @DefaultValue("true") boolean required) {}
}
