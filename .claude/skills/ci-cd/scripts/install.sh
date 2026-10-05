#!/usr/bin/env bash
# Copies ci-cd templates into the repo. Existing files are never overwritten.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
SRC="$ROOT/.claude/skills/ci-cd/templates"
cd "$ROOT"

copy() {
  local from="$1" to="$2"
  if [[ ! -e "$SRC/$from" ]]; then echo "template missing, skipped: $from"; return; fi
  if [[ -e "$to" ]]; then echo "exists, skipped: $to"; return; fi
  mkdir -p "$(dirname "$to")"
  cp "$SRC/$from" "$to"
  echo "created: $to"
}

copy root/package.json package.json
copy root/lint-staged.config.mjs lint-staged.config.mjs
copy root/commitlint.config.mjs commitlint.config.mjs
copy root/.prettierrc.json .prettierrc.json
copy root/.prettierignore .prettierignore
copy root/.editorconfig .editorconfig
copy root/.nvmrc .nvmrc
copy client/eslint.config.js client/eslint.config.js
copy husky/pre-commit .husky/pre-commit
copy husky/commit-msg .husky/commit-msg
copy husky/pre-push .husky/pre-push
copy github/workflows/ci.yml .github/workflows/ci.yml
copy github/workflows/codeql.yml .github/workflows/codeql.yml
copy github/dependabot.yml .github/dependabot.yml
chmod +x .husky/* 2>/dev/null || true

cat <<'EOF'

next:
  1. npm install                      (installs husky and activates .husky hooks)
  2. cd client && npm install -D eslint typescript-eslint angular-eslint eslint-config-prettier prettier vitest
  3. merge .claude/skills/ci-cd/templates/server/pom-snippets.xml into server/pom.xml
  4. add client scripts: lint, format:check, test, test:integration, test:e2e, build
EOF
