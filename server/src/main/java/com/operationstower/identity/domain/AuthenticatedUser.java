package com.operationstower.identity.domain;

import java.util.List;

public record AuthenticatedUser(String subject, String name, String email, List<String> groups) {

  public AuthenticatedUser {
    groups = List.copyOf(groups);
  }
}
