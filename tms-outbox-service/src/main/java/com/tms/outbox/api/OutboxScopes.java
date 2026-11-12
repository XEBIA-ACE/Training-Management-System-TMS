package com.tms.outbox.api;

/**
 * OAuth2 scopes of the Outbox Processing Service API ("API specification - Authentication") and the
 * matching {@code @PreAuthorize} expressions (JWT {@code scope}/{@code scp} claims become {@code
 * SCOPE_*} authorities).
 *
 * <p>GAP: the export does not define a scope hierarchy (e.g. whether {@code outbox:admin} implies
 * {@code outbox:read}); each endpoint requires exactly the scope listed in the OpenAPI document.
 */
public final class OutboxScopes {

  public static final String READ = "outbox:read";
  public static final String MANAGE = "outbox:manage";
  public static final String ADMIN = "outbox:admin";

  public static final String HAS_READ = "hasAuthority('SCOPE_" + READ + "')";
  public static final String HAS_MANAGE = "hasAuthority('SCOPE_" + MANAGE + "')";
  public static final String HAS_ADMIN = "hasAuthority('SCOPE_" + ADMIN + "')";

  private OutboxScopes() {}
}
