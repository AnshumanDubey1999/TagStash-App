package com.anshuman.tagstash.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anshuman.tagstash.data.model.FileItem
import com.anshuman.tagstash.data.utils.openFileWithOS
import com.anshuman.tagstash.data.utils.isImage
import com.anshuman.tagstash.data.utils.isVideo
import com.anshuman.tagstash.ui.components.BreadcrumbsBar
import com.anshuman.tagstash.ui.components.EmptyDirectoryView
import com.anshuman.tagstash.ui.components.ErrorView
import com.anshuman.tagstash.ui.components.FileRowItem
import com.anshuman.tagstash.ui.components.PermissionRequestView
import kotlinx.coroutines.Dispatchers
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

    // Intercept hardware Back Button
    val isHome = currentDirectory.absolutePath == homeDirectory.absolutePath
    BackHandler(enabled = permissionGranted && !isHome) {
        val parent = currentDirectory.parentFile
        if (parent != null && currentDirectory.absolutePath != homeDirectory.absolutePath) {
            currentDirectory = parent
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
                BreadcrumbsBar(
                    currentDir = currentDirectory,
                    onNavigate = { currentDirectory = it },
                    homeDir = homeDirectory
                )
            }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    refreshTrigger++
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
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
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filesList) { fileItem ->
                            FileRowItem(
                                item = fileItem,
                                onClick = {
                                    if (fileItem.isDirectory) {
                                        currentDirectory = File(fileItem.path)
                                    } else {
                                        val targetFile = File(fileItem.path)
                                        if (isImage(targetFile.name) || isVideo(targetFile.name)) {
                                            activeMediaPlayerFile = targetFile
                                        } else {
                                            openFileWithOS(context, targetFile)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
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
