# 0001. Use oidc-client-ts for authentication

Status: accepted

## Context

Operations Tower needs sign-in. We run authentik (open source, self-hosted) as the identity provider and speak plain OpenID Connect to it, so no authentik-specific SDK is needed anywhere. The Angular app is a public client (authorization code + PKCE, no secret). The Spring Boot server is a stateless resource server that validates bearer JWTs.

## Decision

- Server: `spring-boot-starter-security-oauth2-resource-server`. Issuer, signature and audience (`aud` must equal the authentik client id) are validated. The `groups` claim becomes `ROLE_<GROUP>` authorities. Spring Security types stay in `platform.security` and `identity.infrastructure` (ArchUnit enforces it); the rest of the server sees the `CurrentUser` port.
- Client: `oidc-client-ts` (pinned) behind the `Authentication` port. Only `infrastructure/oidc-client-ts/` imports it (ESLint enforces it). Guard, interceptor and callback page depend on the port alone.
- Identity provider: authentik via `infra/authentik/docker-compose.yml`. The provider and application are declared in `infra/authentik/blueprints/operations-tower.yaml`, so no clicking in the admin UI.

## Evaluation

| Check                      | Result                                                                                                                                                                                   |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| License                    | oidc-client-ts: Apache-2.0. authentik: MIT (enterprise features excluded). Spring Security: Apache-2.0                                                                                   |
| Last release / maintenance | Active for all three                                                                                                                                                                     |
| Known advisories           | `npm audit` and dependency-review in CI                                                                                                                                                  |
| Transitive footprint       | oidc-client-ts depends on `jwt-decode` only                                                                                                                                              |
| Alternatives considered    | angular-auth-oidc-client (ships its own guards and interceptors that would leak past the port), Keycloak (heavier to run), session cookies via a BFF (more server state, not needed yet) |

## Consequences

- Access tokens live in `sessionStorage` (per tab, cleared on close). A BFF would remove tokens from the browser entirely; revisit if the threat model asks for it.
- OIDC settings are hard-wired to local development in `oidc.settings.ts`. Production needs a `fileReplacements` entry or runtime config.

## Replacement plan

Implement another adapter for `Authentication`, run the same contract tests against it, switch the provider line in `app.config.ts`. No business code changes.
