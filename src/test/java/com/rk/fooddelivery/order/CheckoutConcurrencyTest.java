package com.rk.fooddelivery.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMockMvc
class CheckoutConcurrencyTest extends IntegrationTestSupport {

  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;

  private UUID item;
  private UUID restaurant;
  private RequestPostProcessor customer;

  @BeforeEach
  void fixture() throws Exception {
    user("customer@checkout.test", "customer");
    UUID owner = user("owner@checkout.test", "restaurant_owner");
    UUID city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Pune','MH','India','INR') RETURNING id",
            UUID.class);
    restaurant =
        jdbc.queryForObject(
            "INSERT INTO restaurants (name,owner_id,city_id,cost_for_two,diet_type,address_line_1,location) VALUES ('Checkout Kitchen',?,?,200,'Veg','Road',ST_GeogFromText('POINT(73.8567 18.5204)')) RETURNING id",
            UUID.class,
            owner,
            city);
    UUID category =
        jdbc.queryForObject(
            "INSERT INTO menu_categories (restaurant_id,name) VALUES (?,'Mains') RETURNING id",
            UUID.class,
            restaurant);
    item =
        jdbc.queryForObject(
            "INSERT INTO menu_items (restaurant_id,category_id,name,diet_type,price,is_available,available_quantity) VALUES (?,?,'Thali','Veg',100,true,2) RETURNING id",
            UUID.class,
            restaurant,
            category);
    customer = bearer("customer@checkout.test", "secret");
    mvc.perform(
            put("/api/cart/items/{itemId}", item)
                .with(customer)
                .contentType("application/json")
                .content("{\"quantity\":2}"))
        .andExpect(status().isOk());
  }

  @Test
  void twentySimultaneousCheckoutsAgainstStockFiveCreateOnlyFiveOrders() throws Exception {
    jdbc.update("UPDATE menu_items SET available_quantity=5 WHERE id=?", item);
    List<RequestPostProcessor> customers = new ArrayList<>();
    for (int number = 0; number < 20; number++) {
      String username = "customer-" + number + "@checkout-race.test";
      UUID customerId = user(username, "customer");
      UUID cartId = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO carts (id,customer_id,restaurant_id) VALUES (?,?,?)",
          cartId,
          customerId,
          restaurant);
      jdbc.update(
          "INSERT INTO cart_items (id,cart_id,restaurant_id,menu_item_id,quantity) VALUES (?,?,?,?,1)",
          UUID.randomUUID(),
          cartId,
          restaurant,
          item);
      customers.add(bearer(username, "secret"));
    }

    CyclicBarrier barrier = new CyclicBarrier(customers.size());
    ExecutorService executor = Executors.newFixedThreadPool(customers.size());
    try {
      List<Future<Integer>> results = new ArrayList<>();
      for (int number = 0; number < customers.size(); number++) {
        RequestPostProcessor customerBearer = customers.get(number);
        int requestNumber = number;
        results.add(
            executor.submit(
                () -> {
                  barrier.await();
                  return mvc.perform(
                          post("/api/orders")
                              .with(customerBearer)
                              .header("Idempotency-Key", "stock-race-" + requestNumber)
                              .contentType("application/json")
                              .content(
                                  "{\"paymentMethod\":\"UPI\",\"addressLine1\":\"42 Lane\",\"city\":\"Pune\",\"state\":\"MH\",\"country\":\"India\",\"cartVersion\":0}"))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      }
      List<Integer> statuses = new ArrayList<>();
      for (Future<Integer> result : results) statuses.add(result.get());

      assertThat(statuses.stream().filter(status -> status == 201).count()).isEqualTo(5);
      assertThat(statuses.stream().filter(status -> status == 409).count()).isEqualTo(15);
    } finally {
      executor.shutdownNow();
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "SELECT available_quantity FROM menu_items WHERE id=?", Integer.class, item))
        .isZero();
  }

  @Test
  void replayedCheckoutKeyCreatesOneOrderAndReservesStockOnce() throws Exception {
    String checkout =
        "{\"paymentMethod\":\"UPI\",\"addressLine1\":\"42 Lane\",\"city\":\"Pune\",\"state\":\"MH\",\"country\":\"India\",\"cartVersion\":1}";
    for (int attempt = 0; attempt < 2; attempt++) {
      mvc.perform(
              post("/api/orders")
                  .with(customer)
                  .header("Idempotency-Key", "checkout-replay")
                  .contentType("application/json")
                  .content(checkout))
          .andExpect(status().isCreated());
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT available_quantity FROM menu_items WHERE id=?", Integer.class, item))
        .isZero();
  }

  private UUID user(String email, String role) {
    UUID id =
        jdbc.queryForObject(
            "INSERT INTO users (name,email,phone_number,role) VALUES (?,?,?,?) RETURNING id",
            UUID.class,
            email,
            email,
            "+9198" + Math.abs(email.hashCode()),
            role);
    jdbc.update(
        "INSERT INTO user_credentials (user_id,username,password_hash) VALUES (?,?,?)",
        id,
        email,
        passwords.encode("secret"));
    return id;
  }
}
