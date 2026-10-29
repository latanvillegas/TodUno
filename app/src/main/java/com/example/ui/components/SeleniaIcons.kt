package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.LunarAlmanacCategory
import com.example.domain.LunarTraditionalGoal
import com.example.domain.MoonPhaseType
import com.example.ui.ModuleThemeTarget
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Custom vector emblem for the Selenia top header:
 * Combines a golden celestial crescent, astrolabe compass ring, and 4-point star sparkle.
 */
@Composable
fun SeleniaBrandEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    primaryColor: Color = Color(0xFFFFD54F),
    secondaryColor: Color = Color(0xFF4DD0E1)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(11.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0E162B),
                        Color(0xFF1A2542)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.65f),
                        secondaryColor.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(11.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.78f)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = w * 0.42f

            // Astrolabe outer ring
            drawCircle(
                color = secondaryColor.copy(alpha = 0.35f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = w * 0.045f)
            )

            // 4 cardinal ticks
            val tickInner = r * 0.84f
            val tickOuter = r * 1.06f
            for (i in 0 until 4) {
                val angle = i * (Math.PI / 2.0)
                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()
                drawLine(
                    color = primaryColor.copy(alpha = 0.65f),
                    start = Offset(cx + cosA * tickInner, cy + sinA * tickInner),
                    end = Offset(cx + cosA * tickOuter, cy + sinA * tickOuter),
                    strokeWidth = w * 0.05f,
                    cap = StrokeCap.Round
                )
            }

            // Sculpted crescent moon path
            val crescentPath = Path().apply {
                val outerR = r * 0.68f
                val innerR = r * 0.54f
                addOval(Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR))
                val cutoutPath = Path().apply {
                    addOval(
                        Rect(
                            cx - innerR + outerR * 0.36f,
                            cy - innerR - outerR * 0.10f,
                            cx + innerR + outerR * 0.36f,
                            cy + innerR - outerR * 0.10f
                        )
                    )
                }
                op(this, cutoutPath, PathOperation.Difference)
            }
            drawPath(
                path = crescentPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF8E1), primaryColor, Color(0xFFFF8F00))
                )
            )

            // 4-point star sparkle in upper-right opening
            val sx = cx + r * 0.30f
            val sy = cy - r * 0.24f
            val sr = r * 0.22f
            val starPath = Path().apply {
                moveTo(sx, sy - sr)
                quadraticTo(sx + sr * 0.22f, sy - sr * 0.22f, sx + sr, sy)
                quadraticTo(sx + sr * 0.22f, sy + sr * 0.22f, sx, sy + sr)
                quadraticTo(sx - sr * 0.22f, sy + sr * 0.22f, sx - sr, sy)
                quadraticTo(sx - sr * 0.22f, sy - sr * 0.22f, sx, sy - sr)
                close()
            }
            drawPath(path = starPath, color = secondaryColor)
        }
    }
}

/**
 * High-precision astronomical medallion for any [MoonPhaseType].
 * Replaces flat OS text emojis (🌑..🌘) with a crisp vector-styled lunar disc,
 * terminator curve, subtle crater texture, and outer bezel ring.
 */
@Composable
fun MoonPhaseBadgeIcon(
    phaseType: MoonPhaseType,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    litColor: Color = Color(0xFFFFE082),
    shadowColor: Color = Color(0xFF10182B),
    southernHemisphere: Boolean = false
) {
    val phaseFraction = when (phaseType) {
        MoonPhaseType.NEW_MOON -> 0.0f
        MoonPhaseType.WAXING_CRESCENT -> 0.125f
        MoonPhaseType.FIRST_QUARTER -> 0.25f
        MoonPhaseType.WAXING_GIBBOUS -> 0.375f
        MoonPhaseType.FULL_MOON -> 0.5f
        MoonPhaseType.WANING_GIBBOUS -> 0.625f
        MoonPhaseType.LAST_QUARTER -> 0.75f
        MoonPhaseType.WANING_CRESCENT -> 0.875f
    }

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f * 0.86f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Subtle outer glow
        drawCircle(
            color = litColor.copy(alpha = 0.16f),
            radius = radius * 1.15f,
            center = center
        )

        // Shadowed lunar base disc
        drawCircle(
            color = shadowColor,
            radius = radius,
            center = center
        )

        // Illuminated portion
        val isWaxing = phaseFraction <= 0.5f
        val illumination = (1f - cos(phaseFraction * 2f * Math.PI.toFloat())) / 2f
        val flip = if (southernHemisphere) -1f else 1f
        val litOnRight = (isWaxing && flip > 0f) || (!isWaxing && flip < 0f)

        if (illumination > 0.02f) {
            if (illumination >= 0.98f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF8E1), litColor),
                        center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                        radius = radius * 1.3f
                    ),
                    radius = radius,
                    center = center
                )
            } else {
                val path = Path()
                val steps = 28
                val k = 2f * illumination - 1f
                for (i in 0..steps) {
                    val t = -1f + 2f * (i.toFloat() / steps)
                    val y = center.y + t * radius
                    val halfChord = sqrt((1f - t * t).coerceAtLeast(0f)) * radius
                    val xLimb = if (litOnRight) center.x + halfChord else center.x - halfChord
                    if (i == 0) path.moveTo(xLimb, y) else path.lineTo(xLimb, y)
                }
                for (i in steps downTo 0) {
                    val t = -1f + 2f * (i.toFloat() / steps)
                    val y = center.y + t * radius
                    val halfChord = sqrt((1f - t * t).coerceAtLeast(0f)) * radius
                    val xTerm = if (litOnRight) {
                        center.x - k * halfChord
                    } else {
                        center.x + k * halfChord
                    }
                    path.lineTo(xTerm, y)
                }
                path.close()
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFF8E1), litColor)
                    )
                )
            }
        }

        // Subtle crater details on lit/dark surface
        drawCircle(
            color = shadowColor.copy(alpha = 0.22f),
            radius = radius * 0.18f,
            center = Offset(center.x - radius * 0.26f, center.y - radius * 0.18f)
        )
        drawCircle(
            color = shadowColor.copy(alpha = 0.18f),
            radius = radius * 0.14f,
            center = Offset(center.x + radius * 0.22f, center.y + radius * 0.24f)
        )

        // Crisp metallic rim
        drawCircle(
            color = litColor.copy(alpha = 0.55f),
            radius = radius,
            center = center,
            style = Stroke(width = (this.size.minDimension * 0.055f).coerceAtLeast(1f))
        )
    }
}

