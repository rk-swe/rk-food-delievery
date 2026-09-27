package com.rk.fooddelivery.restaurant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class RestaurantDiscoveryRegressionTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;
  UUID city, otherCity, owner, customer, a, b, c, closed, hidden, cuisine1, cuisine2;

  @BeforeEach
  void fixtures() {
    owner = user("owner", "restaurant_owner");
    customer = user("customer", "customer");
    UUID other = user("other", "restaurant_owner");
    user("admin", "admin");
    user("partner", "delivery_partner");
    city = city("Mumbai", true);
    otherCity = city("Pune", true);
    UUID inactive = city("Closed", false);
    a = restaurant("Same", owner, city, 100, 2, 72.8777, true);
    b = restaurant("Same", owner, city, 300, 5, 72.8877, true);
    c = restaurant("Other", other, otherCity, 500, 3, 73.8777, true);
    closed = restaurant("Closed", owner, city, 400, 0, 72.8777, false);
    hidden = restaurant("Hidden", other, inactive, 200, 0, 72.8777, true);
    cuisine1 =
        jdbc.queryForObject("insert into cuisines(name) values('Indian') returning id", UUID.class);
    cuisine2 =
        jdbc.queryForObject(
            "insert into cuisines(name) values('Punjabi') returning id", UUID.class);
    jdbc.update(
        "insert into restaurant_cuisines(restaurant_id,cuisine_id) values(?,?),(?,?),(?,?)",
        a,
        cuisine1,
        a,
        cuisine2,
        b,
        cuisine2);
  }

  @Test
  void roleVisibilityAppliesBeforeBothPaginationAndCount() throws Exception {
    for (String role : new String[] {"customer", "partner", "owner", "admin"}) {
      int expected = role.equals("admin") ? 5 : 3;
      mvc.perform(get("/api/restaurants").with(bearer(role, "secret")).param("size", "1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(expected));
    }
    mvc.perform(get("/api/restaurants").with(bearer("owner", "secret")).param("name", "Closed"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(closed.toString()));
    mvc.perform(get("/api/restaurants").with(bearer("owner", "secret")).param("name", "Other"))
        .andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(get("/api/restaurants").with(bearer("admin", "secret")).param("name", "Hidden"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void uuidCuisineMatchAnyCityAndCostFiltersDoNotDuplicateRows() throws Exception {
    mvc.perform(
            get("/api/restaurants")
                .with(bearer("customer", "secret"))
                .param("cityId", city.toString())
                .param("cuisineIds", cuisine1.toString(), cuisine2.toString(), cuisine1.toString())
                .param("minCostForTwo", "200")
                .param("maxCostForTwo", "400"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(b.toString()));
    mvc.perform(
            get("/api/restaurants")
                .with(bearer("customer", "secret"))
                .param("cuisineIds", cuisine1.toString(), cuisine2.toString()))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content.length()").value(2));
    mvc.perform(
            get("/api/restaurants")
                .with(bearer("customer", "secret"))
                .param("cityId", city.toString())
                .param("cuisineIds", cuisine1.toString(), cuisine2.toString())
                .param("minCostForTwo", "200")
                .param("sort", "distance")
                .param("latitude", "19.076")
                .param("longitude", "72.8777")
                .param("radiusMeters", "2000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(b.toString()));
  }

  @Test
  void sortAllowlistAndStablePagination() throws Exception {
    mvc.perform(get("/api/restaurants").with(bearer("customer", "secret")).param("sort", "rating"))
        .andExpect(jsonPath("$.content[0].id").value(b.toString()));
    mvc.perform(
            get("/api/restaurants").with(bearer("customer", "secret")).param("sort", "cost,desc"))
        .andExpect(jsonPath("$.content[0].id").value(c.toString()));
    mvc.perform(
            get("/api/restaurants").with(bearer("customer", "secret")).param("sort", "cost,asc"))
        .andExpect(jsonPath("$.content[0].id").value(a.toString()));
    UUID first = a.toString().compareTo(b.toString()) < 0 ? a : b, second = first.equals(a) ? b : a;
    for (int page = 0; page < 2; page++)
      mvc.perform(
              get("/api/restaurants")
                  .with(bearer("customer", "secret"))
                  .param("name", "Same")
                  .param("size", "1")
                  .param("page", "" + page))
          .andExpect(jsonPath("$.totalElements").value(2))
          .andExpect(jsonPath("$.content[0].id").value((page == 0 ? first : second).toString()));
    for (String sort : new String[] {"ownerId", "name;drop table restaurants", "cost,sideways"})
      mvc.perform(get("/api/restaurants").with(bearer("customer", "secret")).param("sort", sort))
          .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/restaurants")
                .with(bearer("customer", "secret"))
                .param("minCostForTwo", "500")
                .param("maxCostForTwo", "100"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void distanceSortStoredLocationRadiusBoundaryAndVisibility() throws Exception {
    mvc.perform(
            get("/api/restaurants").with(bearer("customer", "secret")).param("sort", "distance"))
        .andExpect(status().isBadRequest());
    jdbc.update(
        "update users set location=ST_SetSRID(ST_MakePoint(72.8777,19.076),4326)::geography where id=?",
        customer);
    mvc.perform(
            get("/api/restaurants").with(bearer("customer", "secret")).param("sort", "distance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(a.toString()))
        .andExpect(jsonPath("$.content[2].id").value(c.toString()));
    mvc.perform(
            get("/api/restaurants").with(bearer("customer", "secret")).param("radiusMeters", "0"))
        .andExpect(jsonPath("$.totalElements").value(1));
    double distance =
        jdbc.queryForObject(
            "select ST_Distance(location,ST_SetSRID(ST_MakePoint(72.8777,19.076),4326)::geography) from restaurants where id=?",
            Double.class,
            b);
    for (double delta : new double[] {-0.1, 0.1})
      mvc.perform(
              get("/api/restaurants")
                  .with(bearer("customer", "secret"))
                  .param("radiusMeters", "" + (distance + delta)))
          .andExpect(jsonPath("$.totalElements").value(delta < 0 ? 1 : 2));
    mvc.perform(
            get("/api/restaurants")
                .with(bearer("owner", "secret"))
                .param("radiusMeters", "0")
                .param("latitude", "19.076")
                .param("longitude", "72.8777"))
        .andExpect(jsonPath("$.totalElements").value(2));
    mvc.perform(
            get("/api/restaurants")
                .with(bearer("admin", "secret"))
                .param("radiusMeters", "0")
                .param("latitude", "19.076")
                .param("longitude", "72.8777"))
        .andExpect(jsonPath("$.totalElements").value(3));
    mvc.perform(
            get("/api/restaurants")
                .with(bearer("customer", "secret"))
                .param("radiusMeters", "100")
                .param("latitude", "19.076"))
        .andExpect(status().isBadRequest());
  }

  UUID city(String name, boolean active) {
    return jdbc.queryForObject(
        "insert into cities(name,state,country,currency,active) values(?,'Maharashtra','India','INR',?) returning id",
        UUID.class,
        name,
        active);
  }

  UUID restaurant(
      String name, UUID owner, UUID city, int cost, int rating, double longitude, boolean active) {
    return jdbc.queryForObject(
        "insert into restaurants(name,owner_id,city_id,cost_for_two,average_rating,diet_type,address_line_1,location,active) values(?,?,?,?,?,'Veg','Road',ST_SetSRID(ST_MakePoint(?,19.076),4326)::geography,?) returning id",
        UUID.class,
        name,
        owner,
        city,
        cost,
        rating,
        longitude,
        active);
  }

  UUID user(String name, String role) {
    UUID id =
        jdbc.queryForObject(
            "insert into users(name,email,phone_number,role) values(?,?,?,?) returning id",
            UUID.class,
            name,
            name + "@test",
            "+9199" + Math.abs(name.hashCode()),
            role);
    jdbc.update(
        "insert into user_credentials(user_id,username,password_hash) values(?,?,?)",
        id,
        name,
        passwords.encode("secret"));
    return id;
  }
}
