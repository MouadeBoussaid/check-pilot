# Plan: the skeleton pull request

The [technical plan](../specs/001-mvp/technical-plan.md#skeleton-before-any-story) says what the skeleton contains.
This plan says how to build it: the versions, the files, the test harness and the order of the commits. The skeleton
has no behaviour the Assessor can see. Its job is that US-1's acceptance tests compile and then fail for the right
reason.

## What the repo has today

- A blank AGP 9.5 template. AGP's built-in Kotlin is on (Kotlin 2.2.10, from AGP's own dependency), but there's no
  Kotlin source apart from the two example tests.
- No `MainActivity` and no Compose. The theme is the View-based `Theme.MaterialComponents`, from `appcompat` and
  `material`.
- `allowBackup="true"`, and the backup and data-extraction rules are still the template's samples.
- Every configuration is locked (`app/gradle.lockfile`). Lint runs with `warningsAsErrors`.
- In `build.gradle.kts`, three ktlint rules are off because the template code failed them: `no-wildcard-imports`,
  `import-ordering` and `final-newline`.
- There's no `.editorconfig` and no `.githooks`, although `CLAUDE.md` refers to both.

## Versions

These are the latest stable versions on 2026-11-21. They go into `gradle/libs.versions.toml`, and the lockfile is
regenerated with `--write-locks`.

| What | Version | Note |
|---|---|---|
| AGP | 9.5.0 | unchanged |
| Kotlin Gradle plugin | 2.4.21 | replaces AGP's bundled 2.2.10, see below |
| Compose compiler and serialization plugins | 2.4.21 | must match Kotlin |
| KSP | 2.3.14 | supports AGP 9's built-in Kotlin from 2.3.1 on |
| Compose BOM | 2026.11.01 | Material 3, ui-test-junit4, ui-test-manifest |
| activity-compose | 1.13.0 | |
| core-ktx | 1.19.1 | from 1.10.1 |
| Navigation 3 (runtime, ui) | 1.2.2 | |
| Lifecycle (viewmodel-navigation3, viewmodel-compose, runtime-compose, process) | 2.11.1 | |
| Room (runtime, compiler, Gradle plugin) | 2.8.5 | |
| sqlcipher-android | 4.20.0 | |
| Koin BOM (koin-android, koin-androidx-compose, koin-test-junit4) | 4.2.2 | |
| DataStore Preferences | 1.3.0 | |
| kotlinx-coroutines (android, test) | 1.11.0 | |
| kotlinx-serialization-core | 1.13.0 | Navigation 3 keys are `@Serializable` |
| Robolectric | 4.18.1 | supports SDK 37, needs JDK 21+ (the Gradle daemon toolchain is 25) |
| androidx.test core / ext-junit | 1.7.0 / 1.3.0 | |
| Turbine | 1.2.1 | |

The `appcompat` and `material` libraries are removed. The `espresso-core` library is removed, and androidTest gets
only what the encryption-at-rest test needs.

**Why Kotlin 2.4.21 and not the bundled 2.2.10:** a 2.2 compiler can read the metadata of libraries built with Kotlin
up to 2.3. The current Compose, Koin and kotlinx libraries may be built with newer Kotlin, and the compiler would
refuse them. The first commit adds the Kotlin Gradle plugin to the build classpath at 2.4.21, the way AGP 9 documents
for a newer Kotlin. It then builds a tiny Compose and Room class to prove that KSP 2.3.14 and Room's compiler work
with it. **Fallback:** if they don't, the highest Kotlin version that KSP and Room accept, and library versions built
with it.

## Files

