package com.pulse.budget.ui.theme

import androidx.compose.ui.graphics.Color

// Shared accents
val ErrorRed = Color(0xFFB33B58)

// Dark theme
val DarkBackground = Color(0xFF060809)
val DarkSurface = Color(0xFF0A0F14)
val DarkBorder = Color(0xFF1B3A54)
val DarkTextPrimary = Color(0xFFEAF2F8)
val DarkTextSecondary = Color(0xFF9DB3C6)
val DarkTextDim = Color(0xFF3D5468)
val DarkAccentBlue = Color(0xFF2FB6F0)
val DarkAccentViolet = Color(0xFFC13FA8)

// Light theme
val LightBackground = Color(0xFFF5F6F2)
val LightSurface = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFDDE1DC)
val LightTextPrimary = Color(0xFF15222C)
val LightTextSecondary = Color(0xFF4E5860)
val LightTextDim = Color(0xFF9AA2A8)
val LightAccentBlue = Color(0xFF1F3F63)
val LightAccentViolet = Color(0xFF9B3F86)

/** Extra semantic colors not covered by Material3's ColorScheme. */
data class PulseColors(
    val border: Color,
    val textDim: Color,
    val accentViolet: Color,
    val overLimit: Color,
    val onSurfaceMuted: Color
)

val DarkPulseColors = PulseColors(
    border = DarkBorder,
    textDim = DarkTextDim,
    accentViolet = DarkAccentViolet,
    overLimit = ErrorRed,
    onSurfaceMuted = DarkTextSecondary
)

val LightPulseColors = PulseColors(
    border = LightBorder,
    textDim = LightTextDim,
    accentViolet = LightAccentViolet,
    overLimit = ErrorRed,
    onSurfaceMuted = LightTextSecondary
)
// Goal-status colours used by the category graph and the 30-day goal tracker.
val OnTrackGreen = Color(0xFF2EBD85)
val GoalMinAmber = Color(0xFFE3A008)
