package com.rk.fooddelivery.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Constructor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiErrorContractTest.ContractProbeController.class)
class ApiErrorContractTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void invalidRequestReturnsBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/_contract/validate")
                .with(user("contract-test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.requestId", not(emptyOrNullString())))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be blank"));
    }

    @Test
    void domainConflictReturnsConflictWithoutSqlDetails() throws Exception {
        mockMvc.perform(get("/_contract/domain-conflict").with(user("contract-test")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value("Order already exists"))
            .andExpect(jsonPath("$.requestId", not(emptyOrNullString())))
            .andExpect(content().string(not(containsString("org.postgresql"))))
            .andExpect(content().string(not(containsString("SQL"))));
    }

    @RestController
    @RequestMapping("/_contract")
    static class ContractProbeController {
        @PostMapping("/validate")
        void validate(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/domain-conflict")
        void domainConflict() {
            throw instantiateDomainException();
        }
    }

    record ValidationRequest(@NotBlank String name) {
    }

    private static RuntimeException instantiateDomainException() {
        try {
            Class<?> type = Class.forName("com.rk.fooddelivery.common.error.DomainException");
            Constructor<?> constructor = type.getConstructor(String.class);
            return (RuntimeException) constructor.newInstance("Order already exists");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("DomainException is required for domain error responses", exception);
        }
    }
}
