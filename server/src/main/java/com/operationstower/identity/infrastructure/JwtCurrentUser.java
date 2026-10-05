package com.operationstower.identity.infrastructure;

import com.operationstower.identity.domain.AuthenticatedUser;
import com.operationstower.identity.domain.CurrentUser;
import com.operationstower.identity.domain.NotAuthenticatedException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
class JwtCurrentUser implements CurrentUser {

  @Override
  public AuthenticatedUser get() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new NotAuthenticatedException();
    }
    return new AuthenticatedUser(jwt.getSubject(), displayName(jwt), emailOf(jwt), groupsOf(jwt));
  }

  private static String displayName(Jwt jwt) {
    return Optional.ofNullable(jwt.getClaimAsString("name"))
        .or(() -> Optional.ofNullable(jwt.getClaimAsString("preferred_username")))
        .orElseGet(jwt::getSubject);
  }

  private static String emailOf(Jwt jwt) {
    return Optional.ofNullable(jwt.getClaimAsString("email")).orElse("");
  }

  private static List<String> groupsOf(Jwt jwt) {
    return Optional.ofNullable(jwt.getClaimAsStringList("groups")).orElse(List.of());
  }
}
