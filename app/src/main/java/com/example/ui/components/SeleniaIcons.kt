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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.LunarAlmanacCategory
import com.example.domain.LunarTraditionalGoal
import com.example.domain.MoonPhaseType
import com.example.ui.AppDestination
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
 * 3D Multi-Color Polychromatic Icon for the 5 main modules:
 * Calendario, Luna, Reloj, Calculadora, and Estudio de Colores.
 * Renders depth shadow, 3-stop volumetric gradients, top-left specular glass reflection,
 * and bespoke 3D internal geometry per module.
 */
@Composable
fun SeleniaModule3DIcon(
    destination: AppDestination,
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    customAccent: Color? = null
) {
    val alphaScale = if (selected) 1f else 0.82f
    Canvas(modifier = modifier.size(size)) {
        when (destination) {
            AppDestination.CALENDAR -> draw3DCalendarIcon(alphaScale, selected, customAccent)
            AppDestination.MOON_OBSERVATORY -> draw3DMoonIcon(alphaScale, selected, customAccent)
            AppDestination.CLOCK -> draw3DClockIcon(alphaScale, selected, customAccent)
            AppDestination.CALCULATOR -> draw3DCalculatorIcon(alphaScale, selected, customAccent)
            AppDestination.COLORS -> draw3DColorStudioIcon(alphaScale, selected, customAccent)
        }
    }
}

/**
 * Overload of [SeleniaModule3DIcon] for [ModuleThemeTarget] (used in Color Studio).
 */
@Composable
fun SeleniaModule3DTargetIcon(
    target: ModuleThemeTarget,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    customAccent: Color? = null
) {
    val mappedDestination = when (target) {
        ModuleThemeTarget.CALENDAR -> AppDestination.CALENDAR
        ModuleThemeTarget.MOON_OBSERVATORY -> AppDestination.MOON_OBSERVATORY
        ModuleThemeTarget.CLOCK -> AppDestination.CLOCK
        ModuleThemeTarget.CALCULATOR -> AppDestination.CALCULATOR
        ModuleThemeTarget.ALL_MODULES -> AppDestination.COLORS
    }
    SeleniaModule3DIcon(
        destination = mappedDestination,
        selected = true,
        modifier = modifier,
        size = size,
        customAccent = customAccent
    )
}

private fun DrawScope.draw3DCalendarIcon(
    alpha: Float,
    selected: Boolean,
    customAccent: Color?
) {
    val w = size.width
    val h = size.height
    val topAccent = customAccent ?: Color(0xFFFF5252) // Coral Ruby
    val bodyGold = Color(0xFFFFD54F)
    val bodyCyan = Color(0xFF4DD0E1)

    // 1. 3D Drop Shadow base
    drawRoundRect(
        color = Color(0xFF050814).copy(alpha = 0.65f * alpha),
        topLeft = Offset(w * 0.12f, h * 0.18f),
        size = Size(w * 0.80f, h * 0.76f),
        cornerRadius = CornerRadius(w * 0.20f, w * 0.20f)
    )

    // 2. Sculpted 3D Glass-Slate Body
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF283558).copy(alpha = alpha),
                Color(0xFF16203A).copy(alpha = alpha),
                Color(0xFF0E1528).copy(alpha = alpha)
            ),
            start = Offset(0f, h * 0.15f),
            end = Offset(w, h)
        ),
        topLeft = Offset(w * 0.10f, h * 0.14f),
        size = Size(w * 0.80f, h * 0.74f),
        cornerRadius = CornerRadius(w * 0.20f, w * 0.20f)
    )

    // 3. 3D Top Header Ribbon (3-color gradient: Coral -> Orange -> Gold)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                topAccent.copy(alpha = alpha),
                Color(0xFFFF8A65).copy(alpha = alpha),
                bodyGold.copy(alpha = alpha)
            )
        ),
        topLeft = Offset(w * 0.10f, h * 0.14f),
        size = Size(w * 0.80f, h * 0.24f),
        cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
    )

    // 4. Metallic 3D Binder Rings
    val ringPositions = listOf(w * 0.30f, w * 0.70f)
    for (rx in ringPositions) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = alpha), bodyGold.copy(alpha = alpha))
            ),
            topLeft = Offset(rx - w * 0.045f, h * 0.06f),
            size = Size(w * 0.09f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
        )
    }

    // 5. Polychromatic Calendar Grid Dots + Mini Golden Moon
    val dotCols = listOf(w * 0.26f, w * 0.43f, w * 0.60f)
    val dotRows = listOf(h * 0.50f, h * 0.66f)
    for ((rIdx, ry) in dotRows.withIndex()) {
        for ((cIdx, cx) in dotCols.withIndex()) {
            val dotColor = when ((rIdx + cIdx) % 3) {
                0 -> bodyCyan
                1 -> bodyGold
                else -> Color(0xFFB388FF)
            }
            drawCircle(
                color = dotColor.copy(alpha = alpha * 0.9f),
                radius = w * 0.045f,
                center = Offset(cx, ry)
            )
        }
    }

    // Mini 3D Glowing Moon badge on bottom-right cell
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, bodyGold, Color(0xFFFF8F00)),
            center = Offset(w * 0.73f, h * 0.64f),
            radius = w * 0.12f
        ),
        radius = w * 0.09f,
        center = Offset(w * 0.74f, h * 0.66f)
    )

    // Outer 3D Specular Rim
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (selected) 0.75f else 0.4f),
                bodyCyan.copy(alpha = 0.35f)
            )
        ),
        topLeft = Offset(w * 0.10f, h * 0.14f),
        size = Size(w * 0.80f, h * 0.74f),
        cornerRadius = CornerRadius(w * 0.20f, w * 0.20f),
        style = Stroke(width = w * 0.04f)
    )
}

