package com.tms.order.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/** Body of {@code POST /api/v1/orders}; quantity must be strictly greater than zero. */
public record CreateOrderRequest(
    @NotNull UUID customerId, @NotNull UUID productId, @NotNull @Positive Integer quantity) {}
