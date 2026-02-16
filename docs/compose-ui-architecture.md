# Compose UI Architecture Plan

This document defines the target architecture for migrating Calculator UI from
Views/XML to Jetpack Compose with behavior parity.

## Top-Level Structure

- `CalculatorActivity` remains the Android entry point.
- A new `CalculatorComposeHost` will be introduced in the activity.
- During migration, a feature flag (or build-time switch) controls View UI vs
  Compose UI rendering.

## State Holder Model

- Introduce `CalculatorUiState` as a single immutable state tree.
  - `formulaText`
  - `resultText`
  - `isError`
  - `isInDegreeMode` (if/when scientific parity requires mode toggle)
  - `activeLayoutMode` (phone/tablet/windowed)
- Introduce `CalculatorUiEvent` sealed interface for user intents:
  - digit/operator pressed
  - delete/clear
  - equals
  - long-press variants (where legacy behavior exists)

## Logic Boundaries

- Keep expression logic in existing engine classes:
  - `CalculatorExpressionTokenizer`
  - `CalculatorExpressionBuilder`
  - `CalculatorExpressionEvaluator`
- Compose layer must not re-implement math parsing/evaluation.
- Introduce a presenter/reducer (`CalculatorUiReducer`) that maps
  `CalculatorUiEvent -> CalculatorUiState` using engine classes.

## Navigation and Screen Boundaries

- No multi-screen navigation is required; model as one route:
  - `CalculatorRoute` (stateful)
  - `CalculatorScreen` (stateless composable UI function)
- Stateless composables for reusable parts:
  - display panel
  - numeric pad
  - operator pad
  - scientific pad/page

## Migration Order

1. Compose host + state/reducer scaffolding.
2. Display panel in Compose.
3. Numeric/operator pad in Compose.
4. Scientific pager/components in Compose.
5. Remove legacy View UI after screenshot parity sign-off.
