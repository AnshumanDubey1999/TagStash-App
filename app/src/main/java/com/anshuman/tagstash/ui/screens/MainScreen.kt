package com.anshuman.tagstash.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.anshuman.tagstash.data.clipboard.AppClipboard
import com.anshuman.tagstash.data.clipboard.ClipboardOpType
import com.anshuman.tagstash.data.database.RecycleBinDatabaseHelper
import com.anshuman.tagstash.data.model.FileItem
import com.anshuman.tagstash.data.utils.ConflictResolution
import com.anshuman.tagstash.data.utils.executeDeleteOperations
import com.anshuman.tagstash.data.utils.executeFolderSearch
import com.anshuman.tagstash.data.utils.executePasteOperations
import com.anshuman.tagstash.data.utils.executeRenameOperation
import com.anshuman.tagstash.data.utils.isImage
import com.anshuman.tagstash.data.utils.isVideo
import com.anshuman.tagstash.ui.components.MainScreenContent
import com.anshuman.tagstash.ui.components.MainScreenDialogs
import com.anshuman.tagstash.ui.components.OperationProgressState
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
    val dirKey = initialDirectory.absolutePath
    var currentDirectory by rememberSaveable(dirKey, stateSaver = FileSaver) { mutableStateOf(initialDirectory) }
    var filesList by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeMediaPlayerFile by rememberSaveable(dirKey, stateSaver = NullableFileSaver) { mutableStateOf<File?>(null) }
    var globalLoopEnabled by rememberSaveable { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var selectedPropertiesFile by rememberSaveable(dirKey, stateSaver = NullableFileSaver) { mutableStateOf<File?>(null) }

    // Selection Mode State
    var isSelectionMode by rememberSaveable(dirKey) { mutableStateOf(false) }
    var selectedFiles by rememberSaveable(dirKey, stateSaver = FileSetSaver) { mutableStateOf(emptySet<File>()) }
    var showSelectionPropertiesDialog by rememberSaveable(dirKey) { mutableStateOf(false) }

    // Screen Navigation State (MAIN, SETTINGS, PAST_ACTIONS, RECYCLE_BIN)
    var currentScreenView by rememberSaveable(dirKey) { mutableStateOf("MAIN") }

    // Search & Filter State
    var showSearchDialog by rememberSaveable(dirKey) { mutableStateOf(false) }
    var activeSearchQuery by rememberSaveable(dirKey) { mutableStateOf<String?>(null) }
    var searchIncludeSubfolders by rememberSaveable(dirKey) { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var scannedCount by remember { mutableStateOf(0) }

    // Clipboard and Paste State
    val coroutineScope = rememberCoroutineScope()
    var showClipboardDialog by rememberSaveable(dirKey) { mutableStateOf(false) }
    var showCapacityLimitDialog by rememberSaveable(dirKey) { mutableStateOf(false) }
    var activeConflictFile by remember { mutableStateOf<File?>(null) }
    var conflictDeferred by remember { mutableStateOf<CompletableDeferred<Pair<ConflictResolution, Boolean>>?>(null) }
    var isPasting by remember { mutableStateOf(false) }
    var operationProgress by remember { mutableStateOf<OperationProgressState?>(null) }
    var isContextMenuOpen by remember { mutableStateOf(false) }
    var filesPendingDelete by remember { mutableStateOf<List<File>?>(null) }
    var filePendingRename by remember { mutableStateOf<File?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

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
            executePasteOperations(
                context = context,
                currentDirectory = currentDirectory,
                itemsToPaste = AppClipboard.items.toList(),
                primaryColor = primaryColor,
                onProgressUpdate = { operationProgress = it },
                onConflictDetected = { sourceFile ->
                    val deferred = CompletableDeferred<Pair<ConflictResolution, Boolean>>()
                    conflictDeferred = deferred
                    activeConflictFile = sourceFile
                    val result = deferred.await()
                    activeConflictFile = null
                    conflictDeferred = null
                    result
                }
            )
            isPasting = false
            refreshTrigger++
        }
    }

    // Reset selection and search on directory navigation
    var previousDirectory by remember { mutableStateOf<File?>(null) }
    LaunchedEffect(currentDirectory) {
        if (previousDirectory != null && previousDirectory?.absolutePath != currentDirectory.absolutePath) {
            if (isSelectionMode) {
                isSelectionMode = false
                selectedFiles = emptySet()
            }
            if (activeSearchQuery != null) {
                activeSearchQuery = null
                searchResults = emptyList()
            }
        }
        previousDirectory = currentDirectory
    }

    // Hardware Back Button interception
    val isHome = currentDirectory.absolutePath == homeDirectory.absolutePath
    BackHandler(enabled = currentScreenView != "MAIN" || (permissionGranted && (activeSearchQuery != null || isSelectionMode || !isHome))) {
        when {
            currentScreenView == "PAST_ACTIONS" || currentScreenView == "RECYCLE_BIN" -> {
                currentScreenView = "SETTINGS"
                refreshTrigger++
            }
            currentScreenView == "SETTINGS" -> currentScreenView = "MAIN"
            activeSearchQuery != null -> {
                activeSearchQuery = null
                searchResults = emptyList()
            }
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

    // Background search execution
    LaunchedEffect(activeSearchQuery, searchIncludeSubfolders, currentDirectory, refreshTrigger) {
        val query = activeSearchQuery
        if (query == null || query.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        scannedCount = 0
        searchResults = executeFolderSearch(
            directory = currentDirectory,
            query = query,
            includeSubfolders = searchIncludeSubfolders,
            onCountUpdate = { scannedCount = it }
        )
        isSearching = false
    }

    val searchMediaPlaylist = remember(searchResults) {
        searchResults.filter { !it.isDirectory && (isImage(it.name) || isVideo(it.name)) }.map { File(it.path) }
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
        MainScreenContent(
            permissionGranted = permissionGranted,
            activeSearchQuery = activeSearchQuery,
            searchResults = searchResults,
            isSearching = isSearching,
            scannedCount = scannedCount,
            searchIncludeSubfolders = searchIncludeSubfolders,
            filesList = filesList,
            selectedFiles = selectedFiles,
            currentDirectory = currentDirectory,
            homeDirectory = homeDirectory,
            isSelectionMode = isSelectionMode,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            errorMessage = errorMessage,
            isContextMenuOpen = isContextMenuOpen,
            context = context,
            onNavigateDirectory = { targetDir ->
                if (isSelectionMode) {
                    isSelectionMode = false
                    selectedFiles = emptySet()
                }
                currentDirectory = targetDir
            },
            onSelectModeToggle = {
                isSelectionMode = true
                selectedFiles = emptySet()
            },
            onSelectAllToggle = {
                val isAll = filesList.isNotEmpty() && selectedFiles.size == filesList.size
                selectedFiles = if (isAll) emptySet() else filesList.map { File(it.path) }.toSet()
            },
            onCutSelected = { cutSelectedFiles() },
            onCopySelected = { copySelectedFiles() },
            onDeleteSelected = {
                if (selectedFiles.isNotEmpty()) {
                    filesPendingDelete = selectedFiles.toList()
                }
            },
            onPasteClick = { executePaste() },
            onClipboardClick = { showClipboardDialog = true },
            onSearchClick = { showSearchDialog = true },
            onInfoClick = { target -> selectedPropertiesFile = target },
            onSelectionInfoClick = {
                if (selectedFiles.isNotEmpty()) {
                    showSelectionPropertiesDialog = true
                }
            },
            onSettingsClick = { currentScreenView = "SETTINGS" },
            onBackFromSearch = {
                activeSearchQuery = null
                searchResults = emptyList()
            },
            onRefresh = {
                isRefreshing = true
                refreshTrigger++
            },
            onRequestPermission = onRequestPermission,
            onBackToHome = { currentDirectory = homeDirectory },
            onBackToParent = { parent -> currentDirectory = parent },
            onOpenMediaFile = { mediaFile -> activeMediaPlayerFile = mediaFile },
            onToggleFileSelection = { targetFile, isSelected ->
                selectedFiles = if (isSelected) selectedFiles - targetFile else selectedFiles + targetFile
            },
            onSelectClick = { targetFile ->
                isSelectionMode = true
                selectedFiles = setOf(targetFile)
            },
            onSingleCut = { targetFile ->
                val success = AppClipboard.addItem(targetFile, ClipboardOpType.CUT)
                if (!success) showCapacityLimitDialog = true
            },
            onSingleCopy = { targetFile ->
                val success = AppClipboard.addItem(targetFile, ClipboardOpType.COPY)
                if (!success) showCapacityLimitDialog = true
            },
            onSingleRename = { targetFile -> filePendingRename = targetFile },
            onSingleDelete = { targetFile -> filesPendingDelete = listOf(targetFile) },
            onContextMenuVisibilityChanged = { isContextMenuOpen = it },
            onCloseSelectionMode = {
                isSelectionMode = false
                selectedFiles = emptySet()
            }
        )
    }

    MainScreenDialogs(
        selectedPropertiesFile = selectedPropertiesFile,
        onDismissProperties = { selectedPropertiesFile = null },
        showSelectionPropertiesDialog = showSelectionPropertiesDialog,
        selectedFiles = selectedFiles,
        onDismissSelectionProperties = { showSelectionPropertiesDialog = false },
        activeConflictFile = activeConflictFile,
        onResolveConflict = { resolution, applyToAll ->
            conflictDeferred?.complete(Pair(resolution, applyToAll))
        },
        onDismissConflict = {
            conflictDeferred?.complete(Pair(ConflictResolution.SKIP, false))
        },
        showClipboardDialog = showClipboardDialog,
        onDismissClipboard = { showClipboardDialog = false },
        showCapacityLimitDialog = showCapacityLimitDialog,
        onDismissCapacityLimit = { showCapacityLimitDialog = false },
        filesPendingDelete = filesPendingDelete,
        onConfirmDelete = {
            val toDelete = filesPendingDelete ?: return@MainScreenDialogs
            filesPendingDelete = null
            coroutineScope.launch {
                executeDeleteOperations(
                    context = context,
                    filesToDelete = toDelete,
                    errorColor = errorColor,
                    onProgressUpdate = { operationProgress = it }
                )
                if (isSelectionMode) {
                    isSelectionMode = false
                    selectedFiles = emptySet()
                }
                refreshTrigger++
            }
        },
        onDismissDelete = { filesPendingDelete = null },
        operationProgress = operationProgress,
        filePendingRename = filePendingRename,
        onConfirmRename = { newName ->
            val oldFile = filePendingRename ?: return@MainScreenDialogs
            filePendingRename = null
            coroutineScope.launch {
                val success = executeRenameOperation(context, oldFile, newName)
                if (success) {
                    refreshTrigger++
                }
            }
        },
        onDismissRename = { filePendingRename = null },
        activeMediaPlayerFile = activeMediaPlayerFile,
        globalLoopEnabled = globalLoopEnabled,
        onToggleGlobalLoop = { globalLoopEnabled = it },
        onCloseMediaPlayer = {
            activeMediaPlayerFile = null
            refreshTrigger++
        },
        onNavigateToMedia = {
            activeMediaPlayerFile = it
            refreshTrigger++
        },
        onMediaPlayerSettingsClick = {
            activeMediaPlayerFile = null
            currentScreenView = "SETTINGS"
            refreshTrigger++
        },
        mediaPlaylist = if (activeSearchQuery != null) searchMediaPlaylist else null,
        onDeleteMedia = { deletedFile ->
            if (activeSearchQuery != null) {
                searchResults = searchResults.filter { it.path != deletedFile.absolutePath }
            }
            refreshTrigger++
        },
        onRenameMedia = { oldFile, newFile ->
            if (activeSearchQuery != null) {
                searchResults = searchResults.map { item ->
                    if (item.path == oldFile.absolutePath) {
                        item.copy(
                            name = newFile.name,
                            path = newFile.absolutePath,
                            lastModified = newFile.lastModified()
                        )
                    } else {
                        item
                    }
                }
            }
            refreshTrigger++
        },
        showSearchDialog = showSearchDialog,
        activeSearchQuery = activeSearchQuery,
        searchIncludeSubfolders = searchIncludeSubfolders,
        onSearch = { query, includeSubfolders ->
            showSearchDialog = false
            searchIncludeSubfolders = includeSubfolders
            activeSearchQuery = query
        },
        onDismissSearch = { showSearchDialog = false }
    )
}
