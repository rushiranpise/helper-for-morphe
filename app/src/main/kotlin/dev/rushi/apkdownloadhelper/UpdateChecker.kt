package dev.rushi.apkdownloadhelper

import android.content.Context
import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONObject

/**
 * One release the helper can offer. [version] never carries the leading `v`;
 * [assetUrl] is empty when the release has no APK attached.
 */
internal data class UpdateInfo(
    val version: String,
    val releaseUrl: String,
    val assetUrl: String,
    val assetName: String,
    val assetSize: Long,
    val notes: String
) {
    fun isNewerThan(currentVersion: String): Boolean =
        compareVersions(version, currentVersion) > 0

    companion object {
        /** True for versions such as `5.3.0-beta.1`. */
        fun isPrerelease(version: String): Boolean = clean(version).contains('-')

        fun compareVersions(left: String, right: String): Int {
            val leftParts = clean(left).split('-', limit = 2)
            val rightParts = clean(right).split('-', limit = 2)
            val numeric = compareNumbers(leftParts[0], rightParts[0])
            if (numeric != 0) return numeric

            val leftPre = leftParts.getOrNull(1)?.isNotEmpty() == true
            val rightPre = rightParts.getOrNull(1)?.isNotEmpty() == true
            if (leftPre != rightPre) return if (leftPre) -1 else 1
            if (!leftPre) return 0
            return comparePrerelease(leftParts[1], rightParts[1])
        }

        private fun compareNumbers(left: String, right: String): Int {
            val leftParts = left.split('.')
            val rightParts = right.split('.')
            val count = maxOf(leftParts.size, rightParts.size)
            for (index in 0 until count) {
                val a = number(leftParts.getOrNull(index))
                val b = number(rightParts.getOrNull(index))
                if (a != b) return a.compareTo(b)
            }
            return 0
        }

        private fun comparePrerelease(left: String, right: String): Int {
            val leftParts = left.split('.')
            val rightParts = right.split('.')
            val count = maxOf(leftParts.size, rightParts.size)
            for (index in 0 until count) {
                val a = leftParts.getOrNull(index).orEmpty()
                val b = rightParts.getOrNull(index).orEmpty()
                val aNumeric = a.isNotEmpty() && a.all { it.isDigit() }
                val bNumeric = b.isNotEmpty() && b.all { it.isDigit() }
                val result = if (aNumeric && bNumeric) {
                    number(a).compareTo(number(b))
                } else {
                    a.compareTo(b)
                }
                if (result != 0) return result
            }
            return 0
        }

        private fun number(value: String?): Int =
            value.orEmpty().takeWhile { it.isDigit() }.toIntOrNull() ?: 0

        private fun clean(value: String): String =
            value.trim().lowercase().removePrefix("v")
    }
}

/**
 * Reads releases from the helper's own GitHub repository. The API is tried
 * first; the checked-in CHANGELOG is the fallback when GitHub's API is rate
 * limited or unavailable. Releases are never accepted from another project.
 */
internal object UpdateChecker {
    const val REPO = "rushiranpise/helper-for-morphe"
    private const val API = "https://api.github.com/repos/$REPO"
    private const val RELEASES = "$API/releases?per_page=30"
    private const val CHANGELOG_RAW = "https://raw.githubusercontent.com/$REPO/main/CHANGELOG.md"
    private const val ASSET_PREFIX = "apk-download-helper-v"
    private const val ASSET_SUFFIX = ".apk"
    private const val USER_AGENT = "HelperForMorphe"

    private val HEADING = Regex("""^##\s+(.+)$""")
    private val VERSION = Regex("""^\d+(\.\d+)*([-.][0-9A-Za-z.-]+)?$""")

    internal val HTTP: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun check(currentVersion: String): UpdateInfo {
        val includePrereleases = UpdateInfo.isPrerelease(currentVersion)
        val apiError: Exception
        try {
            return best(releasesFromApi(includePrereleases))
        } catch (error: Exception) {
            apiError = error
            Log.w(TAG, "GitHub API update check failed; trying CHANGELOG.md", error)
        }

        try {
            return best(changelogReleases(includePrereleases))
        } catch (error: Exception) {
            error.addSuppressed(apiError)
            throw IOException(
                "GitHub update services are not responding; try again later",
                error
            )
        }
    }

    fun releases(includePrereleases: Boolean): List<UpdateInfo> {
        try {
            return releasesFromApi(includePrereleases)
        } catch (error: Exception) {
            Log.w(TAG, "GitHub release list failed; trying CHANGELOG.md", error)
        }
        return changelogReleases(includePrereleases)
    }

