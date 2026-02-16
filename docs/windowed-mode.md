# Windowed Mode Support

## Supported Window Bounds

- Minimum supported width: **320dp**
- Minimum supported height: **320dp**
- Maximum validated bounds: **1200dp x 1200dp**

These bounds target phone split-screen, freeform windows, and larger tablet
windows while keeping calculator input usable.

## Width Breakpoints

- `compact`: `< 600dp`
  - Uses phone-optimized layout behavior.
- `medium`: `600dp .. 839dp`
  - Uses tablet-style calculator layout alias.
- `expanded`: `>= 840dp`
  - Uses tablet-style calculator layout alias with additional space headroom.

## Resource Mapping

- `values-w600dp-port` and `values-w840dp-port` map `activity_calculator` to
  the tablet layout alias for medium/expanded portrait windows.
- `values-w320dp/dimens.xml` adjusts pad page margin for constrained windows to
  reduce clipping risk.

## Validation Strategy

- Screenshot tests cover compact, medium, and expanded window-like qualifiers.
- Instrumentation tests verify core interactions remain functional after
  lifecycle changes in windowed scenarios.
