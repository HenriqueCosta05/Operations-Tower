package com.operationstower.users.api;

import com.operationstower.users.domain.User;

record UserResponse(long id, String username, String name, String email, boolean active) {

  static UserResponse from(User user) {
    return new UserResponse(
        user.id().value(), user.username(), user.name(), user.email(), user.active());
  }
}
