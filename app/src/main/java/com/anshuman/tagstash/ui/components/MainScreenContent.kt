package com.anshuman.tagstash.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshuman.tagstash.data.clipboard.AppClipboard
import com.anshuman.tagstash.data.model.FileItem
import java.io.File

@Composable
fun MainScreenContent(
    permissionGranted: Boolean,
    activeSearchQuery: String?,
    searchResults: List<FileItem>,
    isSearching: Boolean,
    scannedCount: Int,
    searchIncludeSubfolders: Boolean,
    filesList: List<FileItem>,
    selectedFiles: Set<File>,
    currentDirectory: File,
    homeDirectory: File,
    isSelectionMode: Boolean,
    isLoading: Boolean,
    isRefreshing: Boolean,
    errorMessage: String?,
    isContextMenuOpen: Boolean,
    context: Context,
    onNavigateDirectory: (File) -> Unit,
    onSelectModeToggle: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onCutSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onPasteClick: () -> Unit,
    onClipboardClick: () -> Unit,
    onSearchClick: () -> Unit,
    onInfoClick: (File) -> Unit,
    onSelectionInfoClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackFromSearch: () -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onBackToHome: () -> Unit,
    onBackToParent: (File) -> Unit,
    onOpenMediaFile: (File) -> Unit,
    onToggleFileSelection: (File, Boolean) -> Unit,
    onSelectClick: (File) -> Unit,
    onSingleCut: (File) -> Unit,
    onSingleCopy: (File) -> Unit,
    onSingleRename: (File) -> Unit,
    onSingleDelete: (File) -> Unit,
    onContextMenuVisibilityChanged: (Boolean) -> Unit,
    onCloseSelectionMode: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (permissionGranted) {
                if (activeSearchQuery != null) {
                    SearchHeaderBar(
                        query = activeSearchQuery,
                        resultsCount = searchResults.size,
                        isSearching = isSearching,
                        scannedCount = scannedCount,
                        includeSubfolders = searchIncludeSubfolders,
                        onBack = onBackFromSearch,
                        onSearchAgain = onSearchClick
                    )
                } else {
                    val isAllSelected = filesList.isNotEmpty() && selectedFiles.size == filesList.size
                    BreadcrumbsBar(
                        currentDir = currentDirectory,
                        onNavigate = onNavigateDirectory,
                        homeDir = homeDirectory,
                        isSelectionMode = isSelectionMode,
                        selectedCount = selectedFiles.size,
                        isAllSelected = isAllSelected,
                        clipboardCount = AppClipboard.size,
                        onSelectModeToggle = onSelectModeToggle,
                        onSelectAllToggle = onSelectAllToggle,
                        onCutSelected = onCutSelected,
                        onCopySelected = onCopySelected,
                        onDeleteSelected = onDeleteSelected,
                        onPasteClick = onPasteClick,
                        onClipboardClick = onClipboardClick,
                        onSearchClick = onSearchClick,
                        onInfoClick = {
                            if (isSelectionMode) {
                                onSelectionInfoClick()
                            } else {
                                onInfoClick(currentDirectory)
                            }
                        },
                        onSettingsClick = onSettingsClick
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (activeSearchQuery != null) {
                    SearchResultsView(
                        searchResults = searchResults,
                        isSearching = isSearching,
                        scannedCount = scannedCount,
                        query = activeSearchQuery,
                        includeSubfolders = searchIncludeSubfolders,
                        homeDirectory = homeDirectory,
                        onBackToFolder = onBackFromSearch
                    )
                } else {
                    MainScreenFileListView(
                        permissionGranted = permissionGranted,
                        isLoading = isLoading,
                        isRefreshing = isRefreshing,
                        errorMessage = errorMessage,
                        filesList = filesList,
                        currentDirectory = currentDirectory,
                        homeDirectory = homeDirectory,
                        isSelectionMode = isSelectionMode,
                        selectedFiles = selectedFiles,
                        context = context,
                        onRefresh = onRefresh,
                        onRequestPermission = onRequestPermission,
                        onBackToHome = onBackToHome,
                        onBackToParent = onBackToParent,
                        onNavigateToDirectory = onNavigateDirectory,
                        onOpenMediaFile = onOpenMediaFile,
                        onToggleFileSelection = onToggleFileSelection,
                        onInfoClick = onInfoClick,
                        onSelectClick = onSelectClick,
                        onCutClick = onSingleCut,
                        onCopyClick = onSingleCopy,
                        onRenameClick = onSingleRename,
                        onDeleteClick = onSingleDelete,
                        onContextMenuVisibilityChanged = onContextMenuVisibilityChanged
                    )
                }

                // Floating Action Button (FAB) for Paste in normal mode
                if (permissionGranted && activeSearchQuery == null && !isSelectionMode && AppClipboard.items.isNotEmpty() && !isContextMenuOpen) {
                    FloatingActionButton(
                        onClick = onPasteClick,
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
                if (isSelectionMode && activeSearchQuery == null) {
                    SelectionBottomBar(
                        selectedCount = selectedFiles.size,
                        onCut = onCutSelected,
                        onCopy = onCopySelected,
                        onDelete = onDeleteSelected,
                        onClose = onCloseSelectionMode,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
