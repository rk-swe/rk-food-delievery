package com.rk.fooddelivery.city;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rk.fooddelivery.city.service.CityService;
import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CityResourceIntegrationTest extends IntegrationTestSupport {

  @Autowired MockMvc mvc;
  @Autowired CityService cities;
  @Autowired PasswordEncoder passwords;

  private UUID admin;

  @BeforeEach
  void fixtures() {
    admin = user("Admin", "admin@test", "+919000000001", "admin", "admin", "secret");
    user("Customer", "customer@test", "+919000000002", "customer", "customer", "secret");
  }

  @Test
  void cityRoleMatrixAndLocation() throws Exception {
    String body =
        "{\"name\":\"Hyderabad\",\"state\":\"Telangana\",\"country\":\"India\",\"currency\":\"INR\"}";
    String response =
        mvc.perform(
                post("/api/cities")
                    .with(httpBasic("admin", "secret"))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(
                header()
                    .string(
                        "Location",
                        org.hamcrest.Matchers.matchesPattern(".*/api/cities/[0-9a-f-]+")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1"));

    mvc.perform(get("/api/cities/" + id).with(httpBasic("customer", "secret")))
        .andExpect(status().isOk());
    mvc.perform(
            patch("/api/cities/" + id)
                .with(httpBasic("admin", "secret"))
                .contentType("application/json")
                .content("{\"name\":\"Secunderabad\"}"))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/cities/" + id).with(httpBasic("admin", "secret")))
        .andExpect(status().isNoContent());
    mvc.perform(
            post("/api/cities")
                .with(httpBasic("customer", "secret"))
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/cities")).andExpect(status().isUnauthorized());

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "customer",
                "secret",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> cities.deactivate(id))
        .isInstanceOf(AccessDeniedException.class);
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
