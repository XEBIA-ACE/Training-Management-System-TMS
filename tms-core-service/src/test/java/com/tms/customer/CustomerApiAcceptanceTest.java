package com.tms.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * US-001 acceptance tests for the customer REST API under {@code /api/v1/customers}
 * (FR-US-001-AC1..FR-US-001-AC5, tasks T006..T010). Test obligations:
 * test:acceptance:US-001-ac-1..ac-5.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CustomerApiAcceptanceTest {

  private static final String CUSTOMERS = "/api/v1/customers";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clean() {
    jdbcTemplate.update("delete from tms.customer");
  }

  @Test
  @DisplayName(
      "[US-001-ac-1][FR-US-001-AC1][T006] POST /api/v1/customers returns 201 with customer ID, name and city")
  void us001Ac1_createReturns201WithIdNameAndCity() throws Exception {
    mockMvc
        .perform(authed(post(CUSTOMERS)).content("{\"name\":\"Acme Corp\",\"city\":\"Pune\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", Matchers.startsWith(CUSTOMERS + "/")))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.name").value("Acme Corp"))
        .andExpect(jsonPath("$.city").value("Pune"));
  }

  @Test
  @DisplayName(
      "[US-001-ac-1][FR-US-001-AC1][T006] POST /api/v1/customers rejects a missing name/city with 400")
  void us001Ac1_createValidatesPayload() throws Exception {
    mockMvc
        .perform(authed(post(CUSTOMERS)).content("{\"name\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.details.length()").value(2));
  }

  @Test
  @DisplayName(
      "[US-001-ac-2][FR-US-001-AC2][T007] GET /api/v1/customers returns 200 with a page of"
          + " customers (id, name, city)")
  void us001Ac2_listReturnsPageOfCustomers() throws Exception {
    String betaId = create("Beta Ltd", "Delhi");
    String alphaId = create("Alpha Inc", "Mumbai");

    mockMvc
        .perform(authed(get(CUSTOMERS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].id").value(alphaId))
        .andExpect(jsonPath("$.content[0].name").value("Alpha Inc"))
        .andExpect(jsonPath("$.content[0].city").value("Mumbai"))
        .andExpect(jsonPath("$.content[1].id").value(betaId))
        .andExpect(jsonPath("$.content[1].name").value("Beta Ltd"))
        .andExpect(jsonPath("$.content[1].city").value("Delhi"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  @DisplayName("[US-001-ac-2][FR-US-001-AC2][T007] GET /api/v1/customers paginates with page/size")
  void us001Ac2_listSupportsPagination() throws Exception {
    create("Charlie", "Goa");
    create("Alpha Inc", "Mumbai");
    create("Beta Ltd", "Delhi");

    mockMvc
        .perform(authed(get(CUSTOMERS)).param("page", "0").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].name").value("Alpha Inc"))
        .andExpect(jsonPath("$.content[1].name").value("Beta Ltd"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));

    mockMvc
        .perform(authed(get(CUSTOMERS)).param("page", "1").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Charlie"))
        .andExpect(jsonPath("$.content[0].city").value("Goa"))
        .andExpect(jsonPath("$.page").value(1));
  }

  @Test
  @DisplayName(
      "[US-001-ac-2][FR-US-001-AC2][T007] GET /api/v1/customers rejects invalid page/size with 400")
  void us001Ac2_listRejectsInvalidPagination() throws Exception {
    mockMvc
        .perform(authed(get(CUSTOMERS)).param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.details[0].field").value("page"));
    mockMvc
        .perform(authed(get(CUSTOMERS)).param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("size"));
    mockMvc
        .perform(authed(get(CUSTOMERS)).param("size", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName(
      "[US-001-ac-3][FR-US-001-AC3][T008] GET /api/v1/customers/{id} returns 200 for an existing customer")
  void us001Ac3_getByIdReturnsCustomer() throws Exception {
    String id = create("Acme Corp", "Pune");

    mockMvc
        .perform(authed(get(CUSTOMERS + "/" + id)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value("Acme Corp"))
        .andExpect(jsonPath("$.city").value("Pune"));
  }

  @Test
  @DisplayName(
      "[US-001-ac-3][FR-US-001-AC3][T008] GET /api/v1/customers/{id} returns 404 for an unknown customer")
  void us001Ac3_getByIdReturns404WhenMissing() throws Exception {
    mockMvc
        .perform(authed(get(CUSTOMERS + "/" + UUID.randomUUID())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
  }

  @Test
  @DisplayName(
      "[US-001-ac-4][FR-US-001-AC4][T009] PUT /api/v1/customers/{id} updates name and/or city and returns 200")
  void us001Ac4_updateNameAndOrCity() throws Exception {
    String id = create("Acme Corp", "Pune");

    mockMvc
        .perform(authed(put(CUSTOMERS + "/" + id)).content("{\"city\":\"Bengaluru\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Acme Corp"))
        .andExpect(jsonPath("$.city").value("Bengaluru"));

    mockMvc
        .perform(
            authed(put(CUSTOMERS + "/" + id))
                .content("{\"name\":\"Acme Global\",\"city\":\"Chennai\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Acme Global"))
        .andExpect(jsonPath("$.city").value("Chennai"));

    mockMvc
        .perform(authed(get(CUSTOMERS + "/" + id)))
        .andExpect(jsonPath("$.name").value("Acme Global"))
        .andExpect(jsonPath("$.city").value("Chennai"));
  }

  @Test
  @DisplayName(
      "[US-001-ac-4][FR-US-001-AC4][T009] PUT /api/v1/customers/{id} validates the body and"
          + " returns 404 if missing")
  void us001Ac4_updateValidationAndNotFound() throws Exception {
    String id = create("Acme Corp", "Pune");
    mockMvc
        .perform(authed(put(CUSTOMERS + "/" + id)).content("{}"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(authed(put(CUSTOMERS + "/" + UUID.randomUUID())).content("{\"name\":\"X\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName(
      "[US-001-ac-5][FR-US-001-AC5][T010] DELETE /api/v1/customers/{id} returns 204, then 404 for that customer")
  void us001Ac5_deleteRemovesCustomer() throws Exception {
    String id = create("Acme Corp", "Pune");

    mockMvc.perform(authed(delete(CUSTOMERS + "/" + id))).andExpect(status().isNoContent());

    mockMvc.perform(authed(get(CUSTOMERS + "/" + id))).andExpect(status().isNotFound());
    mockMvc.perform(authed(get(CUSTOMERS))).andExpect(jsonPath("$.totalElements").value(0));
    mockMvc.perform(authed(delete(CUSTOMERS + "/" + id))).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName(
      "[US-001-ac-5][FR-US-001-AC5][T010] DELETE is a soft delete: the row is retained and deactivated")
  void us001Ac5_deleteIsSoft() throws Exception {
    String id = create("Acme Corp", "Pune");

    mockMvc.perform(authed(delete(CUSTOMERS + "/" + id))).andExpect(status().isNoContent());

    Boolean active =
        jdbcTemplate.queryForObject(
            "select active from tms.customer where id = ?::uuid", Boolean.class, id);
    assertThat(active).isFalse();
  }

  @Test
  @DisplayName("[US-001] customer endpoints require an authenticated bearer token")
  void customerEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get(CUSTOMERS)).andExpect(status().isUnauthorized());
    mockMvc
        .perform(post(CUSTOMERS).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(get(CUSTOMERS + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    mockMvc
        .perform(delete(CUSTOMERS + "/" + UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  private String create(String name, String city) throws Exception {
    String body =
        mockMvc
            .perform(
                authed(post(CUSTOMERS))
                    .content(objectMapper.writeValueAsString(Map.of("name", name, "city", city))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode node = objectMapper.readTree(body);
    return node.get("id").asText();
  }

  private static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder) {
    return builder.with(jwt()).contentType(MediaType.APPLICATION_JSON);
  }
}
