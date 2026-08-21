package com.anshuman.tagstash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshuman.tagstash.data.database.AuditLogDatabaseHelper
import com.anshuman.tagstash.data.database.RecycleBinDatabaseHelper
import com.anshuman.tagstash.data.model.AuditActionType
import com.anshuman.tagstash.data.model.AuditItemOutcome
import com.anshuman.tagstash.data.model.AuditLogEntry
import com.anshuman.tagstash.data.model.AuditLogItemDetail
import com.anshuman.tagstash.data.model.RecycleBinItem
import com.anshuman.tagstash.ui.components.EmptyRecycleBinView
import com.anshuman.tagstash.ui.components.OperationProgressState
import com.anshuman.tagstash.ui.components.RecycleBinDialogs
import com.anshuman.tagstash.ui.components.RecycleBinHeaderCard
import com.anshuman.tagstash.ui.components.RecycleBinItemCard
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
    var operationProgress by remember { mutableStateOf<OperationProgressState?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

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
                RecycleBinHeaderCard(itemCount = items.size, totalBytes = totalBytes)

                Spacer(modifier = Modifier.height(8.dp))

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

    RecycleBinDialogs(
        itemPendingPermanentDelete = itemPendingPermanentDelete,
        onConfirmPermanentDelete = {
            val toDelete = itemPendingPermanentDelete ?: return@RecycleBinDialogs
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
        onDismissPermanentDelete = { itemPendingPermanentDelete = null },
        showEmptyBinDialog = showEmptyBinDialog,
        itemCount = items.size,
        onConfirmEmptyBin = {
            showEmptyBinDialog = false
            coroutineScope.launch {
                val itemsToDelete = items.toList()
                operationProgress = OperationProgressState(
                    title = "Emptying Recycle Bin...",
                    currentItem = 1,
                    totalItems = itemsToDelete.size,
                    currentFileName = itemsToDelete.firstOrNull()?.fileName ?: "",
                    icon = Icons.Default.DeleteForever,
                    iconTint = errorColor
                )
                recycleBinHelper.emptyRecycleBin(context) { current, total, name ->
                    operationProgress = OperationProgressState(
                        title = "Emptying Recycle Bin...",
                        currentItem = current,
                        totalItems = total,
                        currentFileName = name,
                        icon = Icons.Default.DeleteForever,
                        iconTint = errorColor
                    )
                }
                operationProgress = null
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
        onDismissEmptyBin = { showEmptyBinDialog = false },
        showRestoreAllDialog = showRestoreAllDialog,
        onConfirmRestoreAll = {
            showRestoreAllDialog = false
            coroutineScope.launch {
                val itemsToRestore = items.toList()
                operationProgress = OperationProgressState(
                    title = "Restoring items...",
                    currentItem = 1,
                    totalItems = itemsToRestore.size,
                    currentFileName = itemsToRestore.firstOrNull()?.fileName ?: "",
                    icon = Icons.Default.Restore,
                    iconTint = primaryColor
                )
                val restoredList = recycleBinHelper.restoreAllItems(context) { current, total, name ->
                    operationProgress = OperationProgressState(
                        title = "Restoring items...",
                        currentItem = current,
                        totalItems = total,
                        currentFileName = name,
                        icon = Icons.Default.Restore,
                        iconTint = primaryColor
                    )
                }
                operationProgress = null
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
        onDismissRestoreAll = { showRestoreAllDialog = false },
        operationProgress = operationProgress
    )
}
