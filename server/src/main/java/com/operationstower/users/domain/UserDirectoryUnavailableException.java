package com.operationstower.users.domain;

public class UserDirectoryUnavailableException extends RuntimeException {

  public UserDirectoryUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
