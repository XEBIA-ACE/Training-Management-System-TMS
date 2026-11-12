package com.tms.crosscutting.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Maps role claims (by default {@code roles}, {@code groups}, {@code app_roles}) of a bearer JWT to
 * {@code ROLE_<value>} authorities. Claim values are used verbatim; no roles are created or
 * renamed.
 */
@Component
public class TmsJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final String ROLE_PREFIX = "ROLE_";

  private final TmsSecurityProperties.Jwt properties;

  public TmsJwtAuthenticationConverter(TmsSecurityProperties properties) {
    this.properties = properties.jwt();
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    return new JwtAuthenticationToken(jwt, extractAuthorities(jwt), jwt.getSubject());
  }

  Set<GrantedAuthority> extractAuthorities(Jwt jwt) {
    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    for (String claim : properties.roleClaims()) {
      Object value = jwt.getClaims().get(claim);
      if (value instanceof Collection<?> values) {
        values.forEach(v -> addRole(authorities, v));
      } else {
        addRole(authorities, value);
      }
    }
    return authorities;
  }

  private static void addRole(Set<GrantedAuthority> authorities, Object value) {
    if (value instanceof String role && !role.isBlank()) {
      String trimmed = role.trim();
      authorities.add(
          new SimpleGrantedAuthority(
              trimmed.startsWith(ROLE_PREFIX) ? trimmed : ROLE_PREFIX + trimmed));
    }
  }
}