/**
 * Glowing double-ring vector icon badge for categories, modules, and astronomical indicators.
 */
@Composable
fun SeleniaIconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    isCircular: Boolean = false,
    contentDescription: String? = null
) {
    val shape = if (isCircular) CircleShape else RoundedCornerShape(size * 0.30f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.26f),
                        tint.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.65f),
                        tint.copy(alpha = 0.18f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Maps each [LunarAlmanacCategory] to a dedicated Material Symbol vector icon.
 */
fun LunarAlmanacCategory.iconVector(): ImageVector = when (this) {
    LunarAlmanacCategory.ALL -> Icons.Filled.AutoAwesome
    LunarAlmanacCategory.BEAUTY_SKIN -> Icons.Filled.Spa
    LunarAlmanacCategory.HAIR_CARE -> Icons.Filled.ContentCut
    LunarAlmanacCategory.WELLNESS -> Icons.Filled.SelfImprovement
    LunarAlmanacCategory.GARDENING -> Icons.Filled.Eco
    LunarAlmanacCategory.ASTRONOMY -> Icons.Filled.TravelExplore
    LunarAlmanacCategory.TIDES -> Icons.Filled.Waves
    LunarAlmanacCategory.FISHING -> Icons.Filled.Phishing
    LunarAlmanacCategory.HOME_CRAFTS -> Icons.Filled.Handyman
}

/**
 * Maps each [LunarAlmanacCategory] to a distinct signature accent color.
 */
fun LunarAlmanacCategory.accentColor(): Color = when (this) {
    LunarAlmanacCategory.ALL -> Color(0xFFFFD54F)
    LunarAlmanacCategory.BEAUTY_SKIN -> Color(0xFFF48FB1)
    LunarAlmanacCategory.HAIR_CARE -> Color(0xFFFFD54F)
    LunarAlmanacCategory.WELLNESS -> Color(0xFF80CBC4)
    LunarAlmanacCategory.GARDENING -> Color(0xFF81C784)
    LunarAlmanacCategory.ASTRONOMY -> Color(0xFF90CAF9)
    LunarAlmanacCategory.TIDES -> Color(0xFF4DD0E1)
    LunarAlmanacCategory.FISHING -> Color(0xFFFFB74D)
    LunarAlmanacCategory.HOME_CRAFTS -> Color(0xFFCE93D8)
}

/**
 * Maps each [LunarTraditionalGoal] to a dedicated Material Symbol vector icon.
 */
fun LunarTraditionalGoal.iconVector(): ImageVector = when (this) {
    LunarTraditionalGoal.FAST_HAIR_GROWTH -> Icons.Filled.ContentCut
    LunarTraditionalGoal.STRONG_THICK_HAIR -> Icons.Filled.AutoAwesome
    LunarTraditionalGoal.LONG_LASTING_DEPILATION -> Icons.Filled.AutoFixHigh
    LunarTraditionalGoal.SKIN_NUTRITION_GLOW -> Icons.Filled.Spa
    LunarTraditionalGoal.SKIN_DETOX_EXFOLIATION -> Icons.Filled.WaterDrop
    LunarTraditionalGoal.PLANTING_FRUITS_FLOWERS -> Icons.Filled.LocalFlorist
    LunarTraditionalGoal.PRUNING_AND_TIMBER -> Icons.Filled.Park
}

/**
 * Maps each [LunarTraditionalGoal] to a distinct signature accent color.
 */
fun LunarTraditionalGoal.accentColor(): Color = when (this) {
    LunarTraditionalGoal.FAST_HAIR_GROWTH -> Color(0xFFFFD54F)
    LunarTraditionalGoal.STRONG_THICK_HAIR -> Color(0xFFFFB74D)
    LunarTraditionalGoal.LONG_LASTING_DEPILATION -> Color(0xFFCE93D8)
    LunarTraditionalGoal.SKIN_NUTRITION_GLOW -> Color(0xFFF48FB1)
    LunarTraditionalGoal.SKIN_DETOX_EXFOLIATION -> Color(0xFF4DD0E1)
    LunarTraditionalGoal.PLANTING_FRUITS_FLOWERS -> Color(0xFF81C784)
    LunarTraditionalGoal.PRUNING_AND_TIMBER -> Color(0xFFA1887F)
}

/**
 * Maps each [ModuleThemeTarget] to a crisp Material Symbol vector icon.
 */
fun ModuleThemeTarget.iconVector(): ImageVector = when (this) {
    ModuleThemeTarget.ALL_MODULES -> Icons.Filled.AutoAwesomeMosaic
    ModuleThemeTarget.CALENDAR -> Icons.Filled.CalendarMonth
    ModuleThemeTarget.MOON_OBSERVATORY -> Icons.Filled.NightlightRound
    ModuleThemeTarget.CLOCK -> Icons.Filled.AccessTime
    ModuleThemeTarget.CALCULATOR -> Icons.Filled.Calculate
}
