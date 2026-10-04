package com.ascend.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AscendColorScheme = darkColorScheme(
    primary = AscendColors.Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E1F66),
    onPrimaryContainer = AscendColors.VioletLight,
    secondary = AscendColors.Cyan,
    onSecondary = Color(0xFF00222A),
    tertiary = AscendColors.Gold,
    onTertiary = Color(0xFF2A1C00),
    background = AscendColors.Void,
    onBackground = AscendColors.TextPrimary,
    surface = AscendColors.Surface,
    onSurface = AscendColors.TextPrimary,
    surfaceVariant = AscendColors.SurfaceHigh,
    onSurfaceVariant = AscendColors.TextSecondary,
    surfaceContainerLowest = AscendColors.Void,
    surfaceContainerLow = AscendColors.Night,
    surfaceContainer = AscendColors.Surface,
    surfaceContainerHigh = AscendColors.SurfaceHigh,
    surfaceContainerHighest = AscendColors.SurfaceTop,
    outline = AscendColors.StrokeStrong,
    outlineVariant = AscendColors.Stroke,
    error = AscendColors.Danger,
    onError = Color.White,
)

private val AscendShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AscendTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AscendColorScheme,
        typography = AscendTypography,
        shapes = AscendShapes,
        content = content,
    )
}
