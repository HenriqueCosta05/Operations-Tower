package com.operationstower.users.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserId;

@JsonIgnoreProperties(ignoreUnknown = true)
record AuthentikUserDto(
    long pk,
    String username,
    String name,
    String email,
    @JsonProperty("is_active") boolean active) {

  User toDomain() {
    return new User(new UserId(pk), username, name, email, active);
  }
}
