package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.domain.ColorHarmonyEngine
import com.example.domain.ColorSwatchSpec
import com.example.domain.GradientStyleType
import com.example.ui.ModuleVisualSkin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders the custom solid/linear/radial/conic/mesh skin assigned to a specific module
 * as a rich atmospheric background layer for cards, headers, watch dials, or calculator displays.
 */
@Composable
fun ModuleSkinBox(
    skin: ModuleVisualSkin,
    modifier: Modifier = Modifier,
    alphaIntensity: Float = 0.38f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        if (skin.isCustomApplied) {
            SkinGradientCanvas(
                style = skin.gradientStyle,
                stops = skin.gradientStops,
                angleDeg = skin.gradientAngleDeg,
                focalX = skin.focalX,
                focalY = skin.focalY,
                radiusFactor = skin.radiusFactor,
                alphaIntensity = alphaIntensity,
                modifier = Modifier.matchParentSize()
            )
        }
        content()
    }
}

@Composable
fun SkinGradientCanvas(
    style: GradientStyleType,
    stops: List<ColorSwatchSpec>,
    angleDeg: Int,
    focalX: Float = 0.5f,
    focalY: Float = 0.5f,
    radiusFactor: Float = 0.9f,
    alphaIntensity: Float = 0.45f,
    modifier: Modifier = Modifier
) {
    val baseBg = Color(0xFF0E1530)
    val composeColors = remember(stops, alphaIntensity) {
        val raw = if (stops.size >= 2) {
            stops.map { Color(it.rgb.r, it.rgb.g, it.rgb.b).copy(alpha = alphaIntensity) }
        } else {
            val single = stops.firstOrNull()?.let { Color(it.rgb.r, it.rgb.g, it.rgb.b) } ?: Color(0xFFF5C451)
            listOf(single.copy(alpha = alphaIntensity), Color(0xFF131B3A).copy(alpha = alphaIntensity))
        }
        raw
    }

    val sweepColors = remember(composeColors) {
        composeColors + composeColors.first()
    }

    val meshNodeColorStops = remember(stops, alphaIntensity) {
        ColorHarmonyEngine.buildMeshNodes(stops).map {
            val nodeColor = Color(it.rgb.r, it.rgb.g, it.rgb.b).copy(alpha = alphaIntensity)
            listOf(
                nodeColor,
                nodeColor.copy(alpha = alphaIntensity * 0.4f),
                Color.Transparent
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawRect(color = baseBg)

        when (style) {
            GradientStyleType.LINEAR -> {
                val rad = angleDeg * (PI / 180.0)
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                val halfDiag = sqrt(w * w + h * h) / 2f
                val cx = w / 2f
                val cy = h / 2f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = composeColors,
                        start = Offset(cx - cosA * halfDiag, cy - sinA * halfDiag),
                        end = Offset(cx + cosA * halfDiag, cy + sinA * halfDiag)
                    )
                )
            }

            GradientStyleType.RADIAL -> {
                val center = Offset(w * focalX, h * focalY)
                val radius = (max(w, h) * 0.7f * radiusFactor).coerceAtLeast(20f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = composeColors,
                        center = center,
                        radius = radius
                    )
                )
            }

            GradientStyleType.SWEEP -> {
                drawRect(
                    brush = Brush.sweepGradient(
                        colors = sweepColors,
                        center = Offset(w * focalX, h * focalY)
                    )
                )
            }

            GradientStyleType.MESH -> {
                val baseRadius = (max(w, h) * 0.72f * radiusFactor).coerceAtLeast(20f)
                for (idx in meshNodeColorStops.indices) {
                    val pt = when (idx) {
                        0 -> Offset(w * 0.15f, h * 0.18f)
                        1 -> Offset(w * 0.85f, h * 0.20f)
                        2 -> Offset(w * 0.20f, h * 0.85f)
                        3 -> Offset(w * 0.82f, h * 0.80f)
                        4 -> Offset(w * focalX, h * focalY)
                        5 -> Offset(w * (1f - focalX), h * (1f - focalY))
                        else -> Offset(w * 0.5f, h * 0.5f)
                    }
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = meshNodeColorStops[idx],
                            center = pt,
                            radius = baseRadius
                        )
                    )
                }
            }
        }
    }
}
