package com.operationstower.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

  private final AudienceValidator validator = new AudienceValidator("operations-tower");

  @Test
  void acceptsATokenIssuedForThisApplication() {
    assertThat(validator.validate(tokenFor(List.of("operations-tower"))).hasErrors()).isFalse();
  }

  @Test
  void rejectsATokenIssuedForAnotherApplicationOfTheSameIdentityProvider() {
    assertThat(validator.validate(tokenFor(List.of("some-other-app"))).hasErrors()).isTrue();
  }

  @Test
  void rejectsATokenWithoutAnAudience() {
    Jwt noAudience = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "u").build();

    assertThat(validator.validate(noAudience).hasErrors()).isTrue();
  }

  private static Jwt tokenFor(List<String> audience) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .audience(audience)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
  }
}
