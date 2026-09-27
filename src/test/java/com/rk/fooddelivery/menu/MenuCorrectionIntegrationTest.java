package com.rk.fooddelivery.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    mvc.perform(
            delete("/api/restaurants/{r}/menu-categories/{c}", inactiveRestaurant, category)
                .with(bearer("owner", "secret")))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/menu-items/{id}", item).with(bearer("owner", "secret")))
        .andExpect(status().isNoContent());
    mvc.perform(
            delete(
                    "/api/restaurants/{restaurantId}/menu-categories/{id}",
                    inactiveRestaurant,
                    category)
                .with(bearer("owner", "secret")))
        .andExpect(status().isNoContent());
    assertThat(
            jdbc.queryForObject("select count(*) from menu_items where id=?", Integer.class, item))
        .isEqualTo(1);
    mvc.perform(
            get("/api/restaurants/{r}/menu-categories", inactiveRestaurant)
                .with(bearer("owner", "secret")))
        .andExpect(jsonPath("$.content[0].active").value(false));
    jdbc.update("update restaurants set active=true where id=?", inactiveRestaurant);
    mvc.perform(
            get("/api/restaurants/{r}/menu-categories", inactiveRestaurant)
                .with(bearer("customer", "secret")))
        .andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(
            patch("/api/menu-items/{id}", item)
                .with(bearer("owner", "secret"))
                .contentType("application/json")
                .content("{\"available\":true}"))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/restaurants/{r}/menu-items", inactiveRestaurant)
                .with(bearer("owner", "secret"))
                .contentType("application/json")
                .content(
                    "{\"categoryId\":\""
                        + category
                        + "\",\"name\":\"Blocked\",\"dietType\":\"Veg\",\"price\":10,\"availableQuantity\":1}"))
        .andExpect(status().isConflict());
  }

  @Test
  void incompleteSpatialCoordinatesAreBadRequest() throws Exception {
    mvc.perform(get("/api/restaurants").with(bearer("customer", "secret")).param("latitude", "9.9"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void duplicateCategoryCreateAndRenameReturnConflict() throws Exception {
    mvc.perform(
            post("/api/restaurants/{id}/menu-categories", inactiveRestaurant)
                .with(bearer("owner", "secret"))
                .contentType("application/json")
                .content("{\"name\":\"mains\",\"sortOrder\":0}"))
        .andExpect(status().isConflict());
    UUID other =
        jdbc.queryForObject(
            "insert into menu_categories(restaurant_id,name) values(?,'Other') returning id",
            UUID.class,
            inactiveRestaurant);
    mvc.perform(
            patch("/api/restaurants/{r}/menu-categories/{c}", inactiveRestaurant, other)
                .with(bearer("owner", "secret"))
                .contentType("application/json")
                .content("{\"name\":\"MAINS\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void availabilityMovesAndRepeatedDeletionMaintainActiveCounts() throws Exception {
    UUID next =
        jdbc.queryForObject(
            "insert into menu_categories(restaurant_id,name) values(?,'Other') returning id",
            UUID.class,
            inactiveRestaurant);
    patchItem("{\"available\":false}");
    count(category, 0);
    patchItem("{\"categoryId\":\"" + next + "\"}");
    count(category, 0);
    count(next, 0);
    patchItem("{\"available\":true}");
    count(next, 1);
    patchItem("{\"categoryId\":\"" + category + "\",\"available\":false}");
    count(next, 0);
    count(category, 0);
    mvc.perform(delete("/api/menu-items/{id}", item).with(bearer("owner", "secret")))
        .andExpect(status().isNoContent());
    count(category, 0);
  }

  @Test
  void crossOwnerCannotReadOrMutateMenuAndConcurrentStockDeltasAreNotLost() throws Exception {
    user("other", "restaurant_owner");
    jdbc.update("update restaurants set active=true where id=?", inactiveRestaurant);
    mvc.perform(
            get("/api/restaurants/{id}/menu-items", inactiveRestaurant)
                .with(bearer("other", "secret")))
        .andExpect(status().isNotFound());
    mvc.perform(
            patch("/api/menu-items/{id}", item)
                .with(bearer("other", "secret"))
                .contentType("application/json")
                .content("{\"name\":\"Stolen\"}"))
        .andExpect(status().isNotFound());
    var token = bearer("owner", "secret");
    var start = new java.util.concurrent.CyclicBarrier(2);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> decrement =
          () -> {
            start.await(5, java.util.concurrent.TimeUnit.SECONDS);
            return mvc.perform(
                    post("/api/menu-items/{id}/stock-adjustments", item)
                        .with(token)
                        .contentType("application/json")
                        .content("{\"delta\":-1}"))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var one = pool.submit(decrement);
      var two = pool.submit(decrement);
      assertThat(
              java.util.List.of(
                  one.get(10, java.util.concurrent.TimeUnit.SECONDS),
                  two.get(10, java.util.concurrent.TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
    }
    assertThat(
            jdbc.queryForObject(
                "select available_quantity from menu_items where id=?", Integer.class, item))
        .isZero();
  }

  void patchItem(String body) throws Exception {
    mvc.perform(
            patch("/api/menu-items/{id}", item)
                .with(bearer("owner", "secret"))
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk());
  }

  void count(UUID id, int expected) {
    assertThat(
            jdbc.queryForObject(
                "select item_count from menu_categories where id=?", Integer.class, id))
        .isEqualTo(expected);
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
