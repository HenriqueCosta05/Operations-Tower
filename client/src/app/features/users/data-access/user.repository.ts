import type { Observable } from 'rxjs';
import type { User, UserDraft, UserId } from '../domain/user.model';

export abstract class UserRepository {
  abstract findAll(): Observable<readonly User[]>;
  abstract create(draft: UserDraft): Observable<User>;
  abstract update(id: UserId, draft: UserDraft): Observable<User>;
  abstract deactivate(id: UserId): Observable<User>;
}
