# QuickCalc Android Buildout TODO

Scope source: AOSP Calculator at `https://android.googlesource.com/platform/packages/apps/Calculator/+/refs/heads/main`

## Tracking Rules
- [ ] Keep this file updated as the source of truth.
- [ ] Use small, focused commits tied to one checklist item each.
- [ ] For each completed item, record validation commands in commit/PR notes.
- [ ] Mark blocked items as `[BLOCKED: reason]` and continue with the next unblocked item.

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
- [ ] Kotlin plugin. [BLOCKED: AGP 9 provides built-in Kotlin support; no separate Kotlin Android plugin is required.]
- [x] AndroidX/Compose BOM (where applicable).
- [x] Configure compile SDK and target SDK to latest stable API level.
- [x] Add build variants (`debug`, `release`) and signing placeholders.
- [x] Make baseline builds pass:
- [x] `./gradlew :app:assembleDebug`
- [x] `./gradlew :app:assembleRelease`
- [ ] Fix all build warnings that indicate future incompatibility or deprecation risk. [BLOCKED: AOSP legacy Java uses deprecated framework APIs; full cleanup is coupled to Kotlin migration/Compose port to avoid risky behavior drift.]
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
- [ ] Freeze behavior via tests before conversion of each package.
- [ ] Convert production Java to Kotlin in small batches by feature area.
- [ ] Convert remaining Java unit/instrumentation tests to Kotlin.
- [ ] Remove Java-only utility patterns; replace with idiomatic Kotlin equivalents.
- [ ] Enable strict Kotlin compiler checks (nullability and warning escalation as appropriate).
- [ ] Add static quality tools for Kotlin (`ktlint` with Android style, `detekt`, optional binary compatibility checks).
- [ ] Configure `.editorconfig` for `ktlint` Android style (`ktlint_code_style=android_studio`).
- [ ] Add `ktlintCheck` and `ktlintFormat` tasks at root and module level using Android style rules.
- [ ] Enforce `ktlintCheck` in CI and pre-push validation flow.
- [ ] Remove all Java sources from active modules.
- [ ] Verify full test suite parity after each conversion step.

## Phase 5: Screenshot Baseline for Existing (Pre-Compose) UI (P0)
- [ ] Add screenshot test infrastructure for current XML/View UI.
- [ ] Capture baseline screenshots across representative devices/orientations:
- [ ] phone portrait
- [ ] phone landscape
- [ ] tablet/windowed sizes
- [ ] dark/light themes
- [ ] large font/scaling accessibility settings
- [ ] Add deterministic rendering setup (fonts, locale, animations disabled, fixed clock if needed).
- [ ] Gate UI regressions with image diff thresholds and review workflow.

## Phase 6: Jetpack Compose UI Port with Parity Gates (P0)
- [ ] Define UI architecture for Compose (state holders, events, navigation boundaries).
- [ ] Port screens/components incrementally from Views to Compose.
- [ ] Preserve behavior and interaction parity using baseline tests.
- [ ] Add Roborazzi-based screenshot tests for each migrated screen/state.
- [ ] Compare Compose screenshots against pre-Compose reference outputs.
- [ ] Resolve spacing/typography/elevation differences until within accepted diff threshold.
- [ ] Remove legacy XML/View code only after parity sign-off.

## Phase 7: Instrumentation/E2E Testing (Comprehensive) (P0)
- [ ] Build instrumentation smoke suite for app launch, core input, and result rendering.
- [ ] Add comprehensive interaction tests for:
- [ ] all digit/operator paths
- [ ] orientation changes and process recreation
- [ ] background/foreground lifecycle transitions
- [ ] copy/paste and accessibility actions
- [ ] Add Compose UI tests for migrated screens.
- [ ] Add macrobenchmark/startup/perf tests for critical flows.
- [ ] Ensure connected tests run in CI for at least one stable emulator image.
- [ ] Provide one-command execution:
- [ ] `./gradlew :app:connectedDebugAndroidTest`

## Phase 8: Resizable/Windowed Calculator Mode (P1)
- [ ] Define supported minimum and maximum window sizes.
- [ ] Implement adaptive layout breakpoints for compact/medium/expanded widths.
- [ ] Ensure calculator remains usable at reduced scale (touch targets, readability, clipping).
- [ ] Add screenshot and instrumentation tests for each window class.
- [ ] Validate multi-window behavior (focus, resume/pause, split-screen).

## Phase 9: Quick Settings Tile + Floating Calculator Surface (P1)
- [ ] Validate platform and permission constraints for launching calculator from Quick Settings tile.
- [ ] Decide deployment model: privileged/system app only vs user-installable fallback.
- [ ] Implement `TileService` entry point and tile state updates.
- [ ] Implement launch flow from tile into quick, lightweight calculator surface.
- [ ] Implement floating-on-top behavior where permitted by platform policy.
- [ ] Add fallback behavior when overlay/floating is not permitted.
- [ ] Add instrumentation tests for tile tap -> calculator launch flow.
- [ ] Add manual test checklist for lockscreen, work profile, and permission-denied states.

## Phase 10: CI, Quality Gates, and Developer Experience (P0)
- [ ] Add CI pipelines for:
- [ ] build
- [ ] unit tests
- [ ] lint/static analysis
- [ ] license/header linting
- [ ] screenshot tests
- [ ] instrumentation tests
- [ ] Enforce fail-fast quality gates (no lint/test/screenshot regressions on main).
- [ ] Add pre-commit or pre-push validation shortcuts.
- [ ] Publish `docs/dev-setup.md` with exact local setup and emulator requirements.

## Phase 11: Cleanup, Hardening, and Release Readiness (P0)
- [ ] Remove dead code and temporary migration shims.
- [ ] Verify accessibility (TalkBack labels, contrast, focus order, large text).
- [ ] Verify localization behavior and number formatting assumptions.
- [ ] Run security/privacy review for any overlay/tile behavior.
- [ ] Create `docs/release-checklist.md` and complete dry-run release.
- [ ] Tag first milestone release after all P0 gates pass.

## Definition of Done
- [ ] All P0 checklist items completed.
- [ ] 100% Kotlin for production and tests in active app modules.
- [ ] Non-UI and instrumentation suites are green in CI.
- [ ] Compose UI screenshots are approved and close to legacy baseline.
- [ ] Windowed mode works within defined size classes.
- [ ] Quick Settings launch path works per approved platform constraints.
- [ ] Licensing/provenance documentation is complete and auditable.
