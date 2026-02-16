# Windowed Multi-Window Validation Checklist

## Focus and Resume/Pause

- Launch calculator in normal mode and verify input responsiveness.
- Put app in background and return; verify formula/result state is still usable.
- Switch focus between two apps repeatedly; verify no crashes or stuck input.

## Split-Screen

- Open calculator in split-screen with another app.
- Resize divider through compact -> medium -> expanded widths.
- Verify buttons remain tappable and display/result text remains visible.

## Rotation in Multi-Window

- Rotate device while in split-screen.
- Verify calculator relayouts correctly and interactions remain functional.

## Current Automation Coverage

- Automated instrumentation coverage exists for lifecycle resume/pause behavior
  (`CalculatorInteractionInstrumentedTest.backgroundForegroundTransitionKeepsAppResponsive`).
- Split-screen divider resizing remains manual due SystemUI window-management
  automation requirements in CI.
