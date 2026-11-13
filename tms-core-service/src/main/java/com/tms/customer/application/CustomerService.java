package com.tms.customer.application;

import com.tms.customer.domain.Customer;
import com.tms.customer.domain.CustomerNotFoundException;
import com.tms.customer.domain.CustomerRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer use cases for US-001 (FR-US-001-001..FR-US-001-005). */
@Service
@Transactional
public class CustomerService {

  private final CustomerRepository repository;

  public CustomerService(CustomerRepository repository) {
    this.repository = repository;
  }

  public Customer create(String name, String city) {
    return repository.save(new Customer(name.trim(), city.trim(), now()));
  }

  @Transactional(readOnly = true)
  public List<Customer> list() {
    return repository.findAllByActiveTrueOrderByNameAsc();
  }

  @Transactional(readOnly = true)
  public Customer get(UUID id) {
    return repository
        .findByIdAndActiveTrue(id)
        .orElseThrow(() -> new CustomerNotFoundException(id));
  }

  /** Updates name and/or city; a {@code null} value leaves that field unchanged. */
  public Customer update(UUID id, String name, String city) {
    Customer customer = get(id);
    Instant now = now();
    if (name != null) {
      customer.rename(name.trim(), now);
    }
    if (city != null) {
      customer.relocate(city.trim(), now);
    }
    return customer;
  }

  /** Soft delete: the customer is deactivated and no longer returned by the API. */
  public void delete(UUID id) {
    get(id).deactivate(now());
  }

  private Instant now() {
    return Instant.now();
  }
}
