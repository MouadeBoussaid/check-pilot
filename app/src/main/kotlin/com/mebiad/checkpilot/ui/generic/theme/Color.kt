package com.mebiad.checkpilot.ui.generic.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF1A1712)
private val Card = Color(0xFF25211A)
private val Raised = Color(0xFF2F2A21)
private val Menu = Color(0xFF352F25)
private val SelectedTab = Color(0xFF3A3329)
private val Border = Color(0xFF4A4236)
private val BorderSubtle = Color(0xFF3E372C)

private val Text = Color(0xFFF2EADB)
private val TextSecondary = Color(0xFFB9AE98)
private val TextHint = Color(0xFF968A74)

private val Amber = Color(0xFFF2B544)
private val OnAmber = Color(0xFF231803)
private val Error = Color(0xFFFF8A80)
private val SnackbarAction = Color(0xFF8A5A00)

private val CrewMember1 = Color(0xFF8CCBFF)
private val CrewMember2 = Color(0xFFE8873F)

internal val CheckPilotColorScheme: ColorScheme =
    darkColorScheme(
        primary = Amber,
        onPrimary = OnAmber,
        primaryContainer = Amber,
        onPrimaryContainer = OnAmber,
        inversePrimary = SnackbarAction,
        secondary = TextSecondary,
        onSecondary = Background,
        secondaryContainer = SelectedTab,
        onSecondaryContainer = Text,
        tertiary = Amber,
        onTertiary = OnAmber,
        tertiaryContainer = SelectedTab,
        onTertiaryContainer = Text,
        background = Background,
        onBackground = Text,
        surface = Background,
        onSurface = Text,
        surfaceVariant = Raised,
        onSurfaceVariant = TextSecondary,
        // The surface containers carry the elevation, so no tint is laid over them.
        surfaceTint = Color.Transparent,
        inverseSurface = Text,
        inverseOnSurface = Card,
        error = Error,
        onError = OnAmber,
        outline = Border,
        outlineVariant = BorderSubtle,
        surfaceBright = Menu,
        surfaceDim = Background,
        surfaceContainerLowest = Background,
        surfaceContainerLow = Card,
        surfaceContainer = Raised,
        surfaceContainerHigh = Menu,
        surfaceContainerHighest = SelectedTab
    )

/** The design's colours that Material 3's colour scheme has no role for. */
@Immutable
data class CheckPilotColors(
    val hint: Color,
    val crewMember1: Color,
    val crewMember2: Color,
)

internal val CheckPilotExtraColors =
    CheckPilotColors(
        hint = TextHint,
        crewMember1 = CrewMember1,
        crewMember2 = CrewMember2
    )
