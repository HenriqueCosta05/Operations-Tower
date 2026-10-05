import { render, screen } from '@testing-library/angular';
import { provideRouter, Router } from '@angular/router';
import { Authentication, AuthenticationFailedError } from '../../../core/ports/authentication.port';
import { InMemoryAuthentication } from '../../../core/testing/in-memory-authentication';
import { operatorUser } from '../../../core/testing/authenticated-user.fixture';
import { AuthCallbackPageComponent } from './auth-callback-page.component';

describe('AuthCallbackPageComponent', () => {
  it('finishes sign-in and returns the user to the page they originally wanted', async () => {
    const authentication = new InMemoryAuthentication({ user: operatorUser, token: 't' });
    await authentication.signIn('/operations');
    const navigateByUrl = vi.fn(() => Promise.resolve(true));

    const { fixture } = await render(AuthCallbackPageComponent, {
      providers: [
        { provide: Router, useValue: { navigateByUrl } },
        { provide: Authentication, useValue: authentication },
      ],
    });
    await fixture.whenStable();

    expect(authentication.user()).toEqual(operatorUser);
    expect(navigateByUrl).toHaveBeenCalledWith('/operations', { replaceUrl: true });
  });

  it('tells the user when the identity provider response is rejected', async () => {
    const failing = new InMemoryAuthentication();
    failing.completeSignIn = () => Promise.reject(new AuthenticationFailedError('bad state'));

    await render(AuthCallbackPageComponent, {
      providers: [provideRouter([]), { provide: Authentication, useValue: failing }],
    });

    expect((await screen.findByRole('alert')).textContent).toContain('Sign-in failed');
  });
});