private fun DrawScope.draw3DMoonIcon(
    alpha: Float,
    selected: Boolean,
    customAccent: Color?
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.5f
    val r = w * 0.40f
    val primaryGold = customAccent ?: Color(0xFFFFD54F)
    val cyanGlow = Color(0xFF00E5FF)
    val violetRim = Color(0xFFB388FF)

    // 1. 3D Outer Volumetric Halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryGold.copy(alpha = 0.35f * alpha),
                cyanGlow.copy(alpha = 0.15f * alpha),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = r * 1.25f
        ),
        radius = r * 1.22f,
        center = Offset(cx, cy)
    )

    // 2. 3D Dark Celestial Sphere Base
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF26355E).copy(alpha = alpha),
                Color(0xFF0D1429).copy(alpha = alpha)
            ),
            center = Offset(cx - r * 0.25f, cy - r * 0.25f),
            radius = r * 1.3f
        ),
        radius = r,
        center = Offset(cx, cy)
    )

    // 3. Sculpted 3D Golden-Amber Crescent with Specular Highlight
    val crescentPath = Path().apply {
        val outerR = r * 0.86f
        val innerR = r * 0.68f
        addOval(Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR))
        val cutout = Path().apply {
            addOval(
                Rect(
                    cx - innerR + outerR * 0.38f,
                    cy - innerR - outerR * 0.12f,
                    cx + innerR + outerR * 0.38f,
                    cy + innerR - outerR * 0.12f
                )
            )
        }
        op(this, cutout, PathOperation.Difference)
    }
    drawPath(
        path = crescentPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFDE7).copy(alpha = alpha),
                primaryGold.copy(alpha = alpha),
                Color(0xFFFF8F00).copy(alpha = alpha)
            ),
            start = Offset(cx - r, cy - r),
            end = Offset(cx + r, cy + r)
        )
    )

    // 4. 3D Crater Depths on the Crescent
    drawCircle(
        color = Color(0xFFE65100).copy(alpha = 0.35f * alpha),
        radius = r * 0.14f,
        center = Offset(cx - r * 0.42f, cy - r * 0.10f)
    )
    drawCircle(
        color = Color(0xFFE65100).copy(alpha = 0.30f * alpha),
        radius = r * 0.10f,
        center = Offset(cx - r * 0.28f, cy + r * 0.34f)
    )

    // 5. Cyan 4-Point Star Sparkle + Violet Orbital Ring
    val sx = cx + r * 0.36f
    val sy = cy - r * 0.28f
    val sr = r * 0.26f
    val starPath = Path().apply {
        moveTo(sx, sy - sr)
        quadraticTo(sx + sr * 0.22f, sy - sr * 0.22f, sx + sr, sy)
        quadraticTo(sx + sr * 0.22f, sy + sr * 0.22f, sx, sy + sr)
        quadraticTo(sx - sr * 0.22f, sy + sr * 0.22f, sx - sr, sy)
        quadraticTo(sx - sr * 0.22f, sy - sr * 0.22f, sx, sy - sr)
        close()
    }
    drawPath(
        path = starPath,
        brush = Brush.radialGradient(
            colors = listOf(Color.White, cyanGlow),
            center = Offset(sx, sy),
            radius = sr
        )
    )

    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                primaryGold.copy(alpha = 0.8f * alpha),
                cyanGlow.copy(alpha = 0.65f * alpha),
                violetRim.copy(alpha = 0.7f * alpha),
                primaryGold.copy(alpha = 0.8f * alpha)
            ),
            center = Offset(cx, cy)
        ),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = w * 0.045f)
    )
}

