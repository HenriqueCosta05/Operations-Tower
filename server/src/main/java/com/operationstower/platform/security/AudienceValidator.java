package com.operationstower.platform.security;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidator implements OAuth2TokenValidator<Jwt> {

  private final String expectedAudience;

  AudienceValidator(String expectedAudience) {
    this.expectedAudience = expectedAudience;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    List<String> audience = jwt.getAudience();
    if (audience != null && audience.contains(expectedAudience)) {
      return OAuth2TokenValidatorResult.success();
    }
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token", "Token was not issued for this application", null));
  }
}
