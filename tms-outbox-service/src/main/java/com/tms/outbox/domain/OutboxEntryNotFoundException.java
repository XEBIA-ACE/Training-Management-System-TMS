package com.tms.outbox.domain;

import java.util.UUID;

/** No outbox entry exists for the given id. */
public class OutboxEntryNotFoundException extends RuntimeException {

  public OutboxEntryNotFoundException(UUID id) {
    super("Outbox entry " + id + " was not found");
  }
}
