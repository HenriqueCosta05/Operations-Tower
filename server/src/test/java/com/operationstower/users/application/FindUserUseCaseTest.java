package com.operationstower.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserId;
import com.operationstower.users.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;

class FindUserUseCaseTest {

  private final InMemoryUserDirectory directory = new InMemoryUserDirectory();
  private final FindUserUseCase findUser = new FindUserUseCase(directory);

  @Test
  void findsAnExistingUserById() {
    directory.existing("ada");
    User grace = directory.existing("grace");

    assertThat(findUser.execute(grace.id())).isEqualTo(grace);
  }

  @Test
  void findingAnUnknownUserFailsNamingTheMissingId() {
    assertThatThrownBy(() -> findUser.execute(new UserId(99)))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("User not found: 99");
  }
}
