package com.studentregistry.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    background = WhiteBackground
)

private val DarkColors = darkColorScheme(
    primary = BlueSecondary,
    secondary = BluePrimary,
    background = DarkBlueBackground
)

@Composable
fun StudentRegistryTheme(
    darkTheme: Boolean,
    fontScale: Float,
    content: @Composable () -> Unit
) {
    // Theme is fully controlled by the user's explicit Settings toggle.
    // We intentionally ignore the system/device dark mode so the app
    // always starts in Light theme by default and only switches when the
    // user changes it from Settings, applying consistently across every screen.
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val density = LocalDensity.current
    val scaledDensity = remember(density.density, fontScale) {
        Density(density = density.density, fontScale = fontScale.coerceIn(0.85f, 1.3f))
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
