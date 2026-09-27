package com.rk.fooddelivery.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AdminCrudIntegrationTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;
  UUID admin, owner, otherOwner, partner, city, restaurant;

  @BeforeEach
  void fixtures() {
    admin = user("Admin", "admin@test", "+919000000001", "admin", "admin", "secret");
    owner = user("Owner", "owner@test", "+919000000002", "restaurant_owner", "owner", "secret");
    otherOwner =
        user(
            "Other Owner",
            "other-owner@test",
            "+919000000004",
            "restaurant_owner",
            "other-owner",
            "secret");
    partner =
        user("Partner", "partner@test", "+919000000003", "delivery_partner", "partner", "secret");
  }

  @Test
  void adminsManageCatalogAndPartnerPresenceIsSelfOnly() throws Exception {
    String cityBody =
        "{\"name\":\"Hyderabad\",\"state\":\"Telangana\",\"country\":\"India\",\"currency\":\"INR\"}";
    String cityResult =
        mvc.perform(
                post("/api/cities")
                    .with(httpBasic("admin", "secret"))
                    .contentType("application/json")
                    .content(cityBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Hyderabad"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    city = UUID.fromString(cityResult.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1"));
    mvc.perform(
            post("/api/cities")
                .with(httpBasic("admin", "secret"))
                .contentType("application/json")
                .content(cityBody))
        .andExpect(status().isConflict());
    String restaurantBody =
        "{\"name\":\"Dosa House\",\"ownerId\":\""
            + owner
            + "\",\"cityId\":\""
            + city
            + "\",\"costForTwo\":250.00,\"dietType\":\"Veg\",\"addressLine1\":\"Road 1\",\"latitude\":17.385,\"longitude\":78.486}";
    String result =
        mvc.perform(
                post("/api/admin/restaurants")
                    .with(httpBasic("admin", "secret"))
                    .contentType("application/json")
                    .content(restaurantBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    restaurant = UUID.fromString(result.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1"));
    mvc.perform(get("/api/admin/restaurants").with(httpBasic("admin", "secret")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(restaurant.toString()));
    mvc.perform(
            patch("/api/admin/restaurants/" + restaurant)
                .with(httpBasic("admin", "secret"))
                .contentType("application/json")
                .content("{\"name\":\"Renamed Dosa House\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Renamed Dosa House"));
    mvc.perform(
            post("/api/admin/restaurants")
                .with(httpBasic("admin", "secret"))
                .contentType("application/json")
                .content(restaurantBody.replace(owner.toString(), partner.toString())))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/restaurants/mine").with(httpBasic("owner", "secret")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(restaurant.toString()));
    mvc.perform(
            patch("/api/restaurants/" + restaurant + "/hours")
                .with(httpBasic("owner", "secret"))
                .contentType("application/json")
                .content(
                    "{\"hours\":[{\"day\":\"Monday\",\"open\":true,\"startTime\":\"20:00\",\"endTime\":\"02:00\"}]}"))
        .andExpect(status().isOk());
    mvc.perform(
            patch("/api/delivery-partners/me/availability")
                .with(httpBasic("partner", "secret"))
                .contentType("application/json")
                .content("{\"online\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.online").value(true));
    mvc.perform(
            patch("/api/delivery-partners/me/location")
                .with(httpBasic("partner", "secret"))
                .contentType("application/json")
                .content("{\"latitude\":17.38,\"longitude\":78.48}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.locationUpdatedAt").exists());
  }

  @Test
  void validatesCoordinatesAndProtectsAdminResourcesAndHistory() throws Exception {
    mvc.perform(
            post("/api/cities")
                .with(httpBasic("owner", "secret"))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/cities")
                .with(httpBasic("admin", "secret"))
                .contentType("application/json")
                .content("{\"name\":\"Missing fields\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors").isArray());
    mvc.perform(
            post("/api/admin/restaurants")
                .with(httpBasic("admin", "secret"))
                .contentType("application/json")
                .content(
                    "{\"name\":\"x\",\"ownerId\":\""
                        + owner
                        + "\",\"cityId\":\""
                        + UUID.randomUUID()
                        + "\",\"costForTwo\":1,\"dietType\":\"Veg\",\"addressLine1\":\"x\",\"latitude\":91,\"longitude\":1}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/cities").with(httpBasic("admin", "secret"))).andExpect(status().isOk());
    mvc.perform(
            delete("/api/admin/delivery-partners/" + partner).with(httpBasic("admin", "secret")))
        .andExpect(status().isNoContent());
  }

  @Test
  void cityDeletePreservesHistoryAndRejectsActiveRestaurants() throws Exception {
    UUID busy =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Busy','S','India','INR') RETURNING id",
            UUID.class);
    jdbc.update(
        "INSERT INTO restaurants (name,owner_id,cost_for_two,diet_type,address_line_1,city_id,location) VALUES ('R',?,1,'Veg','x',?,ST_GeogFromText('POINT(1 1)'))",
        owner,
        busy);
    mvc.perform(delete("/api/cities/" + busy).with(httpBasic("admin", "secret")))
        .andExpect(status().isConflict());
    UUID empty =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Empty','S','India','INR') RETURNING id",
            UUID.class);
    mvc.perform(delete("/api/cities/" + empty).with(httpBasic("admin", "secret")))
        .andExpect(status().isNoContent());
    mvc.perform(delete("/api/cities/" + empty).with(httpBasic("admin", "secret")))
        .andExpect(status().isNoContent());
    mvc.perform(delete("/api/cities/" + UUID.randomUUID()).with(httpBasic("admin", "secret")))
        .andExpect(status().isNotFound());
  }

  @Test
  void ownerHoursRejectMalformedOpenWindow() throws Exception {
    city =
        jdbc.queryForObject(
            "INSERT INTO cities (name,state,country,currency) VALUES ('Hours','S','India','INR') RETURNING id",
            UUID.class);
    restaurant =
        jdbc.queryForObject(
            "INSERT INTO restaurants (name,owner_id,cost_for_two,diet_type,address_line_1,city_id,location) VALUES ('Hours',?,1,'Veg','x',?,ST_GeogFromText('POINT(1 1)')) RETURNING id",
            UUID.class,
            owner,
            city);
    mvc.perform(
            patch("/api/restaurants/" + restaurant + "/hours")
                .with(httpBasic("owner", "secret"))
                .contentType("application/json")
                .content(
                    "{\"hours\":[{\"day\":\"Monday\",\"open\":true,\"startTime\":\"10:00\",\"endTime\":\"10:00\"}]}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/restaurants/" + restaurant + "/hours")
                .with(httpBasic("other-owner", "secret"))
                .contentType("application/json")
                .content("{\"hours\":[{\"day\":\"Monday\",\"open\":false}]}"))
        .andExpect(status().isNotFound());
  }

  private UUID user(
      String name, String email, String phone, String role, String login, String password) {
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
        passwords.encode(password));
    return id;
  }
}
