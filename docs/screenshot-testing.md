# Screenshot Testing (Legacy Views)

This project uses Roborazzi + Robolectric to capture and verify screenshots for
the pre-Compose calculator UI.

## Commands

- Record/update baselines:
  - `./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true --no-daemon`
- Verify against checked-in baselines:
  - `./gradlew :app:testDebugUnitTest -Proborazzi.test.verify=true --no-daemon`

## Baseline Coverage

`CalculatorLegacyScreenshotTest` captures:

- phone portrait/light
- phone landscape/light
- tablet portrait/light
- compact windowed/light
- phone portrait/dark
- tablet portrait/dark
- phone portrait/large-font

## Determinism

The test suite enforces deterministic rendering by:

- fixing locale to `en-US`
- fixing timezone to `UTC`
- disabling animation scales
- using fixed Robolectric device qualifiers per scenario

## Review Workflow

1. Run `recordRoborazziDebug` when intentional UI changes are made.
   `./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true --no-daemon`
2. Review changed images under `app/src/test/screenshots/`.
3. Run `testDebugUnitTest -Proborazzi.test.verify=true` before pushing.
