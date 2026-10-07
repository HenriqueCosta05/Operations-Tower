package com.operationstower.users.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDirectoryUnavailableException;
import com.operationstower.users.domain.UserId;
import com.operationstower.users.domain.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AuthentikUserDirectoryTest {

  private static final String ADA =
      """
      {"pk":7,"username":"ada","name":"Ada","email":"ada@example.com","is_active":true,"uid":"x"}
      """;

  private MockRestServiceServer server;
  private AuthentikUserDirectory directory;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://authentik");
    server = MockRestServiceServer.bindTo(builder).build();
    directory = new AuthentikUserDirectory(builder.build());
  }

  @Test
  void mapsAuthentikUsersToDomainUsers() {
    server
        .expect(requestTo("http://authentik/api/v3/core/users/?page_size=200&ordering=username"))
        .andRespond(withSuccess("{\"results\":[" + ADA + "]}", MediaType.APPLICATION_JSON));

    assertThat(directory.findAll())
        .containsExactly(new User(new UserId(7), "ada", "Ada", "ada@example.com", true));
  }

  @Test
  void deactivatingPatchesOnlyTheActiveFlag() {
    server
        .expect(requestTo("http://authentik/api/v3/core/users/7/"))
        .andExpect(method(HttpMethod.PATCH))
        .andExpect(content().json("{\"is_active\":false}"))
        .andRespond(withSuccess(ADA.replace("true", "false"), MediaType.APPLICATION_JSON));

    assertThat(directory.deactivate(new UserId(7)).active()).isFalse();
  }

  @Test
  void aMissingUserIsEmptyNotAnError() {
    server
        .expect(requestTo("http://authentik/api/v3/core/users/9/"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThat(directory.findById(new UserId(9))).isEmpty();
  }

  @Test
  void patchingAMissingUserReportsThatUser() {
    server
        .expect(requestTo("http://authentik/api/v3/core/users/9/"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThatThrownBy(() -> directory.deactivate(new UserId(9)))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("9");
  }

  @Test
  void identityProviderFailuresAreReportedAsUnavailable() {
    server.expect(requestTo("http://authentik/api/v3/core/users/7/")).andRespond(withServerError());

    assertThatThrownBy(() -> directory.findById(new UserId(7)))
        .isInstanceOf(UserDirectoryUnavailableException.class);
  }
}
