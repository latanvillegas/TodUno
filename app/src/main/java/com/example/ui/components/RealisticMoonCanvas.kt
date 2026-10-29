package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders a realistic astronomical Moon disk using Canvas.
 * [phaseFraction] ranges from 0.0 (New Moon) -> 0.25 (First Quarter) -> 0.5 (Full Moon) -> 0.75 (Last Quarter) -> 1.0.
 * [isNorthernHemisphere] controls whether waxing illumination grows from the right (North) or left (South).
 */
@Composable
fun RealisticMoonCanvas(
    phaseFraction: Double,
    modifier: Modifier = Modifier,
    isNorthernHemisphere: Boolean = true,
    showAmbientGlow: Boolean = true,
    litColor: Color = Color(0xFFF7E7B4),
    litHighlightColor: Color = Color(0xFFFFF8E1),
    shadowColor: Color = Color(0xFF101733)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val minDim = size.minDimension
        if (minDim <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = if (showAmbientGlow) minDim * 0.37f else minDim * 0.45f

        // 1. Optional atmospheric starlight & lunar corona glow
        if (showAmbientGlow) {
            val phaseAngle = phaseFraction * 2.0 * PI
            val illuminationRatio = ((1.0 - cos(phaseAngle)) / 2.0).toFloat()
            val glowAlpha = 0.14f + 0.28f * illuminationRatio
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        litColor.copy(alpha = glowAlpha),
                        Color(0xFF64D2FF).copy(alpha = glowAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.45f
                ),
                radius = radius * 1.45f,
                center = center
            )

            // Subtle celestial coordinate ring around the moon
            drawCircle(
                color = Color(0xFFF5C451).copy(alpha = 0.22f),
                radius = radius * 1.18f,
                center = center,
                style = Stroke(width = 1.2f)
            )
        }

        // 2. Base dark lunar sphere (earthshine / night side)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    shadowColor.lighter(0.08f),
                    shadowColor
                ),
                center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                radius = radius * 1.2f
            ),
            radius = radius,
            center = center
        )

        // Subtle maria on dark side (earthshine effect)
        drawLunarMaria(center, radius, alpha = 0.08f)

        // 3. Illuminated spherical portion bounded by limb and elliptical terminator
        val litPath = buildIlluminatedMoonPath(
            center = center,
            radius = radius,
            phaseFraction = phaseFraction,
            isNorthernHemisphere = isNorthernHemisphere
        )

        clipPath(litPath) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        litHighlightColor,
                        litColor,
                        Color(0xFFDDC588)
                    ),
                    center = Offset(center.x - radius * 0.15f, center.y - radius * 0.18f),
                    radius = radius * 1.15f
                ),
                radius = radius,
                center = center
            )
            // Realistic lunar maria & craters on the illuminated face
            drawLunarMaria(center, radius, alpha = 0.22f)
        }

        // 4. Outer subtle rim light
        drawCircle(
            color = litColor.copy(alpha = 0.35f),
            radius = radius,
            center = center,
            style = Stroke(width = (minDim * 0.015f).coerceAtLeast(1f))
        )
    }
}

private fun DrawScope.drawLunarMaria(center: Offset, radius: Float, alpha: Float) {
    val mareColor = Color(0xFF6B655E).copy(alpha = alpha)
    // Oceanus Procellarum & Mare Imbrium (upper left)
    drawCircle(
        color = mareColor,
        radius = radius * 0.28f,
        center = Offset(center.x - radius * 0.28f, center.y - radius * 0.18f)
    )
    // Mare Serenitatis & Mare Tranquillitatis (center right)
    drawCircle(
        color = mareColor,
        radius = radius * 0.22f,
        center = Offset(center.x + radius * 0.20f, center.y - radius * 0.12f)
    )
    drawCircle(
        color = mareColor,
        radius = radius * 0.19f,
        center = Offset(center.x + radius * 0.32f, center.y + radius * 0.08f)
    )
    // Mare Nubium (lower left-center)
    drawCircle(
        color = mareColor,
        radius = radius * 0.20f,
        center = Offset(center.x - radius * 0.15f, center.y + radius * 0.32f)
    )
    // Tycho bright crater hint (southern hemisphere)
    if (radius > 28f) {
        drawCircle(
            color = Color.White.copy(alpha = alpha * 1.1f),
            radius = radius * 0.06f,
            center = Offset(center.x - radius * 0.05f, center.y + radius * 0.56f)
        )
    }
}

private fun buildIlluminatedMoonPath(
    center: Offset,
    radius: Float,
    phaseFraction: Double,
    isNorthernHemisphere: Boolean
): Path {
    val path = Path()
    var p = phaseFraction % 1.0
    if (p < 0) p += 1.0

    // Flip waxing/waning left-right for Southern Hemisphere
    val effectiveWaxing = if (isNorthernHemisphere) (p <= 0.5) else (p > 0.5)
    // Terminator x-scale: cos(2 * pi * p) ranges from +1 (New) -> 0 (Quarter) -> -1 (Full) -> 0 (Last Q) -> +1
    val terminatorScale = cos(2.0 * PI * p).toFloat()

    val steps = 60
    // Sample from top pole (y = -radius) to bottom pole (y = +radius)
    for (i in 0..steps) {
        val t = -1f + (2f * i / steps.toFloat())
        val y = center.y + t * radius
        val halfChord = radius * sqrt((1f - t * t).coerceAtLeast(0f))
        // Limb is on the right (+halfChord) when waxing in North, or left (-halfChord) when waning
        val xLimb = if (effectiveWaxing) {
            center.x + halfChord
        } else {
            center.x - halfChord
        }
        if (i == 0) {
            path.moveTo(xLimb, y)
        } else {
            path.lineTo(xLimb, y)
        }
    }

    // Return from bottom pole (y = +radius) to top pole (y = -radius) along the terminator curve
    for (i in steps downTo 0) {
        val t = -1f + (2f * i / steps.toFloat())
        val y = center.y + t * radius
        val halfChord = radius * sqrt((1f - t * t).coerceAtLeast(0f))
        val xTerminator = if (effectiveWaxing) {
            center.x + halfChord * terminatorScale
        } else {
            center.x - halfChord * terminatorScale
        }
        path.lineTo(xTerminator, y)
    }

    path.close()
    return path
}

private fun Color.lighter(factor: Float): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}
