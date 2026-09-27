package com.rk.fooddelivery.user.dto;

import java.util.UUID;

public record MeResponse(UUID id, String username, String name, String email, String role) {}
