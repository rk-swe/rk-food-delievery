package com.rk.fooddelivery.menu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class MenuCorrectionIntegrationTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;
  UUID owner, customer, inactiveRestaurant, category, item;

  @BeforeEach
  void setUp() {
    owner = user("owner", "restaurant_owner");
    customer = user("customer", "customer");
    UUID city =
        jdbc.queryForObject(
            "insert into cities(name,state,country,currency) values('Kochi','Kerala','India','INR') returning id",
            UUID.class);
    inactiveRestaurant =
        jdbc.queryForObject(
            "insert into restaurants(name,owner_id,city_id,cost_for_two,diet_type,address_line_1,location,active) values('Closed',?,?,100,'Veg','Road',ST_SetSRID(ST_MakePoint(76.2,9.9),4326)::geography,false) returning id",
            UUID.class,
            owner,
            city);
    category =
        jdbc.queryForObject(
            "insert into menu_categories(restaurant_id,name,item_count) values(?,'Mains',1) returning id",
            UUID.class,
            inactiveRestaurant);
    item =
        jdbc.queryForObject(
            "insert into menu_items(restaurant_id,category_id,name,diet_type,price,available_quantity) values(?,?, 'Dish','Veg',100,1) returning id",
            UUID.class,
            inactiveRestaurant,
            category);
  }

  @Test
  void customerCannotReadInactiveRestaurantMenuButOwnerCanAndItemDeleteUpdatesCount()
      throws Exception {
    mvc.perform(
            get("/api/restaurants/{id}/menu-items", inactiveRestaurant)
                .with(bearer("customer", "secret")))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/restaurants/{id}/menu-items", inactiveRestaurant)
                .with(bearer("owner", "secret")))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/menu-items/{id}", item).with(bearer("owner", "secret")))
        .andExpect(status().isNoContent());
    mvc.perform(
            delete(
                    "/api/restaurants/{restaurantId}/menu-categories/{id}",
                    inactiveRestaurant,
                    category)
                .with(bearer("owner", "secret")))
        .andExpect(status().isConflict());
  }

  @Test
  void incompleteSpatialCoordinatesAreBadRequest() throws Exception {
    mvc.perform(get("/api/restaurants").with(bearer("customer", "secret")).param("latitude", "9.9"))
        .andExpect(status().isBadRequest());
  }

  UUID user(String username, String role) {
    UUID id =
        jdbc.queryForObject(
            "insert into users(name,email,phone_number,role) values(?,?,?,?) returning id",
            UUID.class,
            username,
            username + "@test",
            "+9199" + Math.abs(username.hashCode()),
            role);
    jdbc.update(
        "insert into user_credentials(user_id,username,password_hash) values(?,?,?)",
        id,
        username,
        passwords.encode("secret"));
    return id;
  }
}
