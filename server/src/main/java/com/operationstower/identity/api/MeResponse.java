package com.operationstower.identity.api;

import com.operationstower.identity.domain.AuthenticatedUser;
import java.util.List;

record MeResponse(String subject, String name, String email, List<String> groups) {

  static MeResponse from(AuthenticatedUser user) {
    return new MeResponse(user.subject(), user.name(), user.email(), user.groups());
  }
}
