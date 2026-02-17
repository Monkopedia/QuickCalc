#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required_notice_files=(
  "upstream/aosp-calculator/NOTICE"
  "upstream/aosp-calculator/MODULE_LICENSE_APACHE2"
)

missing_notice=0
for f in "${required_notice_files[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "Missing required license artifact: $f"
    missing_notice=1
  fi
done

if [[ "$missing_notice" -ne 0 ]]; then
  exit 1
fi

mapfile -t candidate_files < <(
  {
    find upstream/aosp-calculator/src -type f \( -name "*.java" -o -name "*.kt" \) 2>/dev/null || true
    find app/src -type f \( -name "*.java" -o -name "*.kt" \) 2>/dev/null || true
    find src -type f \( -name "*.java" -o -name "*.kt" \) 2>/dev/null || true
  } | sort -u
)

if [[ "${#candidate_files[@]}" -eq 0 ]]; then
  echo "No source files found for license header checks."
  exit 0
fi

if command -v rg >/dev/null 2>&1; then
  contains_license_marker() {
    local file="$1"
    rg -q --fixed-strings "Licensed under the Apache License" "$file" ||
      rg -q --fixed-strings "SPDX-License-Identifier:" "$file"
  }
else
  contains_license_marker() {
    local file="$1"
    grep -qF "Licensed under the Apache License" "$file" ||
      grep -qF "SPDX-License-Identifier:" "$file"
  }
fi

missing_headers=()
for f in "${candidate_files[@]}"; do
  if contains_license_marker "$f"; then
    continue
  fi
  missing_headers+=("$f")
done

if [[ "${#missing_headers[@]}" -gt 0 ]]; then
  echo "License header check failed for ${#missing_headers[@]} files:"
  printf ' - %s\n' "${missing_headers[@]}"
  exit 1
fi

echo "License header check passed for ${#candidate_files[@]} files."
