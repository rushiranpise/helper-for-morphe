# Morphe Helper

<p align="center">
  <img src="docs/logo.png" alt="Morphe Helper" width="180" />
</p>

Morphe Helper is a standalone Android helper app for finding and returning original APK files requested by [Morphe Manager](https://github.com/MorpheApp). It receives a package/version request through an Android intent, resolves a matching file from a supported APK source, downloads it when a direct download is available, and returns a readable `content://` URI back to the caller.

This project is independent from the APK source websites listed below. Source availability, package versions, download formats, and region/device access can change at any time.

## Setup

Installing the APK is only half of it. **Morphe only offers helpers you have explicitly trusted**, so a fresh install does nothing until this one-time step is done:

1. Install Morphe Helper and open it once.
2. Open **Morphe Manager** and choose **Settings** in the bottom navigation.
3. Scroll to the **Files** section and tap **APK download helper**.
4. Switch on **Morphe Helper**, then confirm **Trust** in the dialog that appears.

The row reads `Trusted: 0 of 1` before that and `Trusted: 1 of 1` after it. Nothing else needs configuring: drive requests from Morphe, not from this app.

From then on, whenever Morphe needs an original APK it cannot find installed, the download screen offers **Use a helper app** -> **Open helper**. That launches this app, which resolves the requested version, downloads it, checks it against Morphe's package/version/signature expectations and hands the file straight back.

## Using the request screen

When Morphe needs an original APK it opens this app with the package and version it wants. The **App info** card shows exactly what was asked for:

| Row | Meaning |
| --- | --- |
| Version | The version Morphe asked for, marked `Unpatched` |
| Format | `APK`, `APKS` or `XAPK`, as the source publishes it |
| Architecture | Which CPU splits the source offers, when it publishes per-architecture builds |

**Selected source** is where the file comes from. Tap **Download source** to switch to another enabled source; the number beside it is how many are available. Enabled sources and their order are managed in Settings -> Sources.

**Version type** decides which file is fetched:

- **Manual** opens the source's website so you download the file yourself; the app then picks it up and returns it.
- **Recommended** fetches exactly the version Morphe asked for. This is the usual choice.
- **Latest** takes the newest version the source offers, and only hands it over when it really is newer than the requested version.
- **History** lists every version the source publishes, so you can pick one.

Each mode also has its own **About ... mode** card under the row, and the **How it works** card above explains the flow on the screen itself.

**Fast Mode** (the bolt in the header) skips all of this: it walks your enabled sources on its own and returns the first file that matches. Settings -> System controls whether it fetches the requested version, the newest version, or asks each time.

Whatever the route, the downloaded file is checked before it can be handed over:

- the **package name** must be the one Morphe asked for;
- the **version** must be the requested version (unless you picked a newer one deliberately);
- variants Morphe cannot patch, such as APKMirror's `-SECONDARY` builds, are rejected;
- the **version code** is enforced only in Fast Mode, where a mismatch asks before continuing.

With VirusTotal enabled the file is also scanned, and the verdict is recorded in the history. Once it passes, the file is handed back and the cache is cleared according to your auto-clear setting. You do not need to do anything further in Morphe: the patch resumes by itself.

## Features

### Request handling

- Handles `app.morphe.manager.action.DOWNLOAD_ORIGINAL_APK` requests, including split-archive requests (APK/APKM/APKS/XAPK), ABI hints, compatible version lists, and stock-install requirements.
- Shows the requested app's **real icon** (Android default when not installed), name, package, version, build, format, and ABI in a compact stat-card header. File-format and architecture hints are shown as `+N` pills when multiple values are allowed.
- Lets you choose one or more file kinds (APK / APKM / APKS / XAPK); Fast Mode tries them in the order you select.
- **Reuse instead of re-download**: when a matching file from a previous run still exists (and auto-clear is off), Helper offers it — listing every candidate by source and file size, with the VirusTotal verdict if one exists — so a failed patch or an identical re-request is instant. The offer also works in Fast Mode (which pauses until you decide). "Download new" always wins if you prefer a fresh copy.

### Sources

- Eight sources: **APKMirror, Uptodown, APKPure, APKCube, Aptoide, Morphe Link, Aurora, Play**.
- Every source can be individually **enabled/disabled** from Settings → Sources; disabled sources disappear from the picker, manual links, Fast Mode, and the default-source dropdown. If you disable *everything*, Play Store remains as the fallback.

- **Six start enabled**: APKMirror, Uptodown, APKPure, APKCube, Morphe Link and Aurora.
  Aptoide and Play are available but off until you turn them on.

- **Morphe's resolver first**: before walking a site, the helper asks Morphe's own resolver
  (`api.morphe.software`) where its download dialog would go, and opens that page
  directly - one request instead of a search, app page and uploads walk. On by default;
  Settings -> System turns it off to always scrape every source itself.
- **APKMirror latest is API-backed**: the helper asks APKMirror for the newest stable
  release and its variant links in one request, then fetches only the variant and download
  pages. If the API is unavailable, it scrapes as before. APKMirror requests identify as
  Helper rather than a browser, which avoids its Cloudflare challenge.
- **Faster source lookups**: Uptodown uses its mobile API first, APKPure resolves exact
  versions without walking every HTML release page, source probes run up front,
  definitive misses are cached, and APKMirror retries a rate limit once.
- **Density-specific APKMirror variants are opt-in** in Settings -> Downloads and storage.
  `nodpi` stays the safe default because density rows can be split APKs.
- A **default source** can be set so the picker opens on your preferred provider instead of the first one.
- Each source offers up to four flows: **Manual** (open the site), **Recommended** (exact requested version), **Latest** (newest compatible), and **History** (browse every version).
- The source picker is a compact **dropdown** with a live enabled-source count; selecting a source collapses it so the page stays clean.
- Sources that gate files behind Cloudflare-style challenges open an **in-app captcha browser** (a real WebView); the download it produces is captured back into the helper. VirusTotal scans are offered in this flow too.

### Fast Mode

- One-tap auto-resolve: walks every enabled source in order and returns the file to Morphe without manual taps.
- **On by default.** The helper exists to hand a file back to Morphe, so Fast Mode
  starts enabled for anyone who has never touched the switch; turning it off is
  remembered and upgrades never re-enable it.
- **Multiple formats**: select more than one format and Fast Mode tries them in your
  chosen order instead of stopping at the first format.
- **Version policy** — requested, latest, or always ask:
  - **Requested version**: finds the exact requested version *and* version code across sources (build mismatches are surfaced for you to accept or skip, never silently downloaded).
  - **Latest version**: fetches the newest version any enabled source offers, and only calls it "latest" when it actually is newer than the requested version.
  - **Always ask**: a simple Requested / Latest choice that appears instantly — no background source scan before you decide.
- Respects the global source toggles; sources that can't deliver a direct APK simply resolve to nothing and the walk moves on.
- Verifies the downloaded file's **real manifest** (package, version name, and version code) before handoff, so a mis-parsed source page can never hand over the wrong or older build.

### VirusTotal scanning

- Optional scanning of every downloaded file before it is returned to Morphe — toggle plus API key in Settings, with **Never / Ask / Always** modes and a link explaining how to get a key.
- **Cached-report reuse**: each file is looked up by its SHA-256 first, so files VirusTotal already analysed are not uploaded again — the result card is labelled **Cached report** vs **Fresh scan**.
- **Split bundles** (APKM/APKS/XAPK) are extracted and each inner APK is scanned individually, since engines often skip large containers; a live progress line names the current APK.
- **Rate-limit aware**: pacing throttles lookups to the free-tier limits (4/min, 500/day) with live quota bars, an automatic countdown + retry when the per-minute cap is hit, and a **Skip wait** action to jump ahead.
- Results show per-engine detections, a tap-to-copy **full SHA-256**, and **Open in VirusTotal**. Verdicts are recorded in the History tab (with cached/fresh badges and a flag filter), and the reuse-offer dialog shows each file's verdict.
- Quota cards appear on the scan flow and the home screen so usage is visible without opening Settings.

### Download & handoff

- **Live speed + ETA** in both the progress notification and the in-app card (`45% · 5.9 MB/s · 0:52 left`), smoothed so readings stay stable.
- **Segmented downloads** with 2 to 32 parallel connections, pause/resume, and source switching that keeps the version you picked.
- **Custom download folders and retention**: save to a folder you choose and clean files up after 1 hour, 24 hours, 7 days, or keep them until you clear storage.
- Download progress is followed by distinct **extracting / scanning** phases with their own percent bars instead of sitting at a stuck 100%.
- Files are validated against the source-published **SHA-256** when available (shown as a verified badge), and against the real APK manifest before handoff.
- Downloads are staged in Helper's private storage by default, or saved as a visible copy in `Downloads/Morphe Helper` or a custom folder. Retention can clear them after 1 hour, 24 hours, or 7 days. A storage card shows both sizes and clears each with one tap; a startup sweep clears stale files and reports how much it freed.

- **Game data (OBB) is reported, never installed.** Android serves expansion files only from `Android/obb/<package>/`, which no other app may write on Android 11 and later, so no helper can place them for you. When the file handed over carries them, the history entry says `Includes game data (2 files · 1.2 GB)` and the request log records it. Copy those files into `Android/obb/<package>/` if a patched game closes on launch. A bare `APK` never contains expansion data, so pick a container format (`XAPK`, `APKS`) for games that need it.
### Find New Apps

- A **live community patch index** (fetched fresh from the morphe-archive JSON on every open) with an archive dashboard showing app and patch totals.
- Searchable app list with real installed-app icons (Android default when not installed), per-app **source count**, install-state filters (All / Installed / Not installed), **favourites** that survive refreshes, and sort options (A-Z, Z-A, recently added, recently updated, most sources).
- Each app opens a redesigned detail screen with a compact version picker, patches grouped **per source** in collapsible lists, "Add to Morphe" actions that deep-link into the manager, and an Open in Play Store shortcut.
- Includes the community disclaimer: the index is community-maintained, bundles are not individually verified, and neither Morphe nor this app's developer is responsible for third-party patches.

### Appearance, DNS & logs

- **Themes**: dark, light, or follow-the-system, plus optional **Material You** wallpaper-tinted accents and surfaces (Android 12+).
- **AdGuard DNS**: resolve app traffic through AdGuard DNS to block ads/trackers on download pages and in the captcha browser, with automatic fallback to the system resolver on failure. On by default.
- **Tabs in Settings** (Appearance / System / Advanced / Logs), with the Logs tab split into **History**, **Logging** and **Request logs** sub-tabs. History records every past hand-off (with scan verdicts), and Request logs show the full in-app request + HTTP log - every URL, status code, redirect, and timing - with a **Share** button exporting it for bug reports (app version and device info included). Logcat mirroring is optional.

## Helper Settings

- **Save downloads**: private storage, a visible copy in `Downloads/Morphe Helper`, or a custom folder; retention can clear files after 1 hour, 24 hours, 7 days, or keep them until you clear storage. A storage card shows cache and Downloads sizes and clears both with one tap.
- **Connection**: Wi-Fi only, mobile data only, or both.
- **Sources**: enable, disable, reorder or set a **default source**.
- **Fast Mode**: enable auto-resolve and choose the version policy (Requested / Latest / Always ask).
- **About**: update channel (stable or beta), update notifications, and optional automatic install through Shizuku.
- **Downloads and storage**: choose the folder, retention, and APKMirror density preference.
- **Security**: VirusTotal scanning with API key and scan mode.
- **AdGuard DNS**: on by default; can be turned off.
- **Appearance**: System / Dark / Light and Material You colors.
- **Logging**: mirror request/HTTP details to Logcat.

Temporary hand-off is the default because Morphe Manager copies the returned APK URI into its own private workspace before patching.

## Debugging & Bug Reports

When a source fails to resolve or a download stalls, the **Logs** tab shows the full story — the request data, every URL fetched, HTTP status codes, redirects, and timing — with no adb needed. Use **Share** to export the log (including the app/device info) to any app.

GitHub issues use a bug report template that asks for this exported log, so reports arrive with the exact failing request and HTTP trace. A feature-request template is also provided.

## Intent Contract

Request action:

```text
app.morphe.manager.action.DOWNLOAD_ORIGINAL_APK
```

Important request extras:

```text
app.morphe.manager.extra.CALLER_PACKAGE
app.morphe.manager.extra.PROTOCOL_VERSION
app.morphe.manager.extra.PACKAGE_NAME
app.morphe.manager.extra.APP_NAME
app.morphe.manager.extra.VERSION_NAME
app.morphe.manager.extra.VERSION_CODES
app.morphe.manager.extra.COMPATIBLE_VERSION_NAMES
app.morphe.manager.extra.SUPPORTED_ABIS
app.morphe.manager.extra.FILE_TYPE
app.morphe.manager.extra.ALLOW_SPLIT_ARCHIVE
app.morphe.manager.extra.STOCK_INSTALL_REQUIRED
app.morphe.manager.extra.FALLBACK_WEB_URL
```

The helper also accepts older local draft keys for compatibility, including `REQUESTED_FILE_TYPE`, `INSTALL_STOCK_AFTER_DOWNLOAD`, `VERSION_CODE`, `COMPATIBLE_VERSION_CODES`, and `SOURCE_HINT_URLS`.

Successful result:

- `Activity.RESULT_OK`
- `Intent.data` points to the downloaded file URI
- `Intent.FLAG_GRANT_READ_URI_PERMISSION` is granted to the caller

## App Icon

The launcher icon (green ribbon "M" over a dark forest-green tile) lives as the master at `tools/app_icon_source.png`; `tools/make_icon.py` regenerates every legacy and adaptive density from it:

```powershell
python tools\make_icon.py
```

## Release Signing

Release APKs must be signed with a private release certificate. Debug signing is only for local test builds and must not be used for public releases.

GitHub Actions expects these repository secrets:

- `HELPER_RELEASE_KEYSTORE_BASE64`: base64-encoded release keystore
- `HELPER_RELEASE_STORE_PASSWORD`
- `HELPER_RELEASE_KEY_ALIAS`
- `HELPER_RELEASE_KEY_PASSWORD`

For local release builds, place the same values in `local.properties` or pass them as Gradle properties/environment variables. Do not commit keystores or signing passwords.

## Source Audit Script

Use `tools/audit_helper_sources.py` to test source availability for every app declared in a Morphe `Constants.kt` file without launching the Android app:

```powershell
python tools\audit_helper_sources.py --output reports\source-audit.csv
```

Useful targeted checks:

```powershell
python tools\audit_helper_sources.py --package club.boxbox.android --sources apkpure,aptoide
python tools\audit_helper_sources.py --limit 20 --sources apkmirror,uptodown,apkpure,aptoide
```

The CSV/JSON output reports whether each source matched the requested version, only found latest, found the wrong file format, or failed to resolve the package.

## Credits

- [APKUpdater](https://github.com/rumboalla/apkupdater)
- [Aurora Store](https://gitlab.com/AuroraOSS/AuroraStore)
- Public web pages and APIs exposed by APKMirror, Uptodown, APKPure, and Aptoide

This helper does not claim ownership of source website data, packages, trademarks, or services.

## Support APK Sources

APK hosting and indexing costs money. If this helper saves you time, consider supporting the services it relies on:

- [APKMirror](https://www.apkmirror.com/premium/)
- [Uptodown](https://en.uptodown.com/turbo)
- [APKPure](https://apkpure.com/premium)
- [Aptoide](https://en.aptoide.com/premium)
- [Aurora Store](https://gitlab.com/AuroraOSS/AuroraStore/#donations)

## License

Morphe Helper is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE).

## Notes

- Only download apps you are allowed to access.
- This helper does not bypass paid apps, license checks, account restrictions, or DRM.
- Always verify downloaded files before installing them on a device you care about.
- Source matching is best-effort because APK providers can change their sites and APIs without notice.
