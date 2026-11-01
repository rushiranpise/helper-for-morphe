#!/usr/bin/env bash
#
# Builds the release notes body for a tag.
#
# The changelog is the source of truth: the section of CHANGELOG.md whose heading
# matches the tag is published as-is, because a release note is written for someone
# using the app, and no amount of grouping turns "keep session cookies across a
# source's hops" into something they care about.
#
# When a tag has no section yet, this falls back to the commits in that tag, grouped
# by Conventional Commit type, and says so in the body. That keeps a release
# possible, and visible as unfinished, instead of publishing a wrong note.
#
# Usage: tools/release-notes.sh <tag> [previous-tag]
#   tools/release-notes.sh v5.2.0            # previous tag found with git describe
#   tools/release-notes.sh v5.2.0 v5.1.0     # explicit range for the fallback
#
set -euo pipefail

tag="${1:?usage: release-notes.sh <tag> [previous-tag]}"
version="${tag#v}"
previous="${2:-}"
changelog="$(git rev-parse --show-toplevel 2>/dev/null || echo .)/CHANGELOG.md"

if [[ -z "$previous" ]]; then
	previous="$(git describe --tags --abbrev=0 "${tag}^" 2>/dev/null || true)"
fi

range="$tag"
if [[ -n "$previous" ]]; then
	range="${previous}..${tag}"
fi

repo="${GITHUB_REPOSITORY:-}"
if [[ -z "$repo" ]]; then
	repo="$(git remote get-url origin 2>/dev/null \
		| sed -E 's#^git@github.com:#https://github.com/#; s#^https://github.com/##; s#\.git$##' \
		|| true)"
fi

# The section for this version: from its "## <version>" heading to the next "## "
# heading. A heading may be bare ("## 5.2.0 - date") or bracketed ("## [5.2.0]").
# The version has to be the heading's first token, so "## 5.1.0 and earlier" cannot
# answer for the v5.1.0 tag.
section=""
if [[ -f "$changelog" ]]; then
	section="$(awk -v ver="$version" '
		/^## / {
			if (found) exit
			line = $0
			sub(/^## +/, "", line)
			sub(/^\[/, "", line)
			split(line, parts, /[ \]]/)
			if (parts[1] == ver) { found = 1; next }
		}
		found { print }
	' "$changelog")"
fi

printf '## Morphe Helper %s\n\n' "$version"

if [[ -n "${section//[[:space:]]/}" ]]; then
	printf '%s\n' "$section" | sed -e 's/[[:space:]]*$//' -e '/./,$!d'
	if [[ -n "$previous" && -n "$repo" ]]; then
		printf '\n**Full diff**: https://github.com/%s/compare/%s...%s\n' "$repo" "$previous" "$tag"
	fi
	exit 0
fi

# ---- Fallback: no changelog section for this tag -----------------------------

printf '> No changelog entry for %s yet. The commits in this tag, for reference:\n\n' "$tag"

subjects="$(
	git log --no-merges --pretty=format:'%s' "$range" \
		| grep -vE '^(chore|docs|ci|test|style|build)(\([^)]*\))?!?: *(release|bump)' \
		| grep -vE '^v?[0-9]+\.[0-9]+\.[0-9]+$' \
		|| true
)"

render() {
	local heading="$1" pattern="$2" body
	body="$(
		printf '%s\n' "$subjects" \
			| grep -E "^${pattern}(\([^)]*\))?!?: " \
			| sed -E 's/^[a-z]+(\(([^)]*)\))?!?: /\2|/' \
			| awk -F'|' '{
				if ($1 == "") printf "- %s\n", $2;
				else printf "- **%s**: %s\n", $1, $2;
			}' \
			|| true
	)"
	if [[ -n "$body" ]]; then
		printf '### %s\n\n%s\n\n' "$heading" "$body"
	fi
}

render "Features" "feat"
render "Fixes" "fix"
render "Performance" "perf"

others="$(
	printf '%s\n' "$subjects" \
		| grep -E '^(refactor|chore|docs|ci|test|style|build)(\([^)]*\))?!?: ' \
		| sed -E 's/^[a-z]+(\(([^)]*)\))?!?: /\2|/' \
		| awk -F'|' '{
			if ($1 == "") printf "- %s\n", $2;
			else printf "- **%s**: %s\n", $1, $2;
		}' \
		|| true
)"
if [[ -n "$others" ]]; then
	printf '<details><summary>Other changes</summary>\n\n%s\n\n</details>\n\n' "$others"
fi

if [[ -n "$previous" && -n "$repo" ]]; then
	printf '**Full diff**: https://github.com/%s/compare/%s...%s\n' "$repo" "$previous" "$tag"
fi
