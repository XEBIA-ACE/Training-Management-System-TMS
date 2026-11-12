package com.tms.outbox.domain;

/** The requested operation is not allowed for the entry's current {@link OutboxStatus}. */
public class InvalidOutboxStateException extends RuntimeException {

  public InvalidOutboxStateException(String message) {
    super(message);
  }
}
