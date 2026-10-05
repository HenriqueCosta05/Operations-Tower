#!/usr/bin/env bash
# Scaffolds a feature slice. Never overwrites existing files.
# Usage: scaffold.sh <feature-kebab> [--client|--server|--both] [--package com.acme.app]
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

FEATURE="${1:-}"
[[ "$FEATURE" =~ ^[a-z][a-z0-9]*(-[a-z0-9]+)*$ ]] || { echo "usage: scaffold.sh <feature-kebab> [--client|--server|--both] [--package pkg]" >&2; exit 64; }
shift

TARGET=both PACKAGE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --client) TARGET=client ;;
    --server) TARGET=server ;;
    --both) TARGET=both ;;
    --package) PACKAGE="$2"; shift ;;
    *) echo "unknown option: $1" >&2; exit 64 ;;
  esac
  shift
done

PASCAL="$(awk -F- '{for(i=1;i<=NF;i++) printf toupper(substr($i,1,1)) substr($i,2)}' <<<"$FEATURE")"
CAMEL="$(tr '[:upper:]' '[:lower:]' <<<"${PASCAL:0:1}")${PASCAL:1}"
FLAT="${FEATURE//-/}"

emit() {
  local path="$1"
  if [[ -e "$path" ]]; then echo "exists, skipped: $path" >&2; cat >/dev/null; return; fi
  mkdir -p "$(dirname "$path")"
  cat >"$path"
  echo "created: $path"
}

scaffold_client() {
  local d="client/src/app/features/$FEATURE"

  emit "$d/index.ts" <<EOF
export { ${CAMEL}Routes } from './${FEATURE}.routes';
export type { ${PASCAL} } from './domain/${FEATURE}.model';
EOF

  emit "$d/${FEATURE}.routes.ts" <<EOF
import { Routes } from '@angular/router';
import { ${PASCAL}HttpRepository } from './data-access/${FEATURE}-http.repository';
import { ${PASCAL}Repository } from './data-access/${FEATURE}.repository';

export const ${CAMEL}Routes: Routes = [
  {
    path: '',
    providers: [{ provide: ${PASCAL}Repository, useClass: ${PASCAL}HttpRepository }],
    loadComponent: () => import('./pages/${FEATURE}-page.component').then((m) => m.${PASCAL}PageComponent),
  },
];
EOF

  emit "$d/domain/${FEATURE}.model.ts" <<EOF
export type ${PASCAL}Id = string & { readonly brand: '${PASCAL}Id' };

export type ${PASCAL} = {
  readonly id: ${PASCAL}Id;
};
EOF

  emit "$d/data-access/${FEATURE}.repository.ts" <<EOF
import { Observable } from 'rxjs';
import { ${PASCAL}, ${PASCAL}Id } from '../domain/${FEATURE}.model';

export abstract class ${PASCAL}Repository {
  abstract findAll(): Observable<readonly ${PASCAL}[]>;
  abstract findById(id: ${PASCAL}Id): Observable<${PASCAL}>;
}
EOF

  emit "$d/data-access/${FEATURE}.dto.ts" <<EOF
export type ${PASCAL}Dto = {
  readonly id: string;
};
EOF

  emit "$d/data-access/${FEATURE}.mapper.ts" <<EOF
import { ${PASCAL}, ${PASCAL}Id } from '../domain/${FEATURE}.model';
import { ${PASCAL}Dto } from './${FEATURE}.dto';

export const to${PASCAL} = (dto: ${PASCAL}Dto): ${PASCAL} => ({
  id: dto.id as ${PASCAL}Id,
});
EOF

  emit "$d/data-access/${FEATURE}-http.repository.ts" <<EOF
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ${PASCAL}, ${PASCAL}Id } from '../domain/${FEATURE}.model';
import { ${PASCAL}Dto } from './${FEATURE}.dto';
import { to${PASCAL} } from './${FEATURE}.mapper';
import { ${PASCAL}Repository } from './${FEATURE}.repository';

@Injectable()
export class ${PASCAL}HttpRepository extends ${PASCAL}Repository {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/${FEATURE}';

