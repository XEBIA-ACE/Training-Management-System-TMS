package com.tms.outbox.infrastructure.acl;

/** Raised when the ACL denies a notification send; the message carries the denial reason. */
public class AclDeniedException extends RuntimeException {

  public AclDeniedException(String reason) {
    super("Notification denied by Integration ACL: " + reason);
  }
}
