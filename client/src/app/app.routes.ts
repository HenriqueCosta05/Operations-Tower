import type { Routes } from '@angular/router';
import { authRoutes } from '@features/auth';

export const routes: Routes = [{ path: 'auth', children: authRoutes }];
