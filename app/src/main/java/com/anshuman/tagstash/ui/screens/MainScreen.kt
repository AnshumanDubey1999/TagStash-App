package com.anshuman.tagstash.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.anshuman.tagstash.ui.components.CapacityLimitDialog
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import com.anshuman.tagstash.data.clipboard.AppClipboard
import com.anshuman.tagstash.data.clipboard.ClipboardOpType
import com.anshuman.tagstash.data.database.AuditLogDatabaseHelper
import com.anshuman.tagstash.data.database.RecycleBinDatabaseHelper
import com.anshuman.tagstash.data.model.AuditActionType
import com.anshuman.tagstash.data.model.AuditItemOutcome
import com.anshuman.tagstash.data.model.AuditLogEntry
import com.anshuman.tagstash.data.model.AuditLogItemDetail
import com.anshuman.tagstash.data.model.FileItem
import com.anshuman.tagstash.data.utils.ConflictResolution
import com.anshuman.tagstash.data.utils.copyFileOrDirectory
import com.anshuman.tagstash.data.utils.generateCopyFileName
import com.anshuman.tagstash.data.utils.isImage
import com.anshuman.tagstash.data.utils.isVideo
import com.anshuman.tagstash.data.utils.moveFileOrDirectory
import com.anshuman.tagstash.data.utils.openFileWithOS
import com.anshuman.tagstash.ui.components.BreadcrumbsBar
import com.anshuman.tagstash.ui.components.ClipboardDialog
import com.anshuman.tagstash.ui.components.ConflictResolutionDialog
import com.anshuman.tagstash.ui.components.DeleteConfirmationDialog
import com.anshuman.tagstash.ui.components.EmptyDirectoryView
import com.anshuman.tagstash.ui.components.ErrorView
import com.anshuman.tagstash.ui.components.FilePropertiesDialog
import com.anshuman.tagstash.ui.components.FileRowItem
import com.anshuman.tagstash.ui.components.PermissionRequestView
import com.anshuman.tagstash.ui.screens.PastActionsScreen
import com.anshuman.tagstash.ui.screens.SettingsScreen
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

val FileSaver = Saver<File, String>(
    save = { it.absolutePath },
    restore = { File(it) }
)

val NullableFileSaver = Saver<File?, String>(
    save = { it?.absolutePath ?: "" },
    restore = { if (it.isEmpty()) null else File(it) }
)

