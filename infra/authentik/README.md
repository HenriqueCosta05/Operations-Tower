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
