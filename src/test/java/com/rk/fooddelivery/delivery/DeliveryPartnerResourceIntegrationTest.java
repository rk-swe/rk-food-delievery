package com.rk.fooddelivery.delivery;

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
class DeliveryPartnerResourceIntegrationTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;

  UUID admin;
  UUID partner;

  @BeforeEach
  void fixtures() {
    admin = user("Admin", "admin@example.test", "+919000000001", "admin", "admin");
    partner = user("Partner", "partner@example.test", "+919000000002", "delivery_partner", "partner");
    user("Owner", "owner@example.test", "+919000000003", "restaurant_owner", "owner");
    user("Customer", "customer@example.test", "+919000000004", "customer", "customer");
  }

  @Test
  void partnerDirectoryIsAdminOnly() throws Exception {
    for (String login : new String[] {"owner", "customer", "partner"}) {
      mvc.perform(get("/api/delivery-partners").with(httpBasic(login, "secret")))
          .andExpect(status().isForbidden());
    }
    mvc.perform(get("/api/delivery-partners")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/delivery-partners").with(httpBasic("admin", "secret")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(partner.toString()));
    mvc.perform(get("/api/delivery-partners/" + UUID.randomUUID()).with(httpBasic("admin", "secret")))
        .andExpect(status().isNotFound());

    mvc.perform(
            post("/api/delivery-partners")
                .with(httpBasic("admin", "secret"))
                .contentType("application/json")
                .content(partnerRequest("new-partner", "new@example.test", "+919000000005")))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
    mvc.perform(get("/api/cities").with(httpBasic("new-partner", "password123")))
        .andExpect(status().isOk());
  }

  @Test
  void duplicateUsernameRollsBackUserAndCredentials() throws Exception {
    int usersBefore = jdbc.queryForObject("SELECT count(*) FROM users", Integer.class);
    int credentialsBefore = jdbc.queryForObject("SELECT count(*) FROM user_credentials", Integer.class);

    mvc.perform(
            post("/api/delivery-partners")
                .with(httpBasic("admin", "secret"))
                .contentType("application/json")
                .content(partnerRequest("PARTNER", "different@example.test", "+919000000099")))
        .andExpect(status().isConflict());

    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT count(*) FROM users", Integer.class))
        .isEqualTo(usersBefore);
    org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject("SELECT count(*) FROM user_credentials", Integer.class))
        .isEqualTo(credentialsBefore);
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

  private String partnerRequest(String username, String email, String phone) {
    return "{\"name\":\"New Partner\",\"email\":\""
        + email
        + "\",\"phoneNumber\":\""
        + phone
        + "\",\"username\":\""
        + username
        + "\",\"password\":\"password123\"}";
  }
}
