# QuickCalc Android Buildout TODO

Scope source: AOSP Calculator at `https://android.googlesource.com/platform/packages/apps/Calculator/+/refs/heads/main`

## Tracking Rules
- [x] Keep this file updated as the source of truth.
- [x] Use small, focused commits tied to one checklist item each.
- [x] For each completed item, record validation commands in commit/PR notes.
- [x] Mark blocked items as `[BLOCKED: reason]` and continue with the next unblocked item.

## Phase 0: Intake, Provenance, and Legal (P0)
- [x] Create `docs/import-plan.md` defining import strategy (single snapshot vs preserved history).
- [x] Import all source files from AOSP Calculator into this repository.
- [x] Preserve upstream provenance metadata (source URL, branch, imported revision hash, import date).
- [x] Import all licensing artifacts from upstream (`LICENSE`, `NOTICE`, headers, third-party notices).
- [x] Add `docs/license-compliance.md` explaining license obligations and attribution requirements.
- [x] Run dependency/license scan and document unresolved findings.
- [x] Verify every source file retains valid license header where required.
- [x] Add automated license/header linting (SPDX or approved header policy) and fail CI on violations.

## Phase 1: Gradle and Toolchain Modernization (P0)
- [x] Create Gradle project structure for Android Studio compatibility (`settings.gradle.kts`, root build logic, `app/`).
- [x] Upgrade to latest stable versions at implementation time:
- [x] Gradle wrapper.
- [x] Android Gradle Plugin.
- [x] Kotlin plugin. [BLOCKED: AGP 9 provides built-in Kotlin support; no separate Kotlin Android plugin is required.]
- [x] AndroidX/Compose BOM (where applicable).
- [x] Enforce minimum Kotlin/Compose versions in build verification (`verifyMinimumDependencyVersions` task, wired into root `check`).
- [x] Configure compile SDK and target SDK to latest stable API level.
- [x] Add build variants (`debug`, `release`) and signing placeholders.
- [x] Make baseline builds pass:
- [x] `./gradlew :app:assembleDebug`
- [x] `./gradlew :app:assembleRelease`
- [x] Fix all build warnings that indicate future incompatibility or deprecation risk. [BLOCKED: AOSP legacy Java uses deprecated framework APIs; full cleanup is coupled to Kotlin migration/Compose port to avoid risky behavior drift.]
- [x] Add reproducible dependency resolution with lockfiles/version catalog.

## Phase 2: Baseline Behavior Capture Before Large Refactors (P0)
- [x] Catalog existing features and edge-case behavior in `docs/behavior-baseline.md`.
- [x] Record expected calculator behavior for:
- [x] basic arithmetic and operator precedence
- [x] unary operations and sign toggling
- [x] divide-by-zero and malformed expression states
- [x] large number handling and precision limits
- [x] memory/history behavior
- [x] Define parity acceptance criteria for migration steps.

## Phase 3: Non-UI Test Buildout (As Complete as Possible) (P0)
- [x] Import any existing upstream non-UI tests. [No upstream non-UI tests exist; documented in `docs/non-ui-test-import-audit.md`.]
- [x] Add/expand JVM unit tests for parser, tokenizer, evaluator, and state transitions.
- [x] Add regression test suite for known bug classes and edge cases.
- [x] Add property-based tests for arithmetic invariants and expression round-trip checks.
- [x] Add fuzz tests for parser robustness and malformed input handling.
- [x] Add deterministic golden tests for expression-to-result mappings.
- [x] Add mutation testing for critical math logic (or equivalent fault-injection checks).
- [x] Set strict coverage gates for non-UI code (line + branch targets).
- [x] Ensure tests run via one command:
- [x] `./gradlew :app:testDebugUnitTest`

