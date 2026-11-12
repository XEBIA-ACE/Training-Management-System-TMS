package com.tms.crosscutting.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Security settings bound from {@code tms.security.*} (populated from environment variables). */
@ConfigurationProperties("tms.security")
public record TmsSecurityProperties(
    @DefaultValue Jwt jwt, @DefaultValue LegacyAuth legacyAuth, boolean openapiPublic) {

  /**
   * Bearer JWT validation and claim mapping.
   *
   * @param issuerUri OIDC issuer; used for discovery and {@code iss} validation
   * @param jwkSetUri explicit JWK set URI; takes precedence over discovery for key lookup
   * @param audiences accepted {@code aud} values; empty disables audience validation
   * @param required fail startup when neither issuer nor JWK set URI is configured
   * @param roleClaims claims whose values are mapped to {@code ROLE_*} authorities
   * @param tenantClaim claim carrying the tenant identifier
   */
  public record Jwt(
      String issuerUri,
      String jwkSetUri,
      @DefaultValue List<String> audiences,
      @DefaultValue("true") boolean required,
      @DefaultValue({"roles", "groups", "app_roles"}) List<String> roleClaims,
      @DefaultValue("tid") String tenantClaim) {}

  /** Legacy username/password endpoints ({@code /api/auth/login}, {@code /api/auth/refresh}). */
  public record LegacyAuth(boolean enabled) {}
}
