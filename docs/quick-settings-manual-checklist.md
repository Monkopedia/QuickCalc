# Quick Settings Manual Test Checklist

## Base Tile Flow

- Add Calculator tile in Quick Settings edit mode.
- Tap tile from unlocked device state.
- Verify calculator activity launches and becomes foreground.

## Lockscreen State

- Lock device, open Quick Settings, tap tile.
- Verify expected unlock/auth flow and eventual calculator launch.

## Work Profile

- Repeat tile launch with work profile enabled.
- Confirm behavior is consistent and no profile policy crash occurs.

## Overlay Permission Denied

- Ensure overlay permission is denied for app.
- Trigger tile launch.
- Verify fallback activity launch still works.

## Regression Sweep

- Rotate device after tile launch and verify app remains stable.
- Move app background/foreground and verify no tile-related crash.
