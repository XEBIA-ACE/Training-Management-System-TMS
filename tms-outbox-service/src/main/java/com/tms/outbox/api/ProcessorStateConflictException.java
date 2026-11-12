package com.tms.outbox.api;

/** Processor start/stop requested while it is already in that state (409). */
public class ProcessorStateConflictException extends RuntimeException {

  public ProcessorStateConflictException(String message) {
    super(message);
  }
}
