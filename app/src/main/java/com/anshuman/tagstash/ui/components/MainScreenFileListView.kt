package com.anshuman.tagstash.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anshuman.tagstash.data.model.FileItem
import com.anshuman.tagstash.data.utils.isImage
import com.anshuman.tagstash.data.utils.isVideo
import com.anshuman.tagstash.data.utils.openFileWithOS
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenFileListView(
    permissionGranted: Boolean,
    isLoading: Boolean,
    isRefreshing: Boolean,
    errorMessage: String?,
    filesList: List<FileItem>,
    currentDirectory: File,
    homeDirectory: File,
    isSelectionMode: Boolean,
    selectedFiles: Set<File>,
    context: Context,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onBackToHome: () -> Unit,
    onBackToParent: (File) -> Unit,
    onNavigateToDirectory: (File) -> Unit,
    onOpenMediaFile: (File) -> Unit,
    onToggleFileSelection: (File, Boolean) -> Unit,
    onInfoClick: (File) -> Unit,
    onSelectClick: (File) -> Unit,
    onCutClick: (File) -> Unit,
    onCopyClick: (File) -> Unit,
    onRenameClick: (File) -> Unit,
    onDeleteClick: (File) -> Unit,
    onContextMenuVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHome = currentDirectory.absolutePath == homeDirectory.absolutePath

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
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
                    message = errorMessage,
                    onBackToHome = onBackToHome
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
                        if (parent != null) onBackToParent(parent)
                    },
                    showBackButton = !isHome
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
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
                                onToggleFileSelection(targetFile, isSelected)
                            } else {
                                if (fileItem.isDirectory) {
                                    onNavigateToDirectory(targetFile)
                                } else {
                                    if (isImage(targetFile.name) || isVideo(targetFile.name)) {
                                        onOpenMediaFile(targetFile)
                                    } else {
                                        openFileWithOS(context, targetFile)
                                    }
                                }
                            }
                        },
                        onInfoClick = { onInfoClick(targetFile) },
                        onSelectClick = { onSelectClick(targetFile) },
                        onCutClick = { onCutClick(targetFile) },
                        onCopyClick = { onCopyClick(targetFile) },
                        onRenameClick = { onRenameClick(targetFile) },
                        onDeleteClick = { onDeleteClick(targetFile) },
                        onContextMenuVisibilityChanged = onContextMenuVisibilityChanged
                    )
                }
            }
        }
    }
}
