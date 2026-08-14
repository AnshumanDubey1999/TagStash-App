package com.anshuman.tagstash

import com.anshuman.tagstash.data.utils.copyFileOrDirectory
import com.anshuman.tagstash.data.utils.generateCopyFileName
import com.anshuman.tagstash.data.utils.moveFileOrDirectory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FileOperationHelperTest {

    @Test
    fun testGenerateCopyFileName() {
        val tempDir = File.createTempFile("testCopyNames", "").apply {
            delete()
            mkdir()
        }
        try {
            // First file doesn't exist -> photo (copy).jpg
            val candidate1 = generateCopyFileName(tempDir, "photo.jpg")
            assertEquals("photo (copy).jpg", candidate1)

            // Create photo (copy).jpg
            File(tempDir, "photo (copy).jpg").createNewFile()
            val candidate2 = generateCopyFileName(tempDir, "photo.jpg")
            assertEquals("photo (copy 1).jpg", candidate2)

            // Create photo (copy 1).jpg
            File(tempDir, "photo (copy 1).jpg").createNewFile()
            val candidate3 = generateCopyFileName(tempDir, "photo.jpg")
            assertEquals("photo (copy 2).jpg", candidate3)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testCopyAndMoveFile() = runBlocking {
        val tempDir = File.createTempFile("testOps", "").apply {
            delete()
            mkdir()
        }
        try {
            val sourceFile = File(tempDir, "source.txt").apply { writeText("Hello TagStash Ops") }
            val copyTarget = File(tempDir, "target_copy.txt")

            val copySuccess = copyFileOrDirectory(sourceFile, copyTarget)
            assertTrue(copySuccess)
            assertTrue(copyTarget.exists())
            assertEquals("Hello TagStash Ops", copyTarget.readText())
            assertTrue(sourceFile.exists()) // Original still exists

            val moveTarget = File(tempDir, "target_move.txt")
            val moveSuccess = moveFileOrDirectory(sourceFile, moveTarget)
            assertTrue(moveSuccess)
            assertTrue(moveTarget.exists())
            assertEquals("Hello TagStash Ops", moveTarget.readText())
            assertFalse(sourceFile.exists()) // Source should be deleted/moved
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testCopyDirectoryRecursively() = runBlocking {
        val tempDir = File.createTempFile("testDirOps", "").apply {
            delete()
            mkdir()
        }
        try {
            val sourceFolder = File(tempDir, "source_folder").apply { mkdir() }
            File(sourceFolder, "f1.txt").writeText("content 1")
            val sub = File(sourceFolder, "sub").apply { mkdir() }
            File(sub, "f2.txt").writeText("content 2")

            val targetFolder = File(tempDir, "target_folder")
            val copied = copyFileOrDirectory(sourceFolder, targetFolder)
            assertTrue(copied)
            assertTrue(File(targetFolder, "f1.txt").exists())
            assertEquals("content 1", File(targetFolder, "f1.txt").readText())
            assertTrue(File(targetFolder, "sub/f2.txt").exists())
            assertEquals("content 2", File(targetFolder, "sub/f2.txt").readText())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
