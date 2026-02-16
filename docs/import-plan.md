# AOSP Calculator Import Plan

## Source
- Upstream project: `platform/packages/apps/Calculator`
- Host: `https://android.googlesource.com/platform/packages/apps/Calculator`
- Branch: `refs/heads/main`

## Import Strategy
- Strategy: snapshot import into this repository, preserving upstream commit identity in metadata.
- Rationale:
  - Fastest path to get all source in-tree for Kotlin/Compose migration work.
  - Keeps this repository independent while retaining auditable upstream provenance.

## Planned Steps
1. Fetch upstream snapshot for `refs/heads/main`.
2. Copy all project files into `QuickCalc/`, excluding upstream VCS metadata.
3. Add `docs/upstream-provenance.md` containing:
   - source URL
   - imported branch
   - imported commit hash
   - import timestamp (UTC)
4. Copy upstream licensing/notice files and retain per-file license headers.
5. Record known import deltas (path moves, removed build files, tooling upgrades).

## Verification Checklist
- Repository contains all upstream source directories and resources.
- `docs/upstream-provenance.md` exists and includes exact revision details.
- Root and module license/notice files are present.
- Sample spot-check confirms source file license headers are intact.

## Follow-on Work
- Modernize Gradle/AGP/Kotlin tooling after baseline import.
- Build behavioral parity tests before major refactors.
