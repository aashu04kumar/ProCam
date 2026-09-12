package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = Color.Black,
    primaryContainer = CameraSurfaceElevated,
    onPrimaryContainer = AmberGoldLight,
    secondary = LeicaRed,
    onSecondary = Color.White,
    secondaryContainer = CameraSurfaceElevated,
    onSecondaryContainer = Color.White,
    tertiary = FrostBlue,
    onTertiary = Color.White,
    background = CameraBackground,
    onBackground = CameraTextPrimary,
    surface = CameraSurface,
    onSurface = CameraTextPrimary,
    surfaceVariant = CameraSurfaceElevated,
    onSurfaceVariant = CameraTextSecondary,
    outline = CameraSurfaceBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

