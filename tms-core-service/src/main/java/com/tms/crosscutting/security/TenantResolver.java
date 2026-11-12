package com.tms.crosscutting.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Resolves the tenant of the caller from the configured JWT claim (default {@code tid}). */
@Component
public class TenantResolver {

  private final String tenantClaim;

  public TenantResolver(TmsSecurityProperties properties) {
    this.tenantClaim = properties.jwt().tenantClaim();
  }

  public Optional<String> resolveTenant(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken token) {
      return Optional.ofNullable(token.getToken().getClaimAsString(tenantClaim))
          .filter(t -> !t.isBlank());
    }
    return Optional.empty();
  }
}
