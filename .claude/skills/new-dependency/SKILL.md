---
name: new-dependency
description: Add a third-party library to client/ (Angular) or server/ (Spring Boot) behind a port and adapter so business code never imports it and swapping it later touches one folder. Use when adding, replacing or wrapping any npm package or Maven/Gradle library (dates, HTTP, PDF, charts, mail, storage, auth, SDK clients).
argument-hint: <capability-kebab> <library> [--client|--server]
---

# new-dependency

Business code depends on a **capability we define** (a port), never on a library. The library lives in exactly one adapter
folder. Replacing it means writing a second adapter and changing one provider line.

## Run

```bash
.claude/skills/new-dependency/scaffold.sh <capability-kebab> <library-kebab> [--client|--server]
# example: scaffold.sh pdf-rendering pdfmake --server
```

It creates the port, the adapter, an in-memory fake, the DI wiring and an ADR in `docs/adr/`. Then follow the steps below.

## Steps

1. **Justify.** Can the platform (Angular, JDK, Spring) do it in under ~50 lines? Then skip the dependency. Otherwise check
   license (permissive only), last release, open security advisories (`npm audit`, OWASP dependency-check), transitive
   weight and bus factor. Record the verdict in the generated ADR.
2. **Design the port from our needs, not the library's API.** Name it for the capability (`PdfRenderer`, `Clock`,
   `DateFormatter`, `Mailer`), expose only the operations the business uses, speak only in our types (domain models,
   primitives, `Instant`, `Uint8Array`). If the signature mentions a library type, the port is wrong.
3. **Write the adapter.** Only this file imports the library. Translate in both directions and convert library failures
   into one of our exceptions (`PdfRenderingException`), so callers never catch library-specific errors.
4. **Wire it in one place.** Client: `provide<Capability>()` in `infrastructure/<library>/`, registered in
   `app.config.ts`. Server: the adapter is a `@Component` implementing the port; select between adapters with
   `@ConditionalOnProperty` or `@Profile` when more than one exists.
5. **Pin it.** Exact version (`npm install -E`, explicit `<version>` in the POM/Gradle catalog). Renovate/Dependabot bumps it.
6. **Lock the boundary.** Add the library to the restricted-import lists below so a stray import fails CI.
7. **Test the contract, not the library.** Write one contract suite against the port (what any implementation must do:
   inputs, outputs, error mapping, edge cases). Run it against the real adapter and the in-memory fake. A replacement
   adapter passes the same suite or it is not a replacement.

## Boundary enforcement

ESLint (`client/eslint.config.js`): list the package in `restrictedThirdParty`; the only override that allows it is
`src/app/infrastructure/**`.

```js
const restrictedThirdParty = ['date-fns', 'date-fns/*', 'lodash', 'lodash/*'];
```

ArchUnit (`server`, in `ArchitectureTest`): one rule per library, allowing only the adapter package.

```java
@ArchTest static final ArchRule pdfmake_is_isolated = noClasses()
    .that().resideOutsideOfPackage("..platform.pdfrendering.pdfmake..")
    .should().dependOnClassesThat().resideInAPackage("com.pdfmake..");
```

## Layout

Client:

```
src/app/core/ports/<capability>.port.ts                abstract class = port + DI token
src/app/core/testing/in-memory-<capability>.ts         fake for tests
src/app/infrastructure/<library>/<library>-<capability>.adapter.ts
src/app/infrastructure/<library>/<capability>.providers.ts
```

Server:

```
<base>/platform/<capability>/<Capability>.java                 port (interface)
<base>/platform/<capability>/<Capability>Exception.java        our failure type
<base>/platform/<capability>/<library>/<Library><Capability>Adapter.java
src/test/java/<base>/platform/<capability>/InMemory<Capability>.java
```

## Rules

- Never leak library types through a port, a DTO, a domain object, an event or a log line.
- One library per adapter folder; two libraries behind one port means two adapters.
- No library-specific configuration outside `infrastructure/` / `platform/<capability>/<library>/`.
- Third-party objects are constructed in the adapter (or a factory it owns), never injected into business classes.
- Prefer composition: if a library has a global/static API (`moment`, `Math.random`, `System.currentTimeMillis`), wrap it
  behind an injectable port so tests control it.
