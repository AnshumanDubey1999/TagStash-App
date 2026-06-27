package com.anshuman.tagstash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
