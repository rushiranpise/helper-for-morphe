package dev.rushi.apkdownloadhelper.sources

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val UPTODOWN_API_BASE = "https://www.uptodown.app/eapi"
private const val UPTODOWN_CLIENT_VERSION = "739"
private const val UPTODOWN_CLIENT_UA =
    "Dalvik/2.1.0 (Linux; U; Android 16; Pixel 8 Pro Build/BP4A.260205.001)"
private const val UPTODOWN_BROWSER_UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
private const val UPTODOWN_HMAC_KEY = "MDGMXUMdvHJBG/vjdFgmqX6LUdy7ecfwvYNd0gyfOCs="

/** The app and exact file the mobile API found for a request. */
internal data class UptodownApp(
    val appId: String,
    val slug: String,
    val name: String,
    val icon: String,
    val packageName: String,
    val lastVersion: String
)

internal data class UptodownRelease(
    val version: String,
    val fileId: Long,
    val kindFile: String
)

internal data class UptodownDownload(
    val url: String,
    val sha256: String
)

internal data class UptodownPrepared(
    val app: UptodownApp,
    val release: UptodownRelease,
    val download: UptodownDownload
)

/** The parser-facing part of Uptodown's mobile API. */
internal interface UptodownApi {
    @Throws(IOException::class)
    fun prepare(packageName: String, version: String?, fileType: String): UptodownPrepared
}

/**
 * Reverse-engineered Uptodown Android client.
 *
 * The public download page normally needs a Cloudflare Turnstile challenge. The app
 * API returns the exact file ID and a signed CDN URL instead, so this is the fast path.
 * Every failure is allowed to escape and the caller keeps the HTML/WebView flow.
 */
