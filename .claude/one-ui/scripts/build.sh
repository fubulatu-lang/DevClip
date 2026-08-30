#!/usr/bin/env bash
# Builds one standalone zip per skill for uploading to claude.ai.
# Each zip has SKILL.md at its root plus a copy of the shared reference files,
# so it works without the rest of the repo.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
rm -rf dist build-tmp
mkdir -p dist

for dir in skills/*/; do
  name="$(basename "$dir")"
  staging="build-tmp/$name"
  mkdir -p "$staging/reference"
  cp "$dir/SKILL.md" "$staging/SKILL.md"
  cp reference/TOKENS.md reference/PLATFORMS.md reference/REPORT.md "$staging/reference/"
  mkdir -p "$staging/scripts"
  cp scripts/oneui_scan.py "$staging/scripts/"
  ( cd "$staging" && zip -qr "$ROOT/dist/$name.zip" . )
  echo "  dist/$name.zip"
done

rm -rf build-tmp
echo
echo "Done. Upload any of these at claude.ai → Settings → Capabilities → Skills."
