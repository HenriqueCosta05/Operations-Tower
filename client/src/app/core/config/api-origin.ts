import { InjectionToken } from '@angular/core';
import { readRuntimeConfig } from './runtime-config';

export const API_ORIGIN = new InjectionToken<string>('API_ORIGIN', {
  factory: () => readRuntimeConfig().apiOrigin,
});
