# Quick Settings + Floating Surface Feasibility

## Platform Constraints

- Quick Settings tiles are supported through `TileService` (API 24+).
- Tile launch APIs differ by SDK:
  - API 34+: `startActivityAndCollapse(PendingIntent)`
  - older: deprecated `startActivityAndCollapse(Intent)` fallback
- Third-party apps can launch activities from a tile, but cannot present
  privileged SystemUI dialogs.

## Overlay/Floating Constraints

- Floating calculator over other apps requires `SYSTEM_ALERT_WINDOW`.
- Overlay behavior can be blocked by user policy, OEM restrictions, or app-op
  denial.
- Reliable foreground floating UX generally requires a foreground service.

## Deployment Decision

- Chosen model: **user-installable app with graceful fallback**.
  - Baseline behavior: tile launches calculator activity immediately.
  - Optional future behavior: overlay/floating mode only when overlay permission
    is explicitly granted.

## Current Implementation Scope

- Implemented: tile entry point, tile state updates, launch flow to calculator.
- Deferred: floating overlay surface and lockscreen/work-profile edge handling.
