package com.tms.crosscutting.security;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@link AuthContext} (actor, roles, tenant) of the current request from the Spring
 * Security context populated by the bearer-JWT resource server. Used by application services for
 * RBAC decisions and audit actor attribution.
 */
@Component
public class AuthContextProvider {

  private static final String ROLE_PREFIX = "ROLE_";

  private final TenantResolver tenantResolver;

  public AuthContextProvider(TenantResolver tenantResolver) {
    this.tenantResolver = tenantResolver;
  }

  public AuthContext current() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return new AuthContext(AuthContext.ANONYMOUS, Set.of(), Optional.empty());
    }
    Set<String> roles =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a != null && a.startsWith(ROLE_PREFIX))
            .map(a -> a.substring(ROLE_PREFIX.length()))
            .collect(Collectors.toUnmodifiableSet());
    return new AuthContext(
        authentication.getName(), roles, tenantResolver.resolveTenant(authentication));
  }
}
