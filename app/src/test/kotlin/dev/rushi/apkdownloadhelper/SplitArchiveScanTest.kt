package dev.rushi.apkdownloadhelper

import dev.rushi.apkdownloadhelper.download.*
import dev.rushi.apkdownloadhelper.sources.*
import dev.rushi.apkdownloadhelper.storage.*
import dev.rushi.apkdownloadhelper.ui.*
import dev.rushi.apkdownloadhelper.updater.*

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `a plain apk is standalone whatever its extension says`() {
        val file = File.createTempFile("split-scan", ".apkm")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                listOf("AndroidManifest.xml", "classes.dex").forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write("placeholder".toByteArray())
                    zip.closeEntry()
                }
            }
            assertTrue(file.isStandaloneApkArchive())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `a split container is not a standalone apk`() {
        val file = zipWithEntries(listOf("base.apk", "split_config.arm64_v8a.apk"))
        try {
            assertFalse(file.isStandaloneApkArchive())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `a split container named apk is given the bundle extension`() {
        val file = zipWithEntries(listOf("base.apk", "split_config.arm64_v8a.apk"))
        try {
            assertEquals("apkm", file.normalizedArchiveExtension())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `a split container keeps a real bundle extension`() {
        val file = File.createTempFile("split-scan", ".apks")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                listOf("base.apk", "split_config.arm64_v8a.apk").forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write("placeholder".toByteArray())
                    zip.closeEntry()
                }
            }
            assertNull(file.normalizedArchiveExtension())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `a plain apk served as download php is given the apk extension`() {
        val file = File.createTempFile("split-scan", ".php")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                listOf("AndroidManifest.xml", "classes.dex").forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write("placeholder".toByteArray())
                    zip.closeEntry()
                }
            }
            assertEquals("apk", file.normalizedArchiveExtension())
        } finally {
            file.delete()
        }
    }
}
