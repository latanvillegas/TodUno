package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CelestialDarkColorScheme = darkColorScheme(
    primary = LunarGold,
    onPrimary = OnLunarGold,
    primaryContainer = LunarGoldContainer,
    onPrimaryContainer = LunarGoldBright,
    secondary = CelestialCyan,
    onSecondary = OnCelestialCyan,
    secondaryContainer = CelestialCyanContainer,
    onSecondaryContainer = CelestialCyan,
    tertiary = AuroraMint,
    onTertiary = MidnightCosmos,
    background = MidnightCosmos,
    onBackground = StarlightWhite,
    surface = DeepObservatory,
    onSurface = StarlightWhite,
    surfaceVariant = ElevatedAstrolabe,
    onSurfaceVariant = MoonstoneSilver,
    outline = SoftNebula,
    outlineVariant = MutedSlate,
    error = EclipseCoral
)

val SeleniaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CelestialDarkColorScheme,
        typography = Typography,
        shapes = SeleniaShapes,
        content = content
    )
}
