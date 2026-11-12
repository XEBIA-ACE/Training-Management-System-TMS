package com.tms.crosscutting.security;

/**
 * Role names referenced by the API specification. Roles are taken as-is from JWT claims and exposed
 * as {@code ROLE_<name>} authorities.
 */
public final class TmsRoles {

  public static final String SUPER_ADMIN = "SUPER_ADMIN";
  public static final String OPS_MONITOR = "OPS_MONITOR";

  private TmsRoles() {}
}
