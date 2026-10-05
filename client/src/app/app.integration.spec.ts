import { render, screen } from '@testing-library/angular';
import { App } from './app';
import { appConfig } from './app.config';

describe('App with the production providers', () => {
  it('renders the heading through the real router configuration', async () => {
    await render(App, { providers: appConfig.providers });

    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('Hello, client');
  });
});
