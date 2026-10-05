package com.operationstower.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class GroupAuthoritiesConverterTest {

  private final GroupAuthoritiesConverter converter = new GroupAuthoritiesConverter();

  @Test
  void turnsEachAuthentikGroupIntoAnUppercaseRole() {
    Jwt jwt = tokenWith("groups", List.of("operators", "Site Admins"));

    assertThat(converter.convert(jwt))
        .extracting(Object::toString)
        .containsExactly("ROLE_OPERATORS", "ROLE_SITE_ADMINS");
  }

  @Test
  void grantsNothingWhenTheTokenCarriesNoGroups() {
    assertThat(converter.convert(tokenWith("sub", "user-1"))).isEmpty();
  }

  private static Jwt tokenWith(String claim, Object value) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("sub", "user-1")
        .claim(claim, value)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
  }
}
