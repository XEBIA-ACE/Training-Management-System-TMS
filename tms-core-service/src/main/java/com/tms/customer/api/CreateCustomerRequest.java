package com.tms.customer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/v1/customers} (FR-US-001-AC1). */
public record CreateCustomerRequest(
    @NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 120) String city) {}
