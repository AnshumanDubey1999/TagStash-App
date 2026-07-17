package com.anshuman.tagstash.data.utils

import android.graphics.BitmapFactory
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.FileProvider
import com.anshuman.tagstash.data.model.FileItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun buildBreadcrumbs(currentDir: File, homeDir: File = File("/storage/emulated/0")): List<Pair<String, File>> {
    val homePath = homeDir.absolutePath
    
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
    return ext in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "avif")
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
    if (ext == "avif") return "image/avif"
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
}

data class ImageDimensions(val width: Int, val height: Int)

fun getSiblingMedia(file: File): List<File> {
    val parent = file.parentFile ?: return listOf(file)
    val siblings = parent.listFiles() ?: return listOf(file)
    return siblings.filter { it.isFile && (isImage(it.name) || isVideo(it.name)) }
        .sortedWith(compareBy { it.name.lowercase() })
}

fun getImageDimensions(file: File): ImageDimensions {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(file.absolutePath, options)
    return ImageDimensions(options.outWidth, options.outHeight)
}
