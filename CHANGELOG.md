# Changelog

What changed in each release, written for someone using the app rather than for
someone reading the diff. The release workflow publishes the section whose heading
matches the tag (see `tools/release-notes.sh`), so a new version needs a section here
before it is tagged. Commit subjects are only used as a fallback when a section is
missing.

## 7.1.0 - 2026-11-28

Full-screen Requests and Tracking, richer download notifications, and safer stored-file cleanup.

### New

- **Full-screen Requests and Tracking**: both open as dedicated pages with a floating close button instead of leaving the bottom navigation on screen. Closing one restores the underlying Find apps tab and scroll position.
- **Requests shortcut in Find apps**: open the community request board without leaving the app.
- **Live download notifications**: progress notifications now show speed and ETA. Tap one to return to the active request, and paused downloads keep their notification time up to date.
- **Release-signed debug APKs**: CI debug builds now use the release certificate, so a debug APK can update over an installed release build.

### Fixed

- Requests vote confirmations and errors now appear above the app's close bar instead of behind it.
- The close bar no longer covers the last row in Find apps and Tracking; its measured height now includes the navigation-bar inset.
- Removed the Requests WebView blur source that could crash, and gave the close bar its own background.
- Stored APK cleanup only deletes files whose size and SHA-256 match the selected copy, and reports per-file failures. APK files shared as content URIs are inspected correctly.
- Exporting a file cleans up its temporary directory, and the delete action runs off the main thread and reports failure.
- The hidden Find apps tab no longer triggers a background index fetch, and the Tracking worker rechecks the hidden settings after fetching.
- The pager is no longer kept composed behind full-screen pages, so the underlying Find apps state and focus are preserved correctly.
- The confirmation dialog scrolls its message, the reuse-offer dialog resets when its options change, selected settings are announced correctly, and scan cards use the "Not flagged" label.

### Changed

- Download history mutations are serialized so entries cannot be lost to concurrent writes.
- Temporary export folders are created per export and removed after use.

### Contributors

- [thegodfather-hub](https://github.com/thegodfather-hub): reported the Requests toast overlap (#69).

## 7.0.1 - 2026-11-25

A fresh install no longer crashes on first launch.

### Fixed

- Fixed a crash on the first launch after a fresh install. The Requests board is now prepared outside the navigation bar's blur layer.

## 7.0.0 - 2026-11-25

The community request board, a redesigned Helper screen, and faster source handling.

### New

- **Requests tab**: the community patch request board now opens inside the app with its Apps, Patches and Done tabs, search and sort. Vote for the patches missing from an app, per category, and let the site's own human check handle the captcha when it is due. The board follows the app theme, and a reload button refreshes a stale list.
- **Redesigned Helper screen**: the request screen is rebuilt around the new navigation, with instant tab switching, button press feedback, and a manual-mode control that leaves the choice to you.
- **Blur effects toggle**: turn the glass blur off for a flatter look and less GPU work, or keep it for the frosted panels.
- **Find apps remembers your view**: the filter and sort you last used are restored per browser mode.
- **Resolver build codes**: when Morphe sends several build codes, the resolver asks for all of them instead of a single one, so the right variant is found.

### Fixed

- Captcha cookies now survive a source's redirect chain, and split packaging no longer loses the session partway through.
- Find apps publishes a refreshed index atomically, reuses an in-flight request, and no longer discards a valid cached index.
- Source resolution handles Uptodown store builds, Aurora release labels, and APKMirror fallbacks more reliably.

### Performance

- Split packaging and source resolution are faster.
- Find apps search and tab switching do less work on the UI thread.

### Contributors

Thanks to the recent pull requests that shaped this release:

- **darkbyteprojects** (#53, #55): Settings and Helper screen redesigns.
- **Akash-Sriram** (#54, #56, #57): search caching, captcha cookies, split packaging, instant tabs, and search badges.
- **parchito** (#59): resolver build codes.

## 6.2.0 - 2026-11-20

A faster Find New Apps and a rebuilt Settings screen.

### New

- **Settings home**: Sources, Appearance, More customization, Downloads, Security & network, System & logs, and About each open a dedicated page instead of one long tabbed screen. The Sources page keeps provider toggles and drag-to-reorder; Appearance collects the theme, colors and app icon.
- **Play Store icons for missing apps**: uninstalled apps and archive entries without an icon now fall back to their Play listing, with the existing placeholder when there is no listing.

### Performance

- **Faster Find New Apps**: search is debounced, scored off the UI thread, and covers app names, packages, bundles, patch names and descriptions.
- **Instant catalog launch**: the last index is cached on disk, validated before use, and swapped atomically, so the list opens immediately and still works offline.

### Changed

- Settings titles are sentence case, and the Sources list no longer repeats the reorder hint on every row.

## 6.1.0 - 2026-11-19

Track apps from inside Helper and get notified when they change.

### New

- **Tracking tab**: tap the bell on any app in Find New Apps, or enter an
exact package name on the Tracking tab.
- **Background checks**: while anything is tracked, Helper checks the Morphe
index every six hours and posts one grouped notification for new versions or
patches. In-app badges stay until the app is opened.
- **Future packages**: enter a package that is not in the index yet. It waits
under Waiting for index, then is tracked automatically when it appears, with a
notification when it becomes available.

### Fixed

- App cards now show only the release date, without the "Updated" prefix.

### Changed

- **Tracking controls**: pending packages can be removed from the Tracking tab,
and the periodic check stays scheduled while tracked or pending packages exist.

## 6.0.1 - 2026-11-18

A smoother and clearer Find New Apps screen.

### Fixed

- Scrolling no longer hitches when the archive dashboard and warning scroll
  away, and the scrollbar no longer rescans the whole catalogue.
- App cards now line up with the header, with the scrollbar placed in the right
  margin.

### Changed

- Find New Apps now calls its sources bundles.

## 6.0.0 - 2026-11-18

A major update for finding apps and resolving downloads.

### New

- **Find New Apps redesign**: archive totals at a glance, a cleaner app detail
  screen, a compact version picker, and real launcher icons for installed apps.
- **Faster source resolution**: Uptodown's mobile API, direct APKPure exact
  builds, prefetched source checks, cached misses, and an APKMirror rate-limit
  retry.

### Fixed

- APKCube no-match pages, Morphe Link captcha gating for direct links, APKMirror
  web-search fallbacks, `download.php` filenames, and bundle extensions.
- Cleaner source order and download controls, with updates moved to About.

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