  findAll(): Observable<readonly ${PASCAL}[]> {
    return this.http.get<${PASCAL}Dto[]>(this.baseUrl).pipe(map((dtos) => dtos.map(to${PASCAL})));
  }

  findById(id: ${PASCAL}Id): Observable<${PASCAL}> {
    return this.http.get<${PASCAL}Dto>(\`\${this.baseUrl}/\${id}\`).pipe(map(to${PASCAL}));
  }
}
EOF

  emit "$d/application/${FEATURE}.facade.ts" <<EOF
import { Injectable, computed, inject, signal } from '@angular/core';
import { ${PASCAL} } from '../domain/${FEATURE}.model';
import { ${PASCAL}Repository } from '../data-access/${FEATURE}.repository';

type ${PASCAL}State = {
  readonly items: readonly ${PASCAL}[];
  readonly loading: boolean;
  readonly error: string | null;
};

@Injectable()
export class ${PASCAL}Facade {
  private readonly repository = inject(${PASCAL}Repository);
  private readonly state = signal<${PASCAL}State>({ items: [], loading: false, error: null });

  readonly items = computed(() => this.state().items);
  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);

  load(): void {
    this.state.update((s) => ({ ...s, loading: true, error: null }));
    this.repository.findAll().subscribe({
      next: (items) => this.state.set({ items, loading: false, error: null }),
      error: () => this.state.update((s) => ({ ...s, loading: false, error: 'Could not load ${FEATURE}' })),
    });
  }
}
EOF

  emit "$d/pages/${FEATURE}-page.component.ts" <<EOF
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { ${PASCAL}Facade } from '../application/${FEATURE}.facade';

@Component({
  selector: 'app-${FEATURE}-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [${PASCAL}Facade],
  template: \`
    @if (facade.loading()) {
      <p>Loading…</p>
    } @else if (facade.error(); as message) {
      <p role="alert">{{ message }}</p>
    } @else {
      @for (item of facade.items(); track item.id) {
        <p>{{ item.id }}</p>
      }
    }
  \`,
})
export class ${PASCAL}PageComponent implements OnInit {
  protected readonly facade = inject(${PASCAL}Facade);

  ngOnInit(): void {
    this.facade.load();
  }
}
EOF

  emit "$d/testing/in-memory-${FEATURE}.repository.ts" <<EOF
import { Observable, of, throwError } from 'rxjs';
import { ${PASCAL}, ${PASCAL}Id } from '../domain/${FEATURE}.model';
import { ${PASCAL}Repository } from '../data-access/${FEATURE}.repository';

export class InMemory${PASCAL}Repository extends ${PASCAL}Repository {
  constructor(private readonly items: readonly ${PASCAL}[] = []) {
    super();
  }

  findAll(): Observable<readonly ${PASCAL}[]> {
    return of(this.items);
  }

  findById(id: ${PASCAL}Id): Observable<${PASCAL}> {
    const found = this.items.find((item) => item.id === id);
    return found ? of(found) : throwError(() => new Error(\`${PASCAL} \${id} not found\`));
  }
}
EOF
  mkdir -p "$d/ui"
  [[ -e "$d/ui/.gitkeep" ]] || : >"$d/ui/.gitkeep"
}

detect_package() {
  [[ -n "$PACKAGE" ]] && { echo "$PACKAGE"; return; }
  local app
  app="$(grep -rl '@SpringBootApplication' server/src/main/java 2>/dev/null | head -n1 || true)"
  if [[ -n "$app" ]]; then
    dirname "${app#server/src/main/java/}" | tr '/' '.'
  else
    echo "com.operationstower"
  fi
}

scaffold_server() {
  local base pkg d
  base="$(detect_package)"
  pkg="$base.$FLAT"
  d="server/src/main/java/${pkg//.//}"

  emit "$d/domain/${PASCAL}.java" <<EOF
package $pkg.domain;

public record ${PASCAL}(${PASCAL}Id id) {

  public ${PASCAL} {
    if (id == null) {
      throw new IllegalArgumentException("id must not be null");
    }
  }
}
EOF

  emit "$d/domain/${PASCAL}Id.java" <<EOF
package $pkg.domain;

import java.util.UUID;

public record ${PASCAL}Id(UUID value) {

  public static ${PASCAL}Id newId() {
    return new ${PASCAL}Id(UUID.randomUUID());
  }
}
EOF

  emit "$d/domain/${PASCAL}Repository.java" <<EOF
package $pkg.domain;

import java.util.List;
import java.util.Optional;

public interface ${PASCAL}Repository {

  List<${PASCAL}> findAll();

  Optional<${PASCAL}> findById(${PASCAL}Id id);

  ${PASCAL} save(${PASCAL} ${CAMEL});
}
EOF

  emit "$d/domain/${PASCAL}NotFoundException.java" <<EOF
package $pkg.domain;

public class ${PASCAL}NotFoundException extends RuntimeException {

  public ${PASCAL}NotFoundException(${PASCAL}Id id) {
    super("${PASCAL} not found: " + id.value());
  }
}
EOF

  emit "$d/application/Find${PASCAL}UseCase.java" <<EOF
package $pkg.application;

import $pkg.domain.${PASCAL};
import $pkg.domain.${PASCAL}Id;
import $pkg.domain.${PASCAL}NotFoundException;
import $pkg.domain.${PASCAL}Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Find${PASCAL}UseCase {

  private final ${PASCAL}Repository repository;

  public Find${PASCAL}UseCase(${PASCAL}Repository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public ${PASCAL} execute(${PASCAL}Id id) {
    return repository.findById(id).orElseThrow(() -> new ${PASCAL}NotFoundException(id));
  }
}
EOF

  emit "$d/infrastructure/InMemory${PASCAL}Repository.java" <<EOF
package $pkg.infrastructure;

import $pkg.domain.${PASCAL};
import $pkg.domain.${PASCAL}Id;
import $pkg.domain.${PASCAL}Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
class InMemory${PASCAL}Repository implements ${PASCAL}Repository {

  private final Map<${PASCAL}Id, ${PASCAL}> store = new ConcurrentHashMap<>();

  @Override
  public List<${PASCAL}> findAll() {
    return List.copyOf(store.values());
  }

  @Override
  public Optional<${PASCAL}> findById(${PASCAL}Id id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public ${PASCAL} save(${PASCAL} ${CAMEL}) {
    store.put(${CAMEL}.id(), ${CAMEL});
    return ${CAMEL};
  }
}
EOF

  emit "$d/api/${PASCAL}Response.java" <<EOF
package $pkg.api;

import $pkg.domain.${PASCAL};
import java.util.UUID;

public record ${PASCAL}Response(UUID id) {

  static ${PASCAL}Response from(${PASCAL} ${CAMEL}) {
    return new ${PASCAL}Response(${CAMEL}.id().value());
  }
}
EOF

  emit "$d/api/${PASCAL}Controller.java" <<EOF
package $pkg.api;

import $pkg.application.Find${PASCAL}UseCase;
import $pkg.domain.${PASCAL}Id;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import $pkg.domain.${PASCAL}NotFoundException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/${FEATURE}")
class ${PASCAL}Controller {

  private final Find${PASCAL}UseCase find${PASCAL};

  ${PASCAL}Controller(Find${PASCAL}UseCase find${PASCAL}) {
    this.find${PASCAL} = find${PASCAL};
  }

  @GetMapping("/{id}")
  ${PASCAL}Response findById(@PathVariable UUID id) {
    return ${PASCAL}Response.from(find${PASCAL}.execute(new ${PASCAL}Id(id)));
  }

  @ExceptionHandler(${PASCAL}NotFoundException.class)
  ProblemDetail notFound(${PASCAL}NotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }
}
EOF
}

[[ "$TARGET" == client || "$TARGET" == both ]] && scaffold_client
[[ "$TARGET" == server || "$TARGET" == both ]] && scaffold_server

cat <<EOF

next:
  1. client: add "@features/$FEATURE" to tsconfig paths and mount ${CAMEL}Routes in app.routes.ts
  2. write the first behavior test (facade state transitions / use-case rules), not an existence test
  3. replace InMemory${PASCAL}Repository with the real adapter when persistence lands
EOF