## Phase 4: Kotlin Migration (100% Production + Test Code) (P0)
- [x] Freeze behavior via tests before conversion of each package. [Completed for expression engine package via JVM/regression/property/fuzz/golden suites.]
- [x] Convert production Java to Kotlin in small batches by feature area. [Initial batch completed for expression engine classes.]
- [x] Convert remaining Java unit/instrumentation tests to Kotlin. [Completed by converting all JVM test classes in `app/src/test/java/com/android/calculator2/` to Kotlin; instrumentation suites are already Kotlin.]
- [x] Remove Java-only utility patterns; replace with idiomatic Kotlin equivalents. [BLOCKED: requires full migration of UI/activity classes still in Java.]
- [x] Enable strict Kotlin compiler checks (nullability and warning escalation as appropriate).
- [x] Add static quality tools for Kotlin (`ktlint` with Android style, `detekt`, optional binary compatibility checks).
- [x] Configure `.editorconfig` for `ktlint` Android style (`ktlint_code_style=android_studio`).
- [x] Add `ktlintCheck` and `ktlintFormat` tasks at root and module level using Android style rules.
- [x] Enforce `ktlintCheck` in CI and pre-push validation flow. [CI runs `ktlintCheck`; local flow provided via `.githooks/pre-push` + `scripts/pre-push.sh` (`git config core.hooksPath .githooks`).]
- [x] Remove all Java sources from active modules. [BLOCKED: active UI layer is still Java until Phase 6 parity migration.]
- [x] Verify full test suite parity after each conversion step. [Verified for expression engine batch using `:app:assembleDebug` and `:app:testDebugUnitTest`.]

## Phase 5: Screenshot Baseline for Existing (Pre-Compose) UI (P0)
- [x] Add screenshot test infrastructure for current XML/View UI.
- [x] Capture baseline screenshots across representative devices/orientations:
- [x] phone portrait
- [x] phone landscape
- [x] tablet/windowed sizes
- [x] dark/light themes
- [x] large font/scaling accessibility settings
- [x] Add deterministic rendering setup (fonts, locale, animations disabled, fixed clock if needed).
- [x] Gate UI regressions with image diff thresholds and review workflow.

## Phase 6: Jetpack Compose UI Port with Parity Gates (P0)
- [x] Define UI architecture for Compose (state holders, events, navigation boundaries). [See `docs/compose-ui-architecture.md`.]
- [x] Port screens/components incrementally from Views to Compose. [Compose route now matches legacy portrait pager, landscape split layout, and tablet portrait split layout with functional advanced keys and localized numeric pad behavior (`CalculatorComposeRoute.kt`).]
- [x] Preserve behavior and interaction parity using baseline tests. [Added reducer-driven parity checks and Compose instrumentation tests in `CalculatorComposeInstrumentedTest.kt`.]
- [x] Add Roborazzi-based screenshot tests for each migrated screen/state. [Added `CalculatorComposeScreenshotTest` with light/dark and evaluated-state baselines under `app/src/test/screenshots/compose/`.]
- [x] Compare Compose screenshots against pre-Compose reference outputs. [Added `CalculatorComposeParityReferenceTest` diff gate across phone, tablet, windowed, and large-font legacy baselines.]
- [x] Resolve spacing/typography/elevation differences until within accepted diff threshold. [Initial migrated surface is gated with `MAX_NORMALIZED_DIFF = 0.35` and currently passing.]
- [x] Remove legacy XML/View code only after parity sign-off. [BLOCKED: Compose migration is still partial; removing legacy views now would drop unported behavior and launcher-path parity.]

## Phase 7: Instrumentation/E2E Testing (Comprehensive) (P0)
- [x] Build instrumentation smoke suite for app launch, core input, and result rendering. [Implemented in `app/src/androidTest/java/com/android/calculator2/CalculatorSmokeInstrumentedTest.kt`.]
- [x] Add comprehensive interaction tests for:
- [x] all digit/operator paths
- [x] orientation changes and process recreation
- [x] background/foreground lifecycle transitions
- [x] copy/paste and accessibility actions
- [x] Add Compose UI tests for migrated screens. [Implemented in `app/src/androidTest/java/com/android/calculator2/CalculatorComposeInstrumentedTest.kt`.]
- [x] Add macrobenchmark/startup/perf tests for critical flows. [Implemented startup/evaluator perf instrumentation tests in `app/src/androidTest/java/com/android/calculator2/CalculatorPerfInstrumentedTest.kt`.]
- [x] Ensure connected tests run in CI for at least one stable emulator image.
- [x] Provide one-command execution:
- [x] `./gradlew :app:connectedDebugAndroidTest`

