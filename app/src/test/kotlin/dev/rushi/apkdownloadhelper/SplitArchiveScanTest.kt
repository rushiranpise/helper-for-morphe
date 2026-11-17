package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.sources.*
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.*
import dev.rushi.apkdownloadhelper.updater.*

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitArchiveScanTest {

    private fun zipWithEntries(entries: List<String>): File {
        val file = File.createTempFile("split-scan", ".apk")
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write("placeholder".toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun `a split container named apk is detected from its inner apks`() {
        val file = zipWithEntries(listOf("base.apk", "split_config.arm64_v8a.apk"))
        try {
            assertTrue(file.isSplitApkContainer())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `a plain apk is not treated as a split container`() {
        val file = zipWithEntries(listOf("AndroidManifest.xml", "classes.dex"))
        try {
            assertFalse(file.isSplitApkContainer())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `a non-zip apk is not treated as a split container`() {
        val file = File.createTempFile("split-scan", ".apk")
        try {
            file.writeText("not a zip")
            assertFalse(file.isSplitApkContainer())
        } finally {
            file.delete()
        }
    }
}
