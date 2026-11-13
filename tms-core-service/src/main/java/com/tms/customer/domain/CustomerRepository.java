package com.tms.customer.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  List<Customer> findAllByActiveTrueOrderByNameAsc();

  Optional<Customer> findByIdAndActiveTrue(UUID id);
}
