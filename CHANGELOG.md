# Changelog

What changed in each release, written for someone using the app rather than for
someone reading the diff. The release workflow publishes the section whose heading
matches the tag (see `tools/release-notes.sh`), so a new version needs a section here
before it is tagged. Commit subjects are only used as a fallback when a section is
missing.

## 5.4.0 - 2026-11-16

Defaults that need less setup, and a clearer Settings screen.

### New

- **APKCube is on by default.** It joins the default source list, so more apps
  resolve without enabling a source first. Existing installs are migrated once.
- **AdGuard DNS is on by default.** It blocks ads and trackers on download pages
  and in the captcha browser, and falls back to the system resolver on failure.
  Existing installs are migrated once.
- **Logs is split into sub-tabs.** History, Logging and Request logs each get
  their own tab, so a long history no longer pushes the request log down the
  screen.
- **Settings are rebalanced.** Connection, download connections, AdGuard DNS and
  Use Morphe's resolver now sit in Advanced alongside VirusTotal. System keeps
  downloads, sources, Fast Mode and updates.

## 5.3.0 - 2026-11-15

Faster APKMirror lookups, more download control, and updates from inside the app.

### New

- **APKMirror latest is API-backed.** The helper gets the newest stable release and its
  variants in one request, then fetches only the variant and download pages. If the API
  is unavailable or only offers a beta or secondary build, it scrapes as before.
  APKMirror also receives the helper's own user agent, so its browser challenge no longer
  blocks the helper's requests.
- **Density-specific APKMirror variants are opt-in.** Keep `nodpi` as the safe default,
  or prefer the variant for your screen in Settings -> Downloads and storage. Density
  rows can be split APKs, so the option warns about that.
- **APKCube is a source.** It resolves apps and versions from APKCube's catalogue and
  opens the exact download page in the in-app browser when a captcha is required.
- **Morphe Link fallback.** The resolver-backed source can hand a request to any enabled
  site in one step and keep the resolved page when a source cannot continue.
- **Multiple formats in Fast Mode.** Select more than one format and Fast Mode tries
  them in your chosen order instead of stopping at the first format.
- **Segmented downloads.** Choose 2 to 32 connections, pause and resume, or switch source
  without losing the version you picked.
- **Custom download folders and retention.** Save to a folder you choose and clean files
  up after 1 hour, 24 hours, 7 days, or keep them until you clear storage.
- **In-app updates.** Stable and beta channels, a notification when a release is
  available, and optional silent install through Shizuku.
- **Source controls.** Enable, disable, reorder and set a default source from the
  dedicated Sources screen.
- **Hide the launcher icon** while keeping the helper available to Morphe.
- **Browser challenge handling.** Challenge pages are tracked and timed out, with a
  clear warning when a site is waiting for verification.

### Fixed

- APKMirror exact build matching, variant numbering and architecture selection.
- APKPure split requests use XAPK instead of returning a plain APK.
- Aptoide builds need a trusted malware verdict before they are offered.
- APKMirror and APKPure browser fallbacks open the right page when direct downloads fail.
- Bundle downloads keep `.apkm`, `.apks` and `.xapk` extensions instead of arriving as
  `.zip` files.
- Temporary downloads are handed off from private storage, avoiding the cache hand-off
  failure on newer Android versions.
- APKCube fallback builds and prelander pages are handled correctly.
- Fast Mode retries download automatically, keeps the selected target when switching
  sources, and offers the captcha flow for exact source matches.
- Update checks handle channel switches and newly covered settings correctly.

### Removed

- APKCombo, Mi9 and APK Downloader. Their domains or APIs are no longer available.

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
