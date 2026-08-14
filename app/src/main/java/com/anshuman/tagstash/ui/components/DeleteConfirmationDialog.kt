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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Confirmation Checkbox Row
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isConfirmed) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else Color(0xFF141414),
                        border = BorderStroke(1.dp, if (isConfirmed) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else Color(0xFF2C2C2C)),
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
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isConfirmed,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.error,
                                    uncheckedColor = Color(0xFFA0A0A0)
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
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isConfirmed) Color.White else Color(0xFFDDDDDD)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable Items List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files) { file ->
                            DeleteItemCard(file = file)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Info notice
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF141414),
                        border = BorderStroke(1.dp, Color(0xFF262626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFA0A0A0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Deleted items will stay in the Recycle Bin for 1 hour before being permanently deleted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA0A0A0),
                                fontSize = 11.sp
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
                            Text("Cancel", color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = onConfirmDelete,
                            enabled = isConfirmed,
                            modifier = Modifier.semantics { contentDescription = "Confirm Delete" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                disabledContainerColor = Color(0xFF333333),
                                disabledContentColor = Color(0xFF777777)
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
                SelectionContainer {
                    Text(
                        text = file.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                SelectionContainer {
                    Text(
                        text = file.parent ?: file.absolutePath,
                        fontSize = 11.sp,
                        color = Color(0xFFA0A0A0)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatFileSize(fileItem.size),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCCCCCC)
            )
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
