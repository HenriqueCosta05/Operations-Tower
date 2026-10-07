package com.operationstower.users.application;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDirectory;
import com.operationstower.users.domain.UserDraft;
import com.operationstower.users.domain.UserId;
import com.operationstower.users.domain.UserNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class InMemoryUserDirectory implements UserDirectory {

  private final List<User> users = new ArrayList<>();
  private long lastId = 0;

  User existing(String username) {
    return create(new UserDraft(username, username, username + "@example.com"));
  }

  @Override
  public List<User> findAll() {
    return List.copyOf(users);
  }

  @Override
  public Optional<User> findById(UserId id) {
    return users.stream().filter(user -> user.id().equals(id)).findFirst();
  }

  @Override
  public User create(UserDraft draft) {
    User user = new User(new UserId(++lastId), draft.username(), draft.name(), draft.email(), true);
    users.add(user);
    return user;
  }

  @Override
  public User update(UserId id, UserDraft draft) {
    return replace(id, new User(id, draft.username(), draft.name(), draft.email(), true));
  }

  @Override
  public User deactivate(UserId id) {
    User current = findById(id).orElseThrow(() -> new UserNotFoundException(id));
    return replace(id, new User(id, current.username(), current.name(), current.email(), false));
  }

  private User replace(UserId id, User replacement) {
    findById(id).orElseThrow(() -> new UserNotFoundException(id));
    users.replaceAll(user -> user.id().equals(id) ? replacement : user);
    return replacement;
  }
}
