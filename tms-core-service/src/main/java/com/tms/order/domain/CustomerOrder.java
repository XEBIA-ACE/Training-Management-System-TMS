package com.tms.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** An order placed by a customer for a quantity of one product (US-002). */
@Entity
@Table(name = "orders")
public class CustomerOrder {

  @Id private UUID id;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected CustomerOrder() {}

  public CustomerOrder(UUID customerId, UUID productId, int quantity, Instant now) {
    this.id = UUID.randomUUID();
    this.customerId = customerId;
    this.productId = productId;
    this.quantity = quantity;
    this.createdAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
