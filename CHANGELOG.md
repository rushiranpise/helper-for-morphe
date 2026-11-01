# Changelog

What changed in each release, written for someone using the app rather than for
someone reading the diff. The release workflow publishes the section whose heading
matches the tag (see `tools/release-notes.sh`), so a new version needs a section here
before it is tagged. Commit subjects are only used as a fallback when a section is
missing.

## 5.2.0 - 2026-11-01

Downloads get faster, and Uptodown finally works.

### New

- **Uptodown works.** The page that holds the file sits behind a captcha, so the helper
  opens it in its own browser, presses the download button once the captcha is solved,
  and takes the file when the page starts it. No more "open the link and download it
  yourself".
- **A shortcut through Morphe's own resolver.** Instead of walking a site's search, app
  and uploads pages, the helper asks the resolver Morphe's dialog uses and lands on the
  exact build it names. On YouTube that is 3 requests instead of 8. Settings -> System
  can turn it off.
- **APKPure in one request.** APKPure's direct file endpoint now names the file, its
  version and its size in a single hop. On Instagram that is 1 request and 0.8 s instead
  of 5 requests and 2.1 s, and a requested APK no longer arrives as an XAPK.
- **Fast Mode on by default**, with the five sources that need no extra setup: APKMirror,
  Uptodown, APKPure, APKCombo and Aptoide.
- **Session cookies now survive the whole chain.** A challenge answered on one page no
  longer restarts on the next request of the same download.

### Fixed

- A downloaded file's real kind is read from its bytes, so a mislabelled bundle no longer
  fails with "could not be read as an APK".
- Aptoide builds that Aptoide itself has not marked as trusted are skipped.

### Faster

- Launch no longer health-checks every source, so the first download is not competing
  with background pings.

### Other

- The source health check and its Settings card are gone.

## Earlier releases

Notes for those are on the GitHub releases page:
https://github.com/rushiranpise/helper-for-morphe/releases
