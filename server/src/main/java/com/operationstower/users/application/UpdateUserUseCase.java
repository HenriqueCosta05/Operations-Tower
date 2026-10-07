package com.operationstower.users.application;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDirectory;
import com.operationstower.users.domain.UserDraft;
import com.operationstower.users.domain.UserId;
import org.springframework.stereotype.Service;

@Service
public class UpdateUserUseCase {

  private final UserDirectory directory;

  public UpdateUserUseCase(UserDirectory directory) {
    this.directory = directory;
  }

  public User execute(UserId id, UserDraft draft) {
    return directory.update(id, draft);
  }
}
