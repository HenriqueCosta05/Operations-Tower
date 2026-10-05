package com.operationstower.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.operationstower.identity.domain.AuthenticatedUser;
import com.operationstower.identity.domain.NotAuthenticatedException;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCurrentUserUseCaseTest {

  @Test
  void returnsTheUserOfTheCurrentRequest() {
    AuthenticatedUser ada =
        new AuthenticatedUser("u-1", "Ada", "ada@example.com", List.of("operators"));

    assertThat(new GetCurrentUserUseCase(() -> ada).execute()).isEqualTo(ada);
  }

  @Test
  void failsWhenNobodyIsAuthenticated() {
    GetCurrentUserUseCase useCase =
        new GetCurrentUserUseCase(
            () -> {
              throw new NotAuthenticatedException();
            });

    assertThatThrownBy(useCase::execute).isInstanceOf(NotAuthenticatedException.class);
  }
}
