package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val ClimaDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = OpaqueCardColor,
    secondary = LightShapeViolet,
    onSecondary = OpaqueCardColor,
    tertiary = SunAmber,
    background = StormMid,
    onBackground = TextPrimary,
    surface = OpaqueCardColor,
    onSurface = TextPrimary,
    surfaceVariant = OpaqueCardColor,
    onSurfaceVariant = TextSecondary,
)

private val ClimaShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun ClimaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClimaDarkColorScheme,
        typography = ClimaTypography,
        shapes = ClimaShapes,
        content = content,
    )
}
