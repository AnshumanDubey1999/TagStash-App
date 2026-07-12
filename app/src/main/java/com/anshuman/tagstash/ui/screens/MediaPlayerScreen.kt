package com.anshuman.tagstash.ui.screens

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import com.anshuman.tagstash.R
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.ImageDecoderDecoder
import com.anshuman.tagstash.data.utils.ImageDimensions
import com.anshuman.tagstash.data.utils.formatFileSize
import com.anshuman.tagstash.data.utils.formatLastModified
import com.anshuman.tagstash.data.utils.getImageDimensions
import com.anshuman.tagstash.data.utils.getSiblingMedia
import com.anshuman.tagstash.data.utils.isImage
import com.anshuman.tagstash.data.utils.isVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MediaPlayerScreen(
    file: File,
    globalLoopEnabled: Boolean,
    onToggleGlobalLoop: (Boolean) -> Unit,
    onClose: () -> Unit,
    onNavigateToMedia: (File) -> Unit
) {
    val context = LocalContext.current
    var showOverlays by rememberSaveable { mutableStateOf(false) }

    // Sibling Media and Navigation Logic
    val siblingMedia = remember(file) { getSiblingMedia(file) }
    val currentIndex = remember(file, siblingMedia) {
        val index = siblingMedia.indexOf(file)
        if (index == -1) 0 else index
    }

    // Intercept back key
    BackHandler(onBack = onClose)

    var currentPosition by rememberSaveable(file) { mutableStateOf(0L) }
    var duration by rememberSaveable(file) { mutableStateOf(0L) }
    var isPlaying by rememberSaveable(file) { mutableStateOf(true) }
    var isMuted by rememberSaveable(file) { mutableStateOf(false) }
    var scale by remember(file) { mutableStateOf(1.0f) }
    var offset by remember(file) { mutableStateOf(Offset.Zero) }
    var videoDimensions by remember(file) { mutableStateOf<ImageDimensions?>(null) }

    // Singleton ExoPlayer instance for this screen
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            volume = 1f
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Pause video when app goes to background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (isVideo(file.name)) {
                    exoPlayer.pause()
                    isPlaying = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Sync globalLoopEnabled repeatMode
    LaunchedEffect(globalLoopEnabled, exoPlayer) {
        exoPlayer.repeatMode = if (globalLoopEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Load / Stop Media on file changes
    LaunchedEffect(file, exoPlayer) {
        if (isImage(file.name)) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        } else if (isVideo(file.name)) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            exoPlayer.prepare()
            exoPlayer.seekTo(currentPosition)
            isPlaying = true
            exoPlayer.playWhenReady = true
            exoPlayer.volume = if (isMuted) 0f else 1f
        }
    }

    // Auto-navigation state completion listener
    DisposableEffect(exoPlayer, currentIndex, siblingMedia, globalLoopEnabled) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !globalLoopEnabled) {
                    if (currentIndex < siblingMedia.size - 1) {
                        onNavigateToMedia(siblingMedia[currentIndex + 1])
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }


    // Sync Compose states to Player states
    DisposableEffect(exoPlayer, file) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                if (isVideo(file.name)) {
                    isPlaying = playing
                }
            }

            override fun onVolumeChanged(volume: Float) {
                if (isVideo(file.name)) {
                    isMuted = volume == 0f
                }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (isVideo(file.name)) {
                    videoDimensions = ImageDimensions(videoSize.width, videoSize.height)
                }
            }
        }
        exoPlayer.addListener(listener)
        if (isVideo(file.name)) {
            videoDimensions = ImageDimensions(exoPlayer.videoSize.width, exoPlayer.videoSize.height)
        }
        isMuted = exoPlayer.volume == 0f
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Timeline position polling
    LaunchedEffect(exoPlayer, isPlaying, file) {
        if (isVideo(file.name)) {
            while (isPlaying) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
                delay(200)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isImage(file.name)) {
            var imageDimensions by remember(file) { mutableStateOf<ImageDimensions?>(null) }

            val imageLoader = remember(context) {
                ImageLoader.Builder(context)
                    .components {
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        }
                    }
                    .build()
            }

            LaunchedEffect(file) {
                withContext(Dispatchers.IO) {
                    imageDimensions = getImageDimensions(file)
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val screenWidthPx = with(density) { maxWidth.toPx() }
                val screenHeightPx = with(density) { maxHeight.toPx() }
                val dims = imageDimensions

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
                ) {
                    SubcomposeAsyncImage(
                        model = file,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Unable to load image",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    )
                }
            }
        } else if (isVideo(file.name)) {
            // RENDER VIDEO VIEW (ExoPlayer)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val screenWidthPx = with(density) { maxWidth.toPx() }
                val screenHeightPx = with(density) { maxHeight.toPx() }
                val dims = videoDimensions

                // Scaled video container
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

                // Transparent Gesture Overlay Box
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.75f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatFileSize(file.length())} • ${formatLastModified(file.lastModified())}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Bottom Video Control Footer Overlay (only visible for videos, drawn on top of the tap zones)
        if (isVideo(file.name)) {
            AnimatedVisibility(
                visible = showOverlays,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.75f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Timeline Slider Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = Color.White,
                                fontSize = 12.sp
                            )

                            Slider(
                                value = currentPosition.toFloat(),
                                onValueChange = {
                                    currentPosition = it.toLong()
                                },
                                onValueChangeFinished = {
                                    exoPlayer.seekTo(currentPosition)
                                },
                                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )

                            Text(
                                text = formatTime(duration),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute Button
                            IconButton(onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            }) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = Color.White
                                )
                            }

                            // Play / Pause Button
                            IconButton(
                                onClick = {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Fast Forward (+10s) Button
                            IconButton(onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Fast Forward 10s",
                                    tint = Color.White
                                )
                            }

                            // Global Loop Toggle Button
                            IconButton(onClick = { onToggleGlobalLoop(!globalLoopEnabled) }) {
                                Icon(
                                    imageVector = Icons.Default.Loop,
                                    contentDescription = "Loop",
                                    tint = if (globalLoopEnabled) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60
    return String.format("%02d:%02d", minutes, seconds)
}

internal fun clampOffset(
    offset: Offset,
    scale: Float,
    dims: ImageDimensions?,
    screenWidth: Float,
    screenHeight: Float
): Offset {
    if (scale <= 1.0f || dims == null) return Offset.Zero

    val imageWidth = dims.width.toFloat()
    val imageHeight = dims.height.toFloat()
    if (imageWidth <= 0f || imageHeight <= 0f) return Offset.Zero

    val imageRatio = imageWidth / imageHeight
    val screenRatio = screenWidth / screenHeight

    val (dispWidth, dispHeight) = if (imageRatio > screenRatio) {
        screenWidth to (screenWidth / imageRatio)
    } else {
        (screenHeight * imageRatio) to screenHeight
    }

    val scaledWidth = dispWidth * scale
    val scaledHeight = dispHeight * scale

    val maxX = if (scaledWidth > screenWidth) (scaledWidth - screenWidth) / 2f else 0f
    val maxY = if (scaledHeight > screenHeight) (scaledHeight - screenHeight) / 2f else 0f

    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}
