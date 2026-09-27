package com.rk.fooddelivery.support;

import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

  private static final String CLEAR_DATABASE =
      """
        TRUNCATE TABLE order_items, payments, orders, cart_items, carts, menu_items,
        menu_categories, restaurant_cuisines, restaurant_timings, restaurants, cuisines,
        coupons, cities, users RESTART IDENTITY CASCADE
        """;

  @Autowired protected JdbcTemplate jdbc;

  @Autowired(required = false)
  protected MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private PlatformTransactionManager transactionManager;

  @BeforeEach
  @AfterEach
  void clearDatabase() {
    String database = jdbc.queryForObject("SELECT current_database()", String.class);
    if (!"fooddelivery_assignment_test".equals(database)) {
      throw new IllegalStateException(
          "Refusing to clear a non-assignment test database: " + database);
    }
    jdbc.execute(CLEAR_DATABASE);
  }

  protected void committed(Consumer<JdbcTemplate> fixture) {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(status -> fixture.accept(jdbc));
  }

  protected RequestPostProcessor bearer(String username, String password) throws Exception {
    if (mockMvc == null) {
      throw new IllegalStateException(
          "MockMvc is required to issue an integration-test bearer token");
    }
    String response =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/auth/tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            java.util.Map.of("username", username, "password", password))))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = objectMapper.readTree(response).path("accessToken").asText();
    if (token.isBlank()) {
      throw new IllegalStateException("Token fixture request did not return an access token");
    }
    return request -> {
      request.addHeader("Authorization", "Bearer " + token);
      return request;
    };
  }
}
