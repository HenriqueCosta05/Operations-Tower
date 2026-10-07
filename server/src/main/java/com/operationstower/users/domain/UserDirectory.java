package com.operationstower.users.domain;

import java.util.List;
import java.util.Optional;

public interface UserDirectory {

  List<User> findAll();

  Optional<User> findById(UserId id);

  User create(UserDraft draft);

  User update(UserId id, UserDraft draft);

  User deactivate(UserId id);
}
