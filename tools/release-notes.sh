#!/usr/bin/env bash
#
# Builds a release notes body for a tag from the commits that tag contains.
#
# The notes are grouped by Conventional Commit type, so a reader sees what changed in
# the app before the plumbing: features, then fixes, then speed, and everything else
# folded into a collapsed block. The type and scope are stripped from the visible text
# and the scope becomes the bold lead-in ("**aptomirror**: pick the variant ...").
#
# The version bump that ships with every release says nothing about the app, so it is
# dropped, as are merge commits and bare version subjects.
#
# Usage: tools/release-notes.sh <tag> [previous-tag]
#   tools/release-notes.sh v5.2.0            # previous tag found with git describe
#   tools/release-notes.sh v5.2.0 v5.1.0     # explicit range
#
set -euo pipefail

tag="${1:?usage: release-notes.sh <tag> [previous-tag]}"
previous="${2:-}"

if [[ -z "$previous" ]]; then
	previous="$(git describe --tags --abbrev=0 "${tag}^" 2>/dev/null || true)"
fi

range="$tag"
if [[ -n "$previous" ]]; then
	range="${previous}..${tag}"
fi

# One subject per line, newest first, without the release commit itself.
subjects="$(
	git log --no-merges --pretty=format:'%s' "$range" \
		| grep -vE '^(chore|docs|ci|test|style|build)(\([^)]*\))?!?: *(release|bump)' \
		| grep -vE '^v?[0-9]+\.[0-9]+\.[0-9]+$' \
		|| true
)"

# Repo URL for the compare link, from the CI environment when it is there.
repo="${GITHUB_REPOSITORY:-}"
if [[ -z "$repo" ]]; then
	repo="$(git remote get-url origin 2>/dev/null \
		| sed -E 's#^git@github.com:#https://github.com/#; s#^https://github.com/##; s#\.git$##' \
		|| true)"
fi

# render <heading> <type-regex>
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

printf '## Morphe Helper %s\n\n' "${tag#v}"
if [[ -n "$previous" ]]; then
	printf 'Changes since %s.\n\n' "$previous"
fi

render "New" "feat"
render "Fixes" "fix"
render "Faster" "perf"

# Everything else is real work but not news: keep it, out of the way.
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
