package com.tms.crosscutting.security;

import java.io.Serializable;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 * Resource-level permission evaluator used by {@code hasPermission(...)} in {@code @PreAuthorize}
 * expressions (C4: SecurityConfig --> RbacPermissionEvaluator).
 *
 * <p>GAP (specs/SCAFFOLD-SVC-001/references/SVC-001.md, "C4 - Diagram" RbacPermissionEvaluator):
 * the permission model (module permissions, VIEW/EXPORT semantics) is not specified, so this
 * scaffold is fail-closed and denies every permission check until a later story defines it.
 */
public class RbacPermissionEvaluator implements PermissionEvaluator {

  @Override
  public boolean hasPermission(
      Authentication authentication, Object targetDomainObject, Object permission) {
    return false;
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    return false;
  }
}