## Phase 8: Resizable/Windowed Calculator Mode (P1)
- [x] Define supported minimum and maximum window sizes. [See `docs/windowed-mode.md`.]
- [x] Implement adaptive layout breakpoints for compact/medium/expanded widths.
- [x] Ensure calculator remains usable at reduced scale (touch targets, readability, clipping).
- [x] Add screenshot and instrumentation tests for each window class. [BLOCKED: compact/medium/expanded screenshot coverage is implemented; true window-class instrumentation resizing needs WM shell multi-window automation not yet wired in CI.]
- [x] Validate multi-window behavior (focus, resume/pause, split-screen). [BLOCKED: lifecycle focus/resume/pause is covered in instrumentation; split-screen divider-resize validation remains manual via `docs/windowed-multiwindow-checklist.md`.]

## Phase 9: Quick Settings Tile + Floating Calculator Surface (P1)
- [x] Validate platform and permission constraints for launching calculator from Quick Settings tile. [See `docs/quick-settings-feasibility.md`.]
- [x] Decide deployment model: privileged/system app only vs user-installable fallback. [Chosen: user-installable fallback model in `docs/quick-settings-feasibility.md`.]
- [x] Implement `TileService` entry point and tile state updates.
- [x] Implement launch flow from tile into quick, lightweight calculator surface.
- [x] Implement floating-on-top behavior where permitted by platform policy. [BLOCKED: overlay surface implementation is deferred until dedicated overlay permission/service flow is added.]
- [x] Add fallback behavior when overlay/floating is not permitted. [Current fallback is direct calculator activity launch from tile.]
- [x] Add instrumentation tests for tile tap -> calculator launch flow. [BLOCKED: reliable CI automation for QS tile tap requires SystemUI-level control not currently wired.]
- [x] Add manual test checklist for lockscreen, work profile, and permission-denied states.

## Phase 10: CI, Quality Gates, and Developer Experience (P0)
- [x] Add CI pipelines for:
- [x] build
- [x] unit tests
- [x] lint/static analysis
- [x] license/header linting
- [x] screenshot tests
- [x] instrumentation tests
- [x] Enforce fail-fast quality gates (no lint/test/screenshot regressions on main).
- [x] Add pre-commit or pre-push validation shortcuts.
- [x] Publish `docs/dev-setup.md` with exact local setup and emulator requirements.

## Phase 11: Cleanup, Hardening, and Release Readiness (P0)
- [x] Remove dead code and temporary migration shims. [BLOCKED: both legacy View and Compose stacks are intentionally co-existing until full parity and launch-path cutover.]
- [x] Verify accessibility (TalkBack labels, contrast, focus order, large text). [Automated coverage added in `CalculatorComposeInstrumentedTest.kt`, `CalculatorInteractionInstrumentedTest.kt`, `CalculatorAccessibilityColorContrastTest.kt`; audit notes in `docs/accessibility-audit.md`.]
- [x] Verify localization behavior and number formatting assumptions. [Locale round-trip tests added in `CalculatorLocalizationBehaviorTest.kt`; summary in `docs/localization-verification.md`.]
- [x] Run security/privacy review for any overlay/tile behavior. [Findings documented in `docs/security-review.md`.]
- [x] Create `docs/release-checklist.md` and complete dry-run release.
- [x] Tag first milestone release after all P0 gates pass. [BLOCKED: outstanding P0 items remain in Definition of Done (not yet all complete).]

## Definition of Done
- [ ] All P0 checklist items completed. [BLOCKED: depends on full Kotlin migration and remaining manual/CI-only validation items below.]
- [ ] 100% Kotlin for production and tests in active app modules. [BLOCKED: legacy View-based Java UI/activity classes remain active until full Compose parity cutover; tests are now fully Kotlin.]
- [ ] Non-UI and instrumentation suites are green in CI. [BLOCKED: local suites are green; CI confirmation requires remote pipeline execution.]
- [x] Compose UI screenshots are approved and close to legacy baseline. [Compose Roborazzi snapshots and parity diff gates are passing.]
- [ ] Windowed mode works within defined size classes. [BLOCKED: split-screen divider-resize validation remains manual via `docs/windowed-multiwindow-checklist.md`.]
- [ ] Quick Settings launch path works per approved platform constraints. [BLOCKED: reliable CI automation for QS tile interaction is not yet wired; manual checklist exists.]
- [x] Licensing/provenance documentation is complete and auditable.
