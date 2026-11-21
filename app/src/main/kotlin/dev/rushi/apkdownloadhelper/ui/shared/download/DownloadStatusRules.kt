package dev.rushi.apkdownloadhelper.ui.shared.download



// True when the scan is paused on the app's own pacing gap ("Waiting Ns for
// rate limit…") — the only wait the Skip button can meaningfully cut short.
// Match the pause anywhere in the status: the bundle scanner prefixes inner
// APK statuses with "APK 2 of 4 (split_1.apk): …", so a plain prefix check
// would never see the "Waiting Ns for rate limit…" inside.
//
// A genuine 429 ("Rate limit hit — retrying in Ns…") is deliberately NOT
// matched: skipping it is futile (the server keeps rejecting until the 60s
// window clears) and its countdown comes from the window, not the gap, so the
// Skip button would show a wrong number. That wait auto-retries instead.
internal fun isRateLimitWait(status: String): Boolean =
    status.contains("waiting", ignoreCase = true) &&
        status.contains("rate limit", ignoreCase = true)

/** True when a status message belongs to the VirusTotal scan phase (as opposed
 *  to the download phase that reuses the same card). */
internal fun isScanStatus(status: String?): Boolean =
    status?.let {
        it.startsWith("APK ") || it.startsWith("Extract") ||
            it.startsWith("Opening") || it.startsWith("Upload") ||
            it.startsWith("Waiting") || it.startsWith("Aggregat") ||
            it.startsWith("Scanning") || it.startsWith("Checking VirusTotal") ||
            it.startsWith("Queued") || it.startsWith("VirusTotal engines")
    } == true

internal fun scanPhaseSplit(status: String): Pair<String, String> {
    val apk = Regex("""APK (\d+) of (\d+) \(([^)]+)\):? ?(.*)""").find(status)
    if (apk != null) {
        val (_, _, name, rest) = apk.destructured
        val phase = "Scanning APK ${apk.groupValues[1]} of ${apk.groupValues[2]}"
        return phase to rest.ifBlank { name }
    }
    val extract = Regex("""Extract(ing|ed) ([^:]+)""").find(status)
    if (extract != null) {
        val action = extract.groupValues[1]  // ing | ed
        val body = extract.groupValues[2]
        return "Extract$action ${body.take(30)}" to (status.removePrefix(extract.value).trim())
    }
    // Everything else: first clause is the phase (before "·"/":"), rest is detail.
    val sep = status.indexOfFirst { it == '·' || it == ':' }
    return if (sep > 0 && sep < status.length - 1) {
        (status.substring(0, sep).trim().replaceFirstChar { it.uppercase() }) to
            status.substring(sep + 1).trim()
    } else {
        status to ""
    }
}
