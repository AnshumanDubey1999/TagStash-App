package com.anshuman.tagstash.ui.screens

import androidx.compose.ui.geometry.Offset
import com.anshuman.tagstash.data.utils.ImageDimensions
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPlayerScreenTest {

    @Test
    fun testClampOffsetZeroWhenNotZoomedOrNullDims() {
        val screenWidth = 1080f
        val screenHeight = 1920f
        val dims = ImageDimensions(800, 600)

        // Scale <= 1.0f -> should return Offset.Zero
        assertEquals(
            Offset.Zero,
            clampOffset(Offset(50f, 50f), 1.0f, dims, screenWidth, screenHeight)
        )
        assertEquals(
            Offset.Zero,
            clampOffset(Offset(50f, 50f), 0.5f, dims, screenWidth, screenHeight)
        )

        // Null dimensions -> should return Offset.Zero
        assertEquals(
            Offset.Zero,
            clampOffset(Offset(50f, 50f), 2.0f, null, screenWidth, screenHeight)
        )
    }

    @Test
    fun testClampOffsetWideImageZoomed() {
        val screenWidth = 1080f
        val screenHeight = 1920f
        
        // Image ratio (2.0) is wider than screen ratio (0.5625)
        // Scaled to fit screen width: width = 1080, height = 540
        val dims = ImageDimensions(2000, 1000)

        // Zoom 2x: scaled width = 2160, scaled height = 1080
        // Horizontal overflow: 2160 - 1080 = 1080 -> maxX = 540f
        // Vertical overflow: 1080 <= 1920 -> maxY = 0f
        val scale = 2.0f

        // Within boundaries
        assertEquals(
            Offset(200f, 0f),
            clampOffset(Offset(200f, 10f), scale, dims, screenWidth, screenHeight)
        )

        // Beyond horizontal boundaries
        assertEquals(
            Offset(540f, 0f),
            clampOffset(Offset(600f, 0f), scale, dims, screenWidth, screenHeight)
        )
        assertEquals(
            Offset(-540f, 0f),
            clampOffset(Offset(-800f, 0f), scale, dims, screenWidth, screenHeight)
        )

        // Vertical panning should be locked to 0
        assertEquals(
            Offset(0f, 0f),
            clampOffset(Offset(0f, 100f), scale, dims, screenWidth, screenHeight)
        )
    }

    @Test
    fun testClampOffsetTallImageZoomed() {
        val screenWidth = 1080f
        val screenHeight = 1920f
        
        // Image ratio (0.5) is taller than screen ratio (0.5625)
        // Scaled to fit screen height: height = 1920, width = 960
        val dims = ImageDimensions(1000, 2000)

        // Zoom 2x: scaled width = 1920, scaled height = 3840
        // Horizontal overflow: 1920 - 1080 = 840 -> maxX = 420f
        // Vertical overflow: 3840 - 1920 = 1920 -> maxY = 960f
        val scale = 2.0f

        // Within boundaries
        assertEquals(
            Offset(100f, 500f),
            clampOffset(Offset(100f, 500f), scale, dims, screenWidth, screenHeight)
        )

        // Beyond horizontal boundaries
        assertEquals(
            Offset(420f, 100f),
            clampOffset(Offset(500f, 100f), scale, dims, screenWidth, screenHeight)
        )
        assertEquals(
            Offset(-420f, 100f),
            clampOffset(Offset(-500f, 100f), scale, dims, screenWidth, screenHeight)
        )

        // Beyond vertical boundaries
        assertEquals(
            Offset(0f, 960f),
            clampOffset(Offset(0f, 1200f), scale, dims, screenWidth, screenHeight)
        )
        assertEquals(
            Offset(0f, -960f),
            clampOffset(Offset(0f, -1000f), scale, dims, screenWidth, screenHeight)
        )
    }
}
