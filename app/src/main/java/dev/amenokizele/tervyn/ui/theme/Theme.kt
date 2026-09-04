package dev.amenokizele.tervyn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import dev.amenokizele.tervyn.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = TervynPrimaryLight,
    onPrimary = TervynSurfaceLight,
    primaryContainer = TervynSurfaceSecondaryLight,
    onPrimaryContainer = TervynPrimaryDarkLight,
    secondary = TervynPrimaryDarkLight,
    onSecondary = TervynSurfaceLight,
    secondaryContainer = TervynSurfaceSecondaryLight,
    onSecondaryContainer = TervynTextPrimaryLight,
    tertiary = TervynInfoLight,
    onTertiary = TervynSurfaceLight,
    background = TervynBackgroundLight,
    onBackground = TervynTextPrimaryLight,
    surface = TervynSurfaceLight,
    onSurface = TervynTextPrimaryLight,
    surfaceVariant = TervynSurfaceSecondaryLight,
    onSurfaceVariant = TervynTextSecondaryLight,
    outline = TervynOutlineLight,
    outlineVariant = TervynOutlineLight,
    error = TervynErrorLight,
    onError = TervynSurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = TervynPrimaryDark,
    onPrimary = TervynBackgroundDark,
    primaryContainer = TervynSurfaceSecondaryDark,
    onPrimaryContainer = TervynPrimaryDark,
    secondary = TervynPrimaryDark,
    onSecondary = TervynBackgroundDark,
    secondaryContainer = TervynSurfaceSecondaryDark,
    onSecondaryContainer = TervynTextPrimaryDark,
    tertiary = TervynInfoDark,
    onTertiary = TervynBackgroundDark,
    background = TervynBackgroundDark,
    onBackground = TervynTextPrimaryDark,
    surface = TervynSurfaceDark,
    onSurface = TervynTextPrimaryDark,
    surfaceVariant = TervynSurfaceSecondaryDark,
    onSurfaceVariant = TervynTextSecondaryDark,
    outline = TervynOutlineDark,
    outlineVariant = TervynOutlineDark,
    error = TervynErrorDark,
    onError = TervynBackgroundDark
)

@Composable
fun TervynTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TervynTypography,
        shapes = TervynShapes,
        content = content
    )
}
