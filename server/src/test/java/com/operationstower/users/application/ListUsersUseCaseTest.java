package com.operationstower.users.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.operationstower.users.domain.User;
import org.junit.jupiter.api.Test;

class ListUsersUseCaseTest {

  private final InMemoryUserDirectory directory = new InMemoryUserDirectory();
  private final ListUsersUseCase listUsers = new ListUsersUseCase(directory);

  @Test
  void anEmptyDirectoryListsNoUsers() {
    assertThat(listUsers.execute()).isEmpty();
  }

  @Test
  void listsEveryUserInTheDirectory() {
    User ada = directory.existing("ada");
    User grace = directory.existing("grace");

    assertThat(listUsers.execute()).containsExactly(ada, grace);
  }

  @Test
  void deactivatedUsersAreStillListed() {
    User ada = directory.existing("ada");
    User deactivated = directory.deactivate(ada.id());

    assertThat(listUsers.execute()).containsExactly(deactivated);
  }
}
