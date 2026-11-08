# Check Pilot

Android app for a pilot instructor. Kotlin, Compose-only UI, single `:app` module, no external backend.

## Build & test

```sh
./gradlew testDebugUnitTest                                 # all unit tests (same task as CI)
./gradlew ktlintFormat                                      # fix formatting
./gradlew ktlintCheck                                       # what the pre-push hook enforces
```
- Git hooks live in `.githooks` (`git config core.hooksPath .githooks`): pre-commit runs `ktlintFormat`,
  pre-push runs `ktlintCheck`.

## Definition of done

1. `./gradlew testDebugUnitTest` passes.
2. `./gradlew ktlintCheck` passes.
3. New use cases / repositories have an interface, an `Impl`, a `*Fake`, and unit tests.
4. New classes are registered in `di/MainModules.kt`.

## Architecture

Layered MVVM, packages `com.mebiad.checkpilot.<layer>.<feature>` under `app/src/main/kotlin`:

```
domain/       — use cases, business logic, interfaces
data/local    — Room database (SQLCipher-encrypted), DataStore settings, bundled assets
ui/apps/      — feature screens (Composables + ViewModels; one Activity, Navigation Compose)
ui/generic/   — shared scaffold, reusable components, theme
di/           — Koin module definitions
core/         — logging and other cross-cutting code
```

- Interface + `Impl` for repositories, use cases and services.
- Use cases own a `CoroutineScope` with `SupervisorJob` + an injected `CoroutineDispatcher` (default `Dispatchers.IO`).
- ViewModels expose `StateFlow` / `SharedFlow`; screens collect with `collectAsStateWithLifecycle()`. No LiveData in new code.
- Async results are wrapped in `AsyncResult<T>` (`Idle`, `Loading`, `Success`, `Error`).

## Knowledge base

`docs/knowledge-base/` holds facts the code doesn't show: devices in the field, OS versions, backend and customer
specifics. Start at its `README.md`, and read it before work that depends on devices or Android versions (SDK
levels, behavior changes, testing). Add new facts of that kind there, not to this file.

## Conventions

### Reuse, don't copy
Search first (`ui/generic/`, shared use cases, repositories, fakes). If the same behavior is needed in a
second place, copy it, if it is needed a third time, lift it into one shared abstraction instead of copying and tweaking. Keep call-site
specifics (labels, navigation, feature state) at the call site.

### Propose a refactor when it doesn't fit
If new functionality needs a new layer, a moved boundary, or a reshaped shared type: stop, explain what
doesn't fit, propose the refactor, and wait for approval. No local workaround in the meantime.
A small local extension of an existing abstraction is fine to do directly.

### Kotlin
- Naming: `*Usecase` / `*UsecaseImpl`, `*Repository` / `*RepositoryImpl`, `*ViewModel`; Composables in PascalCase.
- 4-space indent, 120 char lines; follow `.editorconfig` / ktlint. Trailing commas are allowed in
  declarations but rejected at call sites.
- No underscores in numeric literals: `1000L`, not `1_000L`.
- DI in `di/MainModules.kt`: `single<Interface> { Impl(dep = get()) }`, `factory<…>`, `viewModelOf(::MyViewModel)`.

### Comments
- Preserve existing comments that explain *why* or describe non-obvious side effects when refactoring.
- Don't add comments that narrate what the code does.

### Tests (`app/src/test/kotlin`)
- No MockK in new tests. Create or reuse a `*Fake` next to the interface's test package
  (e.g. `domain/UsecaseFake`) with setters or `reset()` to drive state.
- Use `MainDispatcherRule` from `testutils/`, Turbine for flows, `runTest` + `advanceUntilIdle()`.
- Structure every test body with `// Given`, `// When`, `// Then`; for repeated cycles use
  `// When / Then: <description>`.

## Working on features

- Start non-trivial features with a plan in `docs/plans/<feature>.md` and get it reviewed before coding.
- Keep `local.properties` and `keystore/` out of reach: they hold signing secrets. Never print or commit them.
