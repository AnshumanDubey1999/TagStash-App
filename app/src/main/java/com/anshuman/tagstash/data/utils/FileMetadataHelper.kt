package com.anshuman.tagstash.data.utils

import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileTag(
    val id: Long = 0,
    val name: String,
    val colorHex: Int = 0xFF81C784.toInt()
)

data class MetadataItem(
    val label: String,
    val value: String
)

data class MetadataGroup(
    val title: String,
    val items: List<MetadataItem>
)

data class FilePropertiesData(
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val isDirectory: Boolean,
    val groups: List<MetadataGroup>,
    val tags: List<FileTag> = emptyList()
)

suspend fun getFilePropertiesData(
    file: File,
    tags: List<FileTag> = emptyList()
): FilePropertiesData = withContext(Dispatchers.IO) {
    val groups = mutableListOf<MetadataGroup>()

    // 1. General Properties
    val generalItems = mutableListOf<MetadataItem>()
    val displayName = if (file.name == "0" || file.absolutePath == "/storage/emulated/0") "Internal Storage" else if (file.name.isEmpty()) file.absolutePath else file.name
    generalItems.add(MetadataItem("File Name", displayName))
    generalItems.add(MetadataItem("Location", file.parent ?: file.absolutePath))

    if (file.isDirectory) {
        val childrenCount = file.listFiles()?.size ?: 0
        generalItems.add(MetadataItem("Type", "Directory / Folder"))
        generalItems.add(MetadataItem("Contains", "$childrenCount items"))
    } else {
        val formattedSize = formatFileSize(file.length())
        val byteFormatter = DecimalFormat("#,###")
        generalItems.add(MetadataItem("Size", "$formattedSize (${byteFormatter.format(file.length())} bytes)"))
        val mime = getMimeType(file)
        generalItems.add(MetadataItem("MIME Type", mime))
        val ext = file.extension.uppercase()
        if (ext.isNotEmpty()) {
            generalItems.add(MetadataItem("Extension", ext))
        }
    }

    val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    generalItems.add(MetadataItem("Date Modified", dateFormatter.format(Date(file.lastModified()))))

    groups.add(MetadataGroup("General", generalItems))

    if (file.isFile) {
        val name = file.name
        when {
            isImage(name) -> {
                extractImageMetadata(file)?.let { groups.add(it.first) }
                extractImageExifMetadata(file)?.let { groups.add(it) }
            }
            isVideo(name) -> {
                extractVideoMetadata(file)?.let { (mediaGroup, extendedGroup) ->
                    groups.add(mediaGroup)
                    extendedGroup?.let { groups.add(it) }
                }
            }
            isAudio(name) -> {
                extractAudioMetadata(file)?.let { (mediaGroup, extendedGroup) ->
                    groups.add(mediaGroup)
                    extendedGroup?.let { groups.add(it) }
                }
            }
        }
    }

    FilePropertiesData(
        fileName = file.name,
        filePath = file.absolutePath,
        mimeType = if (file.isDirectory) "folder" else getMimeType(file),
        isDirectory = file.isDirectory,
        groups = groups,
        tags = tags
    )
}

fun calculateDirectoryTotalSize(dir: File): Long {
    var size = 0L
    try {
        dir.walkTopDown().maxDepth(5).forEach { f ->
            if (f.isFile) {
                size += f.length()
            }
        }
    } catch (_: Exception) {}
    return size
}

