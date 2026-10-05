import { InjectionToken } from '@angular/core';

export interface OidcSettings {
  readonly authority: string;
  readonly clientId: string;
  readonly redirectPath: string;
  readonly postLogoutPath: string;
  readonly scope: string;
}

export const OIDC_SETTINGS = new InjectionToken<OidcSettings>('OIDC_SETTINGS');

export const developmentOidcSettings: OidcSettings = {
  authority: 'http://localhost:9000/application/o/operations-tower/',
  clientId: 'operations-tower',
  redirectPath: '/auth/callback',
  postLogoutPath: '/',
  scope: 'openid profile email',
};
