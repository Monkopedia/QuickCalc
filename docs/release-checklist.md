# Release Checklist

## Pre-Release Gates

- [ ] `./gradlew :app:assembleRelease --no-daemon`
- [ ] `./gradlew :app:testDebugUnitTest --no-daemon`
- [ ] `./gradlew :app:connectedDebugAndroidTest --no-daemon`
- [ ] `./gradlew :app:ktlintCheck :app:detektCheck --no-daemon`
- [ ] `./gradlew checkLicenseHeaders --no-daemon`
- [ ] `./gradlew :app:testDebugUnitTest -Proborazzi.test.verify=true --no-daemon`

## Artifact Verification

- [ ] Confirm release APK generated under `app/build/outputs/apk/release/`
- [ ] Verify app launch + basic arithmetic sanity on release build
- [ ] Verify Quick Settings tile launch path

## Compliance and Documentation

- [ ] Confirm provenance/license docs are up to date
- [ ] Confirm TODO/roadmap status reflects current release scope

## Dry-Run Record

- Dry-run date: `2026-02-16`
- Dry-run command status:
  - `./gradlew :app:assembleRelease --no-daemon` -> `PASS`
