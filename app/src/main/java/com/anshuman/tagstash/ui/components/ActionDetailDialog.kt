package com.anshuman.tagstash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anshuman.tagstash.data.model.AuditActionType
import com.anshuman.tagstash.data.model.AuditItemOutcome
import com.anshuman.tagstash.data.model.AuditLogEntry
import com.anshuman.tagstash.data.model.AuditLogItemDetail
import com.anshuman.tagstash.data.model.FileItem
import com.anshuman.tagstash.data.utils.formatFileSize
import com.anshuman.tagstash.data.utils.formatLastModified
import com.anshuman.tagstash.data.utils.getFileIcon
import com.anshuman.tagstash.data.utils.getIconColor

@Composable
fun ActionDetailDialog(
    entry: AuditLogEntry,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 640.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E1E),
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFF2C2C2C))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val actionIcon = when (entry.actionType) {
                                AuditActionType.PASTE -> Icons.Default.ContentPaste
                                AuditActionType.DELETE -> Icons.Default.DeleteOutline
                                AuditActionType.RESTORE -> Icons.Default.Restore
                                AuditActionType.RENAME -> Icons.Default.DriveFileRenameOutline
                                AuditActionType.DELETE_PERMANENT, AuditActionType.AUTO_DELETE -> Icons.Default.DeleteForever
                            }
                            val isPositiveAction = entry.actionType == AuditActionType.PASTE ||
                                entry.actionType == AuditActionType.RESTORE ||
                                entry.actionType == AuditActionType.RENAME
                            val actionTint = if (isPositiveAction) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        actionTint.copy(alpha = 0.15f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = actionIcon,
                                    contentDescription = null,
                                    tint = actionTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Action Details",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = formatLastModified(entry.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA0A0A0)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close action details",
                                tint = Color(0xFFA0A0A0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SelectionContainer {
                        Text(
                            text = "Destination: ${if (entry.destinationDirectory == "/storage/emulated/0") "Internal Storage" else entry.destinationDirectory}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Items List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entry.items) { item ->
                            ActionDetailItemCard(item = item)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Log", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = onDismissRequest,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        Dialog(
            onDismissRequest = { showDeleteConfirm = false }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E1E),
                border = BorderStroke(1.dp, Color(0xFF2C2C2C)),
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Delete Action Log",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Are you sure you want to delete this action from past actions? This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFA0A0A0)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showDeleteConfirm = false }
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showDeleteConfirm = false
                                onDelete()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionDetailItemCard(item: AuditLogItemDetail) {
    val dummyFile = remember(item.fileName, item.isDirectory) {
        FileItem(
            name = item.fileName,
            path = item.sourcePath,
            isDirectory = item.isDirectory,
            size = item.fileSize,
            lastModified = 0L,
            childCount = 0
        )
    }
    val fileIcon = remember(dummyFile) { getFileIcon(dummyFile) }
    val iconColor = remember(dummyFile) { getIconColor(dummyFile) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, Color(0xFF262626)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Icon, File Name, and Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Badges
                val cmdColor = when (item.command.uppercase()) {
                    "CUT" -> Color(0xFFFFB74D)
                    "COPY" -> Color(0xFF4FC3F7)
                    "DELETE", "DELETE_PERMANENT" -> Color(0xFFEF5350)
                    "AUTO_DELETE" -> Color(0xFFFF7043)
                    "RESTORE" -> Color(0xFF81C784)
                    "RENAME" -> Color(0xFF81D4FA)
                    else -> Color(0xFFA0A0A0)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = cmdColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, cmdColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = item.command.uppercase(),
                        color = cmdColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                val (outcomeText, outcomeColor) = when (item.outcome) {
                    AuditItemOutcome.COPIED -> "COPIED" to Color(0xFF81C784)
                    AuditItemOutcome.MOVED -> "MOVED" to Color(0xFF66BB6A)
                    AuditItemOutcome.REPLACED -> "REPLACED" to Color(0xFFFF8A65)
                    AuditItemOutcome.RENAMED_COPY -> "RENAMED" to Color(0xFFBA68C8)
                    AuditItemOutcome.SKIPPED -> "SKIPPED" to Color(0xFF9E9E9E)
                    AuditItemOutcome.TRASHED -> "TRASHED" to Color(0xFFE57373)
                    AuditItemOutcome.RESTORED -> "RESTORED" to Color(0xFF81C784)
                    AuditItemOutcome.RENAMED -> "RENAMED" to Color(0xFF81D4FA)
                    AuditItemOutcome.PERMANENTLY_DELETED -> "DELETED" to Color(0xFFE57373)
                    AuditItemOutcome.AUTO_DELETED_EXPIRED -> "EXPIRED" to Color(0xFFFF7043)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = outcomeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, outcomeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = outcomeText,
                        color = outcomeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF222222), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Key-Value Rows without truncation
            AuditPropertyRow(label = "Source", value = item.sourcePath)
            AuditPropertyRow(
                label = "Destination",
                value = item.destinationPath ?: "(None - Skipped)",
                valueColor = if (item.destinationPath != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else Color(0xFFA0A0A0)
            )
            if (!item.isDirectory) {
                AuditPropertyRow(label = "Size", value = formatFileSize(item.fileSize))
            }
        }
    }
}

@Composable
private fun AuditPropertyRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFFA0A0A0),
            modifier = Modifier.width(90.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                color = valueColor
            )
        }
    }
}
