# Screenshot Testing

## Commands

- Record/update baselines:
  - `./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true --no-daemon`
- Verify against checked-in baselines:
  - `./gradlew :app:testDebugUnitTest -Proborazzi.test.verify=true --no-daemon`

## Visual Dashboard

- Open side-by-side comparison dashboard:
  - `xdg-open docs/screenshot-dashboard.html`
- If your browser blocks `file://` image loading, run a local static server:
  - `python3 -m http.server 8080`
  - then open `http://localhost:8080/docs/screenshot-dashboard.html`

## Baseline Coverage

`CalculatorLegacyScreenshotTest` captures legacy View UI scenarios:

- phone portrait/light
- phone landscape/light
- tablet portrait/light
- compact windowed/light
- medium windowed/light
- expanded windowed/light
- phone portrait/dark
- tablet portrait/dark
- phone portrait/large-font

`CalculatorComposeScreenshotTest` captures migrated Compose UI scenarios:

- phone portrait/light initial state
- phone portrait/light evaluated state (`1+2=`)
- phone portrait/dark initial state

## Determinism

The test suite enforces deterministic rendering by:

- fixing locale to `en-US`
- fixing timezone to `UTC`
- disabling animation scales
- using fixed Robolectric device qualifiers per scenario

## Parity Gate

`CalculatorComposeParityReferenceTest` compares Compose portrait baseline output
to the legacy portrait baseline and enforces a normalized pixel diff threshold.

## Review Workflow

1. Run screenshot record when intentional UI changes are made.
   `./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true --no-daemon`
2. Review changed images under `app/src/test/screenshots/`.
3. Run `testDebugUnitTest -Proborazzi.test.verify=true` before pushing.
