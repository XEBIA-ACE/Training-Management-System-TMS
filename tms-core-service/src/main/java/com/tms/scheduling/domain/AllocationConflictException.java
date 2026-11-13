package com.tms.scheduling.domain;

public class AllocationConflictException extends RuntimeException {
  public AllocationConflictException(String message) {
    super(message);
  }
}
