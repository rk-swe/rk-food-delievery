package com.rk.fooddelivery.auth.controller;

import com.rk.fooddelivery.auth.dto.TokenRequest;
import com.rk.fooddelivery.auth.dto.TokenResponse;
import com.rk.fooddelivery.auth.service.AuthTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/tokens")
@Tag(name = "Authentication")
public class AuthTokenController {

  private final AuthTokenService tokens;

  public AuthTokenController(AuthTokenService tokens) {
    this.tokens = tokens;
  }

  @PostMapping
  @Operation(
      operationId = "issueAuthToken",
      summary = "Exchange credentials for an access token",
      security = {})
  @ApiResponse(responseCode = "200", description = "Token issued")
  public ResponseEntity<TokenResponse> issue(@Valid @RequestBody TokenRequest request) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens.issue(request));
  }
}
