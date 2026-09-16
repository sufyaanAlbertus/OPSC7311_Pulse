package com.pulse.budget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pulse.budget.R

// Labels, headings, body copy
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

// All numeric readouts: amounts, dates, percentages, XP
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium)
)

/** Use for every screen title / section header. Uppercase the string yourself when calling. */
val HeaderStyle = TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    letterSpacing = 0.12.em
)

/** Use for every button label. Uppercase the string yourself when calling. */
val ButtonLabelStyle = TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    letterSpacing = 0.1.em
)

/** Large readout figure, e.g. remaining budget on Dashboard. */
val MonoDisplay = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Medium,
    fontSize = 32.sp
)

/** Standard inline numeric value: list amounts, dates, limits. */
val MonoBody = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp
)

val MonoSmall = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp
)

val PulseTypography = Typography(
    headlineMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = HeaderStyle,
    bodyLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge = ButtonLabelStyle,
    labelSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 11.sp, textAlign = TextAlign.Center)
)
