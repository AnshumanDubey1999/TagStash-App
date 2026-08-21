package com.anshuman.tagstash

import com.anshuman.tagstash.data.model.FileTag
import com.anshuman.tagstash.data.utils.getFilePropertiesData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileMetadataHelperTest {

    @Test
    fun testGetFilePropertiesDataForDirectory() = runBlocking {
        val tempDir = File.createTempFile("testDir", "").apply {
            delete()
            mkdir()
        }
        try {
            File(tempDir, "child1.txt").createNewFile()
            File(tempDir, "child2.txt").createNewFile()

            val data = getFilePropertiesData(tempDir)
            assertEquals(tempDir.name, data.fileName)
            assertEquals("folder", data.mimeType)
            assertTrue(data.isDirectory)

            val generalGroup = data.groups.find { it.title == "General" }
            assertNotNull(generalGroup)
            val containsItem = generalGroup?.items?.find { it.label == "Contains" }
            assertEquals("2 items", containsItem?.value)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testGetFilePropertiesDataForFile() = runBlocking {
        val tempFile = File.createTempFile("sampleDoc", ".txt").apply {
            writeText("Hello TagStash Properties")
        }
        try {
            val tag = FileTag(id = 1, name = "Important", colorHex = 0xFF4285F4.toInt())
            val data = getFilePropertiesData(tempFile, tags = listOf(tag))

            assertEquals(tempFile.name, data.fileName)
            assertFalse(data.isDirectory)
            assertEquals(1, data.tags.size)
            assertEquals("Important", data.tags.first().name)

            val generalGroup = data.groups.find { it.title == "General" }
            assertNotNull(generalGroup)
            val sizeItem = generalGroup?.items?.find { it.label == "Size" }
            assertNotNull(sizeItem)
            assertTrue(sizeItem?.value?.contains("bytes") == true)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testGetMultipleFilesPropertiesData() = runBlocking {
        val tempDir = File.createTempFile("testDirMulti", "").apply {
            delete()
            mkdir()
        }
        val file1 = File(tempDir, "pic.png").apply { writeText("fake image bytes") }
        val file2 = File(tempDir, "vid.mp4").apply { writeText("fake video bytes") }
        val subDir = File(tempDir, "subFolder").apply { mkdir() }

        try {
            val data = com.anshuman.tagstash.data.utils.getMultipleFilesPropertiesData(listOf(file1, file2, subDir))
            assertEquals("3 items selected", data.fileName)
            assertEquals("multiple", data.mimeType)

            val summaryGroup = data.groups.find { it.title == "Summary" }
            assertNotNull(summaryGroup)
            val totalItem = summaryGroup?.items?.find { it.label == "Total Selected" }
            assertNotNull(totalItem)
            assertTrue(totalItem?.value?.contains("3 items") == true)
            assertTrue(totalItem?.value?.contains("2 files") == true)
            assertTrue(totalItem?.value?.contains("1 folder") == true)

            val breakdownGroup = data.groups.find { it.title == "Content Breakdown" }
            assertNotNull(breakdownGroup)
            assertEquals("1", breakdownGroup?.items?.find { it.label == "Images" }?.value)
            assertEquals("1", breakdownGroup?.items?.find { it.label == "Videos" }?.value)
            assertEquals("1", breakdownGroup?.items?.find { it.label == "Folders" }?.value)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
