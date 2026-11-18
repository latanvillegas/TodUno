package com.example.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.example.domain.MoonPhaseType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Precomputed unit-circle chord lookup table: eliminates 122 sqrt() calls per Moon draw pass.
private const val TERMINATOR_STEPS = 44
private val UNIT_T = FloatArray(TERMINATOR_STEPS + 1) { i ->
    -1f + (2f * i / TERMINATOR_STEPS.toFloat())
}
private val UNIT_HALF_CHORD = FloatArray(TERMINATOR_STEPS + 1) { i ->
    val t = UNIT_T[i]
    sqrt((1f - t * t).coerceAtLeast(0f))
}
private val COORDINATE_RING_STROKE = Stroke(width = 1.3f)

// Precomputed 12-tick astrolabe ring angles (every 30 degrees)
private val ASTROLABE_COS = FloatArray(12) { i -> cos(i * PI / 6.0).toFloat() }
private val ASTROLABE_SIN = FloatArray(12) { i -> sin(i * PI / 6.0).toFloat() }

// Precomputed 6-ray Tycho ejecta angles
private val TYCHO_RAY_COS = FloatArray(6) { i -> cos((-0.75 + i * 0.30) * PI).toFloat() }
private val TYCHO_RAY_ABS_SIN = FloatArray(6) { i -> abs(sin((-0.75 + i * 0.30) * PI)).toFloat() }


/**
 * Returns the recommended 3D astronomical color palette for a given [MoonPhaseType] in the calendar.
 */
fun MoonPhaseType.recommendedCalendarLitColor(): Color = when (this) {
    MoonPhaseType.NEW_MOON -> Color(0xFF4DD0E1)
    MoonPhaseType.WAXING_CRESCENT -> Color(0xFFFFE082)
    MoonPhaseType.FIRST_QUARTER -> Color(0xFFFFD54F)
    MoonPhaseType.WAXING_GIBBOUS -> Color(0xFFFFCA28)
    MoonPhaseType.FULL_MOON -> Color(0xFFFFB300)
    MoonPhaseType.WANING_GIBBOUS -> Color(0xFF80DEEA)
    MoonPhaseType.LAST_QUARTER -> Color(0xFF4DD0E1)
    MoonPhaseType.WANING_CRESCENT -> Color(0xFF26C6DA)
}

/**
 * Renders a realistic 3D astronomical Moon disk using [drawWithCache] so all native Skia [Brush]
 * shaders, [Stroke] instances, and terminator [Path] geometry are cached across scroll and frame redraws
 * with zero per-frame heap allocations.
 */
