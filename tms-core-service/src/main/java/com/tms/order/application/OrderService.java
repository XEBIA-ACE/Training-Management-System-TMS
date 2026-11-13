package com.tms.order.application;

import com.tms.customer.domain.CustomerNotFoundException;
import com.tms.customer.domain.CustomerRepository;
import com.tms.order.domain.CustomerOrder;
import com.tms.order.domain.OrderRepository;
import com.tms.product.domain.ProductNotFoundException;
import com.tms.product.domain.ProductRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order use cases for US-002 (FR-US-002-001..FR-US-002-005). */
@Service
@Transactional
public class OrderService {

  private final OrderRepository orders;
  private final CustomerRepository customers;
  private final ProductRepository products;

  public OrderService(
      OrderRepository orders, CustomerRepository customers, ProductRepository products) {
    this.orders = orders;
    this.customers = customers;
    this.products = products;
  }

  /**
   * Creates an order. All lookups run before the single {@code save}, so any failure leaves the
   * Order repository untouched (FR-US-002-005).
   */
  public CustomerOrder create(UUID customerId, UUID productId, int quantity) {
    customers
        .findByIdAndActiveTrue(customerId)
        .orElseThrow(() -> new CustomerNotFoundException(customerId));
    products
        .findByIdAndActiveTrue(productId)
        .orElseThrow(() -> new ProductNotFoundException(productId));
    return orders.save(new CustomerOrder(customerId, productId, quantity, Instant.now()));
  }
}
