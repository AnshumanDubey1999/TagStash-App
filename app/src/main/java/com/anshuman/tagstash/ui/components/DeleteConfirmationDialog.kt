package com.anshuman.tagstash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anshuman.tagstash.data.model.FileItem
import com.anshuman.tagstash.data.utils.formatFileSize
import com.anshuman.tagstash.data.utils.getFileIcon
import com.anshuman.tagstash.data.utils.getIconColor
import java.io.File

@Composable
fun DeleteConfirmationDialog(
    files: List<File>,
    onConfirmDelete: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var isConfirmed by remember { mutableStateOf(false) }

    val totalBytes = remember(files) {
        files.sumOf { if (it.isDirectory) computeDirSize(it) else it.length() }
    }

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
                    .heightIn(max = 620.dp),
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
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (files.size == 1) "Delete File" else "Delete Files",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color.White
                                )
                                if (files.size > 1) {
                                    Text(
                                        text = "${files.size} items • Total: ${formatFileSize(totalBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close delete dialog",
                                tint = Color(0xFFA0A0A0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Cautionary Confirmation Checkbox Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isConfirmed) Color(0xFF38151A) else Color(0xFF23191B),
                        border = BorderStroke(
                            if (isConfirmed) 1.5.dp else 1.dp,
                            if (isConfirmed) MaterialTheme.colorScheme.error.copy(alpha = 0.85f) else Color(0xFF4E262A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = isConfirmed,
                                    role = Role.Checkbox,
                                    onValueChange = { isConfirmed = it }
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isConfirmed,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.error,
                                    uncheckedColor = Color(0xFFBA6868),
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (files.size == 1) {
                                    "Click here if you are sure you want to delete this item"
                                } else {
                                    "Click here if you are sure you want to delete these ${files.size} items"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isConfirmed) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 13.5.sp
                                ),
                                color = if (isConfirmed) Color.White else Color(0xFFE8D0D2)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Inset Well for List of Deleted Items
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF141518),
                        border = BorderStroke(1.dp, Color(0xFF25262B)),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(files) { file ->
                                DeleteItemCard(file = file)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Informational Callout Card (Recycle Bin Retention Notice)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F1E2E),
                        border = BorderStroke(1.dp, Color(0xFF1B3B5F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Deleted items will stay in the Recycle Bin for 1 hour before being permanently deleted.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 16.sp
                                ),
                                color = Color(0xFFB0CDEB),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text("Cancel", color = Color(0xFFCCCCCC))
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = onConfirmDelete,
                            enabled = isConfirmed,
                            modifier = Modifier.semantics { contentDescription = "Confirm Delete" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                disabledContainerColor = Color(0xFF2C2424),
                                disabledContentColor = Color(0xFF6E5656)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (files.size <= 1) "Delete" else "Delete (${files.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteItemCard(file: File) {
    val fileItem = remember(file) {
        FileItem(
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            size = if (file.isDirectory) computeDirSize(file) else file.length(),
            lastModified = file.lastModified(),
            childCount = file.listFiles()?.size ?: 0
        )
    }
    val fileIcon = remember(fileItem) { getFileIcon(fileItem) }
    val iconColor = remember(fileItem) { getIconColor(fileItem) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E2026),
        border = BorderStroke(1.dp, Color(0xFF2B2D35)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
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

            Column(modifier = Modifier.weight(1f)) {
                SelectionContainer {
                    Text(
                        text = file.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                SelectionContainer {
                    Text(
                        text = file.parent ?: file.absolutePath,
                        fontSize = 11.sp,
                        color = Color(0xFF8E95A5)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF16171B),
                border = BorderStroke(1.dp, Color(0xFF2D3039))
            ) {
                Text(
                    text = formatFileSize(fileItem.size),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCCD2E3),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

private fun computeDirSize(dir: File): Long {
    var size = 0L
    dir.walkTopDown().forEach { file ->
        if (file.isFile) {
            size += file.length()
        }
    }
    return size
}
