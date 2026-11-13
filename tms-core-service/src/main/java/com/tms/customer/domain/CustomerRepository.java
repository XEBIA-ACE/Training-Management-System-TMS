package com.tms.customer.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  Page<Customer> findAllByActiveTrue(Pageable pageable);

  Optional<Customer> findByIdAndActiveTrue(UUID id);
}
