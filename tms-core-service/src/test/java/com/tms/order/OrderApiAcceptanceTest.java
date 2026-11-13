package com.tms.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.customer.domain.Customer;
import com.tms.customer.domain.CustomerRepository;
import com.tms.order.domain.OrderRepository;
import com.tms.product.domain.Product;
import com.tms.product.domain.ProductRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * US-002 acceptance tests for {@code POST /api/v1/orders}. Test obligations:
 * bb21c484-7d07-4825-a021-6cb339f07906:test:acceptance:US-002-ac-1..ac-5.
 *
 * <p>The success code (201) and the error codes/body shape are local implementation choices (see
 * docs/adr/US-002-orders-unspecified-behavior.md), not spec requirements; the 400 statuses are.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OrderApiAcceptanceTest {

  private static final String ORDERS = "/api/v1/orders";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired CustomerRepository customers;
  @Autowired ProductRepository products;
  @Autowired OrderRepository orders;

  private UUID customerId;
  private UUID productId;

  @BeforeEach
  void seed() {
    jdbcTemplate.update("delete from tms.orders");
    jdbcTemplate.update("delete from tms.product");
    jdbcTemplate.update("delete from tms.customer");
    customerId = customers.save(new Customer("Acme Corp", "Pune", Instant.now())).getId();
    productId = products.save(new Product("Widget", Instant.now())).getId();
  }

  @Test
  @DisplayName("[US-002-ac-1] valid order returns 2xx with the new order representation")
  void us002Ac1_validOrderReturns2xxWithOrder() throws Exception {
    placeOrder(customerId, productId, 3)
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.customerId").value(customerId.toString()))
        .andExpect(jsonPath("$.productId").value(productId.toString()))
        .andExpect(jsonPath("$.quantity").value(3));
    assertThat(orders.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("[US-002-ac-2] unknown customer ID returns 400 and no order is created")
  void us002Ac2_unknownCustomerReturns400() throws Exception {
    placeOrder(UUID.randomUUID(), productId, 1)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    assertThat(orders.count()).isZero();
  }

  @Test
  @DisplayName("[US-002-ac-3] unknown product ID returns 400 and no order is created")
  void us002Ac3_unknownProductReturns400() throws Exception {
    placeOrder(customerId, UUID.randomUUID(), 1)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    assertThat(orders.count()).isZero();
  }

  @Test
  @DisplayName("[US-002-ac-4] quantity <= 0 returns 400 Bad Request")
  void us002Ac4_nonPositiveQuantityReturns400() throws Exception {
    placeOrder(customerId, productId, 0).andExpect(status().isBadRequest());
    placeOrder(customerId, productId, -5).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("[US-002-ac-5] failed validation attempts never persist an order")
  void us002Ac5_failedAttemptsPersistNothing() throws Exception {
    placeOrder(customerId, productId, 0).andExpect(status().isBadRequest());
    placeOrder(customerId, productId, -1).andExpect(status().isBadRequest());
    placeOrder(UUID.randomUUID(), productId, 1).andExpect(status().isBadRequest());
    placeOrder(customerId, UUID.randomUUID(), 1).andExpect(status().isBadRequest());
    // FR-US-002-007: multiple errors -> still 400 and nothing persisted.
    placeOrder(UUID.randomUUID(), productId, 0).andExpect(status().isBadRequest());
    assertThat(orders.count()).isZero();
  }

  private ResultActions placeOrder(UUID customer, UUID product, int quantity) throws Exception {
    String body =
        "{\"customerId\":\"%s\",\"productId\":\"%s\",\"quantity\":%d}"
            .formatted(customer, product, quantity);
    return mockMvc.perform(
        post(ORDERS).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body));
  }
}
