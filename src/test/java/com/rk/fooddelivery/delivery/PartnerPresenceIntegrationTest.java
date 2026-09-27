package com.rk.fooddelivery.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PartnerPresenceIntegrationTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;

  UUID partner;
  UUID otherPartner;

  @BeforeEach
  void fixtures() {
    partner = user("Partner", "partner@example.test", "+919000000002", "delivery_partner", "partner");
    otherPartner = user("Other", "other@example.test", "+919000000003", "delivery_partner", "other");
    user("Owner", "owner@example.test", "+919000000004", "restaurant_owner", "owner");
  }

  @Test
  void presenceUsesPrincipalAndServerTimestamp() throws Exception {
    mvc.perform(
            patch("/api/me/delivery-partner/location")
                .with(httpBasic("partner", "secret"))
                .contentType("application/json")
                .content("{\"latitude\":12.9716,\"longitude\":77.5946}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(partner.toString()))
        .andExpect(jsonPath("$.locationUpdatedAt").exists());

    assertThat(jdbc.queryForObject("SELECT ST_X(location::geometry) FROM users WHERE id=?", Double.class, partner))
        .isEqualTo(77.5946);
    assertThat(jdbc.queryForObject("SELECT ST_Y(location::geometry) FROM users WHERE id=?", Double.class, partner))
        .isEqualTo(12.9716);
    Instant persisted = jdbc.queryForObject("SELECT location_updated_at FROM users WHERE id=?", Instant.class, partner);
    assertThat(persisted).isNotNull();
    assertThat(jdbc.queryForObject("SELECT location FROM users WHERE id=?", Object.class, otherPartner)).isNull();

    mvc.perform(
            patch("/api/me/delivery-partner/availability")
                .with(httpBasic("partner", "secret"))
                .contentType("application/json")
                .content("{\"online\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.online").value(true));
    mvc.perform(
            patch("/api/me/delivery-partner/availability")
                .with(httpBasic("owner", "secret"))
                .contentType("application/json")
                .content("{\"online\":true}"))
        .andExpect(status().isForbidden());
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
