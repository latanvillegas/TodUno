package com.example.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class RgbColor(
    val r: Int, // 0..255
    val g: Int, // 0..255
    val b: Int  // 0..255
) {
    val hex: String
        get() = "#%02X%02X%02X".format(
            Locale.US,
            r.coerceIn(0, 255),
            g.coerceIn(0, 255),
            b.coerceIn(0, 255)
        )

    fun formatCss(): String = "rgb($r, $g, $b)"
}

data class HslColor(
    val h: Float, // 0f..360f
    val s: Float, // 0f..100f
    val l: Float  // 0f..100f
) {
    fun formatCss(): String = "hsl(${h.roundToInt()}°, ${s.roundToInt()}%, ${l.roundToInt()}%)"
}

data class CmykColor(
    val c: Int, // 0..100
    val m: Int, // 0..100
    val y: Int, // 0..100
    val k: Int  // 0..100
) {
    fun formatCss(): String = "cmyk($c%, $m%, $y%, $k%)"
}

data class ColorSwatchSpec(
    val label: String,
    val rgb: RgbColor,
    val hsl: HslColor,
    val cmyk: CmykColor
) {
    val hex: String get() = rgb.hex
}

data class ColorHarmonyGroup(
    val title: String,
    val description: String,
    val swatches: List<ColorSwatchSpec>
)

object ColorHarmonyEngine {

    fun fromRgb(r: Int, g: Int, b: Int, label: String = ""): ColorSwatchSpec {
        val cleanR = r.coerceIn(0, 255)
        val cleanG = g.coerceIn(0, 255)
        val cleanB = b.coerceIn(0, 255)
        val rgb = RgbColor(cleanR, cleanG, cleanB)
        val hsl = rgbToHsl(cleanR, cleanG, cleanB)
        val cmyk = rgbToCmyk(cleanR, cleanG, cleanB)
        val resolvedLabel = label.ifBlank { getSpanishColorName(hsl.h, hsl.s, hsl.l) }
        return ColorSwatchSpec(
            label = resolvedLabel,
            rgb = rgb,
            hsl = hsl,
            cmyk = cmyk
        )
    }

    fun fromHsl(h: Float, s: Float, l: Float, label: String = ""): ColorSwatchSpec {
        val normH = ((h % 360f) + 360f) % 360f
        val normS = s.coerceIn(0f, 100f)
        val normL = l.coerceIn(0f, 100f)
        val rgb = hslToRgb(normH, normS, normL)
        val cmyk = rgbToCmyk(rgb.r, rgb.g, rgb.b)
        val resolvedLabel = label.ifBlank { getSpanishColorName(normH, normS, normL) }
        return ColorSwatchSpec(
            label = resolvedLabel,
            rgb = rgb,
            hsl = HslColor(normH, normS, normL),
            cmyk = cmyk
        )
    }

    fun fromCmyk(c: Int, m: Int, y: Int, k: Int, label: String = ""): ColorSwatchSpec {
        val normC = c.coerceIn(0, 100)
        val normM = m.coerceIn(0, 100)
        val normY = y.coerceIn(0, 100)
        val normK = k.coerceIn(0, 100)
        val rgb = cmykToRgb(normC, normM, normY, normK)
        val hsl = rgbToHsl(rgb.r, rgb.g, rgb.b)
        val resolvedLabel = label.ifBlank { getSpanishColorName(hsl.h, hsl.s, hsl.l) }
        return ColorSwatchSpec(
            label = resolvedLabel,
            rgb = rgb,
            hsl = hsl,
            cmyk = CmykColor(normC, normM, normY, normK)
        )
    }

    fun parseHexToRgb(hexInput: String): RgbColor? {
        val cleaned = hexInput.trim().removePrefix("#").uppercase(Locale.US)
        val expanded = when (cleaned.length) {
            3 -> buildString {
                cleaned.forEach { ch ->
                    append(ch)
                    append(ch)
                }
            }
            6 -> cleaned
            else -> return null
        }
        return runCatching {
            val r = expanded.substring(0, 2).toInt(16)
            val g = expanded.substring(2, 4).toInt(16)
            val b = expanded.substring(4, 6).toInt(16)
            RgbColor(r, g, b)
        }.getOrNull()
    }

