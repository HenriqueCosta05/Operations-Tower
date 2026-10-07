import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import type { Observable } from 'rxjs';
import { API_ORIGIN } from '../../../core/config/api-origin';
import type { User, UserDraft, UserId } from '../domain/user.model';
import type { UserDto } from './user.dto';
import { toUser, toUserRequest } from './user.mapper';
import { UserRepository } from './user.repository';

@Injectable()
export class UserHttpRepository extends UserRepository {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_ORIGIN)}/api/users`;

  findAll(): Observable<readonly User[]> {
    return this.http.get<UserDto[]>(this.baseUrl).pipe(map((dtos) => dtos.map(toUser)));
  }

  create(draft: UserDraft): Observable<User> {
    return this.http.post<UserDto>(this.baseUrl, toUserRequest(draft)).pipe(map(toUser));
  }

  update(id: UserId, draft: UserDraft): Observable<User> {
    return this.http
      .put<UserDto>(`${this.baseUrl}/${String(id)}`, toUserRequest(draft))
      .pipe(map(toUser));
  }

  deactivate(id: UserId): Observable<User> {
    return this.http
      .post<UserDto>(`${this.baseUrl}/${String(id)}/deactivate`, {})
      .pipe(map(toUser));
  }
}
