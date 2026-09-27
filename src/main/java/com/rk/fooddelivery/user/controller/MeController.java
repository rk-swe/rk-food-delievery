package com.rk.fooddelivery.user.controller;

import com.rk.fooddelivery.auth.AuthenticatedUser;
import com.rk.fooddelivery.auth.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUser currentUser;

    public MeController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping
    MeResponse me() {
        AuthenticatedUser user = currentUser.require();
        return new MeResponse(user.id(), user.username(), user.name(), user.email(), user.role().databaseValue());
    }

    record MeResponse(UUID id, String username, String name, String email, String role) {
    }
}
