import { signal } from '@angular/core';
import type { AuthenticatedUser } from '../models/authenticated-user';
import { Authentication } from '../ports/authentication.port';

export class InMemoryAuthentication extends Authentication {
  readonly user = signal<AuthenticatedUser | null>(null);
  readonly accessToken = signal<string | null>(null);
  readonly signInRequests: string[] = [];
  private pendingReturnUrl = '/';

  constructor(private readonly knownUser?: { user: AuthenticatedUser; token: string }) {
    super();
  }

  override restoreSession(): Promise<void> {
    return Promise.resolve();
  }

  override signIn(returnUrl: string): Promise<void> {
    this.signInRequests.push(returnUrl);
    this.pendingReturnUrl = returnUrl;
    return Promise.resolve();
  }

  override completeSignIn(): Promise<string> {
    if (this.knownUser) {
      this.user.set(this.knownUser.user);
      this.accessToken.set(this.knownUser.token);
    }
    return Promise.resolve(this.pendingReturnUrl);
  }

  override signOut(): Promise<void> {
    this.user.set(null);
    this.accessToken.set(null);
    return Promise.resolve();
  }
}
