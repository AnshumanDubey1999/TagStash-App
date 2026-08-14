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
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anshuman.tagstash.data.clipboard.ClipboardItem
import com.anshuman.tagstash.data.clipboard.ClipboardOpType
import com.anshuman.tagstash.data.utils.formatFileSize
import com.anshuman.tagstash.data.utils.getFileIcon
import com.anshuman.tagstash.data.utils.getIconColor
import com.anshuman.tagstash.data.model.FileItem

@Composable
fun ClipboardDialog(
    clipboardItems: List<ClipboardItem>,
    onClearAll: () -> Unit,
    onDismissRequest: () -> Unit
) {
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
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 560.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E1E),
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFF2C2C2C))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header Row
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
                                    imageVector = Icons.Default.ContentPasteGo,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Clipboard",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "${clipboardItems.size} ${if (clipboardItems.size == 1) "item" else "items"}",
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
                                contentDescription = "Close clipboard dialog",
                                tint = Color(0xFFA0A0A0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (clipboardItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Clipboard is empty",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFA0A0A0)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(clipboardItems) { item ->
                                ClipboardItemRow(item = item)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (clipboardItems.isNotEmpty()) {
                            TextButton(
                                onClick = onClearAll,
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
                                Text(
                                    text = "Clear All",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
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
}

@Composable
private fun ClipboardItemRow(item: ClipboardItem) {
    val fileItem = remember(item.file) {
        FileItem(
            name = item.file.name,
            path = item.file.absolutePath,
            isDirectory = item.file.isDirectory,
            size = if (item.file.isDirectory) 0L else item.file.length(),
            lastModified = item.file.lastModified(),
            childCount = if (item.file.isDirectory) (item.file.list()?.size ?: 0) else 0
        )
    }
    val fileIcon = remember(fileItem) { getFileIcon(fileItem) }
    val iconColor = remember(fileItem) { getIconColor(fileItem) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF141414),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    text = item.file.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.file.isDirectory) "Folder" else formatFileSize(item.file.length()),
                    fontSize = 12.sp,
                    color = Color(0xFFA0A0A0)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Badge (CUT / COPY)
            val isCut = item.opType == ClipboardOpType.CUT
            val badgeColor = if (isCut) Color(0xFFFFB74D) else Color(0xFF4FC3F7)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
            ) {
                Text(
                    text = if (isCut) "CUT" else "COPY",
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
