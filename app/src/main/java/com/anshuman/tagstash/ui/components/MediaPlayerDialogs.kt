package com.anshuman.tagstash.ui.components

import androidx.compose.runtime.Composable
import com.anshuman.tagstash.data.clipboard.AppClipboard
import java.io.File

@Composable
fun MediaPlayerDialogs(
    file: File,
    showRenameDialog: Boolean,
    onConfirmRename: (String) -> Unit,
    onDismissRename: () -> Unit,
    showPropertiesDialog: Boolean,
    onDismissProperties: () -> Unit,
    showClipboardDialog: Boolean,
    onDismissClipboard: () -> Unit,
    showCapacityLimitDialog: Boolean,
    onDismissCapacityLimit: () -> Unit,
    showDeleteDialog: Boolean,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit
) {
    if (showRenameDialog) {
        RenameDialog(
            file = file,
            onConfirmRename = onConfirmRename,
            onDismissRequest = onDismissRename
        )
    }

    if (showPropertiesDialog) {
        FilePropertiesDialog(
            file = file,
            onDismissRequest = onDismissProperties
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

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            files = listOf(file),
            onConfirmDelete = onConfirmDelete,
            onDismissRequest = onDismissDelete
        )
    }
}
