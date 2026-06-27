package com.anshuman.tagstash

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Model representing files
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val childCount: Int = 0
)

class MainActivity : ComponentActivity() {
    private val permissionState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TagStashTheme {
                MainScreen(
                    permissionGranted = permissionState.value,
                    onRequestPermission = { requestAllFilesPermission() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionState.value = checkPermission()
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // Min SDK is 33, so we are guaranteed to be on SDK >= R
            true
        }
    }

    private fun requestAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }
}

@Composable
fun TagStashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6366F1), // Indigo
            secondary = Color(0xFF8B5CF6), // Violet
            background = Color(0xFF121212), // Deep Charcoal
            surface = Color(0xFF1E1E1E), // Slate Card Background
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFFE2E8F0),
            onSurface = Color(0xFFE2E8F0),
            surfaceVariant = Color(0xFF2D3748),
            onSurfaceVariant = Color(0xFFCBD5E1),
            error = Color(0xFFEF4444)
        ),
        content = content
    )
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    var currentDirectory by remember { mutableStateOf(File("/storage/emulated/0")) }
    var filesList by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Intercept hardware Back Button
    val isHome = currentDirectory.absolutePath == "/storage/emulated/0"
    BackHandler(enabled = permissionGranted && !isHome) {
        val parent = currentDirectory.parentFile
        if (parent != null) {
            currentDirectory = parent
        }
    }

    // Load files when directory or permission changes
    LaunchedEffect(currentDirectory, permissionGranted) {
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
                    onNavigate = { currentDirectory = it }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (!permissionGranted) {
                    PermissionRequestView(onRequestPermission = onRequestPermission)
                } else if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (errorMessage != null) {
                    ErrorView(
                        message = errorMessage ?: "",
                        onBackToHome = { currentDirectory = File("/storage/emulated/0") }
                    )
                } else if (filesList.isEmpty()) {
                    EmptyDirectoryView(
                        onBack = {
                            val parent = currentDirectory.parentFile
                            if (parent != null) currentDirectory = parent
                        },
                        showBackButton = !isHome
                    )
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
                                        openFileWithOS(context, File(fileItem.path))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreadcrumbsBar(
    currentDir: File,
    onNavigate: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val breadcrumbs = remember(currentDir) { buildBreadcrumbs(currentDir) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
    }
}

@Composable
fun FileRowItem(
    item: FileItem,
    onClick: () -> Unit
) {
    val fileIcon = remember(item) { getFileIcon(item) }
    val iconColor = remember(item) { getIconColor(item) }
    val formattedSize = remember(item.size) { formatFileSize(item.size) }
    val formattedDate = remember(item.lastModified) { formatLastModified(item.lastModified) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconColor.copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = fileIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (item.isDirectory) {
                    val itemLabel = if (item.childCount == 1) "1 item" else "${item.childCount} items"
                    "$itemLabel • $formattedDate"
                } else {
                    "$formattedSize • $formattedDate"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (item.isDirectory) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PermissionRequestView(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Storage Permission Required",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "TagStash is a local file explorer and media organizer. It requires \"All Files Access\" permission to view, play, and tag the directories in your primary storage.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Grant Permission",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBackToHome,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Back to Home")
        }
    }
}

@Composable
fun EmptyDirectoryView(
    onBack: () -> Unit,
    showBackButton: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This folder is empty",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        if (showBackButton) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Go Back")
            }
        }
    }
}

// Helper methods

fun buildBreadcrumbs(currentDir: File): List<Pair<String, File>> {
    val homePath = "/storage/emulated/0"
    val homeDir = File(homePath)
    
    val list = mutableListOf<Pair<String, File>>()
    list.add(Pair("Home", homeDir))
    
    if (currentDir.absolutePath == homePath) {
        return list
    }
    
    val relativePath = currentDir.absolutePath.substringAfter(homePath).trim('/')
    if (relativePath.isNotEmpty()) {
        val segments = relativePath.split('/')
        var cumulativeFile = homeDir
        for (segment in segments) {
            cumulativeFile = File(cumulativeFile, segment)
            list.add(Pair(segment, cumulativeFile))
        }
    }
    return list
}

fun formatBreadcrumbName(name: String): String {
    return if (name.length > 10) {
        name.take(7) + "..."
    } else {
        name
    }
}

fun getFileIcon(item: FileItem): ImageVector {
    return when {
        item.isDirectory -> Icons.Default.Folder
        isImage(item.name) -> Icons.Default.Image
        isVideo(item.name) -> Icons.Default.Movie
        isAudio(item.name) -> Icons.Default.Audiotrack
        isDocument(item.name) -> Icons.Default.Description
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

fun getIconColor(item: FileItem): Color {
    return when {
        item.isDirectory -> Color(0xFF8B5CF6) // Violet
        isImage(item.name) -> Color(0xFF10B981) // Green/Emerald
        isVideo(item.name) -> Color(0xFFF59E0B) // Amber
        isAudio(item.name) -> Color(0xFF3B82F6) // Blue
        isDocument(item.name) -> Color(0xFF06B6D4) // Cyan
        else -> Color(0xFF94A3B8) // Slate Gray
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatLastModified(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun isImage(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
}

fun isVideo(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf("mp4", "mkv", "webm", "avi", "3gp")
}

fun isAudio(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
}

fun isDocument(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "xml", "json")
}

fun openFileWithOS(context: Context, file: File) {
    val mimeType = getMimeType(file)
    val authority = "${context.packageName}.fileprovider"
    
    try {
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open file with"))
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Error opening file: ${e.localizedMessage ?: "no compatible app found"}",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun getMimeType(file: File): String {
    val ext = file.extension.lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
}