    fun rgbToHsl(r: Int, g: Int, b: Int): HslColor {
        val rf = r.coerceIn(0, 255) / 255f
        val gf = g.coerceIn(0, 255) / 255f
        val bf = b.coerceIn(0, 255) / 255f

        val maxVal = max(rf, max(gf, bf))
        val minVal = min(rf, min(gf, bf))
        val delta = maxVal - minVal

        var h = 0f
        val l = (maxVal + minVal) / 2f
        val s = if (delta == 0f) {
            0f
        } else {
            delta / (1f - abs(2f * l - 1f))
        }

        if (delta != 0f) {
            h = when (maxVal) {
                rf -> 60f * (((gf - bf) / delta) % 6f)
                gf -> 60f * (((bf - rf) / delta) + 2f)
                else -> 60f * (((rf - gf) / delta) + 4f)
            }
            if (h < 0f) h += 360f
        }

        return HslColor(
            h = h.coerceIn(0f, 360f),
            s = (s * 100f).coerceIn(0f, 100f),
            l = (l * 100f).coerceIn(0f, 100f)
        )
    }

    fun hslToRgb(h: Float, s: Float, l: Float): RgbColor {
        val normH = ((h % 360f) + 360f) % 360f
        val normS = (s / 100f).coerceIn(0f, 1f)
        val normL = (l / 100f).coerceIn(0f, 1f)

        val c = (1f - abs(2f * normL - 1f)) * normS
        val x = c * (1f - abs(((normH / 60f) % 2f) - 1f))
        val m = normL - c / 2f

        val (r1, g1, b1) = when {
            normH < 60f -> Triple(c, x, 0f)
            normH < 120f -> Triple(x, c, 0f)
            normH < 180f -> Triple(0f, c, x)
            normH < 240f -> Triple(0f, x, c)
            normH < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return RgbColor(
            r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255),
            g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255),
            b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        )
    }

    fun rgbToCmyk(r: Int, g: Int, b: Int): CmykColor {
        val rf = r.coerceIn(0, 255) / 255f
        val gf = g.coerceIn(0, 255) / 255f
        val bf = b.coerceIn(0, 255) / 255f

        val k = 1f - max(rf, max(gf, bf))
        if (k >= 0.999f) {
            return CmykColor(0, 0, 0, 100)
        }
        val c = ((1f - rf - k) / (1f - k) * 100f).roundToInt().coerceIn(0, 100)
        val m = ((1f - gf - k) / (1f - k) * 100f).roundToInt().coerceIn(0, 100)
        val y = ((1f - bf - k) / (1f - k) * 100f).roundToInt().coerceIn(0, 100)
        val kPct = (k * 100f).roundToInt().coerceIn(0, 100)
        return CmykColor(c, m, y, kPct)
    }

    fun cmykToRgb(c: Int, m: Int, y: Int, k: Int): RgbColor {
        val cf = c.coerceIn(0, 100) / 100f
        val mf = m.coerceIn(0, 100) / 100f
        val yf = y.coerceIn(0, 100) / 100f
        val kf = k.coerceIn(0, 100) / 100f

        val r = (255f * (1f - cf) * (1f - kf)).roundToInt().coerceIn(0, 255)
        val g = (255f * (1f - mf) * (1f - kf)).roundToInt().coerceIn(0, 255)
        val b = (255f * (1f - yf) * (1f - kf)).roundToInt().coerceIn(0, 255)
        return RgbColor(r, g, b)
    }

    fun buildColorHarmonies(hsl: HslColor): List<ColorHarmonyGroup> {
        val h = hsl.h
        val s = hsl.s.coerceAtLeast(25f)
        val l = hsl.l.coerceIn(25f, 75f)

        return listOf(
            ColorHarmonyGroup(
                title = "Complementaria",
                description = "Opuestos en el círculo cromático (180°) para máximo contraste visual.",
                swatches = listOf(
                    fromHsl(h, hsl.s, hsl.l, "Base"),
                    fromHsl(h + 180f, s, l, "Complementario")
                )
            ),
            ColorHarmonyGroup(
                title = "Análoga",
                description = "Colores vecinos (±30°) que crean transiciones suaves y naturales.",
                swatches = listOf(
                    fromHsl(h - 30f, s, l, "Análogo -30°"),
                    fromHsl(h, hsl.s, hsl.l, "Base"),
                    fromHsl(h + 30f, s, l, "Análogo +30°")
                )
            ),
            ColorHarmonyGroup(
                title = "Triádica",
                description = "Tres tonos equidistantes (120°) con equilibrio vibrante.",
                swatches = listOf(
                    fromHsl(h, hsl.s, hsl.l, "Base"),
                    fromHsl(h + 120f, s, l, "Tríada +120°"),
                    fromHsl(h + 240f, s, l, "Tríada +240°")
                )
            ),
            ColorHarmonyGroup(
                title = "Complementaria Dividida",
                description = "El color base junto a los dos adyacentes de su opuesto (+150° y +210°).",
                swatches = listOf(
                    fromHsl(h, hsl.s, hsl.l, "Base"),
                    fromHsl(h + 150f, s, l, "Dividido +150°"),
                    fromHsl(h + 210f, s, l, "Dividido +210°")
                )
            ),
            ColorHarmonyGroup(
                title = "Tetrádica (Cuadrada)",
                description = "Cuatro colores separados por 90° para paletas ricas y dinámicas.",
                swatches = listOf(
                    fromHsl(h, hsl.s, hsl.l, "Base"),
                    fromHsl(h + 90f, s, l, "+90°"),
                    fromHsl(h + 180f, s, l, "+180°"),
                    fromHsl(h + 270f, s, l, "+270°")
                )
            )
        )
    }

    /**
     * Generates Tints & Shades (Luminosity variation from 92% light down to 12% dark).
     */
    fun buildLuminosityScale(hsl: HslColor): List<ColorSwatchSpec> {
        val lightnessSteps = listOf(92f, 82f, 72f, 62f, 52f, 42f, 32f, 22f, 12f)
        return lightnessSteps.map { stepL ->
            fromHsl(hsl.h, hsl.s, stepL, "L ${stepL.roundToInt()}%")
        }
    }

    /**
     * Generates Tones (Saturation variation from 100% pure to 10% muted gray).
     */
    fun buildSaturationTones(hsl: HslColor): List<ColorSwatchSpec> {
        val saturationSteps = listOf(100f, 85f, 70f, 55f, 40f, 25f, 10f)
        val safeL = hsl.l.coerceIn(25f, 75f)
        return saturationSteps.map { stepS ->
            fromHsl(hsl.h, stepS, safeL, "S ${stepS.roundToInt()}%")
        }
    }

    fun getSpanishColorName(h: Float, s: Float, l: Float): String {
        if (l <= 8f) return "Negro Ónix"
        if (l >= 94f) return "Blanco Estrella"
        if (s <= 10f) {
            return when {
                l < 30f -> "Gris Carbón"
                l < 65f -> "Gris Pizarra"
                else -> "Plata Lunar"
            }
        }
        val hueName = when {
            h < 15f -> "Rojo Carmesí"
            h < 40f -> "Naranja Ámbar"
            h < 65f -> "Oro Solar"
            h < 150f -> "Verde Esmeralda"
            h < 195f -> "Cian Turquesa"
            h < 250f -> "Azul Cobalto"
            h < 290f -> "Índigo Celestial"
            h < 335f -> "Magenta Púrpura"
            else -> "Rojo Rubí"
        }
        val toneSuffix = when {
            l < 30f -> "Oscuro"
            l > 75f -> "Pastel"
            else -> ""
        }
        return if (toneSuffix.isEmpty()) hueName else "$hueName $toneSuffix"
    }

    fun relativeLuminance(rgb: RgbColor): Double {
        fun channelLum(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channelLum(rgb.r) + 0.7152 * channelLum(rgb.g) + 0.0722 * channelLum(rgb.b)
    }

    fun contrastRatioWithWhite(rgb: RgbColor): Double {
        val lum = relativeLuminance(rgb)
        return (1.0 + 0.05) / (lum + 0.05)
    }

    fun contrastRatioWithBlack(rgb: RgbColor): Double {
        val lum = relativeLuminance(rgb)
        return (lum + 0.05) / (0.0 + 0.05)
    }
}
