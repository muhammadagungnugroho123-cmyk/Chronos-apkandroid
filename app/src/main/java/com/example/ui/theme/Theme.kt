package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChronosDarkColorScheme = darkColorScheme(
    primary = ChronosGold,
    onPrimary = Color(0xFF090D16),
    primaryContainer = Color(0xFF3B2806),
    onPrimaryContainer = ChronosGoldLight,
    secondary = ChronosCyan,
    onSecondary = Color(0xFF090D16),
    secondaryContainer = Color(0xFF0C2D48),
    onSecondaryContainer = ChronosCyan,
    tertiary = ChronosIndigo,
    onTertiary = Color(0xFF090D16),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianBorder
)

private val ChronosLightColorScheme = lightColorScheme(
    primary = ChronosAmberDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = ChronosBlueDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = Color(0xFF4F46E5),
    onTertiary = Color.White,
    background = AlabasterBg,
    onBackground = TextPrimaryLight,
    surface = AlabasterSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AlabasterCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = AlabasterBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ChronosDarkColorScheme else ChronosLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