suspend fun getMultipleFilesPropertiesData(
    files: List<File>
): FilePropertiesData = withContext(Dispatchers.IO) {
    val groups = mutableListOf<MetadataGroup>()

    val totalItems = files.size
    val folderCount = files.count { it.isDirectory }
    val fileCount = files.count { !it.isDirectory }

    var totalSize = 0L
    var imageCount = 0
    var videoCount = 0
    var audioCount = 0
    var otherDocCount = 0

    files.forEach { file ->
        if (file.isDirectory) {
            totalSize += calculateDirectoryTotalSize(file)
        } else {
            totalSize += file.length()
            val name = file.name
            when {
                isImage(name) -> imageCount++
                isVideo(name) -> videoCount++
                isAudio(name) -> audioCount++
                else -> otherDocCount++
            }
        }
    }

    val summaryItems = mutableListOf<MetadataItem>()
    val filesFoldersLabel = buildString {
        append("$totalItems items")
        val details = mutableListOf<String>()
        if (fileCount > 0) details.add(if (fileCount == 1) "1 file" else "$fileCount files")
        if (folderCount > 0) details.add(if (folderCount == 1) "1 folder" else "$folderCount folders")
        if (details.isNotEmpty()) {
            append(" (${details.joinToString(", ")})")
        }
    }
    summaryItems.add(MetadataItem("Total Selected", filesFoldersLabel))

    val formattedSize = formatFileSize(totalSize)
    val byteFormatter = DecimalFormat("#,###")
    summaryItems.add(MetadataItem("Total Size", "$formattedSize (${byteFormatter.format(totalSize)} bytes)"))

    // Common location
    val parents = files.mapNotNull { it.parent }.toSet()
    val location = if (parents.size == 1) {
        val parentPath = parents.first()
        if (parentPath == "/storage/emulated/0") "Internal Storage" else parentPath
    } else {
        "Multiple locations"
    }
    summaryItems.add(MetadataItem("Location", location))

    groups.add(MetadataGroup("Summary", summaryItems))

    // Content Breakdown Group
    val breakdownItems = mutableListOf<MetadataItem>()
    if (folderCount > 0) {
        breakdownItems.add(MetadataItem("Folders", folderCount.toString()))
    }
    if (imageCount > 0) {
        breakdownItems.add(MetadataItem("Images", imageCount.toString()))
    }
    if (videoCount > 0) {
        breakdownItems.add(MetadataItem("Videos", videoCount.toString()))
    }
    if (audioCount > 0) {
        breakdownItems.add(MetadataItem("Audio", audioCount.toString()))
    }
    if (otherDocCount > 0) {
        breakdownItems.add(MetadataItem("Other Files", otherDocCount.toString()))
    }

    if (breakdownItems.isNotEmpty()) {
        groups.add(MetadataGroup("Content Breakdown", breakdownItems))
    }

    FilePropertiesData(
        fileName = "$totalItems items selected",
        filePath = location,
        mimeType = "multiple",
        isDirectory = false,
        groups = groups,
        tags = emptyList()
    )
}

private fun extractImageMetadata(file: File): Pair<MetadataGroup, Pair<Int, Int>>? {
    val dims = getImageDimensions(file)
    if (dims.width <= 0 || dims.height <= 0) return null

    val items = mutableListOf<MetadataItem>()
    items.add(MetadataItem("Resolution", "${dims.width} × ${dims.height} pixels"))
    val aspect = calculateAspectRatio(dims.width, dims.height)
    if (aspect.isNotEmpty()) {
        items.add(MetadataItem("Aspect Ratio", aspect))
    }

    val totalPixels = dims.width.toLong() * dims.height.toLong()
    val megaPixels = totalPixels.toDouble() / 1_000_000.0
    val mpFormatter = DecimalFormat("#.#")
    items.add(MetadataItem("Megapixels", "${mpFormatter.format(megaPixels)} MP"))

    return Pair(MetadataGroup("Image Details", items), Pair(dims.width, dims.height))
}

private fun extractImageExifMetadata(file: File): MetadataGroup? {
    try {
        val exif = ExifInterface(file.absolutePath)
        val items = mutableListOf<MetadataItem>()

        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
        if (!make.isNullOrBlank() || !model.isNullOrBlank()) {
            val camera = listOfNotNull(make?.trim(), model?.trim()).distinct().joinToString(" ")
            if (camera.isNotEmpty()) {
                items.add(MetadataItem("Camera", camera))
            }
        }

        val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
        if (!iso.isNullOrBlank()) {
            items.add(MetadataItem("ISO Speed", "ISO $iso"))
        }

        val aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
        if (aperture > 0) {
            items.add(MetadataItem("Aperture", "f/$aperture"))
        }

        val exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
        if (exposureTime > 0) {
            val formattedExposure = if (exposureTime < 1.0) {
                val denominator = (1.0 / exposureTime).toInt()
                "1/$denominator s"
            } else {
                "${exposureTime}s"
            }
            items.add(MetadataItem("Shutter Speed", formattedExposure))
        }

        val focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
        if (focalLength > 0) {
            items.add(MetadataItem("Focal Length", "${focalLength} mm"))
        }

        val dateTimeTaken = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        if (!dateTimeTaken.isNullOrBlank()) {
            items.add(MetadataItem("Date Taken", dateTimeTaken))
        }

        val latLong = FloatArray(2)
        if (exif.getLatLong(latLong)) {
            val df = DecimalFormat("#.######")
            items.add(MetadataItem("GPS Location", "${df.format(latLong[0])}, ${df.format(latLong[1])}"))
        }

        val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
        if (!software.isNullOrBlank()) {
            items.add(MetadataItem("Software", software.trim()))
        }

        if (items.isNotEmpty()) {
            return MetadataGroup("EXIF / Camera Metadata", items)
        }
    } catch (_: Exception) {
    }
    return null
}

