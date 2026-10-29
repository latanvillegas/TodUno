package com.example.domain

import androidx.compose.runtime.Immutable
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Immutable
data class RgbColor(
    val r: Int, // 0..255
    val g: Int, // 0..255
    val b: Int  // 0..255
) {
    // Precomputed once on instantiation to avoid repeated String.format allocations during UI rendering
    val hex: String = "#%02X%02X%02X".format(
        Locale.US,
        r.coerceIn(0, 255),
        g.coerceIn(0, 255),
        b.coerceIn(0, 255)
    )

    fun formatCss(): String = "rgb($r, $g, $b)"
}

@Immutable
data class HslColor(
    val h: Float, // 0f..360f
    val s: Float, // 0f..100f
    val l: Float  // 0f..100f
) {
    fun formatCss(): String = "hsl(${h.roundToInt()}°, ${s.roundToInt()}%, ${l.roundToInt()}%)"
}

@Immutable
data class CmykColor(
    val c: Int, // 0..100
    val m: Int, // 0..100
    val y: Int, // 0..100
    val k: Int  // 0..100
) {
    fun formatCss(): String = "cmyk($c%, $m%, $y%, $k%)"
}

@Immutable
data class ColorSwatchSpec(
    val label: String,
    val rgb: RgbColor,
    val hsl: HslColor,
    val cmyk: CmykColor
) {
    val hex: String get() = rgb.hex
}

@Immutable
data class ColorHarmonyGroup(
    val title: String,
    val description: String,
    val swatches: List<ColorSwatchSpec>
)

enum class GradientStyleType(val spanishTitle: String, val badge: String) {
    LINEAR("Degradado Lineal", "Lineal"),
    RADIAL("Degradado Radiante", "Radiante"),
    SWEEP("Degradado Cónico 360°", "Cónico"),
    MESH("Degradado de Malla (Mesh)", "Malla")
}

@Immutable
data class GradientPreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val stops: List<ColorSwatchSpec>
)

