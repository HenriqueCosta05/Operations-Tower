import { Injectable, inject, signal } from '@angular/core';
import { UserManager, WebStorageStateStore } from 'oidc-client-ts';
import type { User } from 'oidc-client-ts';
import type { AuthenticatedUser } from '../../core/models/authenticated-user';
import { Authentication, AuthenticationFailedError } from '../../core/ports/authentication.port';
import { OIDC_SETTINGS } from './oidc.settings';
import { toAuthenticatedUser } from './user-profile.mapper';

@Injectable()
export class OidcClientTsAuthentication extends Authentication {
  private readonly settings = inject(OIDC_SETTINGS);
  private readonly manager = new UserManager({
    authority: this.settings.authority,
    client_id: this.settings.clientId,
    redirect_uri: `${window.location.origin}${this.settings.redirectPath}`,
    post_logout_redirect_uri: `${window.location.origin}${this.settings.postLogoutPath}`,
    response_type: 'code',
    scope: this.settings.scope,
    automaticSilentRenew: true,
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  });
  private readonly currentUser = signal<AuthenticatedUser | null>(null);
  private readonly currentToken = signal<string | null>(null);

  readonly user = this.currentUser.asReadonly();
  readonly accessToken = this.currentToken.asReadonly();

  constructor() {
    super();
    this.manager.events.addUserLoaded((user) => {
      this.remember(user);
    });
    this.manager.events.addUserUnloaded(() => {
      this.forget();
    });
  }

  override async restoreSession(): Promise<void> {
    const stored = await this.manager.getUser();
    if (stored && !stored.expired) this.remember(stored);
  }

  override signIn(returnUrl: string): Promise<void> {
    return this.guarded(() => this.manager.signinRedirect({ state: returnUrl }));
  }

  override async completeSignIn(): Promise<string> {
    const user = await this.guarded(() => this.manager.signinRedirectCallback());
    this.remember(user);
    return typeof user.state === 'string' ? user.state : '/';
  }

  override async signOut(): Promise<void> {
    this.forget();
    await this.guarded(() => this.manager.signoutRedirect());
  }

  private remember(user: User): void {
    this.currentUser.set(toAuthenticatedUser(user.profile));
    this.currentToken.set(user.access_token);
  }

  private forget(): void {
    this.currentUser.set(null);
    this.currentToken.set(null);
  }

  private async guarded<T>(operation: () => Promise<T>): Promise<T> {
    try {
      return await operation();
    } catch (cause) {
      throw new AuthenticationFailedError('Authentication failed', { cause });
    }
  }
}
