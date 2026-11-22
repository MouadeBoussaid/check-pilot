package com.mebiad.checkpilot.ui.generic.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mebiad.checkpilot.R

private val TextWeights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

// Both fonts are bundled variable fonts: the app works offline, so downloadable fonts aren't an option.
private fun variableFontFamily(
    @FontRes resId: Int,
): FontFamily =
    FontFamily(
        TextWeights.map { weight ->
            Font(
                resId = resId,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
            )
        }
    )

private val AtkinsonHyperlegibleNext = variableFontFamily(R.font.atkinson_hyperlegible_next)
private val AtkinsonHyperlegibleMono = variableFontFamily(R.font.atkinson_hyperlegible_mono)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    family: FontFamily = AtkinsonHyperlegibleNext,
    letterSpacing: TextUnit = 0.sp,
): TextStyle =
    TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing
    )

internal val CheckPilotTypography =
    Typography(
        displayLarge = style(size = 57, lineHeight = 64, weight = FontWeight.Normal),
        displayMedium = style(size = 45, lineHeight = 52, weight = FontWeight.Normal),
        displaySmall = style(size = 36, lineHeight = 44, weight = FontWeight.Normal),
        headlineLarge = style(size = 30, lineHeight = 36, weight = FontWeight.SemiBold),
        headlineMedium = style(size = 26, lineHeight = 32, weight = FontWeight.Medium),
        headlineSmall = style(size = 22, lineHeight = 28, weight = FontWeight.SemiBold),
        titleLarge = style(size = 20, lineHeight = 26, weight = FontWeight.SemiBold),
        titleMedium = style(size = 18, lineHeight = 24, weight = FontWeight.SemiBold),
        titleSmall = style(size = 16, lineHeight = 22, weight = FontWeight.SemiBold),
        bodyLarge = style(size = 17, lineHeight = 24, weight = FontWeight.Normal),
        bodyMedium = style(size = 15, lineHeight = 21, weight = FontWeight.Normal),
        bodySmall = style(size = 13, lineHeight = 18, weight = FontWeight.Normal),
        labelLarge = style(size = 18, lineHeight = 24, weight = FontWeight.SemiBold),
        labelMedium = style(size = 14, lineHeight = 20, weight = FontWeight.Medium),
        labelSmall = style(size = 12, lineHeight = 16, weight = FontWeight.Medium)
    )

/** Monospaced styles for Staff Numbers, airport codes and routes. */
@Immutable
data class MonoTypography(
    val large: TextStyle,
    val medium: TextStyle,
    val small: TextStyle,
    val extraSmall: TextStyle,
)

internal val CheckPilotMonoTypography =
    MonoTypography(
        large = monoStyle(size = 34, lineHeight = 40),
        medium = monoStyle(size = 26, lineHeight = 32),
        small = monoStyle(size = 22, lineHeight = 28),
        extraSmall = monoStyle(size = 20, lineHeight = 26)
    )

private fun monoStyle(
    size: Int,
    lineHeight: Int,
): TextStyle =
    style(
        size = size,
        lineHeight = lineHeight,
        weight = FontWeight.SemiBold,
        family = AtkinsonHyperlegibleMono,
        letterSpacing = 1.sp
    )
