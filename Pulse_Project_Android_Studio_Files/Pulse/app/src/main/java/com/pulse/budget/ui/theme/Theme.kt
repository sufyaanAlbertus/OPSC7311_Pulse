package com.pulse.budget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalPulseColors = staticCompositionLocalOf { DarkPulseColors }

private val DarkScheme = darkColorScheme(
    primary = DarkAccentBlue,
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = ErrorRed
)

private val LightScheme = lightColorScheme(
    primary = LightAccentBlue,
    onPrimary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = ErrorRed
)

/**
 * darkTheme defaults to the system setting but is expected to be driven by
 * ThemeViewModel's persisted DataStore preference from the app root, so the
 * runtime toggle in each screen's top bar overrides it instantly app-wide.
 */
@Composable
fun PulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val pulseColors = if (darkTheme) DarkPulseColors else LightPulseColors

    CompositionLocalProvider(LocalPulseColors provides pulseColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PulseTypography,
            shapes = PulseShapes,
            content = content
        )
    }
}