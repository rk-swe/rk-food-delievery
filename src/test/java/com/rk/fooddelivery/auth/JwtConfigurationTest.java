package com.rk.fooddelivery.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rk.fooddelivery.config.JwtConfig;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtConfigurationTest {

  @Test
  void rejectsMissingBlankMalformedAndShortSecretsWithoutEchoingThem() {
    String malformedSecret = "not-base64!";

    assertThatThrownBy(() -> JwtConfig.decodeSecret(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SECRET");
    assertThatThrownBy(() -> JwtConfig.decodeSecret("   "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SECRET");
    assertThatThrownBy(() -> JwtConfig.decodeSecret(malformedSecret))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SECRET")
        .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain(malformedSecret));
    assertThatThrownBy(
            () -> JwtConfig.decodeSecret(Base64.getEncoder().encodeToString(new byte[31])))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 32 bytes");
  }

  @Test
  void acceptsBase64EncodedThirtyTwoByteSecret() {
    byte[] secret = JwtConfig.decodeSecret(Base64.getEncoder().encodeToString(new byte[32]));

    assertThat(secret).hasSize(32);
  }
}
