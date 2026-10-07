package com.operationstower.users.domain;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(UserId id) {
    super("User not found: " + id.value());
  }
}
