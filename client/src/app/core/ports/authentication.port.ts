import type { Signal } from '@angular/core';
import type { AuthenticatedUser } from '../models/authenticated-user';

export abstract class Authentication {
  abstract readonly user: Signal<AuthenticatedUser | null>;
  abstract readonly accessToken: Signal<string | null>;
  abstract restoreSession(): Promise<void>;
  abstract signIn(returnUrl: string): Promise<void>;
  abstract completeSignIn(): Promise<string>;
  abstract signOut(): Promise<void>;
}

export class AuthenticationFailedError extends Error {}