private fun DrawScope.draw3DClockIcon(
    alpha: Float,
    selected: Boolean,
    customAccent: Color?
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.52f
    val r = w * 0.39f
    val bezelCyan = customAccent ?: Color(0xFF00E5FF)
    val handGold = Color(0xFFFFD54F)
    val secondRuby = Color(0xFFFF5252)

    // 1. Top Chronometer Crown Button (3D metallic gold)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(handGold.copy(alpha = alpha), Color(0xFFFF8F00).copy(alpha = alpha))
        ),
        topLeft = Offset(cx - w * 0.10f, h * 0.04f),
        size = Size(w * 0.20f, h * 0.09f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )

    // 2. 3D Drop Shadow
    drawCircle(
        color = Color(0xFF050814).copy(alpha = 0.65f * alpha),
        radius = r * 1.04f,
        center = Offset(cx + w * 0.02f, cy + h * 0.03f)
    )

    // 3. Sapphire 3D Watch Dial
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF243763).copy(alpha = alpha),
                Color(0xFF101933).copy(alpha = alpha),
                Color(0xFF090E1F).copy(alpha = alpha)
            ),
            center = Offset(cx - r * 0.25f, cy - r * 0.25f),
            radius = r * 1.25f
        ),
        radius = r,
        center = Offset(cx, cy)
    )

    // 4. 8 Cardinal & Intercardinal Ticks
    for (i in 0 until 8) {
        val angle = i * (Math.PI / 4.0)
        val isCardinal = i % 2 == 0
        val inner = if (isCardinal) r * 0.72f else r * 0.80f
        val outer = r * 0.90f
        drawLine(
            color = (if (isCardinal) handGold else bezelCyan).copy(alpha = alpha),
            start = Offset(cx + cos(angle).toFloat() * inner, cy + sin(angle).toFloat() * inner),
            end = Offset(cx + cos(angle).toFloat() * outer, cy + sin(angle).toFloat() * outer),
            strokeWidth = if (isCardinal) w * 0.05f else w * 0.035f,
            cap = StrokeCap.Round
        )
    }

    // 5. 3-Color Watch Hands (Gold Hour, Cyan Minute, Ruby Second + Center Jewel)
    // Hour hand (pointing ~10 o'clock)
    drawLine(
        color = handGold.copy(alpha = alpha),
        start = Offset(cx, cy),
        end = Offset(cx - r * 0.36f, cy - r * 0.26f),
        strokeWidth = w * 0.07f,
        cap = StrokeCap.Round
    )
    // Minute hand (pointing ~2 o'clock)
    drawLine(
        color = bezelCyan.copy(alpha = alpha),
        start = Offset(cx, cy),
        end = Offset(cx + r * 0.45f, cy - r * 0.34f),
        strokeWidth = w * 0.055f,
        cap = StrokeCap.Round
    )
    // Ruby second hand (pointing ~5 o'clock)
    drawLine(
        color = secondRuby.copy(alpha = alpha),
        start = Offset(cx, cy),
        end = Offset(cx + r * 0.25f, cy + r * 0.48f),
        strokeWidth = w * 0.035f,
        cap = StrokeCap.Round
    )
    // Center 3D Jewel Cap
    drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = w * 0.06f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = secondRuby.copy(alpha = alpha),
        radius = w * 0.035f,
        center = Offset(cx, cy)
    )

    // 6. 3D Metallic Bezel Rim
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (selected) 0.85f else 0.5f),
                bezelCyan.copy(alpha = alpha),
                handGold.copy(alpha = alpha)
            )
        ),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = w * 0.055f)
    )
}

