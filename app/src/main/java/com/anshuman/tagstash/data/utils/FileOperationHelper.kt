package com.anshuman.tagstash.data.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class ConflictResolution {
    REPLACE,
    KEEP_BOTH,
    SKIP
}

/**
 * Generates an available copy file name in destDir.
 * If photo.jpg exists, tests 'photo (copy).jpg'.
 * If that also exists, tests 'photo (copy 1).jpg', 'photo (copy 2).jpg', etc.
 */
fun generateCopyFileName(destDir: File, originalName: String): String {
    val dotIndex = originalName.lastIndexOf('.')
    val baseName: String
    val extension: String

    if (dotIndex > 0) {
        baseName = originalName.substring(0, dotIndex)
        extension = originalName.substring(dotIndex)
    } else {
        baseName = originalName
        extension = ""
    }

    val firstCandidate = "$baseName (copy)$extension"
    if (!File(destDir, firstCandidate).exists()) {
        return firstCandidate
    }

    var counter = 1
    while (true) {
        val candidate = "$baseName (copy $counter)$extension"
        if (!File(destDir, candidate).exists()) {
            return candidate
        }
        counter++
    }
}

/**
 * Copies a file or directory recursively to target destination.
 */
suspend fun copyFileOrDirectory(source: File, target: File): Boolean = withContext(Dispatchers.IO) {
    try {
        if (source.isDirectory) {
            if (!target.exists()) {
                target.mkdirs()
            }
            source.listFiles()?.forEach { child ->
                val destChild = File(target, child.name)
                copyFileOrDirectory(child, destChild)
            }
            target.setLastModified(source.lastModified())
            true
        } else {
            target.parentFile?.mkdirs()
            FileInputStream(source).use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            target.setLastModified(source.lastModified())
            true
        }
    } catch (_: Exception) {
        false
    }
}

/**
 * Moves a file or directory to target destination.
 */
suspend fun moveFileOrDirectory(source: File, target: File): Boolean = withContext(Dispatchers.IO) {
    try {
        if (source.absolutePath == target.absolutePath) {
            return@withContext true
        }
        target.parentFile?.mkdirs()
        if (source.renameTo(target)) {
            true
        } else {
            val copied = copyFileOrDirectory(source, target)
            if (copied) {
                source.deleteRecursively()
                true
            } else {
                false
            }
        }
    } catch (_: Exception) {
        false
    }
}
