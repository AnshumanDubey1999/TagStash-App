package com.anshuman.tagstash.data.utils

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.ui.graphics.Color
import com.anshuman.tagstash.data.clipboard.AppClipboard
import com.anshuman.tagstash.data.clipboard.ClipboardItem
import com.anshuman.tagstash.data.clipboard.ClipboardOpType
import com.anshuman.tagstash.data.database.AuditLogDatabaseHelper
import com.anshuman.tagstash.data.database.RecycleBinDatabaseHelper
import com.anshuman.tagstash.data.model.AuditActionType
import com.anshuman.tagstash.data.model.AuditItemOutcome
import com.anshuman.tagstash.data.model.AuditLogEntry
import com.anshuman.tagstash.data.model.AuditLogItemDetail
import com.anshuman.tagstash.data.model.FileItem
import com.anshuman.tagstash.ui.components.OperationProgressState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun executePasteOperations(
    context: Context,
    currentDirectory: File,
    itemsToPaste: List<ClipboardItem>,
    primaryColor: Color,
    onProgressUpdate: (OperationProgressState?) -> Unit,
    onConflictDetected: suspend (File) -> Pair<ConflictResolution, Boolean>
) = withContext(Dispatchers.IO) {
    var batchResolution: ConflictResolution? = null
    val auditDetails = mutableListOf<AuditLogItemDetail>()

    for ((index, item) in itemsToPaste.withIndex()) {
        val sourceFile = item.file
        val isCut = item.opType == ClipboardOpType.CUT
        onProgressUpdate(
            OperationProgressState(
                title = if (isCut) "Moving items..." else "Copying items...",
                currentItem = index + 1,
                totalItems = itemsToPaste.size,
                currentFileName = sourceFile.name,
                icon = if (isCut) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                iconTint = primaryColor
            )
        )
        if (!sourceFile.exists()) continue

        if (sourceFile.parentFile?.absolutePath == currentDirectory.absolutePath && item.opType == ClipboardOpType.CUT) {
            continue
        }

        var targetFile = File(currentDirectory, sourceFile.name)
        var outcome: AuditItemOutcome = if (item.opType == ClipboardOpType.CUT) AuditItemOutcome.MOVED else AuditItemOutcome.COPIED
        var actualDestPath: String? = targetFile.absolutePath

        if (targetFile.exists()) {
            val resolution = if (batchResolution != null) {
                batchResolution
            } else {
                val (chosenResolution, applyToAll) = onConflictDetected(sourceFile)
                if (applyToAll) {
                    batchResolution = chosenResolution
                }
                chosenResolution
            }

            when (resolution) {
                ConflictResolution.REPLACE -> {
                    outcome = AuditItemOutcome.REPLACED
                    if (item.opType == ClipboardOpType.CUT) {
                        targetFile.deleteRecursively()
                        moveFileOrDirectory(sourceFile, targetFile)
                    } else {
                        copyFileOrDirectory(sourceFile, targetFile)
                    }
                }
                ConflictResolution.KEEP_BOTH -> {
                    outcome = AuditItemOutcome.RENAMED_COPY
                    val newName = generateCopyFileName(currentDirectory, sourceFile.name)
                    targetFile = File(currentDirectory, newName)
                    actualDestPath = targetFile.absolutePath
                    if (item.opType == ClipboardOpType.CUT) {
                        moveFileOrDirectory(sourceFile, targetFile)
                    } else {
                        copyFileOrDirectory(sourceFile, targetFile)
                    }
                }
                ConflictResolution.SKIP -> {
                    outcome = AuditItemOutcome.SKIPPED
                    actualDestPath = null
                }
            }
        } else {
            if (item.opType == ClipboardOpType.CUT) {
                moveFileOrDirectory(sourceFile, targetFile)
            } else {
                copyFileOrDirectory(sourceFile, targetFile)
            }
        }

        auditDetails.add(
            AuditLogItemDetail(
                sourcePath = sourceFile.absolutePath,
                destinationPath = actualDestPath,
                command = if (item.opType == ClipboardOpType.CUT) "CUT" else "COPY",
                outcome = outcome,
                fileName = sourceFile.name,
                fileSize = sourceFile.length(),
                isDirectory = sourceFile.isDirectory
            )
        )
    }

    if (auditDetails.isNotEmpty()) {
        val hasCut = itemsToPaste.any { it.opType == ClipboardOpType.CUT }
        val summaryText = if (itemsToPaste.size == 1) {
            val op = if (itemsToPaste.first().opType == ClipboardOpType.CUT) "Moved" else "Copied"
            "$op ${itemsToPaste.first().file.name} to ${currentDirectory.name.ifEmpty { "/" }}"
        } else {
            val op = if (hasCut) "Moved" else "Copied"
            "$op ${itemsToPaste.size} items to ${currentDirectory.name.ifEmpty { "/" }}"
        }

        val auditLogHelper = AuditLogDatabaseHelper.getInstance(context)
        auditLogHelper.insertLog(
            AuditLogEntry(
                timestamp = System.currentTimeMillis(),
                actionType = AuditActionType.PASTE,
                summary = summaryText,
                destinationDirectory = currentDirectory.name.ifEmpty { "/" },
                totalItems = itemsToPaste.size,
                items = auditDetails
            )
        )
    }

    AppClipboard.clear()
    onProgressUpdate(null)
}

