package com.rk.fooddelivery.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/** Credentials accepted by the token endpoint. Email is the canonical login identifier. */
public record TokenRequest(
    @NotBlank @JsonAlias("username") String email, @NotBlank String password) {}
