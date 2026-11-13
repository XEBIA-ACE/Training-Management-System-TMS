package com.tms.customer.api;

import com.tms.customer.domain.Customer;
import java.util.UUID;

/** Customer representation returned by the customer REST API (ID, name, city). */
public record CustomerResponse(UUID id, String name, String city) {

  static CustomerResponse from(Customer customer) {
    return new CustomerResponse(customer.getId(), customer.getName(), customer.getCity());
  }
}
