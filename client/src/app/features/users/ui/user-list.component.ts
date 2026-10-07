import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import type { User } from '../domain/user.model';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-user-list',
  template: `
    <table>
      <thead>
        <tr>
          <th>Username</th>
          <th>Name</th>
          <th>Email</th>
          <th>Status</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        @for (user of users(); track user.id) {
          <tr>
            <td>{{ user.username }}</td>
            <td>{{ user.name }}</td>
            <td>{{ user.email }}</td>
            <td>{{ user.active ? 'Active' : 'Inactive' }}</td>
            <td>
              <button type="button" (click)="edit.emit(user)">Edit</button>
              @if (user.active) {
                <button type="button" (click)="deactivate.emit(user)">Deactivate</button>
              }
            </td>
          </tr>
        } @empty {
          <tr>
            <td colspan="5">No users yet</td>
          </tr>
        }
      </tbody>
    </table>
  `,
})
export class UserListComponent {
  readonly users = input.required<readonly User[]>();
  readonly edit = output<User>();
  readonly deactivate = output<User>();
}
