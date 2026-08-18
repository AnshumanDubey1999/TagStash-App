package com.anshuman.tagstash.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshuman.tagstash.data.database.AuditLogDatabaseHelper
import com.anshuman.tagstash.data.database.RecycleBinDatabaseHelper
import com.anshuman.tagstash.data.model.*
import com.anshuman.tagstash.data.utils.formatFileSize
import com.anshuman.tagstash.data.utils.getFileIcon
import com.anshuman.tagstash.data.utils.getIconColor
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val recycleBinHelper = remember { RecycleBinDatabaseHelper.getInstance(context) }
    val auditHelper = remember { AuditLogDatabaseHelper.getInstance(context) }

    var items by remember { mutableStateOf<List<RecycleBinItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }

    // Dialog States
    var itemPendingPermanentDelete by remember { mutableStateOf<RecycleBinItem?>(null) }
    var showEmptyBinDialog by remember { mutableStateOf(false) }
    var showRestoreAllDialog by remember { mutableStateOf(false) }

    fun refreshItems() {
        coroutineScope.launch {
            recycleBinHelper.cleanupExpiredItems(context)
            items = recycleBinHelper.getAllItems()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshItems()
    }

    val totalBytes = remember(items) { items.sumOf { it.fileSize } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recycle Bin",
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
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF1E1E1E))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Restore All", color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showRestoreAllDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Empty Recycle Bin", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showEmptyBinDialog = true
                                }
                            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (items.isEmpty()) {
                EmptyRecycleBinView()
            } else {
                // Header Summary Card
                RecycleBinHeaderCard(itemCount = items.size, totalBytes = totalBytes)

                Spacer(modifier = Modifier.height(8.dp))

                // Trashed Items List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        RecycleBinItemCard(
                            item = item,
                            onRestore = {
                                coroutineScope.launch {
                                    val restored = recycleBinHelper.restoreItem(item.id, context)
                                    if (restored != null) {
                                        auditHelper.insertLog(
                                            AuditLogEntry(
                                                timestamp = System.currentTimeMillis(),
                                                actionType = AuditActionType.RESTORE,
                                                summary = "Restored ${item.fileName}",
                                                destinationDirectory = File(restored.second.parent ?: "").name.ifEmpty { "Root" },
                                                totalItems = 1,
                                                items = listOf(
                                                    AuditLogItemDetail(
                                                        sourcePath = item.trashedPath,
                                                        destinationPath = restored.second.absolutePath,
                                                        command = "RESTORE",
                                                        outcome = AuditItemOutcome.RESTORED,
                                                        fileName = item.fileName,
                                                        fileSize = item.fileSize,
                                                        isDirectory = item.isDirectory
                                                    )
                                                )
                                            )
                                        )
                                        refreshItems()
                                    }
                                }
                            },
                            onDeletePermanently = {
                                itemPendingPermanentDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Single item permanent delete dialog
    if (itemPendingPermanentDelete != null) {
        val item = itemPendingPermanentDelete!!
        AlertDialog(
            onDismissRequest = { itemPendingPermanentDelete = null },
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
                    onClick = {
                        val toDelete = itemPendingPermanentDelete!!
                        itemPendingPermanentDelete = null
                        coroutineScope.launch {
                            recycleBinHelper.permanentlyDeleteItem(toDelete.id, context)
                            auditHelper.insertLog(
                                AuditLogEntry(
                                    timestamp = System.currentTimeMillis(),
                                    actionType = AuditActionType.DELETE_PERMANENT,
                                    summary = "Permanently deleted ${toDelete.fileName}",
                                    destinationDirectory = "Permanent Purge",
                                    totalItems = 1,
                                    items = listOf(
                                        AuditLogItemDetail(
                                            sourcePath = toDelete.trashedPath,
                                            destinationPath = null,
                                            command = "DELETE_PERMANENT",
                                            outcome = AuditItemOutcome.PERMANENTLY_DELETED,
                                            fileName = toDelete.fileName,
                                            fileSize = toDelete.fileSize,
                                            isDirectory = toDelete.isDirectory
                                        )
                                    )
                                )
                            )
                            refreshItems()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingPermanentDelete = null }) {
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
            onDismissRequest = { showEmptyBinDialog = false },
            title = {
                Text(
                    text = "Empty Recycle Bin?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all ${items.size} items in the Recycle Bin. This action cannot be undone.",
                    color = Color(0xFFDDDDDD),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyBinDialog = false
                        coroutineScope.launch {
                            val itemsToDelete = items.toList()
                            recycleBinHelper.emptyRecycleBin(context)
                            val details = itemsToDelete.map {
                                AuditLogItemDetail(
                                    sourcePath = it.trashedPath,
                                    destinationPath = null,
                                    command = "DELETE_PERMANENT",
                                    outcome = AuditItemOutcome.PERMANENTLY_DELETED,
                                    fileName = it.fileName,
                                    fileSize = it.fileSize,
                                    isDirectory = it.isDirectory
                                )
                            }
                            auditHelper.insertLog(
                                AuditLogEntry(
                                    timestamp = System.currentTimeMillis(),
                                    actionType = AuditActionType.DELETE_PERMANENT,
                                    summary = "Emptied Recycle Bin (${itemsToDelete.size} items)",
                                    destinationDirectory = "Permanent Purge",
                                    totalItems = itemsToDelete.size,
                                    items = details
                                )
                            )
                            refreshItems()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Empty All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyBinDialog = false }) {
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
            onDismissRequest = { showRestoreAllDialog = false },
            title = {
                Text(
                    text = "Restore All Items?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Restore all ${items.size} items back to their original locations?",
                    color = Color(0xFFDDDDDD),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreAllDialog = false
                        coroutineScope.launch {
                            val restoredList = recycleBinHelper.restoreAllItems(context)
                            if (restoredList.isNotEmpty()) {
                                val details = restoredList.map { (item, restoredFile) ->
                                    AuditLogItemDetail(
                                        sourcePath = item.trashedPath,
                                        destinationPath = restoredFile.absolutePath,
                                        command = "RESTORE",
                                        outcome = AuditItemOutcome.RESTORED,
                                        fileName = item.fileName,
                                        fileSize = item.fileSize,
                                        isDirectory = item.isDirectory
                                    )
                                }
                                auditHelper.insertLog(
                                    AuditLogEntry(
                                        timestamp = System.currentTimeMillis(),
                                        actionType = AuditActionType.RESTORE,
                                        summary = "Restored ${restoredList.size} items from Recycle Bin",
                                        destinationDirectory = "Various",
                                        totalItems = restoredList.size,
                                        items = details
                                    )
                                )
                            }
                            refreshItems()
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Restore All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreAllDialog = false }) {
                    Text("Cancel", color = Color(0xFFCCCCCC))
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun RecycleBinHeaderCard(itemCount: Int, totalBytes: Long) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F1E2E),
        border = BorderStroke(1.dp, Color(0xFF1B3B5F))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1-Hour Temporary Storage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFB0CDEB)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF162B42),
                    border = BorderStroke(1.dp, Color(0xFF234B73))
                ) {
                    Text(
                        text = "$itemCount items • ${formatFileSize(totalBytes)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF90CAF9),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Items in the Recycle Bin will be automatically and permanently removed 1 hour after deletion.",
                fontSize = 12.sp,
                color = Color(0xFF8EB5D8),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun RecycleBinItemCard(
    item: RecycleBinItem,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
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
        modifier = Modifier.fillMaxWidth()
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

@Composable
private fun EmptyRecycleBinView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFF1E2026), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = Color(0xFF8E95A5),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recycle Bin is Empty",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Deleted files will appear here and be kept for 1 hour before permanent removal.",
                fontSize = 13.sp,
                color = Color(0xFF8E95A5),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

private fun formatTimeRemaining(expiryTimestamp: Long): String {
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
