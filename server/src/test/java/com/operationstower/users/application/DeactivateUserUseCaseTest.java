package com.operationstower.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserId;
import com.operationstower.users.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;

class DeactivateUserUseCaseTest {

  private final InMemoryUserDirectory directory = new InMemoryUserDirectory();
  private final DeactivateUserUseCase deactivateUser = new DeactivateUserUseCase(directory);

  @Test
  void deactivatedUsersAreKeptButInactive() {
    User ada = directory.existing("ada");

    deactivateUser.execute(ada.id());

    assertThat(directory.findById(ada.id()))
        .contains(new User(ada.id(), ada.username(), ada.name(), ada.email(), false));
  }

  @Test
  void deactivatingReturnsTheInactiveUser() {
    User ada = directory.existing("ada");

    assertThat(deactivateUser.execute(ada.id()).active()).isFalse();
  }

  @Test
  void deactivatingAnUnknownUserFails() {
    assertThatThrownBy(() -> deactivateUser.execute(new UserId(99)))
        .isInstanceOf(UserNotFoundException.class);
  }
}