@Immutable
data class NamedColorFamily(
    val familyName: String,
    val colors: List<ColorSwatchSpec>
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
                title = "Análoga (3 Colores)",
                description = "Colores vecinos (±30°) que crean transiciones suaves y naturales.",
                swatches = listOf(
                    fromHsl(h - 30f, s, l, "Análogo -30°"),
                    fromHsl(h, hsl.s, hsl.l, "Base"),
                    fromHsl(h + 30f, s, l, "Análogo +30°")
                )
            ),
            ColorHarmonyGroup(
                title = "Análoga Extendida (5 Colores)",
                description = "Abanico cromático amplio (±60°) ideal para degradados sedosos.",
                swatches = listOf(
                    fromHsl(h - 60f, s, l, "-60°"),
                    fromHsl(h - 30f, s, l, "-30°"),
                    fromHsl(h, hsl.s, hsl.l, "Base"),
                    fromHsl(h + 30f, s, l, "+30°"),
                    fromHsl(h + 60f, s, l, "+60°")
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
            ),
            ColorHarmonyGroup(
                title = "Hexádica (6 Tonos)",
                description = "Seis puntos equidistantes cada 60° alrededor del círculo cromático.",
                swatches = listOf(
                    fromHsl(h, hsl.s, hsl.l, "0°"),
                    fromHsl(h + 60f, s, l, "+60°"),
                    fromHsl(h + 120f, s, l, "+120°"),
                    fromHsl(h + 180f, s, l, "+180°"),
                    fromHsl(h + 240f, s, l, "+240°"),
                    fromHsl(h + 300f, s, l, "+300°")
                )
            )
        )
    }

    /**
     * Generates dynamic multi-stop gradient stops derived from the active color's HSL + variant mode.
     */
    fun buildDynamicGradientStops(base: ColorSwatchSpec, variantIndex: Int = 0): List<ColorSwatchSpec> {
        val h = base.hsl.h
        val s = base.hsl.s.coerceAtLeast(55f)
        val l = base.hsl.l.coerceIn(30f, 72f)
        return when (variantIndex % 5) {
            0 -> listOf(
                fromHsl(h - 35f, s, (l - 14f).coerceAtLeast(14f), "Inicio"),
                base.copy(label = "Centro 1"),
                fromHsl(h + 35f, s, (l + 10f).coerceAtMost(85f), "Centro 2"),
                fromHsl(h + 75f, (s + 10f).coerceAtMost(100f), (l + 18f).coerceAtMost(90f), "Fin")
            )
            1 -> listOf(
                fromHsl(h, s, 16f, "Sombra Profunda"),
                base.copy(label = "Núcleo"),
                fromHsl(h + 180f, s, l, "Complementario"),
                fromHsl(h + 210f, s, 82f, "Destello")
            )
            2 -> listOf(
                base.copy(label = "Primario"),
                fromHsl(h + 120f, s, l, "Tríada A"),
                fromHsl(h + 240f, s, l, "Tríada B"),
                fromHsl(h + 300f, s, (l + 12f).coerceAtMost(88f), "Acento")
            )
            3 -> listOf(
                fromHsl(h - 20f, 90f, 22f, "Abismo"),
                fromHsl(h, 95f, 48f, "Medio"),
                fromHsl(h + 25f, 100f, 68f, "Brillo"),
                fromHsl(h + 50f, 100f, 88f, "Corona")
            )
            else -> listOf(
                fromHsl(h, s, 88f, "Tinte Claro"),
                fromHsl(h + 45f, s, 64f, "Transición"),
                fromHsl(h + 90f, s, 44f, "Profundo"),
                fromHsl(h + 135f, s, 20f, "Nocturno")
            )
        }
    }

    /**
     * Generates 6 organic mesh nodes for a 2D Mesh Gradient Canvas.
     */
    fun buildMeshNodes(stops: List<ColorSwatchSpec>): List<ColorSwatchSpec> {
        if (stops.isEmpty()) return emptyList()
        val first = stops.first()
        val last = stops.last()
        val mid1 = stops.getOrElse(1) { first }
        val mid2 = stops.getOrElse(2) { last }
        return listOf(
            first,
            mid1,
            mid2,
            last,
            fromHsl(first.hsl.h + 60f, first.hsl.s.coerceAtLeast(65f), 62f, "Nodo Malla 5"),
            fromHsl(last.hsl.h - 45f, last.hsl.s.coerceAtLeast(70f), 32f, "Nodo Malla 6")
        )
    }

    fun formatCssGradient(
        style: GradientStyleType,
        angleDeg: Int,
        stops: List<ColorSwatchSpec>
    ): String {
        val hexList = stops.joinToString(", ") { it.hex }
        return when (style) {
            GradientStyleType.LINEAR -> "linear-gradient(${angleDeg}deg, $hexList)"
            GradientStyleType.RADIAL -> "radial-gradient(circle at center, $hexList)"
            GradientStyleType.SWEEP -> "conic-gradient(from ${angleDeg}deg at 50% 50%, $hexList, ${stops.firstOrNull()?.hex ?: "#FFF"})"
            GradientStyleType.MESH -> {
                val nodes = buildMeshNodes(stops)
                val layers = listOf(
                    "radial-gradient(at 15% 20%, ${nodes.getOrElse(0) { stops.first() }.hex} 0px, transparent 55%)",
                    "radial-gradient(at 85% 18%, ${nodes.getOrElse(1) { stops.first() }.hex} 0px, transparent 55%)",
                    "radial-gradient(at 25% 82%, ${nodes.getOrElse(2) { stops.first() }.hex} 0px, transparent 55%)",
                    "radial-gradient(at 82% 78%, ${nodes.getOrElse(3) { stops.first() }.hex} 0px, transparent 55%)",
                    "radial-gradient(at 52% 48%, ${nodes.getOrElse(4) { stops.first() }.hex} 0px, transparent 60%)"
                )
                layers.joinToString(", ")
            }
        }
    }

    val CURATED_GRADIENT_PRESETS: List<GradientPreset> by lazy {
        listOf(
            GradientPreset(
                id = "aurora_boreal",
                name = "Aurora Boreal Ártica",
                subtitle = "Esmeralda, Cian Bioluminiscente e Índigo Profundo",
                stops = listOf(
                    fromRgb(11, 16, 38, "Cosmos"),
                    fromRgb(16, 185, 129, "Esmeralda"),
                    fromRgb(6, 182, 212, "Cian Polar"),
                    fromRgb(139, 92, 246, "Violeta Boreal")
                )
            ),
            GradientPreset(
                id = "supernova_solar",
                name = "Supernova y Corona Solar",
                subtitle = "Oro Radiante, Ámbar, Coral y Carmesí",
                stops = listOf(
                    fromRgb(255, 243, 176, "Luz Solar"),
                    fromRgb(245, 196, 81, "Oro Lunar"),
                    fromRgb(249, 115, 22, "Ámbar Ígneo"),
                    fromRgb(190, 18, 60, "Carmesí")
                )
            ),
            GradientPreset(
                id = "nebulosa_cyber",
                name = "Nebulosa Orión & Magenta",
                subtitle = "Fucsia Neón, Púrpura Real y Azul Eléctrico",
                stops = listOf(
                    fromRgb(236, 72, 153, "Rosa Neón"),
                    fromRgb(168, 85, 247, "Púrpura"),
                    fromRgb(59, 130, 246, "Azul Real"),
                    fromRgb(15, 23, 42, "Noche")
                )
            ),
            GradientPreset(
                id = "oceano_abisal",
                name = "Océano Bioluminiscente",
                subtitle = "Turquesa Tropical, Aguamarina y Azul Abisal",
                stops = listOf(
                    fromRgb(167, 243, 208, "Espuma Menta"),
                    fromRgb(45, 212, 191, "Turquesa"),
                    fromRgb(2, 132, 199, "Azul Océano"),
                    fromRgb(12, 25, 56, "Fosa Abisal")
                )
            ),
            GradientPreset(
                id = "atardecer_terracota",
                name = "Atardecer en el Desierto",
                subtitle = "Melocotón, Terracota, Malva y Violeta Nocturno",
                stops = listOf(
                    fromRgb(253, 186, 116, "Melocotón"),
                    fromRgb(244, 63, 94, "Coral Rosa"),
                    fromRgb(147, 51, 234, "Amatista"),
                    fromRgb(30, 27, 75, "Horizonte")
                )
            ),
            GradientPreset(
                id = "bosque_esmeralda",
                name = "Selva Esmeralda yLima",
                subtitle = "Lima Fresco, Jade, Esmeralda y Pino Oscuro",
                stops = listOf(
                    fromRgb(190, 242, 100, "Lima"),
                    fromRgb(52, 211, 153, "Jade"),
                    fromRgb(5, 150, 105, "Esmeralda"),
                    fromRgb(6, 78, 59, "Bosque")
                )
            ),
            GradientPreset(
                id = "metal_titanio",
                name = "Titanio y Plata Selenita",
                subtitle = "Metálico Industrial de Alto Contraste",
                stops = listOf(
                    fromRgb(248, 250, 252, "Platino"),
                    fromRgb(148, 163, 184, "Acero"),
                    fromRgb(71, 85, 105, "Titanio"),
                    fromRgb(15, 23, 42, "Obsidiana")
                )
            ),
            GradientPreset(
                id = "algodon_pastel",
                name = "Nube Pastel Holográfica",
                subtitle = "Rosa Cuarzo, Lavanda, Celeste y Menta Suave",
                stops = listOf(
                    fromRgb(251, 207, 232, "Rosa Cuarzo"),
                    fromRgb(221, 214, 254, "Lavanda"),
                    fromRgb(186, 230, 253, "Celeste"),
                    fromRgb(167, 243, 208, "Menta Pastel")
                )
            )
        )
    }

    val NAMED_COLOR_FAMILIES: List<NamedColorFamily> by lazy {
        listOf(
            NamedColorFamily(
                familyName = "Rojos, Corales y Rosas",
                colors = listOf(
                    fromRgb(239, 68, 68, "Rojo Vivo"),
                    fromRgb(220, 38, 38, "Rojo Carmesí"),
                    fromRgb(153, 27, 27, "Rojo Borgoña"),
                    fromRgb(255, 122, 107, "Coral Eclipse"),
                    fromRgb(244, 63, 94, "Rosa Rubí"),
                    fromRgb(236, 72, 153, "Rosa Magenta"),
                    fromRgb(251, 113, 133, "Salmón Rosado"),
                    fromRgb(136, 19, 55, "Vino Tinto")
                )
            ),
            NamedColorFamily(
                familyName = "Naranjas, Ámbar y Dorados",
                colors = listOf(
                    fromRgb(249, 115, 22, "Naranja Mandarina"),
                    fromRgb(234, 88, 12, "Terracota"),
                    fromRgb(245, 158, 11, "Ámbar Solar"),
                    fromRgb(245, 196, 81, "Oro Lunar"),
                    fromRgb(250, 204, 21, "Amarillo Canario"),
                    fromRgb(254, 240, 138, "Crema Vainilla"),
                    fromRgb(180, 83, 9, "Bronce Antiguo"),
                    fromRgb(120, 53, 15, "OcreTostado")
                )
            ),
            NamedColorFamily(
                familyName = "Verdes, Esmeraldas y Menta",
                colors = listOf(
                    fromRgb(132, 204, 22, "Verde Lima"),
                    fromRgb(34, 197, 94, "Verde Pradera"),
                    fromRgb(16, 185, 129, "Verde Esmeralda"),
                    fromRgb(92, 225, 166, "Menta Aurora"),
                    fromRgb(20, 184, 166, "Verde Cerceta (Teal)"),
                    fromRgb(101, 163, 13, "Verde Oliva"),
                    fromRgb(6, 95, 70, "Verde Bosque"),
                    fromRgb(167, 243, 208, "Aguamarina Suave")
                )
            ),
            NamedColorFamily(
                familyName = "Cianes, Turquesas y Azules",
                colors = listOf(
                    fromRgb(100, 210, 255, "Cian Celestial"),
                    fromRgb(6, 182, 212, "Turquesa Caribe"),
                    fromRgb(14, 165, 233, "Azul Cielo"),
                    fromRgb(59, 130, 246, "Azul Cobalto"),
                    fromRgb(37, 99, 235, "Azul Real"),
                    fromRgb(29, 78, 216, "Azul Zafiro"),
                    fromRgb(30, 58, 138, "Azul Marino"),
                    fromRgb(11, 16, 38, "Azul Medianoche")
                )
            ),
            NamedColorFamily(
                familyName = "Índigos, Violetas y Púrpuras",
                colors = listOf(
                    fromRgb(99, 102, 241, "Índigo Eléctrico"),
                    fromRgb(139, 92, 246, "Violeta Amatista"),
                    fromRgb(168, 85, 247, "Púrpura Orquídea"),
                    fromRgb(217, 70, 239, "Fucsia Imperial"),
                    fromRgb(126, 34, 206, "Morado Profundo"),
                    fromRgb(196, 181, 253, "Lavanda Suave"),
                    fromRgb(76, 29, 149, "Violeta Cosmos"),
                    fromRgb(49, 46, 129, "Índigo Abisal")
                )
            ),
            NamedColorFamily(
                familyName = "Tonos Pastel, Neón y Neutros",
                colors = listOf(
                    fromRgb(255, 0, 128, "Neón Cyber Pink"),
                    fromRgb(0, 255, 198, "Neón Ciber Menta"),
                    fromRgb(204, 255, 0, "Neón Voltio"),
                    fromRgb(248, 250, 252, "Blanco Estrella"),
                    fromRgb(203, 213, 225, "Plata Selenita"),
                    fromRgb(100, 116, 139, "Gris Pizarra"),
                    fromRgb(51, 65, 85, "Gris Grafito"),
                    fromRgb(15, 23, 42, "Negro Obsidiana")
                )
            )
        )
    }

    /**
     * Generates Tints & Shades (Luminosity variation from 94% light down to 10% dark).
     */
    fun buildLuminosityScale(hsl: HslColor): List<ColorSwatchSpec> {
        val lightnessSteps = listOf(94f, 84f, 74f, 64f, 54f, 44f, 34f, 24f, 14f, 8f)
        return lightnessSteps.map { stepL ->
            fromHsl(hsl.h, hsl.s, stepL, "L ${stepL.roundToInt()}%")
        }
    }

    /**
     * Generates Tones (Saturation variation from 100% pure to 5% muted gray).
     */
    fun buildSaturationTones(hsl: HslColor): List<ColorSwatchSpec> {
        val saturationSteps = listOf(100f, 85f, 70f, 55f, 40f, 25f, 12f, 4f)
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
