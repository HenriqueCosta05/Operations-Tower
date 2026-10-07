package com.operationstower.users.application;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDirectory;
import com.operationstower.users.domain.UserId;
import org.springframework.stereotype.Service;

@Service
public class DeactivateUserUseCase {

  private final UserDirectory directory;

  public DeactivateUserUseCase(UserDirectory directory) {
    this.directory = directory;
  }

  public User execute(UserId id) {
    return directory.deactivate(id);
  }
}
