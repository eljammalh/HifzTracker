package com.hifz.tracker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F5C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDE8DF),
    onPrimaryContainer = Color(0xFF0B3B30),
    secondary = Color(0xFF9A7232),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E6CC),
    onSecondaryContainer = Color(0xFF3D2B0A),
    tertiary = Color(0xFF9C4F3A),
    tertiaryContainer = Color(0xFFF8DDD4),
    onTertiaryContainer = Color(0xFF3E140A),
    background = Color(0xFFFAF7F0),
    onBackground = Color(0xFF1C1B17),
    surface = Color(0xFFFAF7F0),
    onSurface = Color(0xFF1C1B17),
    surfaceVariant = Color(0xFFEDE8DC),
    onSurfaceVariant = Color(0xFF55524A),
    surfaceContainer = Color(0xFFF3EFE5),
    surfaceContainerHigh = Color(0xFFEDE8DC),
    outline = Color(0xFF8A867B),
    outlineVariant = Color(0xFFD6D0C2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ED3BE),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF175446),
    onPrimaryContainer = Color(0xFFCDE8DF),
    secondary = Color(0xFFE3C189),
    onSecondary = Color(0xFF3D2B0A),
    secondaryContainer = Color(0xFF574119),
    onSecondaryContainer = Color(0xFFF3E6CC),
    tertiary = Color(0xFFF0B4A2),
    tertiaryContainer = Color(0xFF6E3324),
    onTertiaryContainer = Color(0xFFF8DDD4),
    background = Color(0xFF121614),
    onBackground = Color(0xFFE4E2DC),
    surface = Color(0xFF121614),
    onSurface = Color(0xFFE4E2DC),
    surfaceVariant = Color(0xFF2A322E),
    onSurfaceVariant = Color(0xFFBEC6C1),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252C28),
    outline = Color(0xFF89928D),
    outlineVariant = Color(0xFF3F4844),
)

@Composable
fun HifzTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
