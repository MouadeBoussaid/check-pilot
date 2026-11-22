# Knowledge base

Facts the code doesn't show. Read this before work that depends on devices, Android versions or the test setup, and
before UI design work.

## Devices in the field

- The app runs mainly on a Samsung Galaxy S23 Ultra, on the latest Android version it gets: Android 17 (API 37) with
  One UI 9.0. The owner named the device in November 2026.
- One UI 9 is expected to be the S23 series' last major upgrade, so this device is likely to stay on Android 17.
- `minSdk` is 26 and `targetSdk` is 37, so the app can run on older devices too, but nobody tests it on them.

## SDK levels

- `compileSdk` is 37.1 (minor version 1), because Compose 1.13 needs it. The technical plan doesn't name it.

## JVM tests (Robolectric)

- Robolectric 4.18.1 runs the tests on SDK 37 (`app/src/test/resources/robolectric.properties`) and needs JDK 21 or
  later to do so.
- SQLCipher's native library doesn't load on the JVM. JVM tests use a plain SQLite database through the framework
  open-helper factory. Only the instrumented `EncryptionAtRestTest` checks the encryption.
- Robolectric has no Android Keystore. JVM tests use `DatabaseKeyProviderFake` / `PassphraseCipherFake`, and
  `KeystorePassphraseCipherImpl` only runs on a device or emulator.
- Robolectric doesn't run androidx.startup initializers, and gives each test a new `Application` while
  `AppInitializer` keeps the first one. `AppHarness` clears `AppInitializer`'s instance and runs
  `InitializationProvider` once per test, so `ProcessLifecycleOwner` works.
- `ProcessLifecycleOwner` reports `ON_STOP` 700 ms after the last activity stops. The harness idles the main looper
  for 1 s after sending the app to the background.

## Design skills

- Two design skills can load for UI work, and both trigger on any screen task, so each has one job:
  - `frontend-design` owns the visual direction: palette, typefaces, layout and what makes the app distinct. Its
    token plan becomes the Compose theme in `ui/generic/`, which stays the only source of truth for tokens.
  - `ui-ux-pro-max` is reference and checklist only: `--domain ux` and `--stack jetpack-compose` lookups and the app
    Pre-Delivery Checklist (accessibility, touch targets, contrast).
- Skip `ui-ux-pro-max`'s `--design-system` step, even where its skill calls it required. Its keyword-picked palette and
  font pairing compete with `frontend-design`'s token plan. If it runs anyway, its output is one input that
  `frontend-design`'s review pass critiques, and it is never persisted (no `design-system/` folder in the repo).
- `ui-ux-pro-max` doesn't detect Android from Gradle files. Always pass `--stack jetpack-compose`.
- Its web-only checklist items don't apply: `cursor-pointer`, the 375–1440px breakpoints, and Heroicons/Lucide (use
  Material icons).

## Formatting

- ktlint runs through Spotless (`spotlessApply`, `spotlessCheck`).
- Spotless doesn't pass `ktlint_function_naming_ignore_when_annotated_with` from `.editorconfig` to ktlint. It is set
  in the root `build.gradle.kts` through `editorConfigOverride` instead. The other `.editorconfig` settings
  (120-character lines, no trailing commas at call sites) do reach ktlint.
