package com.rk.fooddelivery.restaurant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CuisineResourceIntegrationTest extends IntegrationTestSupport {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;

  @BeforeEach
  void fixtures() {
    var id =
        jdbc.queryForObject(
            "INSERT INTO users (name,email,phone_number,role) VALUES ('Customer','c@test','+919000000020','customer') RETURNING id",
            java.util.UUID.class);
    jdbc.update(
        "INSERT INTO user_credentials (user_id,username,password_hash) VALUES (?,?,?)",
        id,
        "customer",
        passwords.encode("secret"));
    jdbc.update("INSERT INTO cuisines (name,image_url) VALUES ('Zest','z'),('Asian','a')");
  }

  @Test
  void cuisinesUseOrderedDtos() throws Exception {
    mvc.perform(get("/api/cuisines")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/cuisines?size=1").with(httpBasic("customer", "secret")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Asian"))
        .andExpect(jsonPath("$[0].imageUrl").value("a"));
  }
}
