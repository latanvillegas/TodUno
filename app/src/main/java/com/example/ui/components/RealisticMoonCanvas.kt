package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

/**
 * 6 curated 3D astronomical color atmospheres for the Moon Observatory.
 */
@Immutable
enum class LunarAtmospherePreset(
    val spanishName: String,
    val shortChip: String,
    val litColor: Color,
    val highlightColor: Color,
    val shadowColor: Color,
    val coronaSecondaryColor: Color,
    val description: String
) {
    AUTO_PHASE(
        spanishName = "Dinámico por Fase",
        shortChip = "Por Fase",
        litColor = Color(0xFFFFD54F),
        highlightColor = Color(0xFFFFFDE7),
        shadowColor = Color(0xFF0E1730),
        coronaSecondaryColor = Color(0xFF4DD0E1),
        description = "Cambia automáticamente: Oro Solar en creciente, Ámbar en llena y Plata-Cian en menguante."
    ),
    SELENITE_PEARL(
        spanishName = "Perla Selenita (Realista)",
        shortChip = "Perla Real",
        litColor = Color(0xFFF3E6C8),
        highlightColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFF0F1932),
        coronaSecondaryColor = Color(0xFF80DEEA),
        description = "Color astrofotográfico real del regolito lunar con luz cenicienta terrestre azulada."
    ),
    SUPERMOON_GOLD(
        spanishName = "Superluna de Cosecha (Oro 3D)",
        shortChip = "Superluna Oro",
        litColor = Color(0xFFFFB300),
        highlightColor = Color(0xFFFFF8E1),
        shadowColor = Color(0xFF1A1228),
        coronaSecondaryColor = Color(0xFFFF6F00),
        description = "Resplandor cálido dorado-ámbar propio del perigeo lunar cerca del horizonte."
    ),
    BLOOD_ECLIPSE(
        spanishName = "Luna de Sangre (Eclipse)",
        shortChip = "Luna Sangre",
        litColor = Color(0xFFE53935),
        highlightColor = Color(0xFFFFCCBC),
        shadowColor = Color(0xFF1F080C),
        coronaSecondaryColor = Color(0xFFFF7043),
        description = "Atmósfera rojiza cobriza producida por la dispersión de Rayleigh en eclipses totales."
    ),
    BLUE_SIDEREAL(
        spanishName = "Luna Azul Sideral",
        shortChip = "Luna Azul",
        litColor = Color(0xFF4DD0E1),
        highlightColor = Color(0xFFE0F7FA),
        shadowColor = Color(0xFF08182B),
        coronaSecondaryColor = Color(0xFF536DFE),
        description = "Tono cian-zafiro de alta nitidez para observación telescópica nocturna."
    ),
    NEBULA_VIOLET(
        spanishName = "Nebulosa Ultravioleta",
        shortChip = "Ultravioleta",
        litColor = Color(0xFFB388FF),
        highlightColor = Color(0xFFF3E5F5),
        shadowColor = Color(0xFF140D2B),
        coronaSecondaryColor = Color(0xFFFF4081),
        description = "Estética de espacio profundo con corona magenta-violeta de alto contraste."
    )
}

