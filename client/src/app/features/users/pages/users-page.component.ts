import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import type { OnInit } from '@angular/core';
import { UserFacade } from '../application/user.facade';
import type { User, UserDraft } from '../domain/user.model';
import { UserFormComponent } from '../ui/user-form.component';
import { UserListComponent } from '../ui/user-list.component';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-users-page',
  imports: [UserFormComponent, UserListComponent],
  providers: [UserFacade],
  template: `
    <h1>Users</h1>
    @if (facade.error(); as message) {
      <p role="alert">{{ message }}</p>
    }
    <app-user-form
      [editing]="editing()"
      [problems]="facade.problems()"
      (submitted)="save($event)"
      (cancelled)="editing.set(null)"
    />
    @if (facade.loading()) {
      <p>Loading…</p>
    } @else {
      <app-user-list
        [users]="facade.users()"
        (edit)="editing.set($event)"
        (deactivate)="facade.deactivate($event.id)"
      />
    }
  `,
})
export class UsersPageComponent implements OnInit {
  protected readonly facade = inject(UserFacade);
  protected readonly editing = signal<User | null>(null);

  ngOnInit(): void {
    this.facade.load();
  }

  protected save(draft: UserDraft): void {
    const user = this.editing();
    if (user) this.facade.update(user.id, draft);
    else this.facade.create(draft);
  }
}
