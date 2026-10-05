import type { Provider } from '@angular/core';
import { Authentication } from '../../core/ports/authentication.port';
import { OidcClientTsAuthentication } from './oidc-client-ts-authentication.adapter';
import type { OidcSettings } from './oidc.settings';
import { OIDC_SETTINGS } from './oidc.settings';

export const provideAuthentication = (settings: OidcSettings): Provider[] => [
  { provide: OIDC_SETTINGS, useValue: settings },
  { provide: Authentication, useClass: OidcClientTsAuthentication },
];
