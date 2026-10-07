package com.operationstower.users.api;

import com.operationstower.users.domain.UserDraft;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record UserRequest(
    @NotBlank String username, @NotBlank String name, @NotBlank @Email String email) {

  UserDraft toDraft() {
    return new UserDraft(username, name, email);
  }
}
