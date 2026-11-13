package com.tms.customer.api;

import com.tms.customer.application.CustomerService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer REST API (US-001). Served under the service's existing {@code /api} base path, so the
 * spec's {@code /customers} and {@code /customers/:id} routes are {@code /api/customers} and {@code
 * /api/customers/{id}}. Access requires an authenticated bearer JWT (SecurityConfig).
 */
@RestController
@RequestMapping(CustomerController.BASE_PATH)
public class CustomerController {

  public static final String BASE_PATH = "/api/customers";

  private final CustomerService service;

  public CustomerController(CustomerService service) {
    this.service = service;
  }

  /** FR-US-001-001: 201 Created with the new customer's ID, name and city. */
  @PostMapping
  public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest body) {
    CustomerResponse created = CustomerResponse.from(service.create(body.name(), body.city()));
    return ResponseEntity.created(URI.create(BASE_PATH + "/" + created.id())).body(created);
  }

  /** FR-US-001-002: 200 OK with all (active) customers. */
  @GetMapping
  public List<CustomerResponse> list() {
    return service.list().stream().map(CustomerResponse::from).toList();
  }

  /** FR-US-001-003: 200 OK, or 404 when the customer does not exist. */
  @GetMapping("/{id}")
  public CustomerResponse get(@PathVariable UUID id) {
    return CustomerResponse.from(service.get(id));
  }

  /** FR-US-001-004: 200 OK with the updated customer, or 404. */
  @PutMapping("/{id}")
  public CustomerResponse update(
      @PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest body) {
    return CustomerResponse.from(service.update(id, body.name(), body.city()));
  }

  /** FR-US-001-005: 204 No Content (soft delete), or 404. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
