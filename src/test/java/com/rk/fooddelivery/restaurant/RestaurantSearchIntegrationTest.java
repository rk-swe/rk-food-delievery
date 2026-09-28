package com.rk.fooddelivery.restaurant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class RestaurantSearchIntegrationTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;

  @BeforeEach
  void fixtures() {
    user("Customer", "customer@example.test", "+919200000001", "customer", "customer");
    UUID owner = user("Owner", "owner@example.test", "+919200000002", "restaurant_owner", "owner");
    UUID city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Mumbai','Maharashtra','India','INR') RETURNING id",
            UUID.class);
    UUID cuisine =
        jdbc.queryForObject(
            "INSERT INTO cuisines (name) VALUES ('Indian') RETURNING id", UUID.class);
    restaurant("Near Veg", owner, city, 19.0760, 72.8777, 200, "Veg", cuisine);
    restaurant("Far Non Veg", owner, city, 19.1760, 72.8777, 600, "Non Veg", cuisine);
  }

  @Test
  void publicSearchCombinesFiltersAndPostgisRadiusWithoutDuplicateCuisineRows() throws Exception {
    mvc.perform(
            get("/api/restaurants")
                .with(bearer("customer", "secret"))
                .param("name", "near")
                .param("cuisine", "Indian")
                .param("dietType", "Veg")
                .param("maxCostForTwo", "250")
                .param("latitude", "19.0760")
                .param("longitude", "72.8777")
                .param("radiusMeters", "1000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Near Veg"));
  }

  @Test
  void customerLocationIsUsedWhenRadiusIsSuppliedWithoutCoordinates() throws Exception {
    jdbc.update(
        "UPDATE users SET location=ST_SetSRID(ST_MakePoint(72.8777,19.0760),4326)::geography WHERE email='customer@example.test'");
    mvc.perform(
            get("/api/restaurants")
                .with(bearer("customer", "secret"))
                .param("radiusMeters", "1000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  private void restaurant(
      String name,
      UUID owner,
      UUID city,
      double latitude,
      double longitude,
      int cost,
      String diet,
      UUID cuisine) {
    UUID restaurant =
        jdbc.queryForObject(
            "INSERT INTO restaurants (name,owner_id,city_id,cost_for_two,diet_type,address_line_1,location) VALUES (?,?,?,?,?,'Road',ST_SetSRID(ST_MakePoint(?,?),4326)::geography) RETURNING id",
            UUID.class,
            name,
            owner,
            city,
            cost,
            diet,
            longitude,
            latitude);
    jdbc.update(
        "INSERT INTO restaurant_cuisines (restaurant_id,cuisine_id) VALUES (?,?)",
        restaurant,
        cuisine);
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