```text
app/src/main/kotlin/com/mebiad/checkpilot/
├── CheckPilotApplication.kt        # startKoin(mainModules)
├── MainActivity.kt                 # enableEdgeToEdge, CheckPilotTheme, AppNavigation
├── core/clock/Clock.kt, ClockImpl.kt
├── data/local/db/
│   ├── CheckPilotDatabase.kt       # Room, 5 entities, exportSchema
│   ├── entity/                     # Session, Note, Grade, Competency, ObservableBehaviour
│   └── CatalogueSeedCallback.kt    # onCreate: Competency 1–9, each with OBS BEH a–g
├── data/local/crypto/
│   ├── DatabaseKeyProvider.kt, DatabaseKeyProviderImpl.kt
│   └── PassphraseCipher.kt, KeystorePassphraseCipherImpl.kt
├── di/MainModules.kt
└── ui/
    ├── apps/home/HomeScreen.kt     # empty home screen
    ├── generic/navigation/         # NavKeys, AppNavigation (NavDisplay)
    └── generic/theme/              # CheckPilotTheme: plain Material 3, replaced by the designs
app/src/main/res/                   # theme parent → android:Theme.Material.Light.NoActionBar,
                                    # unused template colours removed, backup rules exclude everything
app/schemas/                        # Room's exported schema, version 1
app/src/test/kotlin/com/mebiad/checkpilot/
├── testutils/                      # AppHarness, TestApplication, testModule, MainDispatcherRule
├── core/clock/ClockFake.kt
├── data/local/crypto/              # DatabaseKeyProviderFake, PassphraseCipherFake, DatabaseKeyProviderImplTest
├── data/local/db/CatalogueSeedCallbackTest.kt
├── di/MainModulesVerifyTest.kt
└── SmokeTest.kt
app/src/test/resources/robolectric.properties   # application=…TestApplication, sdk=37
app/src/androidTest/kotlin/com/mebiad/checkpilot/EncryptionAtRestTest.kt
.editorconfig  .githooks/pre-commit  .githooks/pre-push
docs/knowledge-base/README.md
```

The two example tests are deleted.

## The data layer in the skeleton

- **Entities:** the five tables, with columns, keys and `ON DELETE` rules exactly as in the technical plan's Data
  model. There are no DAOs or repositories yet: each story adds the ones it needs, test-first.
- **Seed:** `CatalogueSeedCallback.onCreate` inserts the placeholder Catalogue with `execSQL`, because DAOs aren't
  available inside the callback. It never runs on an open that isn't a create.
- **Encryption:** `MainModules` builds the database with `openHelperFactory(SupportOpenHelperFactory(passphrase))`.
  The passphrase comes from `DatabaseKeyProvider`.
- **Key provider:** `DatabaseKeyProviderImpl` creates a random 32-byte passphrase on first use, has `PassphraseCipher`
  encrypt it, and keeps the result in a key file in `noBackupFilesDir`. Later it reads the file and decrypts it.
  `KeystorePassphraseCipherImpl` wraps an AES-GCM Android Keystore key that needs no user authentication. The cipher is
  split out because Robolectric has no Android Keystore. The file logic can then be unit-tested on the JVM with
  `PassphraseCipherFake`, and only the Keystore wrapper is left to the instrumented test.
- **DataStore:** a Koin `single` built with `PreferenceDataStoreFactory.create`, not the `preferencesDataStore`
  delegate. The delegate is one instance per process, which breaks the harness's restart. There's no `PinStore` yet:
  that comes with US-7.
- **Clock:** a `Clock` interface (`now(): Instant`, `today(): LocalDate`), `ClockImpl` on the system clock, and
  `ClockFake`. `ClockFake` is what the technical plan calls the `TestClock`; it's named `*Fake` as the conventions
  require.

## The test harness

`AppHarness` in `testutils/` is a JUnit rule that the acceptance tests use with `createEmptyComposeRule()`:

- `launch()` starts Koin with `mainModules` plus `testModule`, then starts `MainActivity` with `ActivityScenario`.
  `testModule` overrides:
  - the database: Room on a file in a temporary folder, with no `openHelperFactory`, so the framework's SQLite is used
  - the DataStore, on a file in the same folder
  - `Clock` → `ClockFake`
  - `DatabaseKeyProvider` → `DatabaseKeyProviderFake`
- `restart()` closes the activity, the database and the DataStore's scope, and calls `stopKoin`. It then launches
  again over the same files. That's "the app is closed and opened again" and "the app is updated".
