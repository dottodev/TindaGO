package com.tindahan.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand: deep store-green with warm amber accents. Dark-first.

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BC47F),
    onPrimary = Color(0xFF0C381C),
    primaryContainer = Color(0xFF1E5B2A),
    onPrimaryContainer = Color(0xFFC9E9CD),
    secondary = Color(0xFFA9C7A4),
    onSecondary = Color(0xFF17351F),
    secondaryContainer = Color(0xFF334E32),
    onSecondaryContainer = Color(0xFFD4E8D0),
    tertiary = Color(0xFFE5A93D),
    onTertiary = Color(0xFF3A2700),
    tertiaryContainer = Color(0xFF5C3F00),
    onTertiaryContainer = Color(0xFFFFDF9E),
    background = Color(0xFF0F130F),
    onBackground = Color(0xFFE2E5DF),
    surface = Color(0xFF161B16),
    onSurface = Color(0xFFE2E5DF),
    surfaceVariant = Color(0xFF232B23),
    onSurfaceVariant = Color(0xFFBFC9BD),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF680003),
    errorContainer = Color(0xFF93000E),
    onErrorContainer = Color(0xFFFFDAD4)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB3F0B8),
    onPrimaryContainer = Color(0xFF002105),
    secondary = Color(0xFF51634F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E8D2),
    onSecondaryContainer = Color(0xFF0F1F12),
    tertiary = Color(0xFF744B00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF251A00),
    background = Color(0xFFF4F7F1),
    onBackground = Color(0xFF191C18),
    surface = Color(0xFFF4F7F1),
    onSurface = Color(0xFF191C18),
    surfaceVariant = Color(0xFFDFE4DB),
    onSurfaceVariant = Color(0xFF424940),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun TindahanTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
