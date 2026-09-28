package com.rk.fooddelivery.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
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
class CartIntegrationTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;

  private UUID customer;
  private UUID firstItem;
  private UUID secondItem;
  private UUID otherRestaurantItem;

  @BeforeEach
  void fixtures() {
    customer = customer("customer", "customer@example.test", "+919100000001");
    customer("other-customer", "other@example.test", "+919100000002");
    UUID owner = customer("owner", "owner@example.test", "+919100000003", "restaurant_owner");
    UUID otherOwner =
        customer("other-owner", "other-owner@example.test", "+919100000004", "restaurant_owner");
    UUID city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Pune','Maharashtra','India','INR') RETURNING id",
            UUID.class);
    UUID restaurant = restaurant("First", owner, city);
    UUID otherRestaurant = restaurant("Second", otherOwner, city);
    firstItem = menuItem(restaurant, "First item", true);
    secondItem = menuItem(restaurant, "Second item", true);
    otherRestaurantItem = menuItem(otherRestaurant, "Other item", true);
  }

  @Test
  void customerAddsUpdatesRemovesAndClearsTheirCartWithoutChangingStock() throws Exception {
    int stockBefore = stock(firstItem);
    RequestPostProcessor customerBearer = bearer("customer", "secret");

    mvc.perform(
            put("/api/cart/items/{itemId}", firstItem)
                .with(customerBearer)
                .contentType("application/json")
                .content("{\"quantity\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.restaurantChanged").value(false))
        .andExpect(jsonPath("$.items[0].quantity").value(2));
    mvc.perform(
            put("/api/cart/items/{itemId}", firstItem)
                .with(customerBearer)
                .contentType("application/json")
                .content("{\"quantity\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.items[0].quantity").value(3));
    mvc.perform(delete("/api/cart/items/{itemId}", firstItem).with(customerBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(3))
        .andExpect(jsonPath("$.items").isEmpty());
    mvc.perform(delete("/api/cart").with(customerBearer)).andExpect(status().isNoContent());
    mvc.perform(get("/api/cart").with(customerBearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(4))
        .andExpect(jsonPath("$.items").isEmpty());
    assertThat(stock(firstItem)).isEqualTo(stockBefore);
  }

  @Test
  void addingAnotherRestaurantsItemAtomicallyReplacesTheCart() throws Exception {
    RequestPostProcessor customerBearer = bearer("customer", "secret");
    mvc.perform(
            put("/api/cart/items/{itemId}", firstItem)
                .with(customerBearer)
                .contentType("application/json")
                .content("{\"quantity\":1}"))
        .andExpect(status().isOk());

    mvc.perform(
            put("/api/cart/items/{itemId}", otherRestaurantItem)
                .with(customerBearer)
                .contentType("application/json")
                .content("{\"quantity\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.restaurantChanged").value(true))
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].menuItemId").value(otherRestaurantItem.toString()));
  }

  @Test
  void unavailableItemAndAnotherCustomersCartAreDenied() throws Exception {
    jdbc.update("UPDATE menu_items SET is_available = false WHERE id = ?", secondItem);
    mvc.perform(
            put("/api/cart/items/{itemId}", secondItem)
                .with(bearer("customer", "secret"))
                .contentType("application/json")
                .content("{\"quantity\":1}"))
        .andExpect(status().isNotFound());

    mvc.perform(
            put("/api/cart/items/{itemId}", firstItem)
                .with(bearer("customer", "secret"))
                .contentType("application/json")
                .content("{\"quantity\":1}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/cart").with(bearer("other-customer", "secret")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());
  }

  @Test
  void simultaneousFirstCartCreationProducesOneCartAndBothItems() throws Exception {
    RequestPostProcessor customerBearer = bearer("customer", "secret");
    runTogether(
        () -> {
          putItem(customerBearer, firstItem);
          return null;
        },
        () -> {
          putItem(customerBearer, secondItem);
          return null;
        });

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM carts WHERE customer_id = ?", Integer.class, customer))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM cart_items", Integer.class)).isEqualTo(2);
  }

  @Test
  void replacementRacingAnAddLeavesOnlyOneRestaurantsItems() throws Exception {
    RequestPostProcessor customerBearer = bearer("customer", "secret");
    mvc.perform(
            put("/api/cart/items/{itemId}", firstItem)
                .with(customerBearer)
                .contentType("application/json")
                .content("{\"quantity\":1}"))
        .andExpect(status().isOk());
    runTogether(
        () -> {
          putItem(customerBearer, secondItem);
          return null;
        },
        () -> {
          putItem(customerBearer, otherRestaurantItem);
          return null;
        });

    Integer mixedRestaurants =
        jdbc.queryForObject(
            "SELECT count(DISTINCT restaurant_id) FROM cart_items WHERE cart_id = (SELECT id FROM carts WHERE customer_id = ?)",
            Integer.class,
            customer);
    assertThat(mixedRestaurants).isEqualTo(1);
  }

  private void putItem(RequestPostProcessor customerBearer, UUID item) throws Exception {
    mvc.perform(
            put("/api/cart/items/{itemId}", item)
                .with(customerBearer)
                .contentType("application/json")
                .content("{\"quantity\":1}"))
        .andExpect(status().isOk());
  }

  private void runTogether(Callable<Void> first, Callable<Void> second) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Callable<Void>> calls =
          List.of(
              () -> {
                barrier.await();
                return first.call();
              },
              () -> {
                barrier.await();
                return second.call();
              });
      List<Future<Void>> futures = new ArrayList<>();
      for (Callable<Void> call : calls) futures.add(executor.submit(call));
      for (Future<Void> future : futures) future.get();
    } finally {
      executor.shutdownNow();
    }
  }

  private UUID restaurant(String name, UUID owner, UUID city) {
    return jdbc.queryForObject(
        "INSERT INTO restaurants (name,owner_id,city_id,cost_for_two,diet_type,address_line_1,location) VALUES (?,?,?,?,?,'Road',ST_SetSRID(ST_MakePoint(73.8567,18.5204),4326)::geography) RETURNING id",
        UUID.class,
        name,
        owner,
        city,
        300,
        "Veg");
  }

  private UUID menuItem(UUID restaurant, String name, boolean available) {
    UUID category =
        jdbc.queryForObject(
            "INSERT INTO menu_categories (restaurant_id,name) VALUES (?, ?) RETURNING id",
            UUID.class,
            restaurant,
            name + " category");
    return jdbc.queryForObject(
        "INSERT INTO menu_items (restaurant_id,category_id,name,diet_type,price,is_available,available_quantity) VALUES (?,?,?,'Veg',100,?,5) RETURNING id",
        UUID.class,
        restaurant,
        category,
        name,
        available);
  }

  private int stock(UUID item) {
    return jdbc.queryForObject(
        "SELECT available_quantity FROM menu_items WHERE id = ?", Integer.class, item);
  }

  private UUID customer(String username, String email, String phone) {
    return customer(username, email, phone, "customer");
  }

  private UUID customer(String username, String email, String phone, String role) {
    UUID id =
        jdbc.queryForObject(
            "INSERT INTO users (name,email,phone_number,role) VALUES (?,?,?,?) RETURNING id",
            UUID.class,
            username,
            email,
            phone,
            role);
    jdbc.update(
        "INSERT INTO user_credentials (user_id,username,password_hash) VALUES (?,?,?)",
        id,
        username,
        passwords.encode("secret"));
    return id;
  }
}
