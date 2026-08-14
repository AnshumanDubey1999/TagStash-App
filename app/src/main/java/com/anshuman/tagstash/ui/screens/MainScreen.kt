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
import com.anshuman.tagstash.data.clipboard.AppClipboard
import com.anshuman.tagstash.data.clipboard.ClipboardOpType
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
import com.anshuman.tagstash.ui.components.EmptyDirectoryView
import com.anshuman.tagstash.ui.components.ErrorView
import com.anshuman.tagstash.ui.components.FilePropertiesDialog
import com.anshuman.tagstash.ui.components.FileRowItem
import com.anshuman.tagstash.ui.components.PermissionRequestView
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
    var activeMediaPlayerFile by rememberSaveable(stateSaver = NullableFileSaver) { mutableStateOf<File?>(null) }
    var globalLoopEnabled by rememberSaveable { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var selectedPropertiesFile by rememberSaveable(stateSaver = NullableFileSaver) { mutableStateOf<File?>(null) }

    // Selection Mode State
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedFiles by rememberSaveable(stateSaver = FileSetSaver) { mutableStateOf(emptySet<File>()) }
    var showSelectionPropertiesDialog by rememberSaveable { mutableStateOf(false) }

    // Clipboard and Paste State
    val coroutineScope = rememberCoroutineScope()
    var showClipboardDialog by rememberSaveable { mutableStateOf(false) }
    var activeConflictFile by remember { mutableStateOf<File?>(null) }
    var conflictDeferred by remember { mutableStateOf<CompletableDeferred<Pair<ConflictResolution, Boolean>>?>(null) }
    var isPasting by remember { mutableStateOf(false) }

    fun executePaste() {
        if (AppClipboard.items.isEmpty() || isPasting) return
        coroutineScope.launch {
            isPasting = true
            var batchResolution: ConflictResolution? = null
            val itemsToPaste = AppClipboard.items.toList()

            for (item in itemsToPaste) {
                val sourceFile = item.file
                if (!sourceFile.exists()) continue

                // If cutting file in the same directory where it already resides, skip
                if (sourceFile.parentFile?.absolutePath == currentDirectory.absolutePath && item.opType == ClipboardOpType.CUT) {
                    continue
                }

                var targetFile = File(currentDirectory, sourceFile.name)
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
                            if (item.opType == ClipboardOpType.CUT) {
                                targetFile.deleteRecursively()
                                moveFileOrDirectory(sourceFile, targetFile)
                            } else {
                                copyFileOrDirectory(sourceFile, targetFile)
                            }
                        }
                        ConflictResolution.KEEP_BOTH -> {
                            val newName = generateCopyFileName(currentDirectory, sourceFile.name)
                            targetFile = File(currentDirectory, newName)
                            if (item.opType == ClipboardOpType.CUT) {
                                moveFileOrDirectory(sourceFile, targetFile)
                            } else {
                                copyFileOrDirectory(sourceFile, targetFile)
                            }
                        }
                        ConflictResolution.SKIP -> {
                            // Skip pasting this item
                        }
                    }
                } else {
                    if (item.opType == ClipboardOpType.CUT) {
                        moveFileOrDirectory(sourceFile, targetFile)
                    } else {
                        copyFileOrDirectory(sourceFile, targetFile)
                    }
                }
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

    // Intercept hardware Back Button: priority to Selection Mode first, then upward directory navigation
    val isHome = currentDirectory.absolutePath == homeDirectory.absolutePath
    BackHandler(enabled = permissionGranted && (isSelectionMode || !isHome)) {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedFiles = emptySet()
        } else {
            val parent = currentDirectory.parentFile
            if (parent != null && currentDirectory.absolutePath != homeDirectory.absolutePath) {
                currentDirectory = parent
            }
        }
    }

    // Load files when directory or permission changes
    LaunchedEffect(currentDirectory, permissionGranted, refreshTrigger) {
        if (!permissionGranted) return@LaunchedEffect

        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val files = currentDirectory.listFiles()
                if (files == null) {
                    errorMessage = "Access Denied or Directory Unreadable"
                    filesList = emptyList()
                } else {
                    filesList = files.map { file ->
                        val childCount = if (file.isDirectory) (file.list()?.size ?: 0) else 0
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
                            contentPadding = PaddingValues(bottom = if (isSelectionMode) 80.dp else 16.dp)
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
                                    }
                                )
                            }
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

    if (activeMediaPlayerFile != null) {
        MediaPlayerScreen(
            file = activeMediaPlayerFile!!,
            globalLoopEnabled = globalLoopEnabled,
            onToggleGlobalLoop = { globalLoopEnabled = it },
            onClose = { activeMediaPlayerFile = null },
            onNavigateToMedia = { activeMediaPlayerFile = it }
        )
    }
}
