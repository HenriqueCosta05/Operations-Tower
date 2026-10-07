package com.operationstower.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDraft;
import com.operationstower.users.domain.UserId;
import com.operationstower.users.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;

class UpdateUserUseCaseTest {

  private final InMemoryUserDirectory directory = new InMemoryUserDirectory();
  private final UpdateUserUseCase updateUser = new UpdateUserUseCase(directory);

  @Test
  void updateReplacesTheProfileOfAnExistingUser() {
    User ada = directory.existing("ada");

    updateUser.execute(ada.id(), new UserDraft("ada.king", "Ada King", "ada@king.org"));

    assertThat(directory.findById(ada.id()))
        .contains(new User(ada.id(), "ada.king", "Ada King", "ada@king.org", true));
  }

  @Test
  void updateLeavesOtherUsersUntouched() {
    User ada = directory.existing("ada");
    User grace = directory.existing("grace");

    updateUser.execute(ada.id(), new UserDraft("ada.king", "Ada King", "ada@king.org"));

    assertThat(directory.findById(grace.id())).contains(grace);
  }

  @Test
  void updatingAnUnknownUserFails() {
    UserDraft draft = new UserDraft("x", "X", "x@example.com");

    assertThatThrownBy(() -> updateUser.execute(new UserId(99), draft))
        .isInstanceOf(UserNotFoundException.class);
  }
}
