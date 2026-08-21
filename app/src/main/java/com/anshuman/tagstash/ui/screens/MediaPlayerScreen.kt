package com.anshuman.tagstash.ui.screens

import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Precision
import com.anshuman.tagstash.R
import com.anshuman.tagstash.data.clipboard.AppClipboard
import com.anshuman.tagstash.data.clipboard.ClipboardOpType
import com.anshuman.tagstash.data.database.AuditLogDatabaseHelper
import com.anshuman.tagstash.data.database.RecycleBinDatabaseHelper
import com.anshuman.tagstash.data.model.AuditActionType
import com.anshuman.tagstash.data.model.AuditItemOutcome
import com.anshuman.tagstash.data.model.AuditLogEntry
import com.anshuman.tagstash.data.model.AuditLogItemDetail
import com.anshuman.tagstash.data.utils.AvifCoderDecoder
import com.anshuman.tagstash.data.utils.ImageDimensions
import com.anshuman.tagstash.data.utils.getImageDimensions
import com.anshuman.tagstash.data.utils.getSiblingMedia
import com.anshuman.tagstash.data.utils.isImage
import com.anshuman.tagstash.data.utils.isVideo
import com.anshuman.tagstash.ui.components.MediaPlayerBottomControls
import com.anshuman.tagstash.ui.components.MediaPlayerDialogs
import com.anshuman.tagstash.ui.components.MediaPlayerTopBar
import com.anshuman.tagstash.ui.components.ZoomableImageView
import com.anshuman.tagstash.ui.components.clampOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MediaPlayerScreen(
    file: File,
    globalLoopEnabled: Boolean,
    onToggleGlobalLoop: (Boolean) -> Unit,
    onClose: () -> Unit,
    onNavigateToMedia: (File) -> Unit,
    playlist: List<File>? = null,
    onDeleteMedia: ((File) -> Unit)? = null,
    onRenameMedia: ((oldFile: File, newFile: File) -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showOverlays by rememberSaveable { mutableStateOf(false) }
    var showPropertiesDialog by rememberSaveable { mutableStateOf(false) }
    var showClipboardDialog by rememberSaveable { mutableStateOf(false) }
    var showCapacityLimitDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val siblingMedia = remember(file, playlist) { playlist ?: getSiblingMedia(file) }
    val currentIndex = remember(file, siblingMedia) {
        siblingMedia.indexOfFirst { it.absolutePath == file.absolutePath }.coerceAtLeast(0)
    }

    var scale by remember(file) { mutableStateOf(1.0f) }
    var offset by remember(file) { mutableStateOf(Offset.Zero) }

    var imageDimensions by remember(file) { mutableStateOf<ImageDimensions?>(null) }
    var videoDimensions by remember(file) { mutableStateOf<ImageDimensions?>(null) }

    LaunchedEffect(file) {
        if (isImage(file.name)) {
            imageDimensions = withContext(Dispatchers.IO) { getImageDimensions(file) }
        } else if (isVideo(file.name)) {
            videoDimensions = withContext(Dispatchers.IO) { getImageDimensions(file) }
        }
    }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                }
                add(AvifCoderDecoder.Factory())
            }
            .build()
    }

    val imageRequest = remember(file) {
        ImageRequest.Builder(context)
            .data(file)
            .precision(Precision.EXACT)
            .build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = if (globalLoopEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }

    LaunchedEffect(globalLoopEnabled) {
        exoPlayer.repeatMode = if (globalLoopEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    LaunchedEffect(file) {
        if (isVideo(file.name)) {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && isVideo(file.name)) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        if (scale > 1.0f) {
            scale = 1.0f
            offset = Offset.Zero
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showOverlays = !showOverlays
            }
    ) {
        if (isImage(file.name)) {
            ZoomableImageView(
                file = file,
                imageRequest = imageRequest,
                imageLoader = imageLoader,
                imageDimensions = imageDimensions,
                scale = scale,
                offset = offset,
                currentIndex = currentIndex,
                siblingMedia = siblingMedia,
                onScaleChange = { scale = it },
                onOffsetChange = { offset = it },
                onToggleOverlays = { showOverlays = !showOverlays },
                onNavigateToMedia = onNavigateToMedia
            )
        } else if (isVideo(file.name)) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val screenWidthPx = with(density) { maxWidth.toPx() }
                val screenHeightPx = with(density) { maxHeight.toPx() }
                val dims = videoDimensions

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val view = LayoutInflater.from(ctx).inflate(R.layout.player_view_texture, null) as PlayerView
                            view.apply {
                                player = exoPlayer
                                useController = false
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        update = { view ->
                            view.player = exoPlayer
                            view.keepScreenOn = isPlaying
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(file, screenWidthPx) {
                            detectTapGestures(
                                onTap = { position ->
                                    if (scale == 1.0f) {
                                        if (position.x < screenWidthPx * 0.3f) {
                                            if (currentIndex > 0) {
                                                onNavigateToMedia(siblingMedia[currentIndex - 1])
                                            } else {
                                                showOverlays = !showOverlays
                                            }
                                        } else if (position.x > screenWidthPx * 0.7f) {
                                            if (currentIndex < siblingMedia.size - 1) {
                                                onNavigateToMedia(siblingMedia[currentIndex + 1])
                                            } else {
                                                showOverlays = !showOverlays
                                            }
                                        } else {
                                            showOverlays = !showOverlays
                                        }
                                    }
                                },
                                onDoubleTap = { position ->
                                    if (scale == 1.0f) {
                                        val isCenter = position.x in (screenWidthPx * 0.3f)..(screenWidthPx * 0.7f)
                                        if (isCenter) {
                                            scale = 2.0f
                                        }
                                    } else {
                                        scale = 1.0f
                                        offset = Offset.Zero
                                    }
                                }
                            )
                        }
                        .pointerInput(file, dims, screenWidthPx, screenHeightPx) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1.0f, 5.0f)
                                val newOffset = if (newScale > 1.0f) {
                                    offset + pan
                                } else {
                                    Offset.Zero
                                }
                                scale = newScale
                                offset = clampOffset(newOffset, newScale, dims, screenWidthPx, screenHeightPx)
                            }
                        }
                )
            }
        }

        // Top Details Header Overlay
        AnimatedVisibility(
            visible = showOverlays,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            MediaPlayerTopBar(
                file = file,
                onClose = onClose,
                onCut = {
                    val added = AppClipboard.addItem(file, ClipboardOpType.CUT)
                    if (!added) showCapacityLimitDialog = true
                },
                onCopy = {
                    val added = AppClipboard.addItem(file, ClipboardOpType.COPY)
                    if (!added) showCapacityLimitDialog = true
                },
                onOpenClipboard = { showClipboardDialog = true },
                onRename = { showRenameDialog = true },
                onShowProperties = { showPropertiesDialog = true },
                onDelete = { showDeleteDialog = true },
                onSettingsClick = onSettingsClick
            )
        }

        // Bottom Video Control Footer Overlay
        if (isVideo(file.name)) {
            AnimatedVisibility(
                visible = showOverlays,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MediaPlayerBottomControls(
                    currentPosition = currentPosition,
                    duration = duration,
                    isPlaying = isPlaying,
                    isMuted = isMuted,
                    globalLoopEnabled = globalLoopEnabled,
                    onSeek = { currentPosition = it },
                    onSeekFinished = { exoPlayer.seekTo(currentPosition) },
                    onToggleMute = {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else 1f
                    },
                    onTogglePlayPause = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    onFastForward = {
                        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                        exoPlayer.seekTo(newPos)
                        currentPosition = newPos
                    },
                    onToggleGlobalLoop = { onToggleGlobalLoop(!globalLoopEnabled) }
                )
            }
        }
    }

    MediaPlayerDialogs(
        file = file,
        showRenameDialog = showRenameDialog,
        onConfirmRename = { newName ->
            showRenameDialog = false
            val parent = file.parentFile ?: File("/")
            val targetFile = File(parent, newName)
            coroutineScope.launch {
                val renamed = withContext(Dispatchers.IO) {
                    file.renameTo(targetFile)
                }
                if (renamed) {
                    val auditLogHelper = AuditLogDatabaseHelper.getInstance(context)
                    val itemDetail = AuditLogItemDetail(
                        sourcePath = file.absolutePath,
                        destinationPath = targetFile.absolutePath,
                        command = "RENAME",
                        outcome = AuditItemOutcome.RENAMED,
                        fileName = targetFile.name,
                        fileSize = targetFile.length(),
                        isDirectory = false
                    )
                    auditLogHelper.insertLog(
                        AuditLogEntry(
                            timestamp = System.currentTimeMillis(),
                            actionType = AuditActionType.RENAME,
                            summary = "Renamed ${file.name} to ${targetFile.name}",
                            destinationDirectory = parent.name.ifEmpty { "/" },
                            totalItems = 1,
                            items = listOf(itemDetail)
                        )
                    )
                    onRenameMedia?.invoke(file, targetFile)
                    onNavigateToMedia(targetFile)
                }
            }
        },
        onDismissRename = { showRenameDialog = false },
        showPropertiesDialog = showPropertiesDialog,
        onDismissProperties = { showPropertiesDialog = false },
        showClipboardDialog = showClipboardDialog,
        onDismissClipboard = { showClipboardDialog = false },
        showCapacityLimitDialog = showCapacityLimitDialog,
        onDismissCapacityLimit = { showCapacityLimitDialog = false },
        showDeleteDialog = showDeleteDialog,
        onConfirmDelete = {
            showDeleteDialog = false
            val currentMediaList = playlist ?: getSiblingMedia(file)
            val currIdx = currentMediaList.indexOfFirst { it.absolutePath == file.absolutePath }
            val remaining = currentMediaList.filter { it.absolutePath != file.absolutePath }
            val nextTarget = when {
                remaining.isEmpty() -> null
                currIdx in remaining.indices -> remaining[currIdx]
                else -> remaining.last()
            }

            coroutineScope.launch {
                val trashed = RecycleBinDatabaseHelper.getInstance(context).moveToRecycleBin(listOf(file), context)
                if (trashed.isNotEmpty()) {
                    val auditLogHelper = AuditLogDatabaseHelper.getInstance(context)
                    val itemDetail = AuditLogItemDetail(
                        sourcePath = file.absolutePath,
                        destinationPath = trashed.first().trashedPath,
                        command = "DELETE",
                        outcome = AuditItemOutcome.TRASHED,
                        fileName = file.name,
                        fileSize = trashed.first().fileSize,
                        isDirectory = false
                    )
                    auditLogHelper.insertLog(
                        AuditLogEntry(
                            timestamp = System.currentTimeMillis(),
                            actionType = AuditActionType.DELETE,
                            summary = "Deleted ${file.name} to Recycle Bin",
                            destinationDirectory = "Recycle Bin",
                            totalItems = 1,
                            items = listOf(itemDetail)
                        )
                    )
                }
                onDeleteMedia?.invoke(file)
                if (nextTarget == null) {
                    onClose()
                } else {
                    scale = 1.0f
                    offset = Offset.Zero
                    onNavigateToMedia(nextTarget)
                }
            }
        },
        onDismissDelete = { showDeleteDialog = false }
    )
}
