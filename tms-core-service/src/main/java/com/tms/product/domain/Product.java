package com.tms.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Product master record, introduced for US-002 so orders can reference a product. GAP: no product
 * attributes are defined by the story; only ID and name are modelled.
 */
@Entity
@Table(name = "product")
public class Product {

  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Version private long version;

  protected Product() {}

  public Product(String name, Instant now) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.active = true;
    this.createdAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public boolean isActive() {
    return active;
  }
}
