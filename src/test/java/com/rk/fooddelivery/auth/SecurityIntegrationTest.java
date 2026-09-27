package com.rk.fooddelivery.auth;

import com.rk.fooddelivery.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import(SecurityIntegrationTest.IdentityProbeController.class)
class SecurityIntegrationTest extends IntegrationTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID customerId;
    private UUID ownerId;

    @BeforeEach
    void fixtures() {
        customerId = user("Customer", "shared@example.test", "+919876543210", "customer", "customer-login", "customer-password", true);
        ownerId = user("Owner", "shared@example.test", "+919876543211", "restaurant_owner", "owner-login", "owner-password", true);
        user("Inactive", "inactive@example.test", "+919876543212", "customer", "inactive-login", "inactive-password", false);
    }

    @Test
    void missingCredentialsReturnContractualUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
            .andExpect(jsonPath("$.message").value("Authentication is required"))
            .andExpect(jsonPath("$.requestId", not(emptyOrNullString())))
            .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void invalidCredentialsAndLegacyUsersWithoutCredentialsAreUnauthorized() throws Exception {
        jdbc.update("""
            INSERT INTO users (name, email, phone_number, role)
            VALUES ('Legacy', 'legacy@example.test', '+919876543213', 'customer')
            """);

        mockMvc.perform(get("/api/me").with(httpBasic("customer-login", "wrong-password")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(get("/api/me").with(httpBasic("legacy@example.test", "any-password")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void basicCredentialsAuthenticateAndMeDerivesIdentityFromServerPrincipal() throws Exception {
        mockMvc.perform(get("/api/me").with(httpBasic("customer-login", "customer-password")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(customerId.toString()))
            .andExpect(jsonPath("$.email").value("shared@example.test"))
            .andExpect(jsonPath("$.role").value("customer"))
            .andExpect(jsonPath("$.username").value("customer-login"));
    }

    @Test
    void customerCannotAccessAdminPath() throws Exception {
        mockMvc.perform(get("/api/admin/cities").with(httpBasic("customer-login", "customer-password")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void callerSuppliedIdentityCannotOverrideAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/identity-probe")
                .with(httpBasic("customer-login", "customer-password"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"actorId\":\"" + ownerId + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actorId").value(customerId.toString()));
    }

    @Test
    void inactiveAccountIsDenied() throws Exception {
        mockMvc.perform(get("/api/me").with(httpBasic("inactive-login", "inactive-password")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void credentialsAreStoredAsBcryptHashesAndSameEmailCanHaveDifferentRoleLogins() {
        String storedHash = jdbc.queryForObject("SELECT password_hash FROM user_credentials WHERE username = 'customer-login'", String.class);
        assertThat(passwordEncoder.matches("customer-password", storedHash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_credentials", Integer.class)).isEqualTo(3);
    }

    private UUID user(String name, String email, String phone, String role, String username, String password, boolean active) {
        UUID id = jdbc.queryForObject("""
            INSERT INTO users (name, email, phone_number, role, active)
            VALUES (?, ?, ?, ?, ?) RETURNING id
            """, UUID.class, name, email, phone, role, active);
        jdbc.update("INSERT INTO user_credentials (user_id, username, password_hash) VALUES (?, ?, ?)",
            id, username, passwordEncoder.encode(password));
        return id;
    }

    @RestController
    @RequestMapping("/api/identity-probe")
    static class IdentityProbeController {
        private final CurrentUser currentUser;

        IdentityProbeController(CurrentUser currentUser) {
            this.currentUser = currentUser;
        }

        @PostMapping
        Map<String, UUID> probe(@RequestBody IdentityProbeRequest ignored) {
            return Map.of("actorId", currentUser.requireId());
        }
    }

    record IdentityProbeRequest(UUID actorId) {
    }
}
