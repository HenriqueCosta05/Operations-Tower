package com.operationstower.users.api;

import com.operationstower.users.domain.InvalidUserException;
import com.operationstower.users.domain.UserDirectoryUnavailableException;
import com.operationstower.users.domain.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = UserController.class)
class UserExceptionHandler {

  @ExceptionHandler(UserNotFoundException.class)
  ProblemDetail notFound(UserNotFoundException failure) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, failure.getMessage());
  }

  @ExceptionHandler(InvalidUserException.class)
  ProblemDetail invalid(InvalidUserException failure) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, failure.getMessage());
  }

  @ExceptionHandler(UserDirectoryUnavailableException.class)
  ProblemDetail unavailable(UserDirectoryUnavailableException failure) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_GATEWAY, "The identity provider could not be reached");
  }
}
