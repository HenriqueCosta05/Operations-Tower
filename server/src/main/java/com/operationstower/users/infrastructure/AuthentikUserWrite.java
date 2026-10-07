package com.operationstower.users.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.operationstower.users.domain.UserDraft;

record AuthentikUserWrite(
    String username, String name, String email, @JsonProperty("is_active") Boolean active) {

  static AuthentikUserWrite creating(UserDraft draft) {
    return new AuthentikUserWrite(draft.username(), draft.name(), draft.email(), true);
  }

  static AuthentikUserWrite changing(UserDraft draft) {
    return new AuthentikUserWrite(draft.username(), draft.name(), draft.email(), null);
  }

  static AuthentikUserWrite deactivating() {
    return new AuthentikUserWrite(null, null, null, false);
  }
}
