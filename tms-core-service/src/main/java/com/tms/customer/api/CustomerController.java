package com.tms.customer.api;

import com.tms.customer.application.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer REST API (US-001, FR-US-001-AC1..FR-US-001-AC5) under {@code /api/v1/customers}. Access
 * requires an authenticated bearer JWT (SecurityConfig).
 */
@RestController
@RequestMapping(CustomerController.BASE_PATH)
public class CustomerController {

  public static final String BASE_PATH = "/api/v1/customers";

  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 100;

  private final CustomerService service;

  public CustomerController(CustomerService service) {
    this.service = service;
  }

  /** FR-US-001-AC1 (T001): 201 Created with the new customer's ID, name and city. */
  @PostMapping
  public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest body) {
    CustomerResponse created = CustomerResponse.from(service.create(body.name(), body.city()));
    return ResponseEntity.created(URI.create(BASE_PATH + "/" + created.id())).body(created);
  }

  /**
   * FR-US-001-AC2 (T002): 200 OK with one page of (active) customers, ordered by name. {@code page}
   * is zero-based; out-of-range {@code page}/{@code size} values are rejected with 400.
   *
   * <p>GAP(FR-US-001-AC2): pagination parameters, defaults and limits are not specified; offset
   * paging with {@code page} (default 0) and {@code size} (default 20, max 100) is an
   * implementation choice.
   */
  @GetMapping
  public CustomerPageResponse list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
    return CustomerPageResponse.from(service.list(page, size).map(CustomerResponse::from));
  }

  /** FR-US-001-AC3 (T003): 200 OK, or 404 when the customer does not exist. */
  @GetMapping("/{id}")
  public CustomerResponse get(@PathVariable UUID id) {
    return CustomerResponse.from(service.get(id));
  }

  /** FR-US-001-AC4 (T004): 200 OK with the updated customer, or 404. */
  @PutMapping("/{id}")
  public CustomerResponse update(
      @PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest body) {
    return CustomerResponse.from(service.update(id, body.name(), body.city()));
  }

  /** FR-US-001-AC5 (T005): 204 No Content (soft delete), or 404. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
