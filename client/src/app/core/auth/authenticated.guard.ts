import { inject } from '@angular/core';
import type { CanActivateFn } from '@angular/router';
import { Authentication } from '../ports/authentication.port';

export const authenticatedGuard: CanActivateFn = async (_route, state) => {
  const authentication = inject(Authentication);
  if (authentication.user()) return true;
  await authentication.signIn(state.url);
  return false;
};
