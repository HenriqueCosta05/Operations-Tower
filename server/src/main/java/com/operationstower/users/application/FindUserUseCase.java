package com.operationstower.users.application;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDirectory;
import com.operationstower.users.domain.UserId;
import com.operationstower.users.domain.UserNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FindUserUseCase {

  private final UserDirectory directory;

  public FindUserUseCase(UserDirectory directory) {
    this.directory = directory;
  }

  public User execute(UserId id) {
    return directory.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }
}
