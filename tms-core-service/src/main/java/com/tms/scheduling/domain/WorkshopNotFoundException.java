package com.tms.scheduling.domain;

import java.util.UUID;

public class WorkshopNotFoundException extends RuntimeException {
  public WorkshopNotFoundException(UUID id) {
    super("Workshop " + id + " was not found.");
  }
}
