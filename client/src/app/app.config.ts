import { provideHttpClient, withInterceptors } from '@angular/common/http';
import type { ApplicationConfig } from '@angular/core';
import { provideAppInitializer, inject, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { bearerTokenInterceptor } from './core/auth/bearer-token.interceptor';
import { Authentication } from './core/ports/authentication.port';
import { provideAuthentication } from './infrastructure/oidc-client-ts/authentication.providers';
import { developmentOidcSettings } from './infrastructure/oidc-client-ts/oidc.settings';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([bearerTokenInterceptor])),
    provideAuthentication(developmentOidcSettings),
    provideAppInitializer(() => inject(Authentication).restoreSession()),
  ],
};
