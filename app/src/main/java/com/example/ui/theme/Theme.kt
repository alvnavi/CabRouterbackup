package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkMonochromeScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = PureWhite,
    secondary = Color(0xFFE5E5E5),
    onSecondary = PureBlack,
    tertiary = PureWhite,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkBorder,
    error = DangerRedMonochrome,
    onError = PureWhite
)

private val LightMonochromeScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = PureBlack,
    secondary = Color(0xFF262626),
    onSecondary = PureWhite,
    tertiary = PureBlack,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = PureBlack,
    surface = LightSurface,
    onSurface = PureBlack,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorder,
    error = DangerRedMonochrome,
    onError = PureWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkMonochromeScheme else LightMonochromeScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
