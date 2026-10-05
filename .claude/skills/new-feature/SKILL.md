---
name: new-feature
description: Scaffold a new feature-driven vertical slice across client/ (Angular) and server/ (Spring Boot). Each feature owns its domain, application, data-access and UI, exposes one public API, and keeps third-party code behind ports. Use when adding a feature, module, bounded context or "slice" to Operations Tower.
argument-hint: <feature-name-kebab> [--client|--server|--both]
---

# new-feature

Create one self-contained feature. A feature is a vertical slice: deleting its folder deletes the feature, and no other
feature can break because of it.

## Run

```bash
.claude/skills/new-feature/scaffold.sh <feature-kebab> [--client|--server|--both] [--package com.acme.operationstower]
```

Default is `--both`. The server package is derived from the existing `@SpringBootApplication` class; pass `--package`
on an empty project. Then fill in the stubs following the rules below and write the first behavior test before any
further code (see `.claude/hooks/tests-check.sh` for what a good test is).

## Rules that apply to both sides

1. **Independence.** A feature imports only from its own folder, `core/` (client) / `shared/` (server), and other
   features' public API (`index.ts`, `<Feature>Api` interface). Never reach into another feature's internals.
2. **Third-party code stays out of features.** Libraries are reached through a port owned by the feature (or `core/`)
   and implemented by an adapter in `infrastructure/`. Use the `new-dependency` skill to add one.
3. **Dependencies point inward:** `ui/api -> application -> domain`, and `infrastructure -> domain`. `domain` imports
   nothing but the language (no Angular, RxJS, Spring, JPA or Jackson annotations).
4. **One public door.** Client: `index.ts`. Server: the `api` package plus a package-private rest. Everything else is
   private by convention and by lint/ArchUnit.
5. **Clean code.** Names state intent; functions do one thing at one level of abstraction; at most 3 parameters (else a
   value object); guard clauses over nesting; exceptions over error codes; no null returns; no comments that restate
   code (rename instead); no boolean flag parameters.
6. **GoF only when it earns its keep.** Introduce a pattern when it removes a conditional ladder, duplication or a
   dependency on a concrete type. A Strategy with one implementation is noise.

## Client layout (`client/src/app/features/<name>/`)

```
index.ts                     public API: routes + facade type only
<name>.routes.ts             lazy route config
domain/                      pure TypeScript: models, value objects, policies, strategies (no Angular/RxJS)
application/                 <Name>Facade: signal state + use cases; the only thing pages talk to
data-access/                 <Name>Repository (abstract port), <Name>HttpRepository (adapter), mapper, DTOs
pages/                       smart, routed components; inject the facade only
ui/                          dumb presentational components: input()/output(), OnPush, no injection of services
testing/                     InMemory<Name>Repository fake shared by unit tests
```

Angular specifics: standalone components, `ChangeDetectionStrategy.OnPush`, `inject()`, signals for state,
`computed()` for derived values, `@if/@for` with `track`, `takeUntilDestroyed()` for streams. Provide the repository
at route level: `{ provide: <Name>Repository, useClass: <Name>HttpRepository }`.

DTO -> model mapping happens only in `data-access/<name>.mapper.ts`; the wire format never leaks into `domain`.

Alias each feature in `tsconfig.json` (`"@features/<name>": ["src/app/features/<name>/index.ts"]`) and enforce it with
ESLint (`no-restricted-imports` on `@features/*/*`, configured in the `ci-cd` skill).

## Server layout (`server/src/main/java/<base>/<feature>/`)

```
api/             <Name>Controller, request/response records (Bean Validation), exception handler advice
application/     one class per use case (Find<Name>UseCase, Create<Name>UseCase); @Transactional lives here
domain/          entities/records, value objects, domain services, policies, <Name>Repository (port), domain exceptions
infrastructure/  adapters: JPA/JDBC repository, external API clients, mappers (entity <-> domain)
```

Spring specifics: constructor injection only (records/`final` fields, no `@Autowired` on fields); controllers are thin
(map, delegate, map back); use cases return domain objects, controllers return response records; JPA entities never
leave `infrastructure`; configuration via `@ConfigurationProperties` records; `ProblemDetail` for errors.

Enforce the layering with ArchUnit (test dependency `com.tngtech.archunit:archunit-junit5`):

```java
@AnalyzeClasses(packages = "<base>", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  @ArchTest static final ArchRule domain_is_pure = noClasses().that().resideInAPackage("..domain..")
      .should().dependOnClassesThat().resideInAnyPackage("..api..", "..application..", "..infrastructure..",
          "org.springframework..", "jakarta.persistence..", "com.fasterxml..");
  @ArchTest static final ArchRule application_ignores_adapters = noClasses().that().resideInAPackage("..application..")
      .should().dependOnClassesThat().resideInAnyPackage("..api..", "..infrastructure..");
  @ArchTest static final ArchRule features_are_independent = slices().matching("<base>.(*)..").should().notDependOnEachOther()
      .ignoreDependency(alwaysTrue(), resideInAPackage("<base>.shared.."));
}
```

## GoF patterns: where they belong

| Need | Pattern | Client | Server |
| --- | --- | --- | --- |
| Swap algorithms / replace `if-else` on a type | Strategy | `domain/pricing.strategy.ts` + map keyed by union type | `domain/PricingStrategy` + Spring injects `Map<Type, Strategy>` |
| Hide subsystem behind one entry point | Facade | `application/<name>.facade.ts` | use-case class per operation |
| Isolate a library or remote API | Adapter | `data-access/*-http.repository.ts`, `infrastructure/*` | `infrastructure/*Adapter` implementing a domain port |
| Build objects by input type | Factory (method) | `domain/<name>.factory.ts` | static factory on the record / `@Component` factory |
| Many optional constructor fields | Builder | object-literal + `satisfies` | Lombok-free `record` + `Builder` or `toBuilder` |
| Add behavior without subclassing | Decorator | HTTP interceptor, wrapper repository | `@Primary` decorating bean (caching, auditing, metrics) |
| Fixed skeleton, varying steps | Template Method | rarely; prefer composition | abstract importer/processor with `final` skeleton |
| Notify without coupling | Observer | RxJS `Subject`/signals via facade | `ApplicationEventPublisher` + `@EventListener` |
| Undo / queue / audit an action | Command | action objects in the store | `Create<Name>Command` record handled by a use case |
| Ordered, optional processing steps | Chain of Responsibility | validators list reduced in order | `List<Handler>` injected, ordered with `@Order` |
| Whole-feature test double | Null Object / Fake | `testing/InMemory*` | `InMemory*Repository` in `src/test` |

## Done when

- Folder compiles and lints; `index.ts` / `api` exposes only what other features need.
- The first test describes real behavior of the domain or use case (not "should create").
- ESLint boundary rule and ArchUnit tests pass.
