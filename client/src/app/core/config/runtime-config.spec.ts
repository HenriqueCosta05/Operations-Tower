import { readRuntimeConfig } from './runtime-config';

describe('readRuntimeConfig', () => {
  afterEach(() => {
    delete window.__APP_CONFIG__;
  });

  it('falls back to local development endpoints when the host injects nothing', () => {
    expect(readRuntimeConfig().apiOrigin).toBe('http://localhost:8080');
  });

  it('lets the hosting environment override only the values it provides', () => {
    window.__APP_CONFIG__ = { apiOrigin: 'https://api.example.com' };

    expect(readRuntimeConfig()).toMatchObject({
      apiOrigin: 'https://api.example.com',
      oidcClientId: 'operations-tower',
    });
  });
});
