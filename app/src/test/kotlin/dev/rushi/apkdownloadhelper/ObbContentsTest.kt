package dev.rushi.apkdownloadhelper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ObbContentsTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun container(name: String, vararg entries: Pair<String, Int>): File {
        val file = File(temp.root, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (entryName, size) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(ByteArray(size))
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun `obb entries inside a container are counted with their size`() {
        val file = container(
            "game.xapk",
            "Android/obb/com.example.game/main.1.com.example.game.obb" to 2048,
            "Android/obb/com.example.game/patch.1.com.example.game.obb" to 1024,
            "base.apk" to 512
        )

        val obb = obbContentsIn(file)!!

        assertEquals(2, obb.fileCount)
        assertEquals(3072L, obb.totalBytes)
        assertEquals("2 files · 3.0 KB", obb.summary)
    }

    @Test
    fun `a single obb reads only in the summary`() {
        val file = container(
            "game.apks",
            "Android/obb/com.example.game/main.1.com.example.game.obb" to 2048
        )

        assertEquals("1 file · 2.0 KB", obbContentsIn(file)!!.summary)
    }

    @Test
    fun `a container with no obb data reports nothing`() {
        val file = container("plain.apkm", "base.apk" to 512, "split_config.arm64_v8a.apk" to 256)

        assertNull(obbContentsIn(file))
    }

    @Test
    fun `the extension match ignores case`() {
        val file = container("game.apks", "Android/obb/com.example.game/main.1.OBB" to 16)

        assertEquals(1, obbContentsIn(file)!!.fileCount)
    }

    @Test
    fun `a file that is not a container reports nothing instead of throwing`() {
        val plain = File(temp.root, "not-a-zip.apk").apply { writeText("this is not a zip") }

        assertNull(obbContentsIn(plain))
        assertNull(obbContentsIn(File(temp.root, "missing.apk")))
    }
}
