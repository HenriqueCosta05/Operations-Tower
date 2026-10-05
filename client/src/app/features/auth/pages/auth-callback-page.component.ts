import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Authentication } from '../../../core/ports/authentication.port';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-auth-callback-page',
  template: `
    @if (failed()) {
      <p role="alert">Sign-in failed. <a href="/">Try again</a></p>
    } @else {
      <p>Signing you in…</p>
    }
  `,
})
export class AuthCallbackPageComponent {
  private readonly authentication = inject(Authentication);
  private readonly router = inject(Router);
  protected readonly failed = signal(false);

  constructor() {
    void this.finishSignIn();
  }

  private async finishSignIn(): Promise<void> {
    try {
      const returnUrl = await this.authentication.completeSignIn();
      await this.router.navigateByUrl(returnUrl, { replaceUrl: true });
    } catch {
      this.failed.set(true);
    }
  }
}
