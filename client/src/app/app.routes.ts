import type { Routes } from '@angular/router';
import { authRoutes } from '@features/auth';
import { usersRoutes } from '@features/users';
import { authenticatedGuard } from './core/auth/authenticated.guard';

export const routes: Routes = [
  { path: 'auth', children: authRoutes },
  { path: 'users', canActivate: [authenticatedGuard], children: usersRoutes },
];
