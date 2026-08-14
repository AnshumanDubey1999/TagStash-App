package com.anshuman.tagstash

import com.anshuman.tagstash.data.clipboard.AppClipboard
import com.anshuman.tagstash.data.clipboard.ClipboardOpType
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class ClipboardManagerTest {

    @Before
    fun setUp() {
        AppClipboard.clear()
    }

    @After
    fun tearDown() {
        AppClipboard.clear()
    }

    @Test
    fun testAddAndRetrieveItems() {
        val file1 = File("/storage/emulated/0/photo1.jpg")
        val file2 = File("/storage/emulated/0/video1.mp4")

        assertTrue(AppClipboard.addItem(file1, ClipboardOpType.COPY))
        assertTrue(AppClipboard.addItem(file2, ClipboardOpType.CUT))

        assertEquals(2, AppClipboard.size)
        assertTrue(AppClipboard.contains(file1))
        assertEquals(ClipboardOpType.COPY, AppClipboard.getOpType(file1))
        assertEquals(ClipboardOpType.CUT, AppClipboard.getOpType(file2))
    }

    @Test
    fun testUpdateExistingItemOperation() {
        val file1 = File("/storage/emulated/0/photo1.jpg")

        assertTrue(AppClipboard.addItem(file1, ClipboardOpType.COPY))
        assertEquals(1, AppClipboard.size)
        assertEquals(ClipboardOpType.COPY, AppClipboard.getOpType(file1))

        // Change to CUT
        assertTrue(AppClipboard.addItem(file1, ClipboardOpType.CUT))
        assertEquals(1, AppClipboard.size)
        assertEquals(ClipboardOpType.CUT, AppClipboard.getOpType(file1))
    }

    @Test
    fun testCapacityLimit1000() {
        for (i in 1..1000) {
            val file = File("/storage/emulated/0/file$i.txt")
            val added = AppClipboard.addItem(file, ClipboardOpType.COPY)
            assertTrue(added)
        }
        assertEquals(1000, AppClipboard.size)

        // 1001st item should be rejected
        val overflowFile = File("/storage/emulated/0/overflow.txt")
        val overflowAdded = AppClipboard.addItem(overflowFile, ClipboardOpType.COPY)
        assertFalse(overflowAdded)
        assertEquals(1000, AppClipboard.size)

        // Updating an existing file in full clipboard should still succeed
        val existingFile = File("/storage/emulated/0/file1.txt")
        val updateAdded = AppClipboard.addItem(existingFile, ClipboardOpType.CUT)
        assertTrue(updateAdded)
        assertEquals(1000, AppClipboard.size)
        assertEquals(ClipboardOpType.CUT, AppClipboard.getOpType(existingFile))
    }

    @Test
    fun testClearClipboard() {
        val file1 = File("/storage/emulated/0/photo1.jpg")
        AppClipboard.addItem(file1, ClipboardOpType.COPY)
        assertEquals(1, AppClipboard.size)

        AppClipboard.clear()
        assertEquals(0, AppClipboard.size)
        assertFalse(AppClipboard.contains(file1))
    }
}
