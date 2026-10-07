package com.operationstower.users.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDraft;
import org.junit.jupiter.api.Test;

class CreateUserUseCaseTest {

  private final InMemoryUserDirectory directory = new InMemoryUserDirectory();
  private final CreateUserUseCase createUser = new CreateUserUseCase(directory);

  @Test
  void createdUsersStartActiveWithTheDraftedProfile() {
    User created = createUser.execute(new UserDraft("ada", "Ada Lovelace", "ada@example.com"));

    assertThat(created)
        .extracting(User::username, User::name, User::email, User::active)
        .containsExactly("ada", "Ada Lovelace", "ada@example.com", true);
  }

  @Test
  void createdUsersAreStoredInTheDirectory() {
    User created = createUser.execute(new UserDraft("ada", "Ada Lovelace", "ada@example.com"));

    assertThat(directory.findById(created.id())).contains(created);
  }
}
