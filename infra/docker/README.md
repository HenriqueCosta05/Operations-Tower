# Docker stack: dev, stg, prod

One compose stack (`compose.yaml`) with a thin overlay per environment. Everything is served through Caddy, which
terminates TLS (Let's Encrypt) and routes by hostname:

| Hostname    | Service                        |
| ----------- | ------------------------------ |
| `APP_HOST`  | Angular client (nginx)         |
| `API_HOST`  | Spring Boot server             |
| `AUTH_HOST` | authentik (+ postgres, worker) |

The client image is environment-agnostic: its API and OIDC endpoints are written to `config.js` when the container
starts. The authentik redirect URI is derived from `APP_HOST`, and an API token for the server's user management is
created on first boot from `AUTHENTIK_API_TOKEN`.

| Environment | Images                             | TLS                   | Published ports                   |
| ----------- | ---------------------------------- | --------------------- | --------------------------------- |
| `dev`       | built from this checkout           | none (plain HTTP)     | 80, plus 127.0.0.1:8080 and :9000 |
| `stg`       | pulled from `IMAGE_REGISTRY:stg`   | Let's Encrypt staging | 80, 443                           |
| `prod`      | pulled from `IMAGE_REGISTRY:<tag>` | Let's Encrypt         | 80, 443                           |

Run one environment per host: they all want ports 80 and 443. Each environment gets its own compose project and volumes.

## Local

```bash
cd infra/docker
./ops.sh dev init            # writes env/dev.env with generated secrets
./ops.sh dev up -d --build
```

Open http://app.localhost. authentik admin: `akadmin` with `AUTHENTIK_BOOTSTRAP_PASSWORD` from `env/dev.env`, at
http://auth.localhost. Put a user in the `admins` group to use `/users`.

## Application database

The app database is not part of this stack. It lives in the VPS-wide shared PostgreSQL in `/srv/postgres`
(container `pg-shared`, see its README), as database and role `operations_tower`. Credentials are in
`/srv/postgres/credentials/operations_tower.env`; print them with `dbctl url operations_tower`.

- Server started on the host (`./mvnw spring-boot:run`): use `HOST_DATABASE_URL`, i.e. `127.0.0.1:5432`.
- Server started as a container: attach it to the external `pgnet` network and use `JDBC_URL` (`pg-shared:5432`).

## On a VPS

1. **Prepare the host** (Ubuntu/Debian): `sudo bash vps-bootstrap.sh deploy` installs Docker, opens only 22/80/443 and
   creates a `deploy` user in the `docker` group. Add your SSH public key to `/home/deploy/.ssh/authorized_keys`.
2. **DNS**: point three A records (`app.`, `api.`, `auth.` for that environment) at the VPS. No domain yet? Use
   [nip.io](https://nip.io): `app.203-0-113-10.nip.io`, `api.203-0-113-10.nip.io`, `auth.203-0-113-10.nip.io`.
3. **Configure**: `./ops.sh prod init`, then edit `env/prod.env` (hostnames, `ACME_EMAIL`, `IMAGE_REGISTRY`, `IMAGE_TAG`).
   Keep this file out of git; it is ignored.
4. **Deploy**, either way:

   **From your machine**, over SSH, nothing but Docker needed on the VPS:

   ```bash
   ./ops.sh prod --host deploy@203.0.113.10 pull
   ./ops.sh prod --host deploy@203.0.113.10 up -d
   ./ops.sh prod --host deploy@203.0.113.10 logs -f server
   ```

   **On the VPS itself**: clone the repo, copy `env/prod.env` there, and run `./ops.sh prod pull && ./ops.sh prod up -d`.

No registry yet? `BUILD=1 ./ops.sh stg --host deploy@vps up -d --build` builds the images on the VPS from your local
checkout (the build context is sent over SSH).

## Images and releases

`.github/workflows/images.yml` publishes `ghcr.io/<owner>/<repo>/{client,server}` on every push to `main` (tags `stg`
and `sha-<commit>`) and on `v*` tags (tag `1.2.3`). Make the packages readable by the VPS, either public or with
`docker login ghcr.io` on the host using a token with `read:packages`. Release flow: merge to `main`, `stg` redeploys
with `pull`, tag `v1.2.3`, set `IMAGE_TAG=1.2.3` in `env/prod.env`, deploy. Rollback is setting the previous tag.

## Operations

```bash
./ops.sh prod ps
./ops.sh prod logs -f authentik-server
./ops.sh prod down            # keeps volumes; add -v only to destroy data
```

Data lives in the `database` (postgres), `authentik_data`, and `caddy_data` (certificates) volumes. Back up the
`database` volume: `./ops.sh prod exec postgresql pg_dump -U authentik authentik > authentik.sql`.

Changing `PG_PASS` or `AUTHENTIK_SECRET_KEY` after first boot breaks the existing database and sessions. `AUTHENTIK_BOOTSTRAP_*`
only apply on first boot; change the admin password in authentik afterwards.
