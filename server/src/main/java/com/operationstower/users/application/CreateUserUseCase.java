package com.operationstower.users.application;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDirectory;
import com.operationstower.users.domain.UserDraft;
import org.springframework.stereotype.Service;

@Service
public class CreateUserUseCase {

  private final UserDirectory directory;

  public CreateUserUseCase(UserDirectory directory) {
    this.directory = directory;
  }

  public User execute(UserDraft draft) {
    return directory.create(draft);
  }
}
