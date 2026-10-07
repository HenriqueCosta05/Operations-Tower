package com.operationstower.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDraft;
import com.operationstower.users.domain.UserId;
import com.operationstower.users.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;

class UserUseCasesTest {

  private final InMemoryUserDirectory directory = new InMemoryUserDirectory();

  @Test
  void createdUsersStartActiveAndAreListed() {
    User created =
        new CreateUserUseCase(directory)
            .execute(new UserDraft("ada", "Ada Lovelace", "ada@example.com"));

    assertThat(created.active()).isTrue();
    assertThat(new ListUsersUseCase(directory).execute()).containsExactly(created);
  }

  @Test
  void updateReplacesTheProfileOfAnExistingUser() {
    User ada = directory.existing("ada");

    User updated =
        new UpdateUserUseCase(directory)
            .execute(ada.id(), new UserDraft("ada", "Ada King", "ada@king.org"));

    assertThat(updated.name()).isEqualTo("Ada King");
    assertThat(new FindUserUseCase(directory).execute(ada.id()).email()).isEqualTo("ada@king.org");
  }

  @Test
  void deactivatedUsersAreKeptButInactive() {
    User ada = directory.existing("ada");

    new DeactivateUserUseCase(directory).execute(ada.id());

    assertThat(new ListUsersUseCase(directory).execute())
        .singleElement()
        .satisfies(user -> assertThat(user.active()).isFalse());
  }

  @Test
  void changingAnUnknownUserFails() {
    UserId unknown = new UserId(99);
    UserDraft draft = new UserDraft("x", "X", "x@example.com");

    assertThatThrownBy(() -> new UpdateUserUseCase(directory).execute(unknown, draft))
        .isInstanceOf(UserNotFoundException.class);
    assertThatThrownBy(() -> new DeactivateUserUseCase(directory).execute(unknown))
        .isInstanceOf(UserNotFoundException.class);
    assertThatThrownBy(() -> new FindUserUseCase(directory).execute(unknown))
        .isInstanceOf(UserNotFoundException.class);
  }
}