- `sendToBackground()` and `bringToForeground()` move the activity to `CREATED` and back to `RESUMED`.
- `get<T>()` gives the Given steps the real repositories from the test's Koin graph.

`TestApplication` doesn't start Koin. Robolectric uses it for every JVM test through `robolectric.properties`. Unit
tests of plain classes don't touch it.

**A deviation from the technical plan:** that plan names `createAndroidComposeRule`, which starts the activity before
the test body runs and can't restart it. `createEmptyComposeRule()` with an `ActivityScenario` that the harness owns
supports restarts. The tests drive the screen the same way either way.

## Tests in the skeleton

Each is written before its code, following the `tdd` skill.

- `SmokeTest`: the harness launches the app and the home screen shows. A `restart()` shows it again.
- `MainModulesVerifyTest`: Koin's `verify()` passes on `mainModules`.
- `CatalogueSeedCallbackTest` (Robolectric, file database):
  - a new database holds Competency 1–9 in order, each with OBS BEH a–g
  - reopening the same file doesn't seed again, including after a Competency was renamed
- `DatabaseKeyProviderImplTest`:
  - the first call creates and stores a 32-byte passphrase
  - later calls return the same bytes
  - a new instance over the same folder returns the same bytes
  - the key file never holds the plain passphrase
- `EncryptionAtRestTest` (androidTest, real Keystore and SQLCipher): the database opens with the provider's
  passphrase, and its file doesn't start with the plain-SQLite header. It runs only on a phone or emulator
  (`connectedDebugAndroidTest`), not in `forge check`.

## Commits

Each commit builds and passes `forge check`.

1. **Build setup.**
   - Versions and plugins: Kotlin 2.4.21, the Compose compiler, serialization, KSP and the Room plugin with
     `schemaDirectory`.
   - `unitTests.isIncludeAndroidResources = true`.
   - Add the new dependencies, remove `appcompat` and `material`, and regenerate the lockfile.
   - Delete the example tests.
   - Add `.editorconfig`: 120-character lines, `ij_kotlin_allow_trailing_comma_on_call_site = false` as `CLAUDE.md`
     says, and the `ktlint_official` code style.
2. **App shell.**
   - `CheckPilotApplication`, `MainActivity`, the theme, Navigation 3 with a `Home` key and the empty home screen.
   - In `Clock`, `MainModules` and `SmokeTest`, the harness without the database.
   - `allowBackup="false"`, and backup and data-extraction rules that exclude every domain for cloud backup and device
     transfer.
   - Check the merged manifest has no `INTERNET` permission.
3. **Data layer.**
   - The entities, the database, the seed callback, the key provider and cipher, the DataStore `single`, and their
     tests.
   - The harness gains the database and DataStore overrides and `restart()`.
   - `MainModulesVerifyTest`.
   - `EncryptionAtRestTest`.
4. **Repo tooling.**
   - `CLAUDE.md`: `spotlessApply` and `spotlessCheck` instead of `ktlintFormat` and `ktlintCheck`, Navigation 3
     instead of Navigation Compose, and the `ClockFake`/`AppHarness` names in its Tests section.
   - `.githooks/pre-commit` runs `spotlessApply` and stages again the files that were staged.
   - `.githooks/pre-push` runs `spotlessCheck`.
   - `docs/knowledge-base/README.md`:
     - the assumed device (a recent Pixel or Samsung on Android 15+, until the owner names it)
     - Robolectric 4.18.1 runs SDK 37 and needs JDK 21+
     - SQLCipher's native library doesn't load on the JVM, so JVM tests use plain SQLite
     - Robolectric has no Android Keystore
   - Turn the three ktlint rules back on, now that the template code is gone, and fix what they report.

## Decided

1. **Kotlin 2.4.21** over AGP's bundled 2.2.10, with the fallback above.
2. **The three ktlint rules are back on**: only the template code needed them off.
3. **The pre-commit hook stages again what it formats.** A file that was only partly staged gets staged whole.
4. **`createEmptyComposeRule()` plus a harness-owned `ActivityScenario`** instead of `createAndroidComposeRule`, so
   restarts are possible. The technical plan's Testing section gets the same one-line change.
