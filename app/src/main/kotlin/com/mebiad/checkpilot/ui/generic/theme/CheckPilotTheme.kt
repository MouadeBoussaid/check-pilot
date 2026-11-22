package com.mebiad.checkpilot.ui.generic.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalCheckPilotColors = staticCompositionLocalOf { CheckPilotExtraColors }
private val LocalMonoTypography = staticCompositionLocalOf { CheckPilotMonoTypography }

// Dark only, whatever the system setting: the capture screen is used in a dim, vibrating cockpit (functional spec),
// and the Variant A designs keep every screen dark.
@Composable
fun CheckPilotTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCheckPilotColors provides CheckPilotExtraColors,
        LocalMonoTypography provides CheckPilotMonoTypography
    ) {
        MaterialTheme(
            colorScheme = CheckPilotColorScheme,
            typography = CheckPilotTypography,
            shapes = CheckPilotShapes,
            content = content
        )
    }
}

/** The design tokens [MaterialTheme] has no slot for. */
object CheckPilotTheme {
    val colors: CheckPilotColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCheckPilotColors.current

    val monoTypography: MonoTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalMonoTypography.current
}
