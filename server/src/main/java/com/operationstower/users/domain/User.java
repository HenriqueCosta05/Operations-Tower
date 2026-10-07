package com.operationstower.users.domain;

public record User(UserId id, String username, String name, String email, boolean active) {

  public User {
    if (id == null) {
      throw new IllegalArgumentException("id must not be null");
    }
  }
}
