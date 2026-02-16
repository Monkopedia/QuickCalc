# Behavior Baseline (Imported AOSP Calculator)

Baseline source:
- `upstream/aosp-calculator/` imported from `refs/heads/main`
- Runtime module: `app/`

## Feature Inventory
- Core arithmetic: `+`, `-`, `*`, `/`
- Advanced ops/constants: `sin`, `cos`, `tan`, `ln`, `log`, `sqrt`, `^`, `!`, `π`, `e`
- Parentheses: `(`, `)`
- Decimal input with locale-aware decimal separator
- Delete (`del`) and clear (`clr`)
- Equals (`=`) evaluation
- Multi-layout UI behavior:
  - portrait phone: numeric/operator + paged advanced pad
  - landscape phone: numeric/operator/advanced visible together
  - tablet variants: advanced row plus numeric/operator row

## Input and Editing Rules
- Expression input is normalized before evaluation (localized symbols -> internal symbols).
- Leading `+`, `*`, `/` are blocked.
- Repeated operator sequences are collapsed near cursor end (for example, prevents `++`, `**`, `//`).
- `-` is allowed with additional guard to prevent `--` / `+-` near cursor end.
- Multiple decimal points in the same numeric segment are blocked.
- Function button taps append function token plus `(` (for example `sin(`).
- Delete removes one trailing character; long-press delete triggers full clear.

## Evaluation Rules and Error States
- Trailing arithmetic operators are trimmed before evaluation (for example `1+` evaluates as `1`).
- Empty or simple numeric expressions return without error.
- Evaluator uses `org.javia.arity.Symbols`.
- Result formatting:
  - max shown significant digits: `12`
  - uses guard-digit rounding via `Util.doubleToString(..., 12, 5)`
- `NaN` results map to `R.string.error_nan` (`Not a number`).
- Parse/eval syntax errors map to `R.string.error_syntax` (`Error`).
- Localized display mapping includes Infinity token localization.

## State and Interaction Behavior
- States: `INPUT`, `EVALUATE`, `RESULT`, `ERROR`.
- `=` only triggers evaluation in `INPUT` state.
- In `RESULT`/`ERROR`, delete is hidden and clear is shown.
- Successful `=` animation ends with formula replaced by result text.
- Error on evaluate reveals error color and shows error text in result field.
- Back button on advanced page returns to main page first; exits app from main page.
- Saved instance state persists current state + normalized expression.

## Expected Behavior Matrix (Parity Targets)
- Basic arithmetic and precedence:
  - `1+2*3 -> 7`
  - `(1+2)*3 -> 9`
- Unary/sign behavior:
  - `-5+2 -> -3`
  - `1+-2 -> -1` (operator cleanup rules apply)
- Divide-by-zero/malformed:
  - `0/0 -> Not a number`
  - `1/( -> Error`
- Precision/large values:
  - Display rounded to 12 significant digits.
  - Infinity token remains localized for display.
- Memory/history:
  - No memory register or persisted history behavior exists in imported baseline.

## Migration Parity Acceptance Criteria
- Functional parity:
  - All matrix cases above produce equivalent outcomes (value or error class).
  - Input sanitization/operator-collapsing behavior is preserved.
- UI behavior parity:
  - Delete/clear visibility transitions match current state machine.
  - Advanced-page/back-button behavior is preserved.
- Formatting parity:
  - Localized symbols/digits and decimal separator behavior are equivalent.
  - Significant-digit rounding behavior remains consistent.
