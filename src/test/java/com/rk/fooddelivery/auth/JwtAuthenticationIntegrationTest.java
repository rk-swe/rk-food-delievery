package com.rk.fooddelivery.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class JwtAuthenticationIntegrationTest extends IntegrationTestSupport {

  @Autowired MockMvc mockMvc;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired JwtDecoder jwtDecoder;

  private UUID customerId;

  @BeforeEach
  void fixture() {
    customerId =
        jdbc.queryForObject(
            """
            INSERT INTO users (name, email, phone_number, role, active)
            VALUES ('Customer', 'customer@example.test', '+919876543210', 'customer', true)
            RETURNING id
            """,
            UUID.class);
    jdbc.update(
        "INSERT INTO user_credentials (user_id, username, password_hash) VALUES (?, ?, ?)",
        customerId,
        "customer-login",
        passwordEncoder.encode("customer-password"));
  }

  @Test
  void validCredentialsIssueTwoDayJwt() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"customer-login\",\"password\":\"customer-password\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().doesNotExist("Location"))
            .andExpect(jsonPath("$.*", hasSize(3)))
            .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(172800))
            .andReturn();

    Jwt jwt = jwtDecoder.decode(accessToken(result));
    assertThat(jwt.getSubject()).isEqualTo(customerId.toString());
    assertThat(jwt.getClaimAsString("iss")).isEqualTo("fooddelivery");
    assertThat(jwt.getAudience()).containsExactly("fooddelivery-api");
    assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plusSeconds(172800));
    assertThat(jwt.getClaims())
        .doesNotContainKeys("name", "email", "username", "password", "hash", "role", "authority");
  }

  @Test
  void invalidCredentialExchangeUsesExistingGenericUnauthorizedEnvelope() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"customer-login\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.message").value("Authentication is required"));
  }

  @Test
  void bearerUsesCurrentDatabaseIdentity() throws Exception {
    String token = issueToken();
    mockMvc
        .perform(get("/api/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(customerId.toString()))
        .andExpect(jsonPath("$.role").value("customer"));

    jdbc.update("UPDATE users SET role = 'restaurant_owner' WHERE id = ?", customerId);

    mockMvc
        .perform(get("/api/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("restaurant_owner"));

    jdbc.update("UPDATE users SET active = false WHERE id = ?", customerId);
    mockMvc
        .perform(get("/api/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void basicOnlyProtectedRequestsAreRejected() throws Exception {
    mockMvc
        .perform(get("/api/me").with(httpBasic("customer-login", "customer-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(jsonPath("$.fieldErrors", empty()));
  }

  private String issueToken() throws Exception {
    return accessToken(
        mockMvc
            .perform(
                post("/api/auth/tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"customer-login\",\"password\":\"customer-password\"}"))
            .andExpect(status().isOk())
            .andReturn());
  }

  private String accessToken(MvcResult result) throws Exception {
    return new tools.jackson.databind.ObjectMapper()
        .readTree(result.getResponse().getContentAsString())
        .path("accessToken")
        .asString();
  }
}
