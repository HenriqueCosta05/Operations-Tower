import { InjectionToken } from '@angular/core';

export const API_ORIGIN = new InjectionToken<string>('API_ORIGIN', {
  factory: () => 'http://localhost:8080',
});
