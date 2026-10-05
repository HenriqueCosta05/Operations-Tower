package com.operationstower.identity.domain;

public class NotAuthenticatedException extends RuntimeException {

  public NotAuthenticatedException() {
    super("No authenticated user in the current request");
  }
}
