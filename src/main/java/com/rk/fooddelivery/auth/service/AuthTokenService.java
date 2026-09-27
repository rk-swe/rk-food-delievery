package com.rk.fooddelivery.auth.service;

import com.rk.fooddelivery.auth.AuthenticatedUser;
import com.rk.fooddelivery.auth.dto.TokenRequest;
import com.rk.fooddelivery.auth.dto.TokenResponse;
import com.rk.fooddelivery.config.JwtConfig;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

  private final AuthenticationManager authenticationManager;
  private final JwtEncoder jwtEncoder;
  private final Clock clock;

  public AuthTokenService(
      AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, Clock clock) {
    this.authenticationManager = authenticationManager;
    this.jwtEncoder = jwtEncoder;
    this.clock = clock;
  }

  public TokenResponse issue(TokenRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.email(), request.password()));
    AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
    Instant issuedAt = Instant.now(clock);
    Instant expiresAt = issuedAt.plus(JwtConfig.LIFETIME);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.id().toString())
            .issuer(JwtConfig.ISSUER)
            .audience(List.of(JwtConfig.AUDIENCE))
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    String token =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new TokenResponse(token, "Bearer", JwtConfig.LIFETIME.toSeconds());
  }
}
