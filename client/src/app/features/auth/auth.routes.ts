import type { Routes } from '@angular/router';

export const authRoutes: Routes = [
  {
    path: 'callback',
    loadComponent: () =>
      import('./pages/auth-callback-page.component').then((m) => m.AuthCallbackPageComponent),
  },
];
