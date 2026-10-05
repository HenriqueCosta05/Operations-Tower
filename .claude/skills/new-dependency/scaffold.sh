#!/usr/bin/env bash
# Scaffolds a port + adapter + fake + DI wiring + ADR for a third-party library.
# Usage: scaffold.sh <capability-kebab> <library-kebab> [--client|--server]
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

CAPABILITY="${1:-}" LIBRARY="${2:-}"
KEBAB='^[a-z][a-z0-9]*(-[a-z0-9]+)*$'
[[ "$CAPABILITY" =~ $KEBAB && "$LIBRARY" =~ $KEBAB ]] || { echo "usage: scaffold.sh <capability-kebab> <library-kebab> [--client|--server]" >&2; exit 64; }
shift 2

TARGET=client
while [[ $# -gt 0 ]]; do
  case "$1" in
    --client) TARGET=client ;;
    --server) TARGET=server ;;
    *) echo "unknown option: $1" >&2; exit 64 ;;
  esac
  shift
done

pascal() { awk -F- '{for(i=1;i<=NF;i++) printf toupper(substr($i,1,1)) substr($i,2)}' <<<"$1"; }
CAP="$(pascal "$CAPABILITY")" LIB="$(pascal "$LIBRARY")"
CAP_FLAT="${CAPABILITY//-/}" LIB_FLAT="${LIBRARY//-/}"

emit() {
  local path="$1"
  if [[ -e "$path" ]]; then echo "exists, skipped: $path" >&2; cat >/dev/null; return; fi
  mkdir -p "$(dirname "$path")"
  cat >"$path"
  echo "created: $path"
}

adr() {
  local dir=docs/adr n slug
  mkdir -p "$dir"
  n="$(printf '%04d' $(( $(find "$dir" -maxdepth 1 -name '[0-9]*.md' | wc -l) + 1 )))"
  slug="use-$LIBRARY-for-$CAPABILITY"
  emit "$dir/$n-$slug.md" <<EOF
# $n. Use $LIBRARY for $CAPABILITY

Status: proposed

## Context

What business need requires the $CAPABILITY capability, and why the platform alone is not enough.

## Decision

Adopt \`$LIBRARY\` behind the \`$CAP\` port. Only \`$LIB${CAP}Adapter\` imports it.

## Evaluation

| Check | Result |
| --- | --- |
| License | |
| Last release / maintenance | |
| Known advisories | |
| Transitive footprint | |
| Alternatives considered | |

## Replacement plan

Implement another adapter for \`$CAP\`, run the $CAP contract suite against it, switch the provider/profile. No business code changes.
EOF
}

scaffold_client() {
  emit "client/src/app/core/ports/$CAPABILITY.port.ts" <<EOF
export abstract class $CAP {
  abstract execute(input: string): string;
}
EOF

  emit "client/src/app/core/testing/in-memory-$CAPABILITY.ts" <<EOF
import { $CAP } from '../ports/$CAPABILITY.port';

export class InMemory$CAP extends $CAP {
  execute(input: string): string {
    return input;
  }
}
EOF

  emit "client/src/app/infrastructure/$LIBRARY/$LIBRARY-$CAPABILITY.adapter.ts" <<EOF
import { Injectable } from '@angular/core';
import { $CAP } from '../../core/ports/$CAPABILITY.port';

@Injectable()
export class $LIB${CAP}Adapter extends $CAP {
  execute(input: string): string {
    throw new Error(\`$LIB${CAP}Adapter.execute not implemented for \${input}\`);
  }
}
EOF

  emit "client/src/app/infrastructure/$LIBRARY/$CAPABILITY.providers.ts" <<EOF
import { Provider } from '@angular/core';
import { $CAP } from '../../core/ports/$CAPABILITY.port';
import { $LIB${CAP}Adapter } from './$LIBRARY-$CAPABILITY.adapter';

export const provide$CAP = (): Provider => ({ provide: $CAP, useClass: $LIB${CAP}Adapter });
EOF
}

detect_package() {
  local app
  app="$(grep -rl '@SpringBootApplication' server/src/main/java 2>/dev/null | head -n1 || true)"
  if [[ -n "$app" ]]; then dirname "${app#server/src/main/java/}" | tr '/' '.'; else echo "com.operationstower"; fi
}

scaffold_server() {
  local base pkg main test
  base="$(detect_package)"
  pkg="$base.platform.$CAP_FLAT"
  main="server/src/main/java/${pkg//.//}"
  test="server/src/test/java/${pkg//.//}"

  emit "$main/$CAP.java" <<EOF
package $pkg;

public interface $CAP {

  String execute(String input);
}
EOF

  emit "$main/${CAP}Exception.java" <<EOF
package $pkg;

public class ${CAP}Exception extends RuntimeException {

  public ${CAP}Exception(String message, Throwable cause) {
    super(message, cause);
  }
}
EOF

  emit "$main/$LIB_FLAT/$LIB${CAP}Adapter.java" <<EOF
package $pkg.$LIB_FLAT;

import $pkg.$CAP;
import $pkg.${CAP}Exception;
import org.springframework.stereotype.Component;

@Component
class $LIB${CAP}Adapter implements $CAP {

  @Override
  public String execute(String input) {
    try {
      throw new UnsupportedOperationException("not implemented");
    } catch (RuntimeException e) {
      throw new ${CAP}Exception("$CAPABILITY failed", e);
    }
  }
}
EOF

  emit "$test/InMemory$CAP.java" <<EOF
package $pkg;

public class InMemory$CAP implements $CAP {

  @Override
  public String execute(String input) {
    return input;
  }
}
EOF
}

[[ "$TARGET" == client ]] && scaffold_client
[[ "$TARGET" == server ]] && scaffold_server
adr

cat <<EOF

next:
  1. install pinned:  $( [[ "$TARGET" == client ]] && echo "npm install -E $LIBRARY --prefix client" || echo "add <version> for $LIBRARY to server/pom.xml (or the Gradle version catalog)" )
  2. shape the $CAP port around what the business needs, then implement the adapter
  3. add the library to $( [[ "$TARGET" == client ]] && echo "restrictedThirdParty in client/eslint.config.js" || echo "ArchitectureTest (isolation rule)" )
  4. write the contract suite for $CAP and run it against the adapter and the in-memory fake
  5. register: $( [[ "$TARGET" == client ]] && echo "provide$CAP() in app.config.ts" || echo "nothing, the @Component adapter is picked up by component scan" )
EOF
