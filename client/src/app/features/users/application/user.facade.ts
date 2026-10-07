import { Injectable, computed, inject, signal } from '@angular/core';
import type { Observable } from 'rxjs';
import { UserRepository } from '../data-access/user.repository';
import { isValid, validateUserDraft } from '../domain/user-draft.validation';
import type { UserDraftProblems } from '../domain/user-draft.validation';
import type { User, UserDraft, UserId } from '../domain/user.model';

interface UserState {
  readonly users: readonly User[];
  readonly loading: boolean;
  readonly error: string | null;
  readonly problems: UserDraftProblems;
}

interface Submission {
  readonly request: () => Observable<User>;
  readonly failure: string;
  readonly applyTo: (saved: User) => readonly User[];
}

const replaceUser = (users: readonly User[], id: UserId, saved: User): readonly User[] =>
  users.map((user) => (user.id === id ? saved : user));

@Injectable()
export class UserFacade {
  private readonly repository = inject(UserRepository);
  private readonly state = signal<UserState>({
    users: [],
    loading: false,
    error: null,
    problems: {},
  });

  readonly users = computed(() => this.state().users);
  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);
  readonly problems = computed(() => this.state().problems);

  load(): void {
    this.state.update((s) => ({ ...s, loading: true, error: null }));
    this.repository.findAll().subscribe({
      next: (users) => {
        this.state.update((s) => ({ ...s, users, loading: false }));
      },
      error: () => {
        this.fail('Could not load users');
      },
    });
  }

  create(draft: UserDraft): void {
    this.submit(draft, {
      request: () => this.repository.create(draft),
      failure: 'Could not create the user',
      applyTo: (saved) => [...this.users(), saved],
    });
  }

  update(id: UserId, draft: UserDraft): void {
    this.submit(draft, {
      request: () => this.repository.update(id, draft),
      failure: 'Could not update the user',
      applyTo: (saved) => replaceUser(this.users(), id, saved),
    });
  }

  deactivate(id: UserId): void {
    this.state.update((s) => ({ ...s, error: null }));
    this.repository.deactivate(id).subscribe({
      next: (saved) => {
        this.state.update((s) => ({
          ...s,
          users: replaceUser(s.users, id, saved),
        }));
      },
      error: () => {
        this.fail('Could not deactivate the user');
      },
    });
  }

  private submit(draft: UserDraft, submission: Submission): void {
    const problems = validateUserDraft(draft);
    this.state.update((s) => ({ ...s, problems, error: null }));
    if (!isValid(problems)) return;
    submission.request().subscribe({
      next: (saved) => {
        this.state.update((s) => ({ ...s, users: submission.applyTo(saved) }));
      },
      error: () => {
        this.fail(submission.failure);
      },
    });
  }

  private fail(message: string): void {
    this.state.update((s) => ({ ...s, loading: false, error: message }));
  }
}
