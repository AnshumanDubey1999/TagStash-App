package com.anshuman.tagstash

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.anshuman.tagstash.data.database.RecycleBinDatabaseHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecycleBinDatabaseHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var dbHelper: RecycleBinDatabaseHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbHelper = RecycleBinDatabaseHelper(context)
        runBlocking {
            dbHelper.clearAll()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            dbHelper.clearAll()
        }
        dbHelper.close()
    }

    @Test
    fun testMoveFileToRecycleBin() = runBlocking {
        val testFile = tempFolder.newFile("document.pdf").apply {
            writeText("Hello Recycle Bin")
        }
        val originalPath = testFile.absolutePath
        val originalSize = testFile.length()

        val trashedItems = dbHelper.moveToRecycleBin(listOf(testFile), context)
        assertEquals(1, trashedItems.size)

        val trashed = trashedItems.first()
        assertEquals("document.pdf", trashed.fileName)
        assertEquals(originalPath, trashed.originalPath)
        assertEquals(originalSize, trashed.fileSize)
        assertFalse(trashed.isDirectory)
        assertTrue(trashed.expiryTimestamp > trashed.deletedTimestamp)
        assertEquals(3600_000L, trashed.expiryTimestamp - trashed.deletedTimestamp)

        // Original file should no longer exist
        assertFalse(File(originalPath).exists())
        // Trashed file must exist on disk
        assertTrue(File(trashed.trashedPath).exists())

        // Verify DB query
        val allItems = dbHelper.getAllItems()
        assertEquals(1, allItems.size)
        assertEquals(trashed.id, allItems.first().id)
        assertEquals(1, dbHelper.getItemCount())
    }

    @Test
    fun testMoveDirectoryToRecycleBin() = runBlocking {
        val testDir = tempFolder.newFolder("my_folder")
        val childFile = File(testDir, "child.txt").apply {
            writeText("Child file contents")
        }
        val originalPath = testDir.absolutePath

        val trashedItems = dbHelper.moveToRecycleBin(listOf(testDir), context)
        assertEquals(1, trashedItems.size)

        val trashed = trashedItems.first()
        assertEquals("my_folder", trashed.fileName)
        assertEquals(originalPath, trashed.originalPath)
        assertTrue(trashed.isDirectory)
        assertTrue(trashed.fileSize > 0)

        // Original directory should no longer exist
        assertFalse(File(originalPath).exists())
        // Trashed directory must exist on disk
        assertTrue(File(trashed.trashedPath).exists())
        assertTrue(File(trashed.trashedPath, "child.txt").exists())
    }

    @Test
    fun testDeleteItemFromRecycleBinDatabase() = runBlocking {
        val file1 = tempFolder.newFile("file1.txt").apply { writeText("1") }
        val file2 = tempFolder.newFile("file2.txt").apply { writeText("2") }

        val trashed = dbHelper.moveToRecycleBin(listOf(file1, file2), context)
        assertEquals(2, trashed.size)
        assertEquals(2, dbHelper.getItemCount())

        val deleted = dbHelper.deleteItem(trashed[0].id)
        assertTrue(deleted)
        assertEquals(1, dbHelper.getItemCount())
        assertEquals(trashed[1].id, dbHelper.getAllItems().first().id)
    }

    @Test
    fun testClearAllRecycleBinDatabase() = runBlocking {
        val file1 = tempFolder.newFile("file1.txt").apply { writeText("1") }
        dbHelper.moveToRecycleBin(listOf(file1), context)
        assertEquals(1, dbHelper.getItemCount())

        dbHelper.clearAll()
        assertEquals(0, dbHelper.getItemCount())
        assertTrue(dbHelper.getAllItems().isEmpty())
    }
}
