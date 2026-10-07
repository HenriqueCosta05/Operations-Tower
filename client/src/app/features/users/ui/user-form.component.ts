import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import type { UserDraftProblems } from '../domain/user-draft.validation';
import type { User, UserDraft } from '../domain/user.model';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-user-form',
  imports: [ReactiveFormsModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="submitted.emit(form.getRawValue())">
      <label>
        Username
        <input formControlName="username" />
      </label>
      @if (problems().username; as message) {
        <p role="alert">{{ message }}</p>
      }
      <label>
        Name
        <input formControlName="name" />
      </label>
      @if (problems().name; as message) {
        <p role="alert">{{ message }}</p>
      }
      <label>
        Email
        <input formControlName="email" type="email" />
      </label>
      @if (problems().email; as message) {
        <p role="alert">{{ message }}</p>
      }
      <button type="submit">{{ editing() ? 'Save changes' : 'Create user' }}</button>
      @if (editing()) {
        <button type="button" (click)="cancelled.emit()">Cancel</button>
      }
    </form>
  `,
})
export class UserFormComponent {
  private readonly builder = inject(FormBuilder).nonNullable;
  protected readonly form = this.builder.group({ username: '', name: '', email: '' });

  readonly editing = input<User | null>(null);
  readonly problems = input<UserDraftProblems>({});
  readonly submitted = output<UserDraft>();
  readonly cancelled = output();

  constructor() {
    effect(() => {
      const user = this.editing();
      this.form.reset({
        username: user?.username ?? '',
        name: user?.name ?? '',
        email: user?.email ?? '',
      });
    });
  }
}
