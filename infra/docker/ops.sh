#!/usr/bin/env bash
# Run the Operations Tower stack for one environment, on this machine or on a remote Docker host.
#
#   ops.sh <dev|stg|prod> init                      create env/<env>.env with generated secrets
#   ops.sh <dev|stg|prod> [--host user@vps] <docker compose args...>
#
# Examples:
#   ops.sh dev up -d --build
#   ops.sh prod --host deploy@203.0.113.10 pull
#   ops.sh prod --host deploy@203.0.113.10 up -d
#   ops.sh stg logs -f server
#
# dev always builds images from this checkout. stg and prod pull IMAGE_REGISTRY images unless
# BUILD=1 is set (build on the target engine, useful before the registry pipeline exists).
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

usage() { sed -n '2,15p' "$0"; exit 64; }

ENVIRONMENT="${1:-}"
[[ "$ENVIRONMENT" =~ ^(dev|stg|prod)$ ]] || usage
shift

ENV_FILE="env/$ENVIRONMENT.env"
SECRET_KEYS=(PG_PASS AUTHENTIK_SECRET_KEY AUTHENTIK_BOOTSTRAP_PASSWORD AUTHENTIK_API_TOKEN)

init_env_file() {
  [[ ! -e "$ENV_FILE" ]] || { echo "$ENV_FILE already exists" >&2; exit 1; }
  cp "env/$ENVIRONMENT.env.example" "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  for key in "${SECRET_KEYS[@]}"; do
    sed -i "s|^$key=change-me\$|$key=$(openssl rand -base64 48 | tr -d '=+/\n')|" "$ENV_FILE"
  done
  echo "created $ENV_FILE: review the hostnames, then run: ops.sh $ENVIRONMENT up -d"
  echo "authentik admin: akadmin / AUTHENTIK_BOOTSTRAP_PASSWORD from $ENV_FILE"
}

[[ "${1:-}" != init ]] || { init_env_file; exit 0; }

if [[ "${1:-}" == --host ]]; then
  [[ -n "${2:-}" ]] || usage
  export DOCKER_HOST="ssh://$2"
  shift 2
fi

[[ $# -gt 0 ]] || usage
[[ -f "$ENV_FILE" ]] || { echo "missing $ENV_FILE: run ops.sh $ENVIRONMENT init" >&2; exit 1; }

FILES=(-f compose.yaml)
if [[ "$ENVIRONMENT" == dev || "${BUILD:-0}" == 1 ]]; then FILES+=(-f compose.build.yaml); fi
FILES+=(-f "compose.$ENVIRONMENT.yaml")

DEPLOY_ENV="$ENVIRONMENT" exec docker compose --env-file "$ENV_FILE" "${FILES[@]}" "$@"
