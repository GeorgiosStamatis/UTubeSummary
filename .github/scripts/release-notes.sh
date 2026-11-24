#!/usr/bin/env bash
# Prints the release notes of a tag: the matching CHANGELOG.md section when there is one, otherwise the
# commit subjects since the previous v* tag.
# Usage: release-notes.sh <tag> [changelog-file]
set -euo pipefail

tag="${1:?usage: release-notes.sh <tag> [changelog-file]}"
changelog="${2:-CHANGELOG.md}"
repo="${GITHUB_REPOSITORY:-GeorgiosStamatis/UTubeSummary}"

version="${tag#v}"
version="${version%%(*}"

section=""
if [ -f "$changelog" ]; then
  section=$(awk -v v="$version" '
    /^## \[/ {
      if (found) exit
      if (index($0, "[" v "]")) { found = 1; next }
    }
    found { print }
  ' "$changelog")
fi
# Drop leading and trailing blank lines.
section=$(printf '%s\n' "$section" | sed -e '/./,$!d' | sed -e ':a' -e '/^\n*$/{$d;N;ba' -e '}')

if [ -z "${section//[[:space:]]/}" ]; then
  previous=$(git tag --list 'v*' --sort=v:refname | awk -v t="$tag" '$0 == t { print p; exit } { p = $0 }')
  if [ -n "$previous" ]; then range="$previous..$tag"; else range="$tag"; fi
  section=$(git log "$range" --no-merges --pretty='- %s' \
    | grep -v -E '^- (docs|chore)(\(.*\))?:' | head -15 || true)
  [ -n "$section" ] || section="- Maintenance release"
fi

printf '## What'"'"'s included\n\n%s\n\n' "$section"
printf 'Two APKs are attached: `gms` (smaller, needs Google Play Services) and `standalone` (works without it).\n'
printf 'Full history: [CHANGELOG](https://github.com/%s/blob/main/CHANGELOG.md)\n' "$repo"
