package com.anshuman.tagstash.ui.components

import androidx.compose.runtime.Composable
import com.anshuman.tagstash.data.clipboard.AppClipboard
import com.anshuman.tagstash.data.utils.ConflictResolution
import com.anshuman.tagstash.ui.screens.MediaPlayerScreen
import java.io.File

@Composable
fun MainScreenDialogs(
    selectedPropertiesFile: File?,
    onDismissProperties: () -> Unit,
    showSelectionPropertiesDialog: Boolean,
    selectedFiles: Set<File>,
    onDismissSelectionProperties: () -> Unit,
    activeConflictFile: File?,
    onResolveConflict: (ConflictResolution, Boolean) -> Unit,
    onDismissConflict: () -> Unit,
    showClipboardDialog: Boolean,
    onDismissClipboard: () -> Unit,
    showCapacityLimitDialog: Boolean,
    onDismissCapacityLimit: () -> Unit,
    filesPendingDelete: List<File>?,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    operationProgress: OperationProgressState?,
    filePendingRename: File?,
    onConfirmRename: (String) -> Unit,
    onDismissRename: () -> Unit,
    activeMediaPlayerFile: File?,
    globalLoopEnabled: Boolean,
    onToggleGlobalLoop: (Boolean) -> Unit,
    onCloseMediaPlayer: () -> Unit,
    onNavigateToMedia: (File) -> Unit,
    onMediaPlayerSettingsClick: () -> Unit,
    showSearchDialog: Boolean,
    activeSearchQuery: String?,
    searchIncludeSubfolders: Boolean,
    onSearch: (String, Boolean) -> Unit,
    onDismissSearch: () -> Unit
) {
    if (selectedPropertiesFile != null) {
        FilePropertiesDialog(
            file = selectedPropertiesFile,
            onDismissRequest = onDismissProperties
        )
    }

    if (showSelectionPropertiesDialog && selectedFiles.isNotEmpty()) {
        FilePropertiesDialog(
            files = selectedFiles.toList(),
            onDismissRequest = onDismissSelectionProperties
        )
    }

    if (activeConflictFile != null) {
        ConflictResolutionDialog(
            conflictingFile = activeConflictFile,
            onResolve = onResolveConflict,
            onDismissRequest = onDismissConflict
        )
    }

    if (showClipboardDialog) {
        ClipboardDialog(
            clipboardItems = AppClipboard.items,
            onClearAll = { AppClipboard.clear() },
            onDismissRequest = onDismissClipboard
        )
    }

    if (showCapacityLimitDialog) {
        CapacityLimitDialog(onDismiss = onDismissCapacityLimit)
    }

    if (!filesPendingDelete.isNullOrEmpty()) {
        DeleteConfirmationDialog(
            files = filesPendingDelete,
            onConfirmDelete = onConfirmDelete,
            onDismissRequest = onDismissDelete
        )
    }

    if (operationProgress != null) {
        OperationProgressDialog(state = operationProgress)
    }

    if (filePendingRename != null) {
        RenameDialog(
            file = filePendingRename,
            onConfirmRename = onConfirmRename,
            onDismissRequest = onDismissRename
        )
    }

    if (activeMediaPlayerFile != null) {
        MediaPlayerScreen(
            file = activeMediaPlayerFile,
            globalLoopEnabled = globalLoopEnabled,
            onToggleGlobalLoop = onToggleGlobalLoop,
            onClose = onCloseMediaPlayer,
            onNavigateToMedia = onNavigateToMedia,
            onSettingsClick = onMediaPlayerSettingsClick
        )
    }

    if (showSearchDialog) {
        SearchDialog(
            initialQuery = activeSearchQuery ?: "",
            initialIncludeSubfolders = searchIncludeSubfolders,
            onSearch = onSearch,
            onDismissRequest = onDismissSearch
        )
    }
}
