package com.rk.fooddelivery.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.UUID;
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
  private RequestPostProcessor customer;

  @BeforeEach
  void fixture() throws Exception {
    user("customer@checkout.test", "customer");
    UUID owner = user("owner@checkout.test", "restaurant_owner");
    UUID city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Pune','MH','India','INR') RETURNING id",
            UUID.class);
    UUID restaurant =
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
