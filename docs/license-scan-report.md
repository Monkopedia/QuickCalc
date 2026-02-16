# License Scan Report

- Scan timestamp (UTC): `2026-02-16T18:34:00Z`
- Scope scanned: `upstream/aosp-calculator/` snapshot import
- Method: local artifact and source inspection (no external SBOM service)

## Inputs Scanned
- `upstream/aosp-calculator/NOTICE`
- `upstream/aosp-calculator/MODULE_LICENSE_APACHE2`
- `upstream/aosp-calculator/build.gradle`
- `upstream/aosp-calculator/arity-2.1.2.jar`
- Java sources under `upstream/aosp-calculator/src/`

## Findings
- Upstream NOTICE file is present and matches imported source snapshot.
- Module license marker file is present (`MODULE_LICENSE_APACHE2`).
- All imported Java sources have Apache-2.0 header text (`8/8` files).
- Build file declares:
  - local jar dependency: `files("arity-2.1.2.jar")`
  - Maven dependency: `com.android.support:support-v4:+`

## Unresolved / Follow-up
- `arity-2.1.2.jar` does not contain an embedded LICENSE/NOTICE file; attribution relies on upstream project context and NOTICE file.  
  Action: verify canonical upstream license source for arity artifact before release.
- `com.android.support:support-v4:+` uses a dynamic version and legacy coordinate.  
  Action: pin explicit version during Gradle modernization and capture license in third-party inventory.
- A formal dependency license report plugin is not yet configured for this repository.  
  Action: add automated license report task and CI gate in modernization phases.
