package com.anshuman.tagstash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshuman.tagstash.data.model.FileItem
import com.anshuman.tagstash.data.model.RecycleBinItem
import com.anshuman.tagstash.data.utils.formatFileSize
import com.anshuman.tagstash.data.utils.getFileIcon
import com.anshuman.tagstash.data.utils.getIconColor

@Composable
fun RecycleBinItemCard(
    item: RecycleBinItem,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fileItem = remember(item) {
        FileItem(
            name = item.fileName,
            path = item.originalPath,
            isDirectory = item.isDirectory,
            size = item.fileSize,
            lastModified = item.deletedTimestamp,
            childCount = 0
        )
    }
    val fileIcon = remember(fileItem) { getFileIcon(fileItem) }
    val iconColor = remember(fileItem) { getIconColor(fileItem) }
    val timeRemaining = remember(item.expiryTimestamp) { formatTimeRemaining(item.expiryTimestamp) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E2026),
        border = BorderStroke(1.dp, Color(0xFF2B2D35)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    SelectionContainer {
                        Text(
                            text = item.fileName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    SelectionContainer {
                        Text(
                            text = item.originalPath,
                            fontSize = 11.sp,
                            color = Color(0xFF8E95A5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Size Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF141518),
                        border = BorderStroke(1.dp, Color(0xFF25262B))
                    ) {
                        Text(
                            text = formatFileSize(item.fileSize),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCCD2E3),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Expiry Chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF23191B),
                        border = BorderStroke(1.dp, Color(0xFF4E262A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeRemaining,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE8D0D2)
                            )
                        }
                    }
                }

                // Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRestore,
                        modifier = Modifier
                            .size(34.dp)
                            .semantics { contentDescription = "Restore ${item.fileName}" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDeletePermanently,
                        modifier = Modifier
                            .size(34.dp)
                            .semantics { contentDescription = "Delete permanently ${item.fileName}" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatTimeRemaining(expiryTimestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = expiryTimestamp - now
    if (diffMs <= 0) return "Expired"
    val minutes = (diffMs / (60 * 1000)).toInt()
    return if (minutes < 1) {
        "Expires in <1m"
    } else if (minutes < 60) {
        "Expires in ${minutes}m"
    } else {
        val hours = minutes / 60
        val remMin = minutes % 60
        "Expires in ${hours}h ${remMin}m"
    }
}
