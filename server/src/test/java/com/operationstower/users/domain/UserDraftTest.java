package com.operationstower.users.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserDraftTest {

  @Test
  void trimsSurroundingWhitespace() {
    UserDraft draft = new UserDraft("  ada ", " Ada Lovelace ", " ada@example.com ");

    assertThat(draft).isEqualTo(new UserDraft("ada", "Ada Lovelace", "ada@example.com"));
  }

  @Test
  void rejectsBlankUsername() {
    assertThatThrownBy(() -> new UserDraft("  ", "Ada", "ada@example.com"))
        .isInstanceOf(InvalidUserException.class)
        .hasMessageContaining("username");
  }

  @Test
  void rejectsMalformedEmail() {
    assertThatThrownBy(() -> new UserDraft("ada", "Ada", "not-an-email"))
        .isInstanceOf(InvalidUserException.class)
        .hasMessageContaining("email");
  }
}
