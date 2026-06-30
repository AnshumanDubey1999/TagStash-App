package com.anshuman.tagstash.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.compose.SubcomposeAsyncImage
import com.anshuman.tagstash.data.utils.ImageDimensions
import com.anshuman.tagstash.data.utils.formatFileSize
import com.anshuman.tagstash.data.utils.formatLastModified
import com.anshuman.tagstash.data.utils.getImageDimensions
import com.anshuman.tagstash.data.utils.getSiblingImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ImageViewerScreen(
    file: File,
    onClose: () -> Unit,
    onNavigateToImage: (File) -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                }
            }
            .build()
    }

    var showHeader by remember { mutableStateOf(false) }
    var imageDimensions by remember(file) { mutableStateOf<ImageDimensions?>(null) }

    // Sibling Images and Navigation Logic
    val siblingImages = remember(file) { getSiblingImages(file) }
    val currentIndex = remember(file, siblingImages) {
        val index = siblingImages.indexOf(file)
        if (index == -1) 0 else index
    }

    // Intercept back key to close viewer
    BackHandler(onBack = onClose)

    // Load Image Dimensions
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            imageDimensions = getImageDimensions(file)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Image Container with constraints matching screen width/height
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }

            val dims = imageDimensions
            if (dims != null) {
                // Sizing Logic: Check if image resolution is smaller than the screen resolution
                val isSmaller = dims.width < screenWidthPx && dims.height < screenHeightPx

                SubcomposeAsyncImage(
                    model = file,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = if (isSmaller) ContentScale.None else ContentScale.Fit,
                    modifier = if (isSmaller) {
                        val dpWidth = with(density) { dims.width.toDp() }
                        val dpHeight = with(density) { dims.height.toDp() }
                        Modifier
                            .align(Alignment.Center)
                            .size(width = dpWidth, height = dpHeight)
                    } else {
                        Modifier.fillMaxSize()
                    },
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
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
            } else {
                // Loading bounds
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Invisible Interaction Tap Overlay: Left 30%, Center 40%, Right 30%
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Zone
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.3f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentIndex > 0) {
                            onNavigateToImage(siblingImages[currentIndex - 1])
                        } else {
                            showHeader = !showHeader
                        }
                    }
            )

            // Center Zone
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showHeader = !showHeader
                    }
            )

            // Right Zone
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.3f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentIndex < siblingImages.size - 1) {
                            onNavigateToImage(siblingImages[currentIndex + 1])
                        } else {
                            showHeader = !showHeader
                        }
                    }
            )
        }

        // Top Details Header Overlay
        AnimatedVisibility(
            visible = showHeader,
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
                            fontSize = 15.sp,
                            maxLines = 1,
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
    }
}
