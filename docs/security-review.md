# Security and Privacy Review

## Scope

Review covers Quick Settings tile launch flow, current app manifest exposure,
and deferred floating overlay behavior.

## Entry Points and Exposure

- Launcher activity `Calculator` is exported by design (`MAIN` + `LAUNCHER`).
- Compose activity `CalculatorComposeActivity` is non-exported.
- Tile service `CalculatorTileService` is exported, but protected by
  `android.permission.BIND_QUICK_SETTINGS_TILE`.

## Current Risk Assessment

- No sensitive user data is persisted or transmitted by current calculator flow.
- Tile launch intent is explicit (`Intent(this, Calculator::class.java)`) and
  uses immutable/update-current pending intent on API 34+.
- Overlay/floating behavior is not enabled yet; this avoids current
  `SYSTEM_ALERT_WINDOW` attack-surface expansion.

## Hardening Notes

- Keep overlay implementation behind explicit user consent and runtime checks.
- Require a dedicated threat model before enabling always-on-top surfaces.
- Maintain `android:exported="false"` for internal-only activities/services.
- Keep lockscreen/work-profile behavior in manual regression checklist.

## Status

- No high-severity findings for current implementation scope.
- Main outstanding security work is the deferred overlay permission/service
  design and review.
