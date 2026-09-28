package com.rk.fooddelivery.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

  public static final String ISSUER = "fooddelivery";
  public static final String AUDIENCE = "fooddelivery-api";
  public static final Duration LIFETIME = Duration.ofDays(2);

  @Bean
  JwtEncoder jwtEncoder(@Value("${JWT_SECRET}") String encodedSecret) {
    return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey(encodedSecret)));
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${JWT_SECRET}") String encodedSecret, Clock clock) {
    JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
    timestampValidator.setClock(clock);
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(secretKey(encodedSecret))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    decoder.setJwtValidator(new RequiredClaimsValidator(clock, timestampValidator));
    return decoder;
  }

  public static byte[] decodeSecret(String encodedSecret) {
    if (encodedSecret == null || encodedSecret.isBlank()) {
      throw new IllegalStateException("JWT_SECRET must be configured");
    }
    final byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(encodedSecret);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("JWT_SECRET must be valid base64", exception);
    }
    if (decoded.length < 32) {
      throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
    }
    return decoded;
  }

  private SecretKey secretKey(String encodedSecret) {
    return new SecretKeySpec(decodeSecret(encodedSecret), "HmacSHA256");
  }

  private static final class RequiredClaimsValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error("invalid_token");

    private final Clock clock;
    private final JwtTimestampValidator timestamps;

    private RequiredClaimsValidator(Clock clock, JwtTimestampValidator timestamps) {
      this.clock = clock;
      this.timestamps = timestamps;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
      OAuth2TokenValidatorResult timestampResult = timestamps.validate(jwt);
      if (timestampResult.hasErrors()
          || !ISSUER.equals(jwt.getClaimAsString("iss"))
          || !jwt.getAudience().contains(AUDIENCE)
          || !validSubject(jwt.getSubject())
          || !validTimes(jwt.getIssuedAt(), jwt.getExpiresAt(), Instant.now(clock))) {
        return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
      }
      return OAuth2TokenValidatorResult.success();
    }

    private boolean validSubject(String subject) {
      try {
        UUID.fromString(subject);
        return true;
      } catch (IllegalArgumentException | NullPointerException exception) {
        return false;
      }
    }

    private boolean validTimes(Instant issuedAt, Instant expiresAt, Instant now) {
      return issuedAt != null
          && expiresAt != null
          && !issuedAt.isAfter(now)
          && expiresAt.isAfter(issuedAt)
          && expiresAt.isAfter(now);
    }
  }
}