private fun DrawScope.draw3DCalculatorIcon(
    alpha: Float,
    selected: Boolean,
    customAccent: Color?
) {
    val w = size.width
    val h = size.height
    val primaryCyan = customAccent ?: Color(0xFF00E5FF)
    val emeraldScreen = Color(0xFF69F0AE)
    val goldEqual = Color(0xFFFFD54F)
    val coralOp = Color(0xFFFF6E40)

    // 1. 3D Drop Shadow
    drawRoundRect(
        color = Color(0xFF050814).copy(alpha = 0.65f * alpha),
        topLeft = Offset(w * 0.16f, h * 0.12f),
        size = Size(w * 0.72f, h * 0.82f),
        cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
    )

    // 2. 3D Calculator Chassis
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF28375C).copy(alpha = alpha),
                Color(0xFF141E36).copy(alpha = alpha),
                Color(0xFF0B1122).copy(alpha = alpha)
            ),
            start = Offset(w * 0.14f, h * 0.08f),
            end = Offset(w * 0.86f, h * 0.90f)
        ),
        topLeft = Offset(w * 0.14f, h * 0.08f),
        size = Size(w * 0.72f, h * 0.82f),
        cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
    )

    // 3. Glowing 3D LCD Screen Visor (Cyan -> Emerald gradient)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                primaryCyan.copy(alpha = 0.85f * alpha),
                emeraldScreen.copy(alpha = 0.90f * alpha)
            )
        ),
        topLeft = Offset(w * 0.22f, h * 0.16f),
        size = Size(w * 0.56f, h * 0.20f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )

    // 4. 3D Tactile Keycaps (4 polychromatic 3D buttons)
    val keySize = Size(w * 0.24f, h * 0.17f)
    val corner = CornerRadius(w * 0.06f, w * 0.06f)

    // Top-left key (Cyan)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.9f * alpha), primaryCyan.copy(alpha = alpha))
        ),
        topLeft = Offset(w * 0.22f, h * 0.43f),
        size = keySize,
        cornerRadius = corner
    )
    // Top-right key (Coral Orange)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFCCBC).copy(alpha = alpha), coralOp.copy(alpha = alpha))
        ),
        topLeft = Offset(w * 0.54f, h * 0.43f),
        size = keySize,
        cornerRadius = corner
    )
    // Bottom-left key (Violet)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE1BEE7).copy(alpha = alpha), Color(0xFFB388FF).copy(alpha = alpha))
        ),
        topLeft = Offset(w * 0.22f, h * 0.65f),
        size = keySize,
        cornerRadius = corner
    )
    // Bottom-right "=" key (3D Gold)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF9C4).copy(alpha = alpha), goldEqual.copy(alpha = alpha), Color(0xFFFF8F00).copy(alpha = alpha))
        ),
        topLeft = Offset(w * 0.54f, h * 0.65f),
        size = keySize,
        cornerRadius = corner
    )

    // 5. Outer 3D Specular Rim
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (selected) 0.8f else 0.45f),
                primaryCyan.copy(alpha = 0.5f * alpha),
                goldEqual.copy(alpha = 0.5f * alpha)
            )
        ),
        topLeft = Offset(w * 0.14f, h * 0.08f),
        size = Size(w * 0.72f, h * 0.82f),
        cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
        style = Stroke(width = w * 0.04f)
    )
}

private fun DrawScope.draw3DColorStudioIcon(
    alpha: Float,
    selected: Boolean,
    customAccent: Color?
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.5f
    val r = w * 0.42f

    // 1. 3D Outer Chromatic Conic Ring
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFF4081).copy(alpha = alpha),
                Color(0xFFFFD740).copy(alpha = alpha),
                Color(0xFF69F0AE).copy(alpha = alpha),
                Color(0xFF00E5FF).copy(alpha = alpha),
                Color(0xFF7C4DFF).copy(alpha = alpha),
                Color(0xFFFF4081).copy(alpha = alpha)
            ),
            center = Offset(cx, cy)
        ),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = w * 0.07f)
    )

    // 2. Dark Velvet Interior Backdrop
    drawCircle(
        color = Color(0xFF10172A).copy(alpha = 0.9f * alpha),
        radius = r * 0.84f,
        center = Offset(cx, cy)
    )

    // 3. Three Overlapping 3D Volumetric Spheres (Magenta/Coral, Cyan, Solar Gold)
    val orbR = r * 0.42f
    val topAccent = customAccent ?: Color(0xFFFF4081)

    // Top Orb (Magenta / Custom)
    val topCenter = Offset(cx, cy - r * 0.24f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = alpha), topAccent.copy(alpha = alpha), topAccent.copy(alpha = 0.5f * alpha)),
            center = Offset(topCenter.x - orbR * 0.3f, topCenter.y - orbR * 0.3f),
            radius = orbR * 1.2f
        ),
        radius = orbR,
        center = topCenter
    )

    // Bottom-Left Orb (Cyan)
    val leftCenter = Offset(cx - r * 0.24f, cy + r * 0.20f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = alpha), Color(0xFF00E5FF).copy(alpha = alpha), Color(0xFF0097A7).copy(alpha = 0.6f * alpha)),
            center = Offset(leftCenter.x - orbR * 0.3f, leftCenter.y - orbR * 0.3f),
            radius = orbR * 1.2f
        ),
        radius = orbR,
        center = leftCenter
    )

    // Bottom-Right Orb (Solar Gold)
    val rightCenter = Offset(cx + r * 0.24f, cy + r * 0.20f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = alpha), Color(0xFFFFD54F).copy(alpha = alpha), Color(0xFFFF8F00).copy(alpha = 0.6f * alpha)),
            center = Offset(rightCenter.x - orbR * 0.3f, rightCenter.y - orbR * 0.3f),
            radius = orbR * 1.2f
        ),
        radius = orbR,
        center = rightCenter
    )

    // Center White Prism Core Highlight
    if (selected) {
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = r * 0.14f,
            center = Offset(cx, cy + r * 0.04f)
        )
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
