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
    fun testRestoreItem() = runBlocking {
        val file = tempFolder.newFile("restore_me.txt").apply { writeText("Restore test content") }
        val origPath = file.absolutePath
        val trashed = dbHelper.moveToRecycleBin(listOf(file), context).first()

        assertFalse(File(origPath).exists())
        assertEquals(1, dbHelper.getItemCount())

        val result = dbHelper.restoreItem(trashed.id, context)
        assertNotNull(result)
        assertEquals(origPath, result!!.second.absolutePath)
        assertTrue(File(origPath).exists())
        assertEquals("Restore test content", File(origPath).readText())
        assertEquals(0, dbHelper.getItemCount())
        assertFalse(File(trashed.trashedPath).exists())
    }

    @Test
    fun testRestoreItemRecreatesMissingParentDirectory() = runBlocking {
        val subDir = tempFolder.newFolder("nested_folder")
        val file = File(subDir, "deep_file.txt").apply { writeText("Deep content") }
        val origPath = file.absolutePath
        val trashed = dbHelper.moveToRecycleBin(listOf(file), context).first()

        // Delete parent folder
        subDir.deleteRecursively()
        assertFalse(subDir.exists())

        val result = dbHelper.restoreItem(trashed.id, context)
        assertNotNull(result)
        assertTrue(subDir.exists())
        assertTrue(File(origPath).exists())
        assertEquals("Deep content", File(origPath).readText())
    }

    @Test
    fun testRestoreItemWithCollisionAppendsRestoredSuffix() = runBlocking {
        val file = tempFolder.newFile("duplicate.txt").apply { writeText("Original in trash") }
        val origPath = file.absolutePath
        val trashed = dbHelper.moveToRecycleBin(listOf(file), context).first()

        // Recreate file at original path
        File(origPath).writeText("New file at original path")

        val result = dbHelper.restoreItem(trashed.id, context)
        assertNotNull(result)
        val restoredFile = result!!.second
        assertTrue(restoredFile.name.contains("(restored)"))
        assertTrue(restoredFile.exists())
        assertEquals("Original in trash", restoredFile.readText())
        assertEquals("New file at original path", File(origPath).readText())
    }

    @Test
    fun testRestoreAllItems() = runBlocking {
        val file1 = tempFolder.newFile("file1.txt").apply { writeText("1") }
        val file2 = tempFolder.newFile("file2.txt").apply { writeText("2") }

        dbHelper.moveToRecycleBin(listOf(file1, file2), context)
        assertEquals(2, dbHelper.getItemCount())

        val restored = dbHelper.restoreAllItems(context)
        assertEquals(2, restored.size)
        assertEquals(0, dbHelper.getItemCount())
        assertTrue(file1.exists())
        assertTrue(file2.exists())
    }

    @Test
    fun testPermanentlyDeleteItem() = runBlocking {
        val file = tempFolder.newFile("purge_me.txt").apply { writeText("Purge") }
        val trashed = dbHelper.moveToRecycleBin(listOf(file), context).first()
        assertTrue(File(trashed.trashedPath).exists())

        val success = dbHelper.permanentlyDeleteItem(trashed.id, context)
        assertTrue(success)
        assertFalse(File(trashed.trashedPath).exists())
        assertEquals(0, dbHelper.getItemCount())
    }

    @Test
    fun testEmptyRecycleBin() = runBlocking {
        val file1 = tempFolder.newFile("f1.txt").apply { writeText("1") }
        val file2 = tempFolder.newFile("f2.txt").apply { writeText("2") }
        val trashed = dbHelper.moveToRecycleBin(listOf(file1, file2), context)

        assertEquals(2, dbHelper.getItemCount())
        val deletedCount = dbHelper.emptyRecycleBin(context)
        assertEquals(0, dbHelper.getItemCount())
        trashed.forEach { assertFalse(File(it.trashedPath).exists()) }
    }

    @Test
    fun testCleanupExpiredItems() = runBlocking {
        val file1 = tempFolder.newFile("expired.txt").apply { writeText("Expired") }
        val file2 = tempFolder.newFile("fresh.txt").apply { writeText("Fresh") }

        val trashed = dbHelper.moveToRecycleBin(listOf(file1, file2), context)
        assertEquals(2, trashed.size)

        // Manually set item 1 expiry to the past in database
        val db = dbHelper.writableDatabase
        val pastTime = System.currentTimeMillis() - 10000L
        val values = android.content.ContentValues().apply {
            put(RecycleBinDatabaseHelper.COLUMN_EXPIRY_TIMESTAMP, pastTime)
        }
        db.update(RecycleBinDatabaseHelper.TABLE_RECYCLE_BIN, values, "${RecycleBinDatabaseHelper.COLUMN_ID} = ?", arrayOf(trashed[0].id.toString()))

        val expired = dbHelper.cleanupExpiredItems(context)
        assertEquals(1, expired.size)
        assertEquals(trashed[0].id, expired[0].id)
        assertFalse(File(trashed[0].trashedPath).exists())

        // Non-expired item remains
        assertEquals(1, dbHelper.getItemCount())
        assertTrue(File(trashed[1].trashedPath).exists())
    }
}