private fun extractVideoMetadata(file: File): Pair<MetadataGroup, MetadataGroup?>? {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(file.absolutePath)

        val mediaItems = mutableListOf<MetadataItem>()
        val extendedItems = mutableListOf<MetadataItem>()

        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        if (durationMs != null && durationMs > 0) {
            mediaItems.add(MetadataItem("Duration", formatDuration(durationMs)))
        }

        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

        if (width != null && height != null && width > 0 && height > 0) {
            val actualW = if (rotation == 90 || rotation == 270) height else width
            val actualH = if (rotation == 90 || rotation == 270) width else height
            mediaItems.add(MetadataItem("Resolution", "$actualW × $actualH pixels"))
            val aspect = calculateAspectRatio(actualW, actualH)
            if (aspect.isNotEmpty()) {
                mediaItems.add(MetadataItem("Aspect Ratio", aspect))
            }
        }

        val videoMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        if (!videoMime.isNullOrBlank()) {
            mediaItems.add(MetadataItem("Video Codec / Format", formatCodecName(videoMime)))
        }

        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
        if (bitrate != null && bitrate > 0) {
            val kbps = bitrate / 1000
            mediaItems.add(MetadataItem("Bitrate", "$kbps kbps"))
        }

        val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
        if (hasAudio == "yes" || hasAudio == "true") {
            mediaItems.add(MetadataItem("Audio Track", "Present"))
        }

        // Extended Metadata
        val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
        if (!date.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Creation Date", date.trim()))
        }

        val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
        if (!location.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Location Tag", location.trim()))
        }

        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        if (!title.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Title", title.trim()))
        }

        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
        if (!artist.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Artist / Author", artist.trim()))
        }

        val mediaGroup = MetadataGroup("Video Details", mediaItems)
        val extendedGroup = if (extendedItems.isNotEmpty()) MetadataGroup("Additional Metadata", extendedItems) else null

        return Pair(mediaGroup, extendedGroup)
    } catch (_: Exception) {
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
    return null
}

private fun extractAudioMetadata(file: File): Pair<MetadataGroup, MetadataGroup?>? {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(file.absolutePath)

        val mediaItems = mutableListOf<MetadataItem>()
        val extendedItems = mutableListOf<MetadataItem>()

        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        if (durationMs != null && durationMs > 0) {
            mediaItems.add(MetadataItem("Duration", formatDuration(durationMs)))
        }

        val audioMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        if (!audioMime.isNullOrBlank()) {
            mediaItems.add(MetadataItem("Audio Format", formatCodecName(audioMime)))
        }

        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
        if (bitrate != null && bitrate > 0) {
            val kbps = bitrate / 1000
            mediaItems.add(MetadataItem("Bitrate", "$kbps kbps"))
        }

        val samplerate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
        if (samplerate != null && samplerate > 0) {
            val khz = samplerate.toDouble() / 1000.0
            val df = DecimalFormat("#.#")
            mediaItems.add(MetadataItem("Sample Rate", "${df.format(khz)} kHz (${samplerate} Hz)"))
        }

        // Track Info & Extended ID3 Tags
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        if (!title.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Title", title.trim()))
        }

        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        if (!artist.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Artist", artist.trim()))
        }

        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        if (!album.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Album", album.trim()))
        }

        val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
        if (!genre.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Genre", genre.trim()))
        }

        val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
        if (!year.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Year / Release", year.trim()))
        }

        val composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
        if (!composer.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Composer", composer.trim()))
        }

        val discNum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
        if (!discNum.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Disc Number", discNum.trim()))
        }

        val trackNum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
        if (!trackNum.isNullOrBlank()) {
            extendedItems.add(MetadataItem("Track Number", trackNum.trim()))
        }

        val mediaGroup = MetadataGroup("Audio Details", mediaItems)
        val extendedGroup = if (extendedItems.isNotEmpty()) MetadataGroup("Additional Metadata", extendedItems) else null

        return Pair(mediaGroup, extendedGroup)
    } catch (_: Exception) {
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
    return null
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private fun calculateAspectRatio(width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return ""
    val gcd = gcd(width, height)
    val wRatio = width / gcd
    val hRatio = height / gcd
    return "$wRatio:$hRatio"
}

private fun gcd(a: Int, b: Int): Int {
    var x = a
    var y = b
    while (y != 0) {
        val temp = y
        y = x % y
        x = temp
    }
    return x
}

private fun formatCodecName(mime: String): String {
    return when (mime.lowercase()) {
        "video/mp4", "video/avc" -> "H.264 / AVC ($mime)"
        "video/hevc" -> "H.265 / HEVC ($mime)"
        "video/av01" -> "AV1 ($mime)"
        "video/x-vnd.on2.vp9" -> "VP9 ($mime)"
        "video/x-vnd.on2.vp8" -> "VP8 ($mime)"
        "audio/mp4a-latm", "audio/aac" -> "AAC ($mime)"
        "audio/mpeg" -> "MP3 ($mime)"
        "audio/flac" -> "FLAC ($mime)"
        "audio/opus" -> "Opus ($mime)"
        "audio/vorbis" -> "Vorbis ($mime)"
        else -> mime
    }
}
