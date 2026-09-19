package com.studentregistry.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
    val colorScheme = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors
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
