package com.anshuman.tagstash

import com.anshuman.tagstash.data.model.FileItem
import com.anshuman.tagstash.data.utils.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.webkit.MimeTypeMap
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileHelperTest {

    @Test
    fun testFormatBreadcrumbName() {
        // Under 10 chars
        assertEquals("Home", formatBreadcrumbName("Home"))
        assertEquals("Download", formatBreadcrumbName("Download"))
        // Exactly 10 chars
        assertEquals("abcdefghij", formatBreadcrumbName("abcdefghij"))
        // Over 10 chars -> should truncate to 7 chars + "..." (total 10 chars)
        assertEquals("VeryLon...", formatBreadcrumbName("VeryLongFolderName"))
        assertEquals("abcdefg...", formatBreadcrumbName("abcdefghijkl"))
    }

    @Test
    fun testBuildBreadcrumbs() {
        val home = File("/storage/emulated/0")
        val crumbsHome = buildBreadcrumbs(home)
        assertEquals(1, crumbsHome.size)
        assertEquals("Home", crumbsHome[0].first)
        assertEquals(home.absolutePath, crumbsHome[0].second.absolutePath)

        val documents = File("/storage/emulated/0/Documents")
        val crumbsDocs = buildBreadcrumbs(documents)
        assertEquals(2, crumbsDocs.size)
        assertEquals("Home", crumbsDocs[0].first)
        assertEquals("Documents", crumbsDocs[1].first)
        assertEquals(documents.absolutePath, crumbsDocs[1].second.absolutePath)

        val reports = File("/storage/emulated/0/Documents/Reports/2026")
        val crumbsReports = buildBreadcrumbs(reports)
        assertEquals(4, crumbsReports.size)
        assertEquals("Home", crumbsReports[0].first)
        assertEquals("Documents", crumbsReports[1].first)
        assertEquals("Reports", crumbsReports[2].first)
        assertEquals("2026", crumbsReports[3].first)
        assertEquals(reports.absolutePath, crumbsReports[3].second.absolutePath)
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", formatFileSize(0L))
        assertEquals("0 B", formatFileSize(-100L))
        assertEquals("512.0 B", formatFileSize(512L))
        assertEquals("1.0 KB", formatFileSize(1024L))
        assertEquals("1.5 KB", formatFileSize(1536L))
        assertEquals("1.0 MB", formatFileSize(1024L * 1024L))
        assertEquals("2.3 MB", formatFileSize((2.3 * 1024 * 1024).toLong()))
        assertEquals("1.0 GB", formatFileSize(1024L * 1024L * 1024L))
    }

    @Test
    fun testFormatLastModified() {
        val timestamp = 1782554400000L
        val formatted = formatLastModified(timestamp)
        assertTrue(formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")))
    }

    @Test
    fun testFileTypeCheckers() {
        // Image formats
        assertTrue(isImage("photo.png"))
        assertTrue(isImage("PICTURE.JPG"))
        assertTrue(isImage("anim.gif"))
        assertTrue(isImage("photo.avif"))
        assertTrue(isImage("PICTURE.AVIF"))
        assertFalse(isImage("doc.pdf"))

        // Video formats
        assertTrue(isVideo("movie.mp4"))
        assertTrue(isVideo("film.mkv"))
        assertFalse(isVideo("song.mp3"))

        // Audio formats
        assertTrue(isAudio("music.mp3"))
        assertTrue(isAudio("sound.wav"))
        assertFalse(isAudio("image.jpg"))

        // Document formats
        assertTrue(isDocument("doc.pdf"))
        assertTrue(isDocument("notes.txt"))
        assertTrue(isDocument("data.json"))
        assertFalse(isDocument("script.sh"))
    }

    @Test
    fun testGetMimeType() {
        val shadowMap = shadowOf(MimeTypeMap.getSingleton())
        shadowMap.addExtensionMimeTypeMapping("png", "image/png")
        shadowMap.addExtensionMimeTypeMapping("mp4", "video/mp4")

        assertEquals("image/png", getMimeType(File("test.png")))
        assertEquals("image/avif", getMimeType(File("test.avif")))
        assertEquals("image/avif", getMimeType(File("test.AVIF")))
        assertEquals("video/mp4", getMimeType(File("test.mp4")))
    }

    @Test
    fun testGetSiblingMedia() {
        val testDataDir = if (File("src/test/resources/testData").exists()) {
            File("src/test/resources/testData")
        } else {
            File("app/src/test/resources/testData")
        }
        val pngsFolder = File(testDataDir, "images/pngs")
        val file1 = File(pngsFolder, "1.png")
        
        val siblings = getSiblingMedia(file1)
        
        assertEquals(4, siblings.size)
        assertEquals("1.png", siblings[0].name)
        assertEquals("2.png", siblings[1].name)
        assertEquals("3.png", siblings[2].name)
        assertEquals("4.png", siblings[3].name)

        // Verify combined image and video list under videos directory
        val videosFolder = File(testDataDir, "videos")
        val videoFile = File(videosFolder, "1.mp4")
        val mediaSiblings = getSiblingMedia(videoFile)
        
        assertEquals(7, mediaSiblings.size)
        assertEquals("1.mp4", mediaSiblings[0].name)
        assertEquals("2.mp4", mediaSiblings[1].name)
        assertEquals("2.png", mediaSiblings[2].name)
        assertEquals("3.mp4", mediaSiblings[3].name)
        assertEquals("3.webp", mediaSiblings[4].name)
        assertEquals("4.gif", mediaSiblings[5].name)
        assertEquals("4.mp4", mediaSiblings[6].name)
    }

    @Test
    fun testGetImageDimensions() {
        val testDataDir = if (File("src/test/resources/testData").exists()) {
            File("src/test/resources/testData")
        } else {
            File("app/src/test/resources/testData")
        }
        val pngsFolder = File(testDataDir, "images/pngs")
        val file1 = File(pngsFolder, "1.png")
        
        val dims = getImageDimensions(file1)
        assertTrue(dims.width > 0)
        assertTrue(dims.height > 0)

        val singleFolder = File(testDataDir, "images/single_image")
        val avifFile = File(singleFolder, "sample.avif")
        val avifDims = getImageDimensions(avifFile)
        assertNotNull(avifDims)
    }

    @Test
    fun testPreloadIndicesCalculation() {
        val getPreloadIndices = { currentIndex: Int, size: Int ->
            val targets = listOf(
                currentIndex + 1, // 1st Next
                currentIndex - 1, // 1st Prev
                currentIndex + 2  // 2nd Next
            )
            targets.filter { it in 0 until size }
        }

        // Start of list (size 10)
        assertEquals(listOf(1, 2), getPreloadIndices(0, 10))

        // Middle of list (size 10)
        assertEquals(listOf(6, 4, 7), getPreloadIndices(5, 10))

        // Near end of list (size 10)
        assertEquals(listOf(9, 7), getPreloadIndices(8, 10))

        // End of list (size 10)
        assertEquals(listOf(8), getPreloadIndices(9, 10))

        // Single item list (size 1)
        assertEquals(emptyList<Int>(), getPreloadIndices(0, 1))

        // Empty list (size 0)
        assertEquals(emptyList<Int>(), getPreloadIndices(0, 0))
    }
}
