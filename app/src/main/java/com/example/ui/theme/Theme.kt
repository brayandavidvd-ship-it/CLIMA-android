package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ClimaColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = SkyClearNight,
    secondary = SunYellow,
    onSecondary = SkyClearNight,
    background = SettingsSurfaceDark,
    onBackground = PureWhite,
    surface = SettingsCardDark,
    onSurface = PureWhite,
    surfaceVariant = SettingsCardDark,
    onSurfaceVariant = PureWhite.copy(alpha = 0.78f),
    outline = GlassCardBorder,
)

@Composable
fun ClimaTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ClimaColorScheme,
        typography = ClimaTypography,
        content = content,
    )
}
