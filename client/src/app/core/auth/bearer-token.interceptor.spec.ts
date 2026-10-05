import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_ORIGIN } from '../config/api-origin';
import { Authentication } from '../ports/authentication.port';
import { InMemoryAuthentication } from '../testing/in-memory-authentication';
import { bearerTokenInterceptor } from './bearer-token.interceptor';

describe('bearerTokenInterceptor', () => {
  const authentication = new InMemoryAuthentication();
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    authentication.accessToken.set(null);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([bearerTokenInterceptor])),
        provideHttpClientTesting(),
        { provide: Authentication, useValue: authentication },
        { provide: API_ORIGIN, useValue: 'https://api.example.com' },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  it('adds the access token to requests for our API', () => {
    authentication.accessToken.set('token-123');

    http.get('https://api.example.com/api/me').subscribe();

    const request = backend.expectOne('https://api.example.com/api/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-123');
  });

  it('never leaks the token to another origin', () => {
    authentication.accessToken.set('token-123');

    http.get('https://third-party.example.org/data').subscribe();

    const request = backend.expectOne('https://third-party.example.org/data');
    expect(request.request.headers.has('Authorization')).toBe(false);
  });

  it('sends API requests unchanged when nobody is signed in', () => {
    http.get('https://api.example.com/api/me').subscribe();

    const request = backend.expectOne('https://api.example.com/api/me');
    expect(request.request.headers.has('Authorization')).toBe(false);
  });
});
