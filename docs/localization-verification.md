# Localization and Number Formatting Verification

## Scope

Validation focuses on expression localization/normalization behavior in
`CalculatorExpressionTokenizer`.

## Assumptions

- Internal evaluator expression format is canonical ASCII (for example
  `3.14`, `1/2*3-4`).
- UI may show locale-specific decimal separator, digits, and operator strings.
- Round-trip behavior must hold:
  - canonical -> localized -> canonical

## Automated Coverage

- Existing operator/infinity checks:
  - `CalculatorExpressionTokenizerTest`
- Added locale-specific coverage:
  - `CalculatorLocalizationBehaviorTest.frenchLocaleUsesDecimalCommaAndRoundTripsToCanonical`
  - `CalculatorLocalizationBehaviorTest.persianLocaleUsesLocalizedDigitsAndRoundTripsToCanonical`

## Result

- French locale correctly localizes decimal separator to comma and normalizes
  back to canonical decimal dot.
- Persian locale (`values-fa` enables localized digits) localizes digits and
  still normalizes back to canonical expression safely.

## Follow-ups

- Extend coverage when Compose UI introduces locale-aware formatting in
  additional display fields (history/memory/advanced functions).
