package com.operationstower.users.application;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDirectory;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListUsersUseCase {

  private final UserDirectory directory;

  public ListUsersUseCase(UserDirectory directory) {
    this.directory = directory;
  }

  public List<User> execute() {
    return directory.findAll();
  }
}
