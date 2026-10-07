import { of, throwError } from 'rxjs';
import type { Observable } from 'rxjs';
import { UserRepository } from '../data-access/user.repository';
import type { User, UserDraft, UserId } from '../domain/user.model';

export class InMemoryUserRepository extends UserRepository {
  failing = false;
  private nextId = 1000;

  constructor(private users: readonly User[] = []) {
    super();
  }

  findAll(): Observable<readonly User[]> {
    return this.failing ? this.failure() : of(this.users);
  }

  create(draft: UserDraft): Observable<User> {
    if (this.failing) return this.failure();
    const created: User = { ...draft, id: ++this.nextId as UserId, active: true };
    this.users = [...this.users, created];
    return of(created);
  }

  update(id: UserId, draft: UserDraft): Observable<User> {
    return this.change(id, (user) => ({ ...user, ...draft }));
  }

  deactivate(id: UserId): Observable<User> {
    return this.change(id, (user) => ({ ...user, active: false }));
  }

  private change(id: UserId, apply: (user: User) => User): Observable<User> {
    const current = this.users.find((user) => user.id === id);
    if (this.failing || !current) return this.failure();
    const changed = apply(current);
    this.users = this.users.map((user) => (user.id === id ? changed : user));
    return of(changed);
  }

  private failure(): Observable<never> {
    return throwError(() => new Error('repository failure'));
  }
}
