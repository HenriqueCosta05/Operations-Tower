package com.operationstower.users.infrastructure;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDirectory;
import com.operationstower.users.domain.UserDirectoryUnavailableException;
import com.operationstower.users.domain.UserDraft;
import com.operationstower.users.domain.UserId;
import com.operationstower.users.domain.UserNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

class AuthentikUserDirectory implements UserDirectory {

  private static final String USERS = "/api/v3/core/users/";
  private static final String USER = "/api/v3/core/users/{id}/";
  private static final int PAGE_SIZE = 200;

  private final RestClient client;

  AuthentikUserDirectory(RestClient client) {
    this.client = client;
  }

  @Override
  public List<User> findAll() {
    return call(
        () ->
            client
                .get()
                .uri(USERS + "?page_size={size}&ordering=username", PAGE_SIZE)
                .retrieve()
                .body(AuthentikUserPage.class)
                .results()
                .stream()
                .map(AuthentikUserDto::toDomain)
                .toList());
  }

  @Override
  public Optional<User> findById(UserId id) {
    try {
      return Optional.of(call(() -> fetch(id)));
    } catch (UserDirectoryUnavailableException failure) {
      if (isNotFound(failure)) {
        return Optional.empty();
      }
      throw failure;
    }
  }

  @Override
  public User create(UserDraft draft) {
    return call(
        () ->
            client
                .post()
                .uri(USERS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(AuthentikUserWrite.creating(draft))
                .retrieve()
                .body(AuthentikUserDto.class)
                .toDomain());
  }

  @Override
  public User update(UserId id, UserDraft draft) {
    return patch(id, AuthentikUserWrite.changing(draft));
  }

  @Override
  public User deactivate(UserId id) {
    return patch(id, AuthentikUserWrite.deactivating());
  }

  private User fetch(UserId id) {
    return client.get().uri(USER, id.value()).retrieve().body(AuthentikUserDto.class).toDomain();
  }

  private User patch(UserId id, AuthentikUserWrite write) {
    return forUser(
        id,
        () ->
            client
                .patch()
                .uri(USER, id.value())
                .contentType(MediaType.APPLICATION_JSON)
                .body(write)
                .retrieve()
                .body(AuthentikUserDto.class)
                .toDomain());
  }

  private <T> T forUser(UserId id, Supplier<T> request) {
    try {
      return call(request);
    } catch (UserDirectoryUnavailableException failure) {
      if (isNotFound(failure)) {
        throw new UserNotFoundException(id);
      }
      throw failure;
    }
  }

  private static boolean isNotFound(UserDirectoryUnavailableException failure) {
    return failure.getCause() instanceof RestClientResponseException response
        && response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND);
  }

  private <T> T call(Supplier<T> request) {
    try {
      return request.get();
    } catch (RestClientException failure) {
      throw new UserDirectoryUnavailableException("User directory request failed", failure);
    }
  }
}
