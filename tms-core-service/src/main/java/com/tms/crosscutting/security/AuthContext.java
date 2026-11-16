package com.tms.crosscutting.security;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Identity of the current caller as resolved from the bearer JWT by {@link AuthContextProvider}.
 *
 * @param actorId subject ({@code sub}) of the token, or {@code anonymous} when unauthenticated
 * @param roles role names without the {@code ROLE_} prefix
 * @param tenant tenant claim when present
 */
public record AuthContext(String actorId, Set<String> roles, Optional<String> tenant) {

  public static final String ANONYMOUS = "anonymous";

  public boolean authenticated() {
    return !ANONYMOUS.equals(actorId);
  }

  public boolean hasAnyRole(Collection<String> candidates) {
    return candidates.stream().anyMatch(roles::contains);
  }
}
