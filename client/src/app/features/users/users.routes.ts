import type { Routes } from '@angular/router';
import { UserHttpRepository } from './data-access/user-http.repository';
import { UserRepository } from './data-access/user.repository';

export const usersRoutes: Routes = [
  {
    path: '',
    providers: [{ provide: UserRepository, useClass: UserHttpRepository }],
    loadComponent: () => import('./pages/users-page.component').then((m) => m.UsersPageComponent),
  },
];
