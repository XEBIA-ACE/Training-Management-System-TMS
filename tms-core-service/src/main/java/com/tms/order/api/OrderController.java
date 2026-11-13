package com.tms.order.api;

import com.tms.order.application.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Order REST API (US-002) under {@code /api/v1/orders}. Requires an authenticated bearer JWT. */
@RestController
@RequestMapping(OrderController.BASE_PATH)
public class OrderController {

  public static final String BASE_PATH = "/api/v1/orders";

  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  /**
   * FR-US-002-001..006: 400 when the customer or product does not exist or the quantity is
   * invalid, otherwise 201 Created with the order ID and details.
   *
   * <p>GAP(FR-US-002-002/003/004/005): error codes ({@code CUSTOMER_NOT_FOUND}, {@code
   * PRODUCT_NOT_FOUND}, {@code VALIDATION_FAILED}), 201 and the payload shape are provisional local
   * choices. See docs/adr/US-002-orders-unspecified-behavior.md.
   */
  @PostMapping
  public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest body) {
    OrderResponse created =
        OrderResponse.from(service.create(body.customerId(), body.productId(), body.quantity()));
    return ResponseEntity.created(URI.create(BASE_PATH + "/" + created.id())).body(created);
  }
}
