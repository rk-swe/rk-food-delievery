package com.rk.fooddelivery.user.controller;

import com.rk.fooddelivery.auth.AuthenticatedUser;
import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.user.dto.MeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Account")
public class MeController {

  private final CurrentUser currentUser;

  public MeController(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @GetMapping
  @Operation(
      operationId = "getMyAccount",
      summary = "Get the current account",
      description = "Available to every authenticated account.")
  MeResponse me() {
    AuthenticatedUser user = currentUser.require();
    return new MeResponse(
        user.id(), user.username(), user.name(), user.email(), user.role().databaseValue());
  }
}
