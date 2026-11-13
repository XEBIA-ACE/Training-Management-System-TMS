package com.tms.customer.domain;

import java.util.UUID;

/** No active customer exists with the requested ID (mapped to HTTP 404). */
public class CustomerNotFoundException extends RuntimeException {

  public CustomerNotFoundException(UUID id) {
    super("Customer " + id + " was not found.");
  }
}
