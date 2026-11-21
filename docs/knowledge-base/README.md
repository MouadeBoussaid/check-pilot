# Knowledge base

Facts the code doesn't show. Read this before work that depends on devices, Android versions or the test setup.

## Devices in the field

- Assumed until the owner names the device: one recent Pixel or Samsung phone on Android 15 or later.
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

## Formatting

- ktlint runs through Spotless (`spotlessApply`, `spotlessCheck`).
- Spotless doesn't pass `ktlint_function_naming_ignore_when_annotated_with` from `.editorconfig` to ktlint. It is set
  in the root `build.gradle.kts` through `editorConfigOverride` instead. The other `.editorconfig` settings
  (120-character lines, no trailing commas at call sites) do reach ktlint.
