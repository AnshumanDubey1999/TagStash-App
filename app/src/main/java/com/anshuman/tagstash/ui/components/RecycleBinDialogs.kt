package com.anshuman.tagstash.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshuman.tagstash.data.model.RecycleBinItem

@Composable
fun RecycleBinDialogs(
    itemPendingPermanentDelete: RecycleBinItem?,
    onConfirmPermanentDelete: () -> Unit,
    onDismissPermanentDelete: () -> Unit,
    showEmptyBinDialog: Boolean,
    itemCount: Int,
    onConfirmEmptyBin: () -> Unit,
    onDismissEmptyBin: () -> Unit,
    showRestoreAllDialog: Boolean,
    onConfirmRestoreAll: () -> Unit,
    onDismissRestoreAll: () -> Unit,
    operationProgress: OperationProgressState?
) {
    // Single item permanent delete dialog
    if (itemPendingPermanentDelete != null) {
        val item = itemPendingPermanentDelete
        AlertDialog(
            onDismissRequest = onDismissPermanentDelete,
            title = {
                Text(
                    text = "Delete Permanently?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${item.fileName}\"? This action cannot be undone.",
                    color = Color(0xFFDDDDDD),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmPermanentDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPermanentDelete) {
                    Text("Cancel", color = Color(0xFFCCCCCC))
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Empty Recycle Bin dialog
    if (showEmptyBinDialog) {
        AlertDialog(
            onDismissRequest = onDismissEmptyBin,
            title = {
                Text(
                    text = "Empty Recycle Bin?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all $itemCount items in the Recycle Bin. This action cannot be undone.",
                    color = Color(0xFFDDDDDD),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmEmptyBin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Empty All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissEmptyBin) {
                    Text("Cancel", color = Color(0xFFCCCCCC))
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Restore All dialog
    if (showRestoreAllDialog) {
        AlertDialog(
            onDismissRequest = onDismissRestoreAll,
            title = {
                Text(
                    text = "Restore All Items?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Restore all $itemCount items back to their original locations?",
                    color = Color(0xFFDDDDDD),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmRestoreAll,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Restore All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRestoreAll) {
                    Text("Cancel", color = Color(0xFFCCCCCC))
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (operationProgress != null) {
        OperationProgressDialog(state = operationProgress)
    }
}
