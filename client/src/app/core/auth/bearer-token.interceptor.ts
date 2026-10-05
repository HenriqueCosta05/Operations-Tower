import { inject } from '@angular/core';
import type { HttpInterceptorFn } from '@angular/common/http';
import { API_ORIGIN } from '../config/api-origin';
import { Authentication } from '../ports/authentication.port';

export const bearerTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(Authentication).accessToken();
  const apiOrigin = inject(API_ORIGIN);
  if (!token || !request.url.startsWith(`${apiOrigin}/`)) return next(request);
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
