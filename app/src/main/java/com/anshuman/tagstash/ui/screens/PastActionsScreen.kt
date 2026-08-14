package com.anshuman.tagstash.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anshuman.tagstash.data.database.AuditLogDatabaseHelper
import com.anshuman.tagstash.data.model.AuditActionType
import com.anshuman.tagstash.data.model.AuditItemOutcome
import com.anshuman.tagstash.data.model.AuditLogEntry
import com.anshuman.tagstash.data.utils.formatLastModified
import com.anshuman.tagstash.ui.components.ActionDetailDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastActionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { AuditLogDatabaseHelper.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var logs by remember { mutableStateOf<List<AuditLogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedActionForDetail by remember { mutableStateOf<AuditLogEntry?>(null) }

    fun refreshLogs() {
        coroutineScope.launch {
            isLoading = true
            logs = dbHelper.getAllLogs()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshLogs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Past Actions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (logs.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Clear History") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showClearConfirm = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (logs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Past Actions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Actions that modify files (like pasting, renaming, or deleting) will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0A0A0),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs, key = { it.id }) { logEntry ->
                        PastActionCard(
                            entry = logEntry,
                            onClick = { selectedActionForDetail = logEntry }
                        )
                    }
                }
            }
        }
    }

    if (selectedActionForDetail != null) {
        ActionDetailDialog(
            entry = selectedActionForDetail!!,
            onDelete = {
                val entryId = selectedActionForDetail!!.id
                selectedActionForDetail = null
                coroutineScope.launch {
                    dbHelper.deleteLog(entryId)
                    refreshLogs()
                }
            },
            onDismissRequest = { selectedActionForDetail = null }
        )
    }

    if (showClearConfirm) {
        Dialog(
            onDismissRequest = { showClearConfirm = false }
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
                        text = "Clear Action History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Are you sure you want to clear all past action logs? This action cannot be undone.",
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
                            onClick = { showClearConfirm = false }
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showClearConfirm = false
                                coroutineScope.launch {
                                    dbHelper.clearAllLogs()
                                    refreshLogs()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastActionCard(
    entry: AuditLogEntry,
    onClick: () -> Unit
) {
    val formattedDate = remember(entry.timestamp) { formatLastModified(entry.timestamp) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1E1E),
        border = BorderStroke(1.dp, Color(0xFF2C2C2C)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val actionIcon = when (entry.actionType) {
                AuditActionType.PASTE -> Icons.Default.ContentPaste
                AuditActionType.DELETE -> Icons.Default.DeleteOutline
                AuditActionType.RESTORE -> Icons.Default.Restore
                AuditActionType.DELETE_PERMANENT, AuditActionType.AUTO_DELETE -> Icons.Default.DeleteForever
            }
            val actionTint = if (entry.actionType == AuditActionType.PASTE || entry.actionType == AuditActionType.RESTORE) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }

            val actionDescription = when (entry.actionType) {
                AuditActionType.PASTE -> "Paste Action"
                AuditActionType.DELETE -> "Delete Action"
                AuditActionType.RESTORE -> "Restore Action"
                AuditActionType.DELETE_PERMANENT, AuditActionType.AUTO_DELETE -> "Purge Action"
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        actionTint.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionDescription,
                    tint = actionTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                val itemLabel = if (entry.totalItems == 1) "1 item" else "${entry.totalItems} items"
                val destLabel = if (entry.destinationDirectory == "/storage/emulated/0") {
                    "Internal Storage"
                } else if (entry.destinationDirectory.contains('/')) {
                    entry.destinationDirectory.substringAfterLast('/')
                } else {
                    entry.destinationDirectory
                }
                val preposition = if (entry.actionType == AuditActionType.DELETE) "To" else "To"
                Text(
                    text = "$itemLabel • $preposition: $destLabel",
                    fontSize = 12.sp,
                    color = Color(0xFFCCCCCC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Breakdown counts
                val copiedCount = entry.items.count { it.outcome == AuditItemOutcome.COPIED }
                val movedCount = entry.items.count { it.outcome == AuditItemOutcome.MOVED }
                val replacedCount = entry.items.count { it.outcome == AuditItemOutcome.REPLACED }
                val renamedCount = entry.items.count { it.outcome == AuditItemOutcome.RENAMED_COPY }
                val skippedCount = entry.items.count { it.outcome == AuditItemOutcome.SKIPPED }
                val trashedCount = entry.items.count { it.outcome == AuditItemOutcome.TRASHED }
                val restoredCount = entry.items.count { it.outcome == AuditItemOutcome.RESTORED }
                val deletedCount = entry.items.count { it.outcome == AuditItemOutcome.PERMANENTLY_DELETED || it.outcome == AuditItemOutcome.AUTO_DELETED_EXPIRED }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (copiedCount > 0) {
                        CountBadge(label = "$copiedCount copied", color = Color(0xFF81C784))
                    }
                    if (movedCount > 0) {
                        CountBadge(label = "$movedCount moved", color = Color(0xFF66BB6A))
                    }
                    if (replacedCount > 0) {
                        CountBadge(label = "$replacedCount replaced", color = Color(0xFFFF8A65))
                    }
                    if (renamedCount > 0) {
                        CountBadge(label = "$renamedCount renamed", color = Color(0xFFBA68C8))
                    }
                    if (skippedCount > 0) {
                        CountBadge(label = "$skippedCount skipped", color = Color(0xFF9E9E9E))
                    }
                    if (trashedCount > 0) {
                        CountBadge(label = "$trashedCount trashed", color = Color(0xFFE57373))
                    }
                    if (restoredCount > 0) {
                        CountBadge(label = "$restoredCount restored", color = Color(0xFF81C784))
                    }
                    if (deletedCount > 0) {
                        CountBadge(label = "$deletedCount deleted", color = Color(0xFFE57373))
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF666666),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CountBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