/**
 * Returns the recommended 3D astronomical color palette for a given [MoonPhaseType] in the calendar:
 * - Waxing phases: Warm Solar Gold & Pearl (#FFE082 -> #FFB300) for ascending energy.
 * - Full Moon: Radiant Supermoon Gold-Amber (#FFD54F -> #FF8F00) with high specular brilliance.
 * - Waning phases: Celestial Silver-Cyan (#80DEEA -> #26C6DA) to visually distinguish waning vs waxing weeks.
 * - New Moon: Deep Sidereal Sapphire with Cyan Earthshine rim (#4DD0E1) so New Moon cells remain visible in 3D.
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
 * Renders a realistic 3D astronomical Moon disk using Canvas with zero per-frame Path allocations
 * and precomputed trigonometry/chord tables.
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
    litHighlightColor: Color = litColor.lighter(0.48f),
    shadowColor: Color = Color(0xFF101733),
    coronaSecondaryColor: Color = Color(0xFF4DD0E1)
) {
    // Reuse a single Path instance across draw passes to avoid GC churn when rendering 42+ calendar moons.
    val reusableLitPath = remember { Path() }
    val lighterShadowColor = remember(shadowColor) { shadowColor.lighter(0.16f) }
    val deepLitTone = remember(litColor) { litColor.darker(0.28f) }
    val rimColor = remember(litColor) { litColor.copy(alpha = 0.56f) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val minDim = size.minDimension
        if (minDim <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = if (showAmbientGlow) minDim * 0.37f else minDim * 0.45f
        val isLargeObservatoryMoon = radius > 45f

        val phaseAngle = phaseFraction * 2.0 * PI
        val illuminationRatio = ((1.0 - cos(phaseAngle)) / 2.0).toFloat()

        // 1. Optional atmospheric starlight, lunar corona glow & telescope astrolabe bezel
        if (showAmbientGlow) {
            val glowAlpha = 0.20f + 0.32f * illuminationRatio
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        litColor.copy(alpha = glowAlpha),
                        coronaSecondaryColor.copy(alpha = glowAlpha * 0.42f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.48f
                ),
                radius = radius * 1.48f,
                center = center
            )

            // Celestial coordinate ring around the moon
            val ringRadius = radius * 1.18f
            drawCircle(
                color = litColor.copy(alpha = 0.30f),
                radius = ringRadius,
                center = center,
                style = COORDINATE_RING_STROKE
            )

            // For the large Hero Moon in the Observatory, render 12 astrolabe degree ticks + starlight points
            if (isLargeObservatoryMoon) {
                for (i in 0 until 12) {
                    val isCardinal = i % 3 == 0
                    val innerR = if (isCardinal) ringRadius * 0.94f else ringRadius * 0.97f
                    val outerR = if (isCardinal) ringRadius * 1.06f else ringRadius * 1.03f
                    val cosA = ASTROLABE_COS[i]
                    val sinA = ASTROLABE_SIN[i]
                    drawLine(
                        color = if (isCardinal) litColor.copy(alpha = 0.65f) else coronaSecondaryColor.copy(alpha = 0.40f),
                        start = Offset(center.x + cosA * innerR, center.y + sinA * innerR),
                        end = Offset(center.x + cosA * outerR, center.y + sinA * outerR),
                        strokeWidth = if (isCardinal) 2.2f else 1.2f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // 2. Base 3D dark lunar sphere (earthshine / night side with volumetric lighting)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    lighterShadowColor,
                    shadowColor,
                    Color(0xFF060A17)
                ),
                center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                radius = radius * 1.25f
            ),
            radius = radius,
            center = center
        )

        // Subtle maria on dark side (earthshine effect)
        drawLunarMaria(center, radius, alpha = 0.12f, isIlluminatedSide = false, litHighlightColor = litHighlightColor)

        // 3. Illuminated spherical portion bounded by limb and elliptical terminator (zero allocation)
        populateIlluminatedMoonPath(
            targetPath = reusableLitPath,
            center = center,
            radius = radius,
            phaseFraction = phaseFraction,
            isNorthernHemisphere = isNorthernHemisphere
        )

        clipPath(reusableLitPath) {
            // 3-stop 3D volumetric sphere shader
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        litHighlightColor,
                        litColor,
                        deepLitTone
                    ),
                    center = Offset(center.x - radius * 0.22f, center.y - radius * 0.22f),
                    radius = radius * 1.22f
                ),
                radius = radius,
                center = center
            )
            // Realistic lunar maria, 3D rimmed craters & Tycho ejecta rays on the illuminated face
            drawLunarMaria(center, radius, alpha = 0.24f, isIlluminatedSide = true, litHighlightColor = litHighlightColor)
        }

        // 4. Outer 3D rim light (stronger on New Moon + subtle earthshine cyan tint on night limb)
        val effectiveRimAlpha = if (illuminationRatio < 0.08f) 0.75f else 0.50f
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    rimColor.copy(alpha = effectiveRimAlpha),
                    coronaSecondaryColor.copy(alpha = effectiveRimAlpha * 0.7f),
                    rimColor.copy(alpha = effectiveRimAlpha)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = (minDim * 0.026f).coerceAtLeast(1f))
        )
    }
}

private fun DrawScope.drawLunarMaria(
    center: Offset,
    radius: Float,
    alpha: Float,
    isIlluminatedSide: Boolean,
    litHighlightColor: Color
) {
    val mareColor = Color(0xFF4E4944).copy(alpha = alpha)
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
    // Mare Crisium (far right limb)
    drawCircle(
        color = mareColor,
        radius = radius * 0.11f,
        center = Offset(center.x + radius * 0.58f, center.y - radius * 0.16f)
    )
    // Mare Nubium & Mare Humorum (lower left-center)
    drawCircle(
        color = mareColor,
        radius = radius * 0.20f,
        center = Offset(center.x - radius * 0.15f, center.y + radius * 0.32f)
    )

    // High-detail 3D craters & Tycho ray system for larger moons (Observatory & Day Inspector)
    if (radius > 28f && isIlluminatedSide) {
        val tychoCenter = Offset(center.x - radius * 0.06f, center.y + radius * 0.54f)
        val copernicusCenter = Offset(center.x - radius * 0.22f, center.y + radius * 0.05f)
        val platoCenter = Offset(center.x - radius * 0.14f, center.y - radius * 0.58f)

        // Subtle Tycho ejecta rays when in Observatory size
        if (radius > 45f) {
            val rayColor = litHighlightColor.copy(alpha = alpha * 0.75f)
            for (i in 0 until 6) {
                val angle = (-0.75f + i * 0.30f) * PI.toFloat()
                drawLine(
                    color = rayColor,
                    start = tychoCenter,
                    end = Offset(
                        tychoCenter.x + cos(angle) * radius * 0.55f,
                        tychoCenter.y - abs(sin(angle)) * radius * 0.55f
                    ),
                    strokeWidth = 1.4f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Tycho crater (bright 3D rim + inner floor)
        draw3DCrater(tychoCenter, radius * 0.085f, litHighlightColor, alpha)
        // Copernicus crater
        draw3DCrater(copernicusCenter, radius * 0.095f, litHighlightColor, alpha)
        // Plato dark-floored northern crater
        draw3DCrater(platoCenter, radius * 0.075f, litHighlightColor, alpha)
    }
}

private fun DrawScope.draw3DCrater(
    craterCenter: Offset,
    craterRadius: Float,
    highlightColor: Color,
    baseAlpha: Float
) {
    // Crater shadow rim (offset top-left)
    drawCircle(
        color = Color(0xFF2B2623).copy(alpha = baseAlpha * 1.1f),
        radius = craterRadius,
        center = Offset(craterCenter.x - craterRadius * 0.15f, craterCenter.y - craterRadius * 0.15f)
    )
    // Crater sunlit rim (offset bottom-right)
    drawCircle(
        color = highlightColor.copy(alpha = (baseAlpha * 1.8f).coerceAtMost(0.65f)),
        radius = craterRadius * 0.92f,
        center = Offset(craterCenter.x + craterRadius * 0.12f, craterCenter.y + craterRadius * 0.12f),
        style = Stroke(width = (craterRadius * 0.28f).coerceAtLeast(1f))
    )
    // Central peak
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

    // Flip waxing/waning left-right for Southern Hemisphere
    val effectiveWaxing = if (isNorthernHemisphere) (p <= 0.5) else (p > 0.5)
    val limbSign = if (effectiveWaxing) 1f else -1f
    // Terminator x-scale: cos(2 * pi * p) ranges from +1 (New) -> 0 (Quarter) -> -1 (Full) -> 0 (Last Q) -> +1
    val terminatorScale = cos(2.0 * PI * p).toFloat() * limbSign

    // Sample from top pole (y = -radius) to bottom pole (y = +radius) using precomputed LUT
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

    // Return from bottom pole (y = +radius) to top pole (y = -radius) along the terminator curve
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
