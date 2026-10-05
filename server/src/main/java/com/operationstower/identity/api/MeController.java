package com.operationstower.identity.api;

import com.operationstower.identity.application.GetCurrentUserUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

  private final GetCurrentUserUseCase getCurrentUser;

  MeController(GetCurrentUserUseCase getCurrentUser) {
    this.getCurrentUser = getCurrentUser;
  }

  @GetMapping("/api/me")
  MeResponse me() {
    return MeResponse.from(getCurrentUser.execute());
  }
}
