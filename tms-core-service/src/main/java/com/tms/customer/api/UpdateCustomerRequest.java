package com.tms.customer.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/customers/{id}} (FR-US-001-004): name and/or city. Omitted fields are
 * left unchanged; at least one must be provided and provided values must not be blank.
 */
public record UpdateCustomerRequest(
    @Size(max = 200) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String name,
    @Size(max = 120) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String city) {

  @AssertTrue(message = "name and/or city must be provided")
  boolean isNameOrCityPresent() {
    return name != null || city != null;
  }
}
