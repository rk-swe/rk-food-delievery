package com.rk.fooddelivery.menu;

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
class MenuSearchIntegrationTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;
  private UUID restaurant;

  @BeforeEach
  void fixtures() {
    UUID owner = user("Owner", "owner@menu.test", "+919300000001", "restaurant_owner", "owner");
    UUID customer = user("Customer", "customer@menu.test", "+919300000002", "customer", "customer");
    UUID city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Delhi','Delhi','India','INR') RETURNING id",
            UUID.class);
    restaurant =
        jdbc.queryForObject(
            "INSERT INTO restaurants (name,owner_id,city_id,cost_for_two,diet_type,address_line_1,location) VALUES ('Filter House',?,?,200,'Veg','Road',ST_SetSRID(ST_MakePoint(77.2,28.6),4326)::geography) RETURNING id",
            UUID.class,
            owner,
            city);
    UUID category =
        jdbc.queryForObject(
            "INSERT INTO menu_categories (restaurant_id,name) VALUES (?, 'Mains') RETURNING id",
            UUID.class,
            restaurant);
    jdbc.update(
        "INSERT INTO menu_items (restaurant_id,category_id,name,diet_type,price,is_available,available_quantity) VALUES (?,?, 'Veg Thali','Veg',180,true,2),(?,?, 'Hidden Chicken','Non Veg',220,false,4)",
        restaurant,
        category,
        restaurant,
        category);
  }

  @Test
  void catalogSearchFiltersAvailableItemsByNameDietAndPrice() throws Exception {
    mvc.perform(
            get("/api/restaurants/{id}/menu-items", restaurant)
                .with(bearer("customer", "secret"))
                .param("name", "thali")
                .param("dietType", "Veg")
                .param("maxPrice", "200"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Veg Thali"));
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
