package com.operationstower.identity.application;

import com.operationstower.identity.domain.AuthenticatedUser;
import com.operationstower.identity.domain.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentUserUseCase {

  private final CurrentUser currentUser;

  public GetCurrentUserUseCase(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  public AuthenticatedUser execute() {
    return currentUser.get();
  }
}
