# Local authentik

```bash
cp .env.example .env   # then replace every change-me
docker compose up -d
```

Open http://localhost:9000/if/flow/initial-setup/ once (or log in as `akadmin` with `AUTHENTIK_BOOTSTRAP_PASSWORD`). The `Operations Tower` provider and application are created from `blueprints/operations-tower.yaml`.

| Setting      | Value                                                   |
| ------------ | ------------------------------------------------------- |
| Issuer       | `http://localhost:9000/application/o/operations-tower/` |
| Client id    | `operations-tower` (public, PKCE)                       |
| Redirect URI | `http://localhost:4200/auth/callback`                   |

Server overrides: `AUTHENTIK_ISSUER_URI`, `AUTHENTIK_CLIENT_ID`, `APP_ALLOWED_ORIGINS`. Add users to the `operators` group to get `ROLE_OPERATORS`.

## User management

The `/api/users` endpoints manage authentik users through its admin API and require the `admins` group (`ROLE_ADMINS`).
Create an API token in authentik (Directory > Tokens and App passwords, intent `API`) for a user allowed to manage users,
then start the server with `AUTHENTIK_API_TOKEN` (and `AUTHENTIK_API_URL` if not `http://localhost:9000`).

For deployed environments (dev/stg/prod on Docker, including a VPS) see `infra/docker/README.md`.