@Composable
fun RealisticMoonCanvas(
    phaseFraction: Double,
    modifier: Modifier = Modifier,
    isNorthernHemisphere: Boolean = true,
    showAmbientGlow: Boolean = true,
    litColor: Color = Color(0xFFF3E6C8),
    litHighlightColor: Color = Color(0xFFFFFFFF),
    shadowColor: Color = Color(0xFF0F1932),
    coronaSecondaryColor: Color = Color(0xFF80DEEA)
) {
    val cachedLitPath = remember { Path() }
    val lighterShadowColor = remember(shadowColor) { shadowColor.lighter(0.16f) }
    val deepLitTone = remember(litColor) { litColor.darker(0.28f) }
    val rimColor = remember(litColor) { litColor.copy(alpha = 0.56f) }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val minDim = size.minDimension
                if (minDim <= 0f) {
                    return@drawWithCache onDrawBehind {}
                }
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = if (showAmbientGlow) minDim * 0.37f else minDim * 0.45f
                val isLargeObservatoryMoon = radius > 45f

                val phaseAngle = phaseFraction * 2.0 * PI
                val illuminationRatio = ((1.0 - cos(phaseAngle)) / 2.0).toFloat()

                val glowBrush = if (showAmbientGlow) {
                    val glowAlpha = 0.20f + 0.32f * illuminationRatio
                    Brush.radialGradient(
                        colors = listOf(
                            litColor.copy(alpha = glowAlpha),
                            coronaSecondaryColor.copy(alpha = glowAlpha * 0.42f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 1.48f
                    )
                } else {
                    null
                }

                val darkSphereBrush = Brush.radialGradient(
                    colors = listOf(
                        lighterShadowColor,
                        shadowColor,
                        Color(0xFF060A17)
                    ),
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                    radius = radius * 1.25f
                )

                val litSphereBrush = Brush.radialGradient(
                    colors = listOf(
                        litHighlightColor,
                        litColor,
                        deepLitTone
                    ),
                    center = Offset(center.x - radius * 0.22f, center.y - radius * 0.22f),
                    radius = radius * 1.22f
                )

                val effectiveRimAlpha = if (illuminationRatio < 0.08f) 0.75f else 0.50f
                val rimBrush = Brush.sweepGradient(
                    colors = listOf(
                        rimColor.copy(alpha = effectiveRimAlpha),
                        coronaSecondaryColor.copy(alpha = effectiveRimAlpha * 0.7f),
                        rimColor.copy(alpha = effectiveRimAlpha)
                    ),
                    center = center
                )
                val rimStroke = Stroke(width = (minDim * 0.026f).coerceAtLeast(1f))

                populateIlluminatedMoonPath(
                    targetPath = cachedLitPath,
                    center = center,
                    radius = radius,
                    phaseFraction = phaseFraction,
                    isNorthernHemisphere = isNorthernHemisphere
                )

                val ringColor = litColor.copy(alpha = 0.30f)
                val cardinalTickColor = litColor.copy(alpha = 0.65f)
                val minorTickColor = coronaSecondaryColor.copy(alpha = 0.40f)

                onDrawBehind {
                    // 1. Optional atmospheric starlight, lunar corona glow & telescope astrolabe bezel
                    if (glowBrush != null) {
                        drawCircle(
                            brush = glowBrush,
                            radius = radius * 1.48f,
                            center = center
                        )

                        val ringRadius = radius * 1.18f
                        drawCircle(
                            color = ringColor,
                            radius = ringRadius,
                            center = center,
                            style = COORDINATE_RING_STROKE
                        )

                        if (isLargeObservatoryMoon) {
                            for (i in 0 until 12) {
                                val isCardinal = i % 3 == 0
                                val innerR = if (isCardinal) ringRadius * 0.94f else ringRadius * 0.97f
                                val outerR = if (isCardinal) ringRadius * 1.06f else ringRadius * 1.03f
                                val cosA = ASTROLABE_COS[i]
                                val sinA = ASTROLABE_SIN[i]
                                drawLine(
                                    color = if (isCardinal) cardinalTickColor else minorTickColor,
                                    start = Offset(center.x + cosA * innerR, center.y + sinA * innerR),
                                    end = Offset(center.x + cosA * outerR, center.y + sinA * outerR),
                                    strokeWidth = if (isCardinal) 2.2f else 1.2f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    // 2. Base 3D dark lunar sphere (earthshine / night side)
                    drawCircle(
                        brush = darkSphereBrush,
                        radius = radius,
                        center = center
                    )

                    drawLunarMaria(
                        center = center,
                        radius = radius,
                        alpha = 0.12f,
                        isIlluminatedSide = false,
                        litHighlightColor = litHighlightColor
                    )

                    // 3. Illuminated spherical portion clipped to prebuilt terminator Path
                    clipPath(cachedLitPath) {
                        drawCircle(
                            brush = litSphereBrush,
                            radius = radius,
                            center = center
                        )
                        drawLunarMaria(
                            center = center,
                            radius = radius,
                            alpha = 0.24f,
                            isIlluminatedSide = true,
                            litHighlightColor = litHighlightColor
                        )
                    }

                    // 4. Outer 3D rim light
                    drawCircle(
                        brush = rimBrush,
                        radius = radius,
                        center = center,
                        style = rimStroke
                    )
                }
            }
    )
}

private fun DrawScope.drawLunarMaria(
    center: Offset,
    radius: Float,
    alpha: Float,
    isIlluminatedSide: Boolean,
    litHighlightColor: Color
) {
    val mareColor = Color(0xFF4E4944).copy(alpha = alpha)
    drawCircle(
        color = mareColor,
        radius = radius * 0.28f,
        center = Offset(center.x - radius * 0.28f, center.y - radius * 0.18f)
    )
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
    drawCircle(
        color = mareColor,
        radius = radius * 0.11f,
        center = Offset(center.x + radius * 0.58f, center.y - radius * 0.16f)
    )
    drawCircle(
        color = mareColor,
        radius = radius * 0.20f,
        center = Offset(center.x - radius * 0.15f, center.y + radius * 0.32f)
    )

    if (radius > 28f && isIlluminatedSide) {
        val tychoCenter = Offset(center.x - radius * 0.06f, center.y + radius * 0.54f)
        val copernicusCenter = Offset(center.x - radius * 0.22f, center.y + radius * 0.05f)
        val platoCenter = Offset(center.x - radius * 0.14f, center.y - radius * 0.58f)

        if (radius > 45f) {
            val rayColor = litHighlightColor.copy(alpha = alpha * 0.75f)
            val rayLen = radius * 0.55f
            for (i in 0 until 6) {
                drawLine(
                    color = rayColor,
                    start = tychoCenter,
                    end = Offset(
                        tychoCenter.x + TYCHO_RAY_COS[i] * rayLen,
                        tychoCenter.y - TYCHO_RAY_ABS_SIN[i] * rayLen
                    ),
                    strokeWidth = 1.4f,
                    cap = StrokeCap.Round
                )
            }
        }

        draw3DCrater(tychoCenter, radius * 0.085f, litHighlightColor, alpha)
        draw3DCrater(copernicusCenter, radius * 0.095f, litHighlightColor, alpha)
        draw3DCrater(platoCenter, radius * 0.075f, litHighlightColor, alpha)
    }
}

private fun DrawScope.draw3DCrater(
    craterCenter: Offset,
    craterRadius: Float,
    highlightColor: Color,
    baseAlpha: Float
) {
    drawCircle(
        color = Color(0xFF2B2623).copy(alpha = baseAlpha * 1.1f),
        radius = craterRadius,
        center = Offset(craterCenter.x - craterRadius * 0.15f, craterCenter.y - craterRadius * 0.15f)
    )
    drawCircle(
        color = highlightColor.copy(alpha = (baseAlpha * 1.8f).coerceAtMost(0.65f)),
        radius = craterRadius * 0.92f,
        center = Offset(craterCenter.x + craterRadius * 0.12f, craterCenter.y + craterRadius * 0.12f),
        style = Stroke(width = (craterRadius * 0.28f).coerceAtLeast(1f))
    )
    drawCircle(
        color = highlightColor.copy(alpha = (baseAlpha * 1.9f).coerceAtMost(0.75f)),
        radius = craterRadius * 0.25f,
        center = craterCenter
    )
}

private fun populateIlluminatedMoonPath(
    targetPath: Path,
    center: Offset,
    radius: Float,
    phaseFraction: Double,
    isNorthernHemisphere: Boolean
) {
    targetPath.reset()
    var p = phaseFraction % 1.0
    if (p < 0) p += 1.0

    val effectiveWaxing = if (isNorthernHemisphere) (p <= 0.5) else (p > 0.5)
    val limbSign = if (effectiveWaxing) 1f else -1f
    val terminatorScale = cos(2.0 * PI * p).toFloat() * limbSign

    for (i in 0..TERMINATOR_STEPS) {
        val y = center.y + UNIT_T[i] * radius
        val halfChord = radius * UNIT_HALF_CHORD[i]
        val xLimb = center.x + halfChord * limbSign
        if (i == 0) {
            targetPath.moveTo(xLimb, y)
        } else {
            targetPath.lineTo(xLimb, y)
        }
    }

    for (i in TERMINATOR_STEPS downTo 0) {
        val y = center.y + UNIT_T[i] * radius
        val halfChord = radius * UNIT_HALF_CHORD[i]
        val xTerminator = center.x + halfChord * terminatorScale
        targetPath.lineTo(xTerminator, y)
    }

    targetPath.close()
}

private fun Color.lighter(factor: Float): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.darker(factor: Float): Color {
    val scale = (1f - factor).coerceIn(0f, 1f)
    return Color(
        red = (red * scale).coerceIn(0f, 1f),
        green = (green * scale).coerceIn(0f, 1f),
        blue = (blue * scale).coerceIn(0f, 1f),
        alpha = alpha
    )
}
