#!/bin/sh
set -eu

: "${API_ORIGIN:?API_ORIGIN is required}"
: "${OIDC_AUTHORITY:?OIDC_AUTHORITY is required}"
: "${OIDC_CLIENT_ID:?OIDC_CLIENT_ID is required}"

cat > /usr/share/nginx/html/config.js <<CONFIG
window.__APP_CONFIG__ = {
  apiOrigin: "${API_ORIGIN}",
  oidcAuthority: "${OIDC_AUTHORITY}",
  oidcClientId: "${OIDC_CLIENT_ID}",
};
CONFIG