    private fun releasesFromApi(includePrereleases: Boolean): List<UpdateInfo> {
        val body = fetch(RELEASES, "application/vnd.github+json")
        val array = org.json.JSONArray(body)
        return buildList {
            for (index in 0 until array.length()) {
                val release = array.optJSONObject(index) ?: continue
                if (release.optBoolean("draft", false)) continue
                if (!includePrereleases && release.optBoolean("prerelease", false)) continue
                val info = parseRelease(release)
                if (info.version.isBlank()) continue
                if (!includePrereleases && UpdateInfo.isPrerelease(info.version)) continue
                add(info)
            }
        }
    }

    private fun parseRelease(release: JSONObject): UpdateInfo {
        val tag = release.optString("tag_name", "")
        val version = tag.removePrefix("v").removePrefix("V")
        val assets = release.optJSONArray("assets")
        var chosen: JSONObject? = null
        var fallback: JSONObject? = null
        if (assets != null) {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                if (!name.endsWith(".apk", ignoreCase = true) || !url.startsWith("https://")) {
                    continue
                }
                if (fallback == null) fallback = asset
                if (name.contains("universal", ignoreCase = true)) {
                    chosen = asset
                    break
                }
            }
        }
        val asset = chosen ?: fallback
        return UpdateInfo(
            version = version,
            releaseUrl = release.optString("html_url", ""),
            assetUrl = asset?.optString("browser_download_url", "").orEmpty(),
            assetName = asset?.optString("name", "").orEmpty(),
            assetSize = asset?.optLong("size", 0L) ?: 0L,
            notes = release.optString("body", "")
        )
    }

    private fun changelogReleases(includePrereleases: Boolean): List<UpdateInfo> {
        val text = fetch(CHANGELOG_RAW, "text/plain")
        return parseChangelog(text, includePrereleases)
    }

    internal fun parseChangelog(text: String, includePrereleases: Boolean): List<UpdateInfo> {
        val result = mutableListOf<UpdateInfo>()
        var version: String? = null
        val notes = StringBuilder()

        fun addCurrent() {
            val rawVersion = version ?: return
            val cleanVersion = rawVersion.removePrefix("v").removePrefix("V")
            if (!VERSION.matches(cleanVersion)) return
            if (!includePrereleases && UpdateInfo.isPrerelease(cleanVersion)) return
            val tag = "v$cleanVersion"
            val assetName = "$ASSET_PREFIX$cleanVersion$ASSET_SUFFIX"
            result += UpdateInfo(
                version = cleanVersion,
                releaseUrl = "https://github.com/$REPO/releases/tag/$tag",
                assetUrl = "https://github.com/$REPO/releases/download/$tag/$assetName",
                assetName = assetName,
                assetSize = 0L,
                notes = notes.toString().trim()
            )
        }

        for (line in text.lineSequence()) {
            val match = HEADING.matchEntire(line.trim())
            if (match != null) {
                addCurrent()
                val heading = match.groupValues[1].trim()
                version = heading
                    .removePrefix("[")
                    .substringBefore("]")
                    .substringBefore(" ")
                    .trim()
                notes.setLength(0)
            } else if (version != null) {
                notes.append(line).append('\n')
            }
        }
        addCurrent()
        return result
    }

    private fun best(releases: List<UpdateInfo>): UpdateInfo {
        var best: UpdateInfo? = null
        for (candidate in releases) {
            if (best == null || candidate.isNewerThan(best.version)) {
                best = candidate
            }
        }
        return best ?: throw IOException("No releases found")
    }

    private fun fetch(url: String, accept: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "$USER_AGENT/${BuildConfig.VERSION_NAME}")
            .header("Accept", accept)
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

        var last: IOException? = null
        for (attempt in 0 until 2) {
            try {
                HTTP.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    if (!response.isSuccessful) {
                        throw IOException("GitHub HTTP ${response.code}")
                    }
                    return body
                }
            } catch (error: IOException) {
                last = error
                if (attempt == 0) {
                    runCatching { Thread.sleep(900L) }
                }
            }
        }
        throw last ?: IOException("GitHub request failed")
    }

    fun installedVersion(context: Context): String = try {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
            .ifBlank { BuildConfig.VERSION_NAME }
    } catch (error: Exception) {
        BuildConfig.VERSION_NAME
    }
}
