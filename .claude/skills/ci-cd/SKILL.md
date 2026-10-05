---
name: ci-cd
description: Set up and maintain CI/CD quality gates for the Angular client and Spring Boot server — GitHub Actions, ESLint, Prettier, Husky pre-commit/commit-msg/pre-push hooks, lint-staged, commitlint, and Java tooling (Spotless, Surefire/Failsafe, JaCoCo, Enforcer, ArchUnit, Dependabot, CodeQL). Use when asked about pipelines, linting, formatting, git hooks, coverage or build quality.
argument-hint: [install|audit]
---

# ci-cd

One pipeline definition of "good", enforced three times: in the editor/pre-commit (fast), in `pre-push` (tests), and in
GitHub Actions (authoritative). Templates live in `templates/`; `scripts/install.sh` copies them without overwriting.

## Run

```bash
.claude/skills/ci-cd/scripts/install.sh        # copy templates that do not exist yet
```

Then follow "Wire it up". `audit` mode: diff the repo against the templates and report drift.

## What each layer does

| Layer | Trigger | Runs | Budget |
| --- | --- | --- | --- |
| Editor | save | Prettier, ESLint, Spotless (IDE plugins) | instant |
| `pre-commit` | `git commit` | lint-staged: ESLint `--fix` + Prettier on staged client files, Spotless on server | seconds |
| `commit-msg` | `git commit` | commitlint (Conventional Commits) | instant |
| `pre-push` | `git push` | `.claude/hooks/tests-check.sh` unit + integration | minutes |
| GitHub Actions | PR / push to main | lint, format check, unit, integration, build, e2e, CodeQL, dependency review | authoritative |

Never `--no-verify`. If a hook is wrong, fix the hook.

## Wire it up

1. **Root `package.json`** (monorepo root, holds only tooling): copy `templates/root/package.json`, then
   `npm install` — `prepare` runs `husky` and installs the hooks.
2. **Client scripts** the pipeline and `tests-check.sh` expect in `client/package.json`:
   `lint`, `format:check`, `test` (single run, non-watch), `test:integration`, `test:e2e`, `build`.
   Dev deps: `eslint`, `typescript-eslint`, `angular-eslint`, `eslint-config-prettier`, `prettier`, `vitest`.
3. **ESLint**: copy `templates/client/eslint.config.js` to `client/`. Strict TS, Angular template a11y, complexity caps,
   feature-boundary and third-party import restrictions (see `new-feature` and `new-dependency` skills).
4. **Server**: merge `templates/server/pom-snippets.xml` into `server/pom.xml` (Enforcer, Spotless/google-java-format,
   Surefire, Failsafe, JaCoCo with a coverage gate, ArchUnit). Gradle projects: apply `com.diffplug.spotless`, `jacoco`,
   `java` test suites for `integrationTest` / `e2eTest` with the same JUnit tags.
5. **Git hooks**: `templates/husky/*` -> `.husky/`. They call `lint-staged`, `commitlint` and `tests-check.sh`.
6. **GitHub**: `templates/github/` -> `.github/` (CI workflow, CodeQL, Dependabot).
7. **Branch protection** on `main`: require `client`, `server`, `e2e` checks, linear history, PR review, up-to-date branch.

## Conventions

- **Tests are tagged**, not located: `*Test` = unit (untagged), `*IT` = `@Tag("integration")`, `*E2EIT` = `@Tag("e2e")`.
  CI selects with `-Dgroups`; an untagged IT silently never runs, so `tests-check.sh` rejects it.
- **Java 21**, Node 22 LTS, pinned in the workflow and in `.nvmrc` / `maven-enforcer`.
- **Reproducible installs**: `npm ci`, committed lockfiles, Maven wrapper (`./mvnw`).
- **Fast feedback first**: lint/format/unit run before slower integration and e2e; jobs are cancelled when a newer commit
  lands (`concurrency`).
- **Coverage is a floor, not a goal**: JaCoCo gate at 70% lines; raise it per module, never lower it to merge.
- **Conventional Commits** (`feat:`, `fix:`, `refactor:`, `chore:`) drive changelogs and release tooling.
- **Secrets** never in the repo; use GitHub environments + OIDC for deploys. Workflows request least-privilege
  `permissions:` per job.
- **Supply chain**: Dependabot weekly for npm, Maven, GitHub Actions; `dependency-review-action` blocks risky PR deps;
  new libraries go through the `new-dependency` skill.

## Extending to CD

Add a `deploy.yml` triggered by tags or `workflow_run` of CI on `main`: build the Angular bundle (`ng build`) and the
Spring Boot image (`./mvnw spring-boot:build-image`), push to the registry, deploy to a GitHub Environment with required
reviewers for production. Keep deploy logic in scripts under `scripts/` so it is runnable locally.
