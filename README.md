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

**Selected source** is where the file comes from. Tap **Download source** to switch to another enabled source; the number beside it is how many are available. Per-source reachability is checked in Settings -> Advanced.

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
- Lets you narrow a multi-format request to a single file kind (APK / APKM / APKS / XAPK).
- **Reuse instead of re-download**: when a matching file from a previous run still exists (and auto-clear is off), Helper offers it — listing every candidate by source and file size, with the VirusTotal verdict if one exists — so a failed patch or an identical re-request is instant. The offer also works in Fast Mode (which pauses until you decide). "Download new" always wins if you prefer a fresh copy.

### Sources

- Ten sources: **APKMirror, Uptodown, APKPure, APKCombo, Aptoide, Evozi, Mi9, APK Downloader, Aurora, Play**.
- Every source can be individually **enabled/disabled** from Settings → Sources; disabled sources disappear from the picker, manual links, Fast Mode, and the default-source dropdown. If you disable *everything*, Play Store remains as the fallback.
- A **default source** can be set so the picker opens on your preferred provider instead of the first one.
- Each source offers up to four flows: **Manual** (open the site), **Recommended** (exact requested version), **Latest** (newest compatible), and **History** (browse every version).
- The source picker is a compact **dropdown** with a live enabled-source count; selecting a source collapses it so the page stays clean.
- Sources that gate files behind Cloudflare-style challenges open an **in-app captcha browser** (a real WebView); the download it produces is captured back into the helper. VirusTotal scans are offered in this flow too.

### Fast Mode

- One-tap auto-resolve: walks every enabled source in order and returns the file to Morphe without manual taps.
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
- Download progress is followed by distinct **extracting / scanning** phases with their own percent bars instead of sitting at a stuck 100%.
- Files are validated against the source-published **SHA-256** when available (shown as a verified badge), and against the real APK manifest before handoff.
- Downloads land in Helper's cache by default (honouring the auto-clear-after-handoff toggle) or as a visible copy in `Downloads/Morphe Helper`. A storage card shows both sizes and clears each with one tap; a startup sweep clears stale cache and reports how much it freed.

### Find New Apps

- A **live community patch index** (fetched fresh from the morphe-archive JSON on every open — never cached) listing every app with available patches, shown when the request does not come from Morphe.
- Searchable app list with real installed-app icons (Android default when not installed), per-app **source count**, install-state filters (All / Installed / Not installed), **favourites** that survive refreshes, and sort options (A–Z, Z–A, recently added, recently updated, most sources).
- Each app's detail groups patches **per source** in collapsible lists, with "Add to Morphe" actions that deep-link into the manager and an Open in Play Store shortcut.
- Includes the community disclaimer: the index is community-maintained, bundles are not individually verified, and neither Morphe nor this app's developer is responsible for third-party patches.

### Appearance, DNS & logs

- **Themes**: dark, light, or follow-the-system, plus optional **Material You** wallpaper-tinted accents and surfaces (Android 12+). A quick theme toggle lives in the home header.
- **AdGuard DNS**: resolve app traffic through AdGuard DNS to block ads/trackers on download pages and in the captcha browser, with automatic fallback to the system resolver on failure.
- **Tabs in Settings** (Settings / Health / History / Logs): per-source health checks, every past hand-off (with scan verdicts), and the full in-app request + HTTP log — every URL, status code, redirect, and timing — with a **Share** button exporting it for bug reports (app version and device info included). Logcat mirroring is optional.

## Helper Settings

- **Save downloads**: cache (honouring auto-clear) or a visible copy in `Downloads/Morphe Helper`; a storage card shows cache and Downloads sizes and clears both with one tap.
- **Connection**: Wi-Fi only, mobile data only, or both.
- **Sources**: toggle each APK source, or set a **default source**.
- **Fast Mode**: enable auto-resolve and choose the version policy (Requested / Latest / Always ask).
- **Security**: VirusTotal scanning with API key and scan mode.
- **AdGuard DNS**: on/off.
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
python tools\audit_helper_sources.py --package club.boxbox.android --sources apkpure,aptoide,apkcombo
python tools\audit_helper_sources.py --limit 20 --sources apkmirror,uptodown,apkpure,apkcombo,aptoide
```

The CSV/JSON output reports whether each source matched the requested version, only found latest, found the wrong file format, or failed to resolve the package.

## Credits

- [APKUpdater](https://github.com/rumboalla/apkupdater)
- [Aurora Store](https://gitlab.com/AuroraOSS/AuroraStore)
- Public web pages and APIs exposed by APKMirror, Uptodown, APKPure, APKCombo, and Aptoide

This helper does not claim ownership of source website data, packages, trademarks, or services.

## Support APK Sources

APK hosting and indexing costs money. If this helper saves you time, consider supporting the services it relies on:

- [APKMirror](https://www.apkmirror.com/premium/)
- [Uptodown](https://en.uptodown.com/turbo)
- [APKPure](https://apkpure.com/premium)
- [APKCombo](https://apkcombo.com/premium/)
- [Aptoide](https://en.aptoide.com/premium)
- [Aurora Store](https://gitlab.com/AuroraOSS/AuroraStore/#donations)

## License

Morphe Helper is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE).

## Notes

- Only download apps you are allowed to access.
- This helper does not bypass paid apps, license checks, account restrictions, or DRM.
- Always verify downloaded files before installing them on a device you care about.
- Source matching is best-effort because APK providers can change their sites and APIs without notice.
