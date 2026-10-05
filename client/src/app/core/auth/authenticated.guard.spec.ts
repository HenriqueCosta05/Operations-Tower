import { TestBed } from '@angular/core/testing';
import type { RouterStateSnapshot } from '@angular/router';
import { Authentication } from '../ports/authentication.port';
import { InMemoryAuthentication } from '../testing/in-memory-authentication';
import { operatorUser } from '../testing/authenticated-user.fixture';
import { authenticatedGuard } from './authenticated.guard';

const navigateTo = (url: string) =>
  TestBed.runInInjectionContext(() =>
    authenticatedGuard({} as never, { url } as RouterStateSnapshot),
  );

describe('authenticatedGuard', () => {
  const authentication = new InMemoryAuthentication();

  beforeEach(() => {
    authentication.user.set(null);
    authentication.signInRequests.length = 0;
    TestBed.configureTestingModule({
      providers: [{ provide: Authentication, useValue: authentication }],
    });
  });

  it('lets a signed-in user through without starting a new sign-in', async () => {
    authentication.user.set(operatorUser);

    expect(await navigateTo('/operations')).toBe(true);
    expect(authentication.signInRequests).toEqual([]);
  });

  it('blocks an anonymous visitor and sends them to sign in, remembering the page they wanted', async () => {
    expect(await navigateTo('/operations?tab=alerts')).toBe(false);
    expect(authentication.signInRequests).toEqual(['/operations?tab=alerts']);
  });
});