suspend fun executeDeleteOperations(
    context: Context,
    filesToDelete: List<File>,
    errorColor: Color,
    onProgressUpdate: (OperationProgressState?) -> Unit
) = withContext(Dispatchers.IO) {
    val toDeleteCount = filesToDelete.size
    onProgressUpdate(
        OperationProgressState(
            title = "Moving to Recycle Bin...",
            currentItem = 1,
            totalItems = toDeleteCount,
            currentFileName = filesToDelete.firstOrNull()?.name ?: "",
            icon = Icons.Default.DeleteOutline,
            iconTint = errorColor
        )
    )
    val trashed = RecycleBinDatabaseHelper.getInstance(context).moveToRecycleBin(
        filesToDelete,
        context
    ) { current, total, name ->
        onProgressUpdate(
            OperationProgressState(
                title = "Moving to Recycle Bin...",
                currentItem = current,
                totalItems = total,
                currentFileName = name,
                icon = Icons.Default.DeleteOutline,
                iconTint = errorColor
            )
        )
    }
    onProgressUpdate(null)

    if (trashed.isNotEmpty()) {
        val auditLogHelper = AuditLogDatabaseHelper.getInstance(context)
        val details = trashed.map {
            AuditLogItemDetail(
                sourcePath = it.originalPath,
                destinationPath = it.trashedPath,
                command = "DELETE",
                outcome = AuditItemOutcome.TRASHED,
                fileName = it.fileName,
                fileSize = it.fileSize,
                isDirectory = it.isDirectory
            )
        }
        val summaryText = if (trashed.size == 1) "Deleted ${trashed.first().fileName} to Recycle Bin" else "Deleted ${trashed.size} items to Recycle Bin"
        auditLogHelper.insertLog(
            AuditLogEntry(
                timestamp = System.currentTimeMillis(),
                actionType = AuditActionType.DELETE,
                summary = summaryText,
                destinationDirectory = "Recycle Bin",
                totalItems = trashed.size,
                items = details
            )
        )
    }
}

suspend fun executeRenameOperation(
    context: Context,
    oldFile: File,
    newName: String
): Boolean = withContext(Dispatchers.IO) {
    val parent = oldFile.parentFile ?: File("/")
    val targetFile = File(parent, newName)
    val success = oldFile.renameTo(targetFile)
    if (success) {
        val auditLogHelper = AuditLogDatabaseHelper.getInstance(context)
        val itemDetail = AuditLogItemDetail(
            sourcePath = oldFile.absolutePath,
            destinationPath = targetFile.absolutePath,
            command = "RENAME",
            outcome = AuditItemOutcome.RENAMED,
            fileName = targetFile.name,
            fileSize = targetFile.length(),
            isDirectory = targetFile.isDirectory
        )
        auditLogHelper.insertLog(
            AuditLogEntry(
                timestamp = System.currentTimeMillis(),
                actionType = AuditActionType.RENAME,
                summary = "Renamed ${oldFile.name} to ${targetFile.name}",
                destinationDirectory = parent.name.ifEmpty { "/" },
                totalItems = 1,
                items = listOf(itemDetail)
            )
        )
    }
    success
}

suspend fun executeFolderSearch(
    directory: File,
    query: String,
    includeSubfolders: Boolean,
    onCountUpdate: (Int) -> Unit
): List<FileItem> = withContext(Dispatchers.IO) {
    val matching = mutableListOf<FileItem>()
    val queryLower = query.lowercase()
    var count = 0

    fun scanDir(dir: File, recurse: Boolean) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            count++
            if (count % 20 == 0) {
                onCountUpdate(count)
            }
            if (file.name.lowercase().contains(queryLower)) {
                val childCount = if (file.isDirectory) file.listFiles()?.size ?: 0 else 0
                matching.add(
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
                        size = if (file.isDirectory) 0L else file.length(),
                        lastModified = file.lastModified(),
                        childCount = childCount
                    )
                )
            }
            if (recurse && file.isDirectory && file.canRead()) {
                scanDir(file, true)
            }
        }
    }

    try {
        scanDir(directory, includeSubfolders)
    } catch (_: Exception) {
    }

    onCountUpdate(count)
    matching.sortedWith(
        compareBy<FileItem> { !it.isDirectory }
            .thenBy { it.name.lowercase() }
    )
}
