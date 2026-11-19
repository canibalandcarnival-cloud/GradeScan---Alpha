package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val GradeScanDarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = AmberGoldOnDark,
    primaryContainer = AmberGoldMuted,
    onPrimaryContainer = AmberGoldBright,
    secondary = CyanTeal,
    onSecondary = StudioBackground,
    secondaryContainer = CyanTealMuted,
    onSecondaryContainer = CyanTeal,
    tertiary = AmberGoldBright,
    onTertiary = AmberGoldOnDark,
    background = StudioBackground,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = StudioBackground,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighest,
    surfaceContainerHighest = StudioSurfaceHighest,
    outline = StudioBorder,
    outlineVariant = StudioBorderSubtle,
    error = ErrorCoral,
    onError = StudioBackground,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorCoral
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GradeScanTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GradeScanDarkColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
