package com.tms.product.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  Optional<Product> findByIdAndActiveTrue(UUID id);
}
