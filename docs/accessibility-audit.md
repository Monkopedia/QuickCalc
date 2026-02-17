# Accessibility Verification

## Scope

This audit covers the currently shipped legacy View UI (`Calculator`) and the
incremental Compose UI surface (`CalculatorComposeActivity`).

## Automated Checks

- TalkBack click/action wiring for legacy controls:
  - `CalculatorInteractionInstrumentedTest.keyButtonsExposeAccessibilityClickAction`
- TalkBack labels for legacy operator/equal/delete-clear buttons:
  - `CalculatorInteractionInstrumentedTest.operatorButtonsExposeExpectedTalkbackLabels`
- Compose accessibility labels + touch targets (48dp minimum):
  - `CalculatorComposeInstrumentedTest.primaryButtonsExposeTalkbackLabelAndTouchTarget`
- Contrast validation for display/pad text colors:
  - `CalculatorAccessibilityColorContrastTest`
  - Thresholds: `3.0:1` for large display text, `4.5:1` for pad button text.
- Large text rendering baseline (legacy UI):
  - `app/src/test/screenshots/legacy/phone_portrait_large_font.png`

## Manual Checks

- Verify TalkBack focus navigation order in portrait and landscape.
- Verify accessibility announcements after orientation change and process
  recreation.
- Re-run checks when Compose replaces additional legacy screens/components.

## Current Status

- Automated checks listed above are in CI (`:app:testDebugUnitTest` and
  `:app:connectedDebugAndroidTest`).
- Focus order remains a manual checklist item until full Compose migration is
  complete.
