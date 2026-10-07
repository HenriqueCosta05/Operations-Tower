package com.operationstower.users.api;

import com.operationstower.users.application.CreateUserUseCase;
import com.operationstower.users.application.DeactivateUserUseCase;
import com.operationstower.users.application.FindUserUseCase;
import com.operationstower.users.application.ListUsersUseCase;
import com.operationstower.users.application.UpdateUserUseCase;
import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserId;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class UserController {

  private final ListUsersUseCase listUsers;
  private final FindUserUseCase findUser;
  private final CreateUserUseCase createUser;
  private final UpdateUserUseCase updateUser;
  private final DeactivateUserUseCase deactivateUser;

  UserController(
      ListUsersUseCase listUsers,
      FindUserUseCase findUser,
      CreateUserUseCase createUser,
      UpdateUserUseCase updateUser,
      DeactivateUserUseCase deactivateUser) {
    this.listUsers = listUsers;
    this.findUser = findUser;
    this.createUser = createUser;
    this.updateUser = updateUser;
    this.deactivateUser = deactivateUser;
  }

  @GetMapping
  List<UserResponse> list() {
    return listUsers.execute().stream().map(UserResponse::from).toList();
  }

  @GetMapping("/{id}")
  UserResponse find(@PathVariable long id) {
    return UserResponse.from(findUser.execute(new UserId(id)));
  }

  @PostMapping
  ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
    User created = createUser.execute(request.toDraft());
    return ResponseEntity.created(URI.create("/api/users/" + created.id().value()))
        .body(UserResponse.from(created));
  }

  @PutMapping("/{id}")
  UserResponse update(@PathVariable long id, @Valid @RequestBody UserRequest request) {
    return UserResponse.from(updateUser.execute(new UserId(id), request.toDraft()));
  }

  @PostMapping("/{id}/deactivate")
  UserResponse deactivate(@PathVariable long id) {
    return UserResponse.from(deactivateUser.execute(new UserId(id)));
  }
}
