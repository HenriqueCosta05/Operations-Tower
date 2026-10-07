package com.operationstower.users.domain;

import java.util.regex.Pattern;

public record UserDraft(String username, String name, String email) {

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  public UserDraft {
    username = requireText(username, "username");
    name = requireText(name, "name");
    email = requireText(email, "email");
    if (!EMAIL.matcher(email).matches()) {
      throw new InvalidUserException("email is not a valid address: " + email);
    }
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new InvalidUserException(field + " must not be blank");
    }
    return value.trim();
  }
}
