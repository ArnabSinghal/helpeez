package com.example.helpeez.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BlueTertiary,
    secondary = BlueSecondary,
    tertiary = BluePrimary,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onPrimary = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    tertiary = BlueTertiary,
    background = LightBackground,
    surface = LightSurface,
    primaryContainer = LightPrimaryContainer,
    onPrimary = TextMain,
    onSecondary = TextMain,
    onTertiary = TextMain,
    onBackground = TextMain,
    onSurface = TextMain
)

@Composable
fun HelpeezTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set default dynamicColor to false to force our custom light-blue theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Force LightColorScheme regardless of darkTheme to ensure the theme is always lightish blue & white
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
