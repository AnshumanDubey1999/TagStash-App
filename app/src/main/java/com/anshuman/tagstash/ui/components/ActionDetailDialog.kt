package com.anshuman.tagstash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anshuman.tagstash.data.model.AuditItemOutcome
import com.anshuman.tagstash.data.model.AuditLogEntry
import com.anshuman.tagstash.data.model.AuditLogItemDetail
import com.anshuman.tagstash.data.model.FileItem
import com.anshuman.tagstash.data.utils.formatFileSize
import com.anshuman.tagstash.data.utils.formatLastModified
import com.anshuman.tagstash.data.utils.getFileIcon
import com.anshuman.tagstash.data.utils.getIconColor
import java.io.File

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
                    .heightIn(max = 600.dp),
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
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
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

                    Text(
                        text = "Destination: ${if (entry.destinationDirectory == "/storage/emulated/0") "Internal Storage" else entry.destinationDirectory}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Items List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Action Log", color = Color.White) },
            text = { Text("Are you sure you want to delete this action from past actions?", color = Color(0xFFA0A0A0)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
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
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, Color(0xFF262626)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "From: ${item.sourcePath}",
                    fontSize = 11.sp,
                    color = Color(0xFFA0A0A0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.destinationPath != null) {
                    Text(
                        text = "To: ${item.destinationPath}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                // Command badge
                val isCut = item.command.equals("CUT", ignoreCase = true)
                val cmdColor = if (isCut) Color(0xFFFFB74D) else Color(0xFF4FC3F7)
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

                Spacer(modifier = Modifier.height(4.dp))

                // Outcome badge
                val (outcomeText, outcomeColor) = when (item.outcome) {
                    AuditItemOutcome.COPIED -> "COPIED" to Color(0xFF81C784)
                    AuditItemOutcome.MOVED -> "MOVED" to Color(0xFF66BB6A)
                    AuditItemOutcome.REPLACED -> "REPLACED" to Color(0xFFFF8A65)
                    AuditItemOutcome.RENAMED_COPY -> "RENAMED" to Color(0xFFBA68C8)
                    AuditItemOutcome.SKIPPED -> "SKIPPED" to Color(0xFF9E9E9E)
                }
                Text(
                    text = outcomeText,
                    color = outcomeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
