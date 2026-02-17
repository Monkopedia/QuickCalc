# Developer Setup

## Prerequisites

- Android Studio (latest stable) with Android SDK 36 platform + build tools
- JDK 21
- Android Emulator image (recommended: API 34, Pixel 8 Pro)

## Initial Setup

1. Validate Gradle wrapper:
   - `./gradlew --version`
2. Install pre-push hook flow:
   - `git config core.hooksPath .githooks`
3. Confirm project tasks:
   - `./gradlew :app:tasks --no-daemon`

## Core Commands

- Build app:
  - `./gradlew :app:assembleDebug --no-daemon`
- JVM/unit tests:
  - `./gradlew :app:testDebugUnitTest --no-daemon`
- Connected instrumentation tests (requires emulator/device):
  - `./gradlew :app:connectedDebugAndroidTest --no-daemon`
- Kotlin formatting/lint:
  - `./gradlew :app:ktlintFormat :app:ktlintCheck --no-daemon`
- Static analysis:
  - `./gradlew :app:detektCheck --no-daemon`
- License/header validation:
  - `./gradlew checkLicenseHeaders --no-daemon`
- Toolchain minimums (Kotlin/Compose guard):
  - `./gradlew verifyMinimumDependencyVersions --no-daemon`

## Screenshot Tests

- Record/update baselines:
  - `./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true --no-daemon`
- Verify against baselines:
  - `./gradlew :app:testDebugUnitTest -Proborazzi.test.verify=true --no-daemon`

## Recommended Local Gate

- `./scripts/pre-push.sh`
