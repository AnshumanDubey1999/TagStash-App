package com.anshuman.tagstash.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshuman.tagstash.data.utils.buildBreadcrumbs
import com.anshuman.tagstash.data.utils.formatBreadcrumbName
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreadcrumbsBar(
    currentDir: File,
    onNavigate: (File) -> Unit,
    modifier: Modifier = Modifier,
    homeDir: File = File("/storage/emulated/0"),
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    isAllSelected: Boolean = false,
    clipboardCount: Int = 0,
    onSelectModeToggle: () -> Unit = {},
    onSelectAllToggle: () -> Unit = {},
    onCutSelected: (() -> Unit)? = null,
    onCopySelected: (() -> Unit)? = null,
    onPasteClick: (() -> Unit)? = null,
    onClipboardClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null
) {
    val breadcrumbs = remember(currentDir, homeDir) { buildBreadcrumbs(currentDir, homeDir) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowRow(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.Center
            ) {
                breadcrumbs.forEachIndexed { index, pair ->
                    val name = pair.first
                    val file = pair.second
                    val formattedName = remember(name) { formatBreadcrumbName(name) }
                    val isLast = index == breadcrumbs.lastIndex

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = formattedName,
                            color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(enabled = !isLast) { onNavigate(file) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )

                        if (!isLast) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Separator",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (!isSelectionMode) {
                        DropdownMenuItem(
                            text = { Text("Select") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                showMenu = false
                                onSelectModeToggle()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(if (isAllSelected) "Deselect All" else "Select All") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                showMenu = false
                                onSelectAllToggle()
                            }
                        )

                        val isActionEnabled = selectedCount > 0
                        DropdownMenuItem(
                            text = { Text("Cut") },
                            enabled = isActionEnabled,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = if (isActionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onCutSelected?.invoke()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Copy") },
                            enabled = isActionEnabled,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (isActionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onCopySelected?.invoke()
                            }
                        )
                    }

                    if (!isSelectionMode && clipboardCount > 0) {
                        DropdownMenuItem(
                            text = { Text("Paste ($clipboardCount)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                showMenu = false
                                onPasteClick?.invoke()
                            }
                        )
                    }

                    if (clipboardCount > 0) {
                        DropdownMenuItem(
                            text = { Text("Clipboard ($clipboardCount)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentPasteGo,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                showMenu = false
                                onClipboardClick?.invoke()
                            }
                        )
                    }

                    val isInfoEnabled = !isSelectionMode || selectedCount > 0
                    DropdownMenuItem(
                        text = { Text("Info") },
                        enabled = isInfoEnabled,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isInfoEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onInfoClick?.invoke()
                        }
                    )
                }
            }
        }
    }
}
