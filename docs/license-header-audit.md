# License Header Audit

- Audit timestamp (UTC): `2026-02-16T18:32:35Z`
- Command: `./scripts/check_license_headers.sh`
- Result: pass

## Summary
- Verified required upstream license artifacts exist:
  - `upstream/aosp-calculator/NOTICE`
  - `upstream/aosp-calculator/MODULE_LICENSE_APACHE2`
- Verified source header policy on scanned files:
  - scanned files: `8`
  - missing headers: `0`

## Policy Used
- A file passes if it contains one of:
  - `Licensed under the Apache License`
  - `SPDX-License-Identifier:`
