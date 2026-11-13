package com.tms.order.api;

import com.tms.order.domain.CustomerOrder;
import java.time.Instant;
import java.util.UUID;

/** Created order: its ID plus details (customer, product, quantity, creation time). */
public record OrderResponse(
    UUID id, UUID customerId, UUID productId, int quantity, Instant createdAt) {

  static OrderResponse from(CustomerOrder order) {
    return new OrderResponse(
        order.getId(),
        order.getCustomerId(),
        order.getProductId(),
        order.getQuantity(),
        order.getCreatedAt());
  }
}
