package com.anshuman.tagstash.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.anshuman.tagstash.data.utils.ImageDimensions
import java.io.File

@Composable
fun ZoomableImageView(
    file: File,
    imageRequest: ImageRequest,
    imageLoader: ImageLoader,
    imageDimensions: ImageDimensions?,
    scale: Float,
    offset: Offset,
    currentIndex: Int,
    siblingMedia: List<File>,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onToggleOverlays: () -> Unit,
    onNavigateToMedia: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
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
                                        onToggleOverlays()
                                    }
                                } else if (position.x > screenWidthPx * 0.7f) {
                                    if (currentIndex < siblingMedia.size - 1) {
                                        onNavigateToMedia(siblingMedia[currentIndex + 1])
                                    } else {
                                        onToggleOverlays()
                                    }
                                } else {
                                    onToggleOverlays()
                                }
                            }
                        },
                        onDoubleTap = { position ->
                            if (scale == 1.0f) {
                                val isCenter = position.x in (screenWidthPx * 0.3f)..(screenWidthPx * 0.7f)
                                if (isCenter) {
                                    onScaleChange(2.0f)
                                }
                            } else {
                                onScaleChange(1.0f)
                                onOffsetChange(Offset.Zero)
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
                        onScaleChange(newScale)
                        onOffsetChange(clampOffset(newOffset, newScale, dims, screenWidthPx, screenHeightPx))
                    }
                }
        ) {
            SubcomposeAsyncImage(
                model = imageRequest,
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