internal class UptodownMobileApi(
    private val client: OkHttpClient = defaultClient()
) : UptodownApi {

    @Volatile
    private var token: String? = null

    @Volatile
    private var tokenExpiresAt: Long = 0L

    override fun prepare(
        packageName: String,
        version: String?,
        fileType: String
    ): UptodownPrepared {
        val app = resolve(packageName)
        val release = findRelease(app, version, fileType)
            ?: throw IOException(
                if (version.isNullOrBlank()) {
                    "No Uptodown release found"
                } else {
                    "Requested version $version not found on Uptodown"
                }
            )
        val download = resolveDownload(app, release.fileId)
        return UptodownPrepared(app, release, download)
    }

    private fun resolve(packageName: String): UptodownApp {
        if (packageName.isBlank()) throw IOException("Missing package name")
        val lookup = apiGet("/apps/byPackagename/${encode(packageName)}")
        val data = lookup.objectOrNull("data")
            ?: throw IOException("Uptodown returned no package lookup")
        val appId = data.string("appID")
        if (appId.isBlank()) {
            throw IOException("Uptodown has no app for $packageName")
        }

        val detail = apiGet("/v3/apps/$appId/device/0?countryIsoCode=US")
        val app = detail.objectOrNull("data")
            ?: throw IOException("Uptodown detail request returned no app")
        val slug = app.string("urlShare")
        if (slug.isBlank()) {
            throw IOException("Uptodown returned no app page")
        }
        return UptodownApp(
            appId = appId,
            slug = slug,
            name = app.string("name"),
            icon = app.string("icon"),
            packageName = app.string("packagename").ifBlank { packageName },
            lastVersion = app.string("lastVersion")
        )
    }

    private fun findRelease(
        app: UptodownApp,
        version: String?,
        fileType: String
    ): UptodownRelease? {
        val base = app.slug.trimEnd('/')
        val requested = version?.trim().orEmpty()
        val wantedKind = normalizeKind(fileType)
        var fallback: UptodownRelease? = null

        for (page in 1..50) {
            val root = browserGet("$base/apps/${app.appId}/versions/$page")
            val items = root.arrayOrNull("data") ?: break
            if (items.size() == 0) break

            val picked = pickRelease(items, requested, wantedKind)
            if (picked != null) {
                if (requested.isNotEmpty() && requested.equals(picked.version, ignoreCase = true)) {
                    return picked
                }
                if (requested.isEmpty() && fallback == null) {
                    fallback = picked
                }
            }
        }
        if (requested.isNotEmpty()) return null
        return fallback
    }

    private fun pickRelease(
        items: JsonArray,
        requested: String,
        wantedKind: String
    ): UptodownRelease? {
        for (element in items) {
            val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val fileId = item.long("fileID")
            if (fileId <= 0L) continue
            val version = item.string("version").trim()
            val kind = normalizeKind(item.string("kindFile"))
            if (wantedKind.isNotEmpty() && kind != wantedKind) continue
            if (requested.isNotEmpty() && !requested.equals(version, ignoreCase = true)) continue
            return UptodownRelease(version, fileId, kind)
        }
        return null
    }

    private fun resolveDownload(app: UptodownApp, fileId: Long): UptodownDownload {
        if (app.appId.isBlank() || fileId <= 0L) {
            throw IOException("Invalid Uptodown file")
        }
        val root = apiGet("/apps/${app.appId}/file/$fileId/downloadUrl?update=0")
        val data = root.objectOrNull("data")
            ?: throw IOException("Uptodown returned no download data")
        val url = data.string("downloadURL")
        if (url.isBlank()) throw IOException("Uptodown returned no download URL")
        return UptodownDownload(url, data.string("sha256"))
    }

    private fun apiGet(path: String, retried: Boolean = false): JsonObject {
        val request = Request.Builder()
            .url(if (path.startsWith("http")) path else "$UPTODOWN_API_BASE$path")
            .header("User-Agent", UPTODOWN_CLIENT_UA)
            .header("Identificador", "Uptodown_Android")
            .header("Identificador-Version", UPTODOWN_CLIENT_VERSION)
            .header("Authorization", "Bearer ${authorize(force = retried)}")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401 && !retried) {
                return apiGet(path, retried = true)
            }
            if (!response.isSuccessful) {
                throw IOException("Uptodown API HTTP ${response.code}")
            }
            return parseObject(response.body?.string().orEmpty())
        }
    }

    private fun browserGet(url: String): JsonObject {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UPTODOWN_BROWSER_UA)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Uptodown versions HTTP ${response.code}")
            }
            return parseObject(response.body?.string().orEmpty())
        }
    }

    private fun authorize(force: Boolean): String {
        val now = System.currentTimeMillis()
        val cached = token
        if (!force && cached != null && now < tokenExpiresAt - 60_000L) return cached

        val timestamp = (now / 1000L).toString()
        val identifier = randomHex(8)
        val signature = hmac(timestamp)
        val form = FormBody.Builder()
            .add("identifier", identifier)
            .add("id_plataforma", "13")
            .add("lang", "en")
            .add("unixtime", timestamp)
            .add("hmac", signature)
            .build()
        val request = Request.Builder()
            .url("$UPTODOWN_API_BASE/auth/token?identifier=$identifier")
            .header("User-Agent", UPTODOWN_CLIENT_UA)
            .header("Identificador", "Uptodown_Android")
            .header("Identificador-Version", UPTODOWN_CLIENT_VERSION)
            .post(form)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Uptodown auth HTTP ${response.code}")
            }
            val value = parseObject(response.body?.string().orEmpty()).string("token")
            if (value.isBlank()) throw IOException("Uptodown auth returned no token")
            token = value
            tokenExpiresAt = jwtExpiry(value, now + 30L * 60_000L)
            return value
        }
    }

    private fun jwtExpiry(jwt: String, fallback: Long): Long {
        val payload = jwt.split('.').getOrNull(1) ?: return fallback
        return runCatching {
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decoded = String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
            val expiry = parseObject(decoded).long("exp")
            if (expiry > 0L) expiry * 1000L else fallback
        }.getOrDefault(fallback)
    }

    private fun hmac(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(UPTODOWN_HMAC_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return hex(mac.doFinal(value.toByteArray(Charsets.UTF_8)))
    }

    private fun randomHex(bytes: Int): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return hex(data)
    }

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun normalizeKind(value: String): String {
        val lower = value.trim().lowercase(Locale.US)
        return when (lower) {
            "xapk", "apks", "apkm" -> "xapk"
            "" -> ""
            else -> "apk"
        }
    }

    private fun parseObject(value: String): JsonObject =
        JsonParser.parseString(value).asJsonObject

    private fun JsonObject.objectOrNull(name: String): JsonObject? =
        get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.arrayOrNull(name: String): JsonArray? =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.string(name: String): String =
        get(name)?.takeIf { !it.isJsonNull }?.asString.orEmpty()

    private fun JsonObject.long(name: String): Long =
        get(name)?.takeIf { !it.isJsonNull }?.asLong ?: 0L

    private companion object {
        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
    }
}
