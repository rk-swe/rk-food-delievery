package com.rk.fooddelivery.menu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class MenuManagementIntegrationTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;

  private UUID owner;
  private UUID anotherOwner;
  private UUID restaurant;
  private UUID anotherRestaurant;

  @BeforeEach
  void fixtures() {
    owner = user("Owner", "owner@example.test", "+919100000001", "restaurant_owner", "owner");
    anotherOwner =
        user("Other owner", "other@example.test", "+919100000002", "restaurant_owner", "other");
    UUID city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Pune','Maharashtra','India','INR') RETURNING id",
            UUID.class);
    restaurant = restaurant("Aloo House", owner, city, 18.5204, 73.8567);
    anotherRestaurant = restaurant("Biryani House", anotherOwner, city, 18.5304, 73.8567);
  }

  @Test
  void ownerCreatesCatalogAndOnlyUsesOwnRestaurantCategories() throws Exception {
    String category =
        mvc.perform(
                post("/api/restaurants/{id}/menu-categories", restaurant)
                    .with(bearer("owner", "secret"))
                    .contentType("application/json")
                    .content("{\"name\":\"Mains\",\"sortOrder\":1}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String categoryId = category.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1");

    mvc.perform(
            post("/api/restaurants/{id}/menu-items", restaurant)
                .with(bearer("owner", "secret"))
                .contentType("application/json")
                .content(
                    "{\"categoryId\":\""
                        + categoryId
                        + "\",\"name\":\"Aloo Paratha\",\"dietType\":\"Veg\",\"price\":120.00,\"availableQuantity\":4}"))
        .andExpect(status().isCreated());

    mvc.perform(get("/api/restaurants/{id}/menu-categories", restaurant).with(bearer("owner", "secret")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].itemCount").value(1));

    mvc.perform(
            post("/api/restaurants/{id}/menu-items", anotherRestaurant)
                .with(bearer("other", "secret"))
                .contentType("application/json")
                .content(
                    "{\"categoryId\":\""
                        + categoryId
                        + "\",\"name\":\"Invalid\",\"dietType\":\"Veg\",\"price\":100.00,\"availableQuantity\":1}"))
        .andExpect(status().isConflict());
  }

  @Test
  void stockDeltaRequiresPositiveAmountAndCannotDropBelowZero() throws Exception {
    UUID category =
        jdbc.queryForObject(
            "INSERT INTO menu_categories (restaurant_id,name) VALUES (?, 'Mains') RETURNING id",
            UUID.class,
            restaurant);
    UUID item =
        jdbc.queryForObject(
            "INSERT INTO menu_items (restaurant_id,category_id,name,diet_type,price,available_quantity) VALUES (?,?, 'Thali','Veg',150,3) RETURNING id",
            UUID.class,
            restaurant,
            category);

    mvc.perform(
            post("/api/menu-items/{id}/stock-adjustments", item)
                .with(bearer("owner", "secret"))
                .contentType("application/json")
                .content("{\"delta\":0}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/menu-items/{id}/stock-adjustments", item)
                .with(bearer("owner", "secret"))
                .contentType("application/json")
                .content("{\"delta\":-4}"))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/menu-items/{id}/stock-adjustments", item)
                .with(bearer("owner", "secret"))
                .contentType("application/json")
                .content("{\"delta\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availableQuantity").value(5));
  }

  private UUID restaurant(String name, UUID ownerId, UUID cityId, double latitude, double longitude) {
    return jdbc.queryForObject(
        "INSERT INTO restaurants (name,owner_id,city_id,cost_for_two,diet_type,address_line_1,location) VALUES (?,?,?,?,?,'Road',ST_SetSRID(ST_MakePoint(?,?),4326)::geography) RETURNING id",
        UUID.class,
        name,
        ownerId,
        cityId,
        300,
        "Veg",
        longitude,
        latitude);
  }

  private UUID user(String name, String email, String phone, String role, String username) {
    UUID id =
        jdbc.queryForObject(
            "INSERT INTO users (name,email,phone_number,role) VALUES (?,?,?,?) RETURNING id",
            UUID.class,
            name,
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
