package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.sources.*
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.*
import dev.rushi.apkdownloadhelper.updater.*

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadMimeTypeTest {

    @Test
    fun fileMimeType_keepsArchiveSuffixByUsingGenericType() {
        assertEquals("application/vnd.android.package-archive", File("app.apk").mimeType())
        assertEquals("application/octet-stream", File("app.apkm").mimeType())
        assertEquals("application/octet-stream", File("app.apks").mimeType())
        assertEquals("application/octet-stream", File("app.xapk").mimeType())
        assertEquals("application/octet-stream", File("app.zip").mimeType())
    }

    @Test
    fun fileNameMimeType_keepsArchiveSuffixByUsingGenericType() {
        assertEquals("application/vnd.android.package-archive", fileNameMimeType("app.apk"))
        assertEquals("application/octet-stream", fileNameMimeType("app.apkm"))
        assertEquals("application/octet-stream", fileNameMimeType("app.apks"))
        assertEquals("application/octet-stream", fileNameMimeType("app.xapk"))
        assertEquals("application/octet-stream", fileNameMimeType("app.zip"))
    }
}