val FileSetSaver = Saver<Set<File>, List<String>>(
    save = { set -> set.map { it.absolutePath } },
    restore = { list -> list.map { File(it) }.toSet() }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    homeDirectory: File = File("/storage/emulated/0"),
    initialDirectory: File = homeDirectory
) {
    val context = LocalContext.current
    var currentDirectory by rememberSaveable(initialDirectory, stateSaver = FileSaver) { mutableStateOf(initialDirectory) }
    var filesList by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeMediaPlayerFile by rememberSaveable(initialDirectory, stateSaver = NullableFileSaver) { mutableStateOf<File?>(null) }
    var globalLoopEnabled by rememberSaveable { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var selectedPropertiesFile by rememberSaveable(initialDirectory, stateSaver = NullableFileSaver) { mutableStateOf<File?>(null) }

    // Selection Mode State
    var isSelectionMode by rememberSaveable(initialDirectory) { mutableStateOf(false) }
    var selectedFiles by rememberSaveable(initialDirectory, stateSaver = FileSetSaver) { mutableStateOf(emptySet<File>()) }
    var showSelectionPropertiesDialog by rememberSaveable(initialDirectory) { mutableStateOf(false) }

    // Screen Navigation State (MAIN, SETTINGS, PAST_ACTIONS)
    var currentScreenView by rememberSaveable(initialDirectory) { mutableStateOf("MAIN") }

    // Clipboard and Paste State
    val coroutineScope = rememberCoroutineScope()
    var showClipboardDialog by rememberSaveable(initialDirectory) { mutableStateOf(false) }
    var showCapacityLimitDialog by rememberSaveable(initialDirectory) { mutableStateOf(false) }
    var activeConflictFile by remember { mutableStateOf<File?>(null) }
    var conflictDeferred by remember { mutableStateOf<CompletableDeferred<Pair<ConflictResolution, Boolean>>?>(null) }
    var isPasting by remember { mutableStateOf(false) }
    var isContextMenuOpen by remember { mutableStateOf(false) }
    var filesPendingDelete by remember { mutableStateOf<List<File>?>(null) }

    fun cutSelectedFiles() {
        val files = selectedFiles.toList()
        val success = AppClipboard.addItems(files, ClipboardOpType.CUT)
        if (!success) {
            showCapacityLimitDialog = true
        } else {
            isSelectionMode = false
            selectedFiles = emptySet()
        }
    }

    fun copySelectedFiles() {
        val files = selectedFiles.toList()
        val success = AppClipboard.addItems(files, ClipboardOpType.COPY)
        if (!success) {
            showCapacityLimitDialog = true
        } else {
            isSelectionMode = false
            selectedFiles = emptySet()
        }
    }

    fun executePaste() {
        if (AppClipboard.items.isEmpty() || isPasting) return
        coroutineScope.launch {
            isPasting = true
            var batchResolution: ConflictResolution? = null
            val itemsToPaste = AppClipboard.items.toList()
            val auditDetails = mutableListOf<AuditLogItemDetail>()

            for (item in itemsToPaste) {
                val sourceFile = item.file
                if (!sourceFile.exists()) continue

                // If cutting file in the same directory where it already resides, skip
                if (sourceFile.parentFile?.absolutePath == currentDirectory.absolutePath && item.opType == ClipboardOpType.CUT) {
                    continue
                }

                var targetFile = File(currentDirectory, sourceFile.name)
                var outcome: AuditItemOutcome = if (item.opType == ClipboardOpType.CUT) AuditItemOutcome.MOVED else AuditItemOutcome.COPIED
                var actualDestPath: String? = targetFile.absolutePath

                if (targetFile.exists()) {
                    val resolution = if (batchResolution != null) {
                        batchResolution
                    } else {
                        val deferred = CompletableDeferred<Pair<ConflictResolution, Boolean>>()
                        conflictDeferred = deferred
                        activeConflictFile = sourceFile
                        val (chosenResolution, applyToAll) = deferred.await()
                        activeConflictFile = null
                        conflictDeferred = null
                        if (applyToAll) {
                            batchResolution = chosenResolution
                        }
                        chosenResolution
                    }

                    when (resolution) {
                        ConflictResolution.REPLACE -> {
                            outcome = AuditItemOutcome.REPLACED
                            if (item.opType == ClipboardOpType.CUT) {
                                targetFile.deleteRecursively()
                                moveFileOrDirectory(sourceFile, targetFile)
                            } else {
                                copyFileOrDirectory(sourceFile, targetFile)
                            }
                        }
                        ConflictResolution.KEEP_BOTH -> {
                            outcome = AuditItemOutcome.RENAMED_COPY
                            val newName = generateCopyFileName(currentDirectory, sourceFile.name)
                            targetFile = File(currentDirectory, newName)
                            actualDestPath = targetFile.absolutePath
                            if (item.opType == ClipboardOpType.CUT) {
                                moveFileOrDirectory(sourceFile, targetFile)
                            } else {
                                copyFileOrDirectory(sourceFile, targetFile)
                            }
                        }
                        ConflictResolution.SKIP -> {
                            outcome = AuditItemOutcome.SKIPPED
                            actualDestPath = null
                        }
                    }
                } else {
                    if (item.opType == ClipboardOpType.CUT) {
                        moveFileOrDirectory(sourceFile, targetFile)
                    } else {
                        copyFileOrDirectory(sourceFile, targetFile)
                    }
                }

                auditDetails.add(
                    AuditLogItemDetail(
                        sourcePath = sourceFile.absolutePath,
                        destinationPath = actualDestPath,
                        command = item.opType.name,
                        outcome = outcome,
                        fileName = sourceFile.name,
                        fileSize = if (sourceFile.isDirectory) 0L else sourceFile.length(),
                        isDirectory = sourceFile.isDirectory
                    )
                )
            }

            if (auditDetails.isNotEmpty()) {
                val dbHelper = AuditLogDatabaseHelper.getInstance(context)
                val destName = if (currentDirectory.absolutePath == homeDirectory.absolutePath) "Internal Storage" else currentDirectory.name
                val entry = AuditLogEntry(
                    timestamp = System.currentTimeMillis(),
                    actionType = AuditActionType.PASTE,
                    summary = "Pasted ${auditDetails.size} items to $destName",
                    destinationDirectory = currentDirectory.absolutePath,
                    totalItems = auditDetails.size,
                    items = auditDetails
                )
                dbHelper.insertLog(entry)
            }

            AppClipboard.clear()
            isPasting = false
            refreshTrigger++
        }
    }

    // Clear selection mode when currentDirectory changes
    LaunchedEffect(currentDirectory) {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedFiles = emptySet()
        }
    }

    // Intercept hardware Back Button: Navigation stack order:
    // PastActions / RecycleBin -> Settings -> File Explorer -> Parent directories
    val isHome = currentDirectory.absolutePath == homeDirectory.absolutePath
    BackHandler(enabled = currentScreenView != "MAIN" || (permissionGranted && (isSelectionMode || !isHome))) {
        when {
            currentScreenView == "PAST_ACTIONS" || currentScreenView == "RECYCLE_BIN" -> {
                currentScreenView = "SETTINGS"
                refreshTrigger++
            }
            currentScreenView == "SETTINGS" -> currentScreenView = "MAIN"
            isSelectionMode -> {
                isSelectionMode = false
                selectedFiles = emptySet()
            }
            else -> {
                val parent = currentDirectory.parentFile
                if (parent != null && currentDirectory.absolutePath != homeDirectory.absolutePath) {
                    currentDirectory = parent
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            RecycleBinDatabaseHelper.getInstance(context).cleanupExpiredItems(context)
        }
    }

    // Load files when directory or permission changes
    LaunchedEffect(currentDirectory, permissionGranted, refreshTrigger) {
        if (!permissionGranted) return@LaunchedEffect

        isLoading = true
        errorMessage = null

        withContext(Dispatchers.IO) {
            try {
                if (!currentDirectory.exists() || !currentDirectory.canRead()) {
                    errorMessage = "Cannot read directory: ${currentDirectory.name}"
                    filesList = emptyList()
                } else {
                    val files = currentDirectory.listFiles() ?: emptyArray()
                    filesList = files.map { file ->
                        val childCount = if (file.isDirectory) file.listFiles()?.size ?: 0 else 0
                        FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = file.isDirectory,
                            size = if (file.isDirectory) 0L else file.length(),
                            lastModified = file.lastModified(),
                            childCount = childCount
                        )
                    }.sortedWith(
                        compareBy<FileItem> { !it.isDirectory }
                            .thenBy { it.name.lowercase() }
                    )
                }
            } catch (e: SecurityException) {
                errorMessage = "Access Denied: Restricted system directory"
                filesList = emptyList()
            } catch (e: Exception) {
                errorMessage = e.message ?: "An unknown error occurred"
                filesList = emptyList()
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    if (currentScreenView == "SETTINGS") {
        SettingsScreen(
            onBack = { currentScreenView = "MAIN" },
            onNavigateToPastActions = { currentScreenView = "PAST_ACTIONS" },
            onNavigateToRecycleBin = { currentScreenView = "RECYCLE_BIN" }
        )
    } else if (currentScreenView == "PAST_ACTIONS") {
        PastActionsScreen(
            onBack = { currentScreenView = "SETTINGS" }
        )
    } else if (currentScreenView == "RECYCLE_BIN") {
        RecycleBinScreen(
            onBack = {
                currentScreenView = "SETTINGS"
                refreshTrigger++
            }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (permissionGranted) {
                    val isAllSelected = filesList.isNotEmpty() && selectedFiles.size == filesList.size
                    BreadcrumbsBar(
                        currentDir = currentDirectory,
                        onNavigate = {
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedFiles = emptySet()
                            }
                            currentDirectory = it
                        },
                        homeDir = homeDirectory,
                        isSelectionMode = isSelectionMode,
                        selectedCount = selectedFiles.size,
                        isAllSelected = isAllSelected,
                        clipboardCount = AppClipboard.size,
                        onSelectModeToggle = {
                            isSelectionMode = true
                            selectedFiles = emptySet()
                        },
                        onSelectAllToggle = {
                            if (isAllSelected) {
                                selectedFiles = emptySet()
                            } else {
                                selectedFiles = filesList.map { File(it.path) }.toSet()
                            }
                        },
                        onCutSelected = {
                            cutSelectedFiles()
                        },
                        onCopySelected = {
                            copySelectedFiles()
                        },
                        onDeleteSelected = {
                            if (selectedFiles.isNotEmpty()) {
                                filesPendingDelete = selectedFiles.toList()
                            }
                        },
                        onPasteClick = {
                            executePaste()
                        },
                        onClipboardClick = {
                            showClipboardDialog = true
                        },
                        onInfoClick = {
                            if (isSelectionMode) {
                                if (selectedFiles.isNotEmpty()) {
                                    showSelectionPropertiesDialog = true
                                }
                            } else {
                                selectedPropertiesFile = currentDirectory
                            }
                        },
                        onSettingsClick = {
                            currentScreenView = "SETTINGS"
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            refreshTrigger++
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (!permissionGranted) {
                            PermissionRequestView(onRequestPermission = onRequestPermission)
                        } else if (isLoading && !isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (errorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                ErrorView(
                                    message = errorMessage ?: "",
                                    onBackToHome = { currentDirectory = homeDirectory }
                                )
                            }
                        } else if (filesList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyDirectoryView(
                                    onBack = {
                                        val parent = currentDirectory.parentFile
                                        if (parent != null) currentDirectory = parent
                                    },
                                    showBackButton = !isHome
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = if (isSelectionMode) 80.dp else 80.dp)
                            ) {
                                items(filesList) { fileItem ->
                                    val targetFile = File(fileItem.path)
                                    val isSelected = selectedFiles.contains(targetFile)
                                    FileRowItem(
                                        item = fileItem,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedFiles = if (isSelected) {
                                                    selectedFiles - targetFile
                                                } else {
                                                    selectedFiles + targetFile
                                                }
                                            } else {
                                                if (fileItem.isDirectory) {
                                                    currentDirectory = targetFile
                                                } else {
                                                    if (isImage(targetFile.name) || isVideo(targetFile.name)) {
                                                        activeMediaPlayerFile = targetFile
                                                    } else {
                                                        openFileWithOS(context, targetFile)
                                                    }
                                                }
                                            }
                                        },
                                        onInfoClick = {
                                            selectedPropertiesFile = targetFile
                                        },
                                        onSelectClick = {
                                            isSelectionMode = true
                                            selectedFiles = setOf(targetFile)
                                        },
                                        onCutClick = {
                                            val added = AppClipboard.addItem(targetFile, ClipboardOpType.CUT)
                                            if (!added) {
                                                showCapacityLimitDialog = true
                                            }
                                        },
                                        onCopyClick = {
                                            val added = AppClipboard.addItem(targetFile, ClipboardOpType.COPY)
                                            if (!added) {
                                                showCapacityLimitDialog = true
                                            }
                                        },
                                        onDeleteClick = {
                                            filesPendingDelete = listOf(targetFile)
                                        },
                                        onContextMenuVisibilityChanged = {
                                            isContextMenuOpen = it
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Floating Action Button (FAB) for Paste in normal mode
                    if (permissionGranted && !isSelectionMode && AppClipboard.items.isNotEmpty() && !isContextMenuOpen) {
                        FloatingActionButton(
                            onClick = { executePaste() },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 20.dp)
                                .navigationBarsPadding(),
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(
                                            text = AppClipboard.size.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste from clipboard",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Bottom Floating Selection Bar
                    if (isSelectionMode) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                                .navigationBarsPadding(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E1E1E),
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, Color(0xFF2C2C2C))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val selectedCount = selectedFiles.size
                                val label = if (selectedCount == 1) "1 item selected" else "$selectedCount items selected"
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = Color.White
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { cutSelectedFiles() },
                                        enabled = selectedFiles.isNotEmpty(),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCut,
                                            contentDescription = "Cut selected files",
                                            tint = if (selectedFiles.isNotEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { copySelectedFiles() },
                                        enabled = selectedFiles.isNotEmpty(),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy selected files",
                                            tint = if (selectedFiles.isNotEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = {
                                            if (selectedFiles.isNotEmpty()) {
                                                filesPendingDelete = selectedFiles.toList()
                                            }
                                        },
                                        enabled = selectedFiles.isNotEmpty(),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete selected files",
                                            tint = if (selectedFiles.isNotEmpty()) MaterialTheme.colorScheme.error else Color(0xFF666666)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = {
                                            isSelectionMode = false
                                            selectedFiles = emptySet()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close selection mode",
                                            tint = Color(0xFFA0A0A0)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedPropertiesFile != null) {
        FilePropertiesDialog(
            file = selectedPropertiesFile!!,
            onDismissRequest = { selectedPropertiesFile = null }
        )
    }

    if (showSelectionPropertiesDialog && selectedFiles.isNotEmpty()) {
        FilePropertiesDialog(
            files = selectedFiles.toList(),
            onDismissRequest = { showSelectionPropertiesDialog = false }
        )
    }

    if (activeConflictFile != null) {
        ConflictResolutionDialog(
            conflictingFile = activeConflictFile!!,
            onResolve = { resolution, applyToAll ->
                conflictDeferred?.complete(Pair(resolution, applyToAll))
            },
            onDismissRequest = {
                conflictDeferred?.complete(Pair(ConflictResolution.SKIP, false))
            }
        )
    }

    if (showClipboardDialog) {
        ClipboardDialog(
            clipboardItems = AppClipboard.items,
            onClearAll = { AppClipboard.clear() },
            onDismissRequest = { showClipboardDialog = false }
        )
    }

    if (showCapacityLimitDialog) {
        CapacityLimitDialog(onDismiss = { showCapacityLimitDialog = false })
    }

    if (!filesPendingDelete.isNullOrEmpty()) {
        DeleteConfirmationDialog(
            files = filesPendingDelete!!,
            onConfirmDelete = {
                val toDelete = filesPendingDelete!!
                filesPendingDelete = null
                coroutineScope.launch {
                    val trashed = RecycleBinDatabaseHelper.getInstance(context).moveToRecycleBin(toDelete, context)
                    if (trashed.isNotEmpty()) {
                        val auditLogHelper = AuditLogDatabaseHelper.getInstance(context)
                        val details = trashed.map {
                            AuditLogItemDetail(
                                sourcePath = it.originalPath,
                                destinationPath = it.trashedPath,
                                command = "DELETE",
                                outcome = AuditItemOutcome.TRASHED,
                                fileName = it.fileName,
                                fileSize = it.fileSize,
                                isDirectory = it.isDirectory
                            )
                        }
                        val summaryText = if (trashed.size == 1) "Deleted ${trashed.first().fileName} to Recycle Bin" else "Deleted ${trashed.size} items to Recycle Bin"
                        auditLogHelper.insertLog(
                            AuditLogEntry(
                                timestamp = System.currentTimeMillis(),
                                actionType = AuditActionType.DELETE,
                                summary = summaryText,
                                destinationDirectory = "Recycle Bin",
                                totalItems = trashed.size,
                                items = details
                            )
                        )
                    }
                    if (isSelectionMode) {
                        isSelectionMode = false
                        selectedFiles = emptySet()
                    }
                    refreshTrigger++
                }
            },
            onDismissRequest = { filesPendingDelete = null }
        )
    }

    if (activeMediaPlayerFile != null) {
        MediaPlayerScreen(
            file = activeMediaPlayerFile!!,
            globalLoopEnabled = globalLoopEnabled,
            onToggleGlobalLoop = { globalLoopEnabled = it },
            onClose = {
                activeMediaPlayerFile = null
                refreshTrigger++
            },
            onNavigateToMedia = {
                activeMediaPlayerFile = it
                refreshTrigger++
            },
            onSettingsClick = {
                activeMediaPlayerFile = null
                currentScreenView = "SETTINGS"
                refreshTrigger++
            }
        )
    }
}
