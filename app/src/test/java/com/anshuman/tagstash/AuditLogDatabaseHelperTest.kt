package com.anshuman.tagstash

import androidx.test.core.app.ApplicationProvider
import com.anshuman.tagstash.data.database.AuditLogDatabaseHelper
import com.anshuman.tagstash.data.model.AuditActionType
import com.anshuman.tagstash.data.model.AuditItemOutcome
import com.anshuman.tagstash.data.model.AuditLogEntry
import com.anshuman.tagstash.data.model.AuditLogItemDetail
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AuditLogDatabaseHelperTest {

    private lateinit var dbHelper: AuditLogDatabaseHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        dbHelper = AuditLogDatabaseHelper(context)
        runBlocking {
            dbHelper.clearAllLogs()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            dbHelper.clearAllLogs()
        }
        dbHelper.close()
    }

    @Test
    fun testInsertAndRetrieveAuditLog() = runBlocking {
        val items = listOf(
            AuditLogItemDetail(
                sourcePath = "/storage/emulated/0/DCIM/photo1.jpg",
                destinationPath = "/storage/emulated/0/Pictures/photo1.jpg",
                command = "COPY",
                outcome = AuditItemOutcome.COPIED,
                fileName = "photo1.jpg",
                fileSize = 1024L,
                isDirectory = false
            ),
            AuditLogItemDetail(
                sourcePath = "/storage/emulated/0/Movies/video1.mp4",
                destinationPath = "/storage/emulated/0/Pictures/video1 (copy).mp4",
                command = "CUT",
                outcome = AuditItemOutcome.RENAMED_COPY,
                fileName = "video1.mp4",
                fileSize = 20480L,
                isDirectory = false
            )
        )

        val entry = AuditLogEntry(
            timestamp = System.currentTimeMillis(),
            actionType = AuditActionType.PASTE,
            summary = "Pasted 2 items to Pictures",
            destinationDirectory = "/storage/emulated/0/Pictures",
            totalItems = 2,
            items = items
        )

        val insertedId = dbHelper.insertLog(entry)
        assertTrue(insertedId > 0)

        val logs = dbHelper.getAllLogs()
        assertEquals(1, logs.size)

        val retrieved = logs.first()
        assertEquals(insertedId, retrieved.id)
        assertEquals(AuditActionType.PASTE, retrieved.actionType)
        assertEquals("Pasted 2 items to Pictures", retrieved.summary)
        assertEquals("/storage/emulated/0/Pictures", retrieved.destinationDirectory)
        assertEquals(2, retrieved.totalItems)
        assertEquals(2, retrieved.items.size)

        // Item 1
        assertEquals("/storage/emulated/0/DCIM/photo1.jpg", retrieved.items[0].sourcePath)
        assertEquals("/storage/emulated/0/Pictures/photo1.jpg", retrieved.items[0].destinationPath)
        assertEquals("COPY", retrieved.items[0].command)
        assertEquals(AuditItemOutcome.COPIED, retrieved.items[0].outcome)
        assertEquals("photo1.jpg", retrieved.items[0].fileName)

        // Item 2
        assertEquals("/storage/emulated/0/Movies/video1.mp4", retrieved.items[1].sourcePath)
        assertEquals("/storage/emulated/0/Pictures/video1 (copy).mp4", retrieved.items[1].destinationPath)
        assertEquals("CUT", retrieved.items[1].command)
        assertEquals(AuditItemOutcome.RENAMED_COPY, retrieved.items[1].outcome)
        assertEquals("video1.mp4", retrieved.items[1].fileName)
    }

    @Test
    fun testDeleteSingleLog() = runBlocking {
        val entry1 = AuditLogEntry(
            timestamp = 1000L,
            actionType = AuditActionType.PASTE,
            summary = "Action 1",
            destinationDirectory = "/storage/emulated/0",
            totalItems = 1,
            items = emptyList()
        )
        val entry2 = AuditLogEntry(
            timestamp = 2000L,
            actionType = AuditActionType.PASTE,
            summary = "Action 2",
            destinationDirectory = "/storage/emulated/0",
            totalItems = 1,
            items = emptyList()
        )

        val id1 = dbHelper.insertLog(entry1)
        val id2 = dbHelper.insertLog(entry2)

        assertEquals(2, dbHelper.getAllLogs().size)

        // Delete id1
        val deleted = dbHelper.deleteLog(id1)
        assertTrue(deleted)

        val remaining = dbHelper.getAllLogs()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining.first().id)
    }

    @Test
    fun testClearAllLogs() = runBlocking {
        val entry1 = AuditLogEntry(
            timestamp = 1000L,
            actionType = AuditActionType.PASTE,
            summary = "Action 1",
            destinationDirectory = "/storage/emulated/0",
            totalItems = 1,
            items = emptyList()
        )
        dbHelper.insertLog(entry1)
        assertEquals(1, dbHelper.getAllLogs().size)

        dbHelper.clearAllLogs()
        assertEquals(0, dbHelper.getAllLogs().size)
    }
}
