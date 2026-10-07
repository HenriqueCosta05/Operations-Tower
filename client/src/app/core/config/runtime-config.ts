export interface RuntimeConfig {
  readonly apiOrigin: string;
  readonly oidcAuthority: string;
  readonly oidcClientId: string;
}

declare global {
  interface Window {
    __APP_CONFIG__?: Partial<RuntimeConfig>;
  }
}

const localDefaults: RuntimeConfig = {
  apiOrigin: 'http://localhost:8080',
  oidcAuthority: 'http://localhost:9000/application/o/operations-tower/',
  oidcClientId: 'operations-tower',
};

export const readRuntimeConfig = (): RuntimeConfig => ({
  ...localDefaults,
  ...window.__APP_CONFIG__,
});
