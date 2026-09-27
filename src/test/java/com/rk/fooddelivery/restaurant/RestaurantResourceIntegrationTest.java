package com.rk.fooddelivery.restaurant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class RestaurantResourceIntegrationTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;

  UUID admin;
  UUID owner;
  UUID city;

  @BeforeEach
  void fixtures() {
    admin = user("Admin", "admin@test", "+919000000001", "admin", "admin");
    owner = user("Owner", "owner@test", "+919000000002", "restaurant_owner", "owner");
    city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Hyderabad','Telangana','India','INR') RETURNING id",
            UUID.class);
  }

  @Test
  void administratorCreatesRestaurantAtResourceRouteWithLocation() throws Exception {
    String body =
        "{\"name\":\"Dosa House\",\"ownerId\":\""
            + owner
            + "\",\"cityId\":\""
            + city
            + "\",\"costForTwo\":250.00,\"dietType\":\"Veg\",\"addressLine1\":\"Road 1\",\"latitude\":17.385,\"longitude\":78.486}";

    mvc.perform(
            post("/api/restaurants")
                .with(httpBasic("admin", "secret"))
                .contentType("application/json")
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/restaurants/[0-9a-f-]+$")));
  }

  @Test
  void ownerSeesOwnRestaurantAndHoursLastDuplicateDayWins() throws Exception {
    String body =
        "{\"name\":\"Dosa House\",\"ownerId\":\"" + owner + "\",\"cityId\":\"" + city
            + "\",\"costForTwo\":250.00,\"dietType\":\"Veg\",\"addressLine1\":\"Road 1\",\"latitude\":17.385,\"longitude\":78.486}";
    String response = mvc.perform(post("/api/restaurants").with(httpBasic("admin", "secret")).contentType("application/json").content(body)).andReturn().getResponse().getContentAsString();
    String id = response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1");
    mvc.perform(get("/api/me/restaurants").with(httpBasic("owner", "secret")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id));
    mvc.perform(patch("/api/restaurants/" + id + "/hours").with(httpBasic("owner", "secret")).contentType("application/json").content("{\"hours\":[{\"day\":\"Monday\",\"open\":true,\"startTime\":\"09:00\",\"endTime\":\"10:00\"},{\"day\":\"Monday\",\"open\":false}]}"))
        .andExpect(status().isOk());
    Integer count = jdbc.queryForObject("select count(*) from restaurant_timings where restaurant_id=? and day='Monday' and is_open=false and start_time is null", Integer.class, UUID.fromString(id));
    org.junit.jupiter.api.Assertions.assertEquals(1, count);
  }

  private UUID user(String name, String email, String phone, String role, String login) {
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
        login,
        passwords.encode("secret"));
    return id;
  }
}
