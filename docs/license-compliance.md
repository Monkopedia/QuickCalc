# License Compliance Guide

## Upstream and Scope
- Upstream source: `https://android.googlesource.com/platform/packages/apps/Calculator`
- Imported snapshot location: `upstream/aosp-calculator/`
- Provenance record: `docs/upstream-provenance.md`

## License Baseline
- The imported AOSP Calculator sources are Apache-2.0 licensed.
- Upstream license artifacts retained in-tree:
  - `upstream/aosp-calculator/NOTICE`
  - `upstream/aosp-calculator/MODULE_LICENSE_APACHE2`
- Imported Java source files retain upstream Apache-2.0 headers.

## Project Obligations
- Preserve existing upstream copyright and license headers.
- Keep NOTICE attributions with redistributed builds.
- Do not remove third-party attributions without legal review.
- When adding new code, include an approved header policy (Apache-2.0 or project-standard SPDX header once finalized).

## Dependency and Notice Process
- For each added dependency, record:
  - artifact coordinates and version
  - license type
  - whether attribution/NOTICE updates are required
- Update NOTICE/third-party attribution docs before release when required.

## Verification Checklist
- Run license/header checks before merge (`./gradlew checkLicenseHeaders` once task is added).
- Ensure `docs/upstream-provenance.md` matches imported revision.
- Confirm no files with required headers are missing them.
- Confirm any new third-party libs are documented with license terms.
