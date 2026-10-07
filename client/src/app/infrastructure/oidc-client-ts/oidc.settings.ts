import { InjectionToken } from '@angular/core';
import type { RuntimeConfig } from '../../core/config/runtime-config';

export interface OidcSettings {
  readonly authority: string;
  readonly clientId: string;
  readonly redirectPath: string;
  readonly postLogoutPath: string;
  readonly scope: string;
}

export const OIDC_SETTINGS = new InjectionToken<OidcSettings>('OIDC_SETTINGS');

export const oidcSettingsFrom = (config: RuntimeConfig): OidcSettings => ({
  authority: config.oidcAuthority,
  clientId: config.oidcClientId,
  redirectPath: '/auth/callback',
  postLogoutPath: '/',
  scope: 'openid profile email',
});
