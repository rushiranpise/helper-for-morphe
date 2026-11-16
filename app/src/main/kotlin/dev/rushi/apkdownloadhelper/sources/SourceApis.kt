package dev.rushi.apkdownloadhelper.sources

import android.os.Build
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.random.Random


internal interface ApkMirrorApi {
    @Headers(
        "Authorization: Basic YXBpLWFwa3VwZGF0ZXI6cm01cmNmcnVVakt5MDRzTXB5TVBKWFc4",
        "Content-Type: application/json"
    )
    @POST("wp-json/apkm/v1/app_exists/")
    suspend fun appExists(@Body request: ApkMirrorAppExistsRequest): ApkMirrorAppExistsResponse
}

internal data class ApkMirrorAppExistsRequest(
    val pnames: List<String>,
    val exclude: List<String> = listOf("alpha", "beta")
)

internal data class ApkMirrorAppExistsResponse(
    val data: List<ApkMirrorAppExistsData> = emptyList()
)

internal data class ApkMirrorAppExistsData(
    val pname: String = "",
    val exists: Boolean = false,
    val release: ApkMirrorAppExistsRelease = ApkMirrorAppExistsRelease(),
    val apks: List<ApkMirrorAppExistsApk> = emptyList()
)

internal data class ApkMirrorAppExistsRelease(
    val version: String = "",
    val link: String = ""
)

internal data class ApkMirrorAppExistsApk(
    @SerializedName("version_code")
    val versionCode: String = "",
    val link: String = "",
    val arches: List<String> = emptyList(),
    val dpis: List<String>? = null,
    @SerializedName("download_types")
    val downloadTypes: List<String> = emptyList()
)

internal interface ApkPureApi {
    @Headers(
        "content-type: application/json",
        "ual-access-businessid: projecta"
    )
    @POST("v3/get_app_update")
    suspend fun getAppUpdate(
        @Header("ual-access-projecta") header: String,
        @Body request: ApkPureUpdateRequest
    ): ApkPureUpdateResponse
}

internal data class ApkPureUpdateRequest(
    val app_info_for_update: List<ApkPureAppInfo>,
    val android_id: String = Random.nextLong().toString(16),
    val application_id: String = "com.apkpure.aegon",
    val cached_size: Long = -1
)

internal data class ApkPureAppInfo(
    val package_name: String,
    val version_code: Long,
    val is_system: Boolean = false,
    val version_id: String = "",
    val cached_size: Int = -1
)

internal data class ApkPureUpdateResponse(
    val retcode: Int = 0,
    val app_update_response: List<ApkPureAppUpdate> = emptyList()
)

internal data class ApkPureAppUpdate(
    val package_name: String = "",
    val version_code: Long = 0L,
    val version_name: String = "",
    val label: String = "",
    val asset: ApkPureAsset = ApkPureAsset()
)

internal data class ApkPureAsset(
    val type: String = "",
    val url: String = ""
)

internal data class ApkPureVersionEntry(
    val versionName: String?,
    val versionCode: Long?,
    val downloadPageUrl: String,
    val fileKind: String
)

internal data class ApkPureDeviceHeader(
    val device_info: ApkPureDeviceInfo = ApkPureDeviceInfo()
)

internal data class ApkPureDeviceInfo(
    val abis: List<String> = runCatching { Build.SUPPORTED_ABIS.toList() }.getOrDefault(emptyList()),
    val android_id: String = Random.nextLong().toString(16),
    val os_ver: String = runCatching { Build.VERSION.SDK_INT.toString() }.getOrDefault(""),
    val os_ver_name: String = runCatching { Build.VERSION.RELEASE }.getOrNull() ?: "",
    val platform: Int = 1
)

internal interface AptoideApi {
    @POST("listSearchApps")
    suspend fun searchApps(@Body request: AptoideSearchRequest): AptoideSearchResponse

    @GET("getApp")
    suspend fun getAppByPackage(@Query("package_name") packageName: String): AptoideGetAppResponse

    @GET("getApp")
    suspend fun getAppById(@Query("app_id") appId: Long): AptoideGetAppResponse

    @GET("listAppVersions")
    suspend fun listAppVersionsByPackage(
        @Query("package_name") packageName: String,
        @Query("limit") limit: Long = 100L
    ): AptoideVersionListResponse

    @GET("listAppVersions")
    suspend fun listAppVersionsById(
        @Query("app_id") appId: Long,
        @Query("limit") limit: Long = 100L
    ): AptoideVersionListResponse
}

internal data class AptoideSearchRequest(
    val query: String = "",
    val limit: String = "10",
    val q: String? = null,
    val not_apk_tags: String = "alpha,beta",
    val store_ids: List<Long>? = listOf(15L, 711454L)
)

internal data class AptoideSearchResponse(
    val datalist: AptoideDataList = AptoideDataList()
)

internal data class AptoideDataList(
    val list: List<AptoideApp> = emptyList()
)

internal data class AptoideGetAppResponse(
    val nodes: AptoideNodes = AptoideNodes()
)

internal data class AptoideVersionListResponse(
    val list: List<AptoideApp> = emptyList()
)

internal data class AptoideNodes(
    val meta: AptoideMetaNode = AptoideMetaNode()
)

internal data class AptoideMetaNode(
    val data: AptoideApp = AptoideApp()
)

internal data class AptoideNextData(
    val props: AptoideNextProps = AptoideNextProps()
)

internal data class AptoideNextProps(
    val pageProps: AptoidePageProps = AptoidePageProps()
)

internal data class AptoidePageProps(
    val app: AptoideApp = AptoideApp(),
    val packageName: String = "",
    val versions: List<AptoideVersionItem> = emptyList()
)

internal data class AptoideVersionItem(
    val id: Long = 0L,
    val name: String = "",
    val vername: String = "",
    val vercode: Long = 0L
)

internal data class AptoideApp(
    val id: Long = 0L,
    val name: String = "",
    @SerializedName("package")
    val packageName: String = "",
    val file: AptoideFile = AptoideFile(),
    val urls: AptoideUrls = AptoideUrls()
)

internal data class AptoideFile(
    val vername: String = "",
    val vercode: String = "0",
    val path: String = "",
    /** Aptoide's own malware verdict for this build; not every endpoint reports it. */
    val malware: AptoideMalware? = null,
    @SerializedName(value = "path_alt", alternate = ["pathAlt"])
    val pathAlt: String = ""
)

internal data class AptoideMalware(
    val rank: String = ""
)

internal data class AptoideUrls(
    val w: String = "",
    val m: String = ""
)




