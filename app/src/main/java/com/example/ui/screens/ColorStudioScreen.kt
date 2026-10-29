package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.ColorHarmonyEngine
import com.example.domain.ColorSwatchSpec
import com.example.ui.ColorStudioSubTab
import com.example.ui.SeleniaUiState
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorStudioScreen(
    uiState: SeleniaUiState,
    onSelectSubTab: (ColorStudioSubTab) -> Unit,
    onUpdateRgb: (Int, Int, Int) -> Unit,
    onUpdateHsl: (Float, Float, Float) -> Unit,
    onUpdateCmyk: (Int, Int, Int, Int) -> Unit,
    onApplyHex: (String) -> Unit,
    onSaveFavoriteColor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedBannerText by remember { mutableStateOf<String?>(null) }
    var hexInputDraft by rememberSaveable(uiState.activeColorSwatch.hex) {
        mutableStateOf(uiState.activeColorSwatch.hex)
    }

    val active = uiState.activeColorSwatch
    val composeColor = Color(active.rgb.r, active.rgb.g, active.rgb.b)
    val whiteContrast = ColorHarmonyEngine.contrastRatioWithWhite(active.rgb)
    val blackContrast = ColorHarmonyEngine.contrastRatioWithBlack(active.rgb)
    val idealOnColor = if (whiteContrast >= blackContrast) Color.White else Color(0xFF0B1026)

    fun copyValue(label: String, text: String) {
        clipboardManager.setText(AnnotatedString(text))
        copiedBannerText = "Copiado $label: $text"
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("color_studio_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Color Preview & Instant Codes Card (HEX, RGB, CMYK, HSL)
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Live Color Swatch Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .background(composeColor)
                                .padding(16.dp)
                                .testTag("active_color_preview_banner")
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = active.label,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = idealOnColor
                                )
                                Text(
                                    text = active.hex,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = idealOnColor,
                                    modifier = Modifier.testTag("active_color_hex_title")
                                )
                            }

                            Surface(
                                onClick = onSaveFavoriteColor,
                                color = idealOnColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .testTag("save_favorite_color_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkAdd,
                                        contentDescription = "Guardar color",
                                        tint = idealOnColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Guardar",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = idealOnColor
                                    )
                                }
                            }

                            // WCAG Contrast ratio badges at bottom of banner
                            Row(
                                modifier = Modifier.align(Alignment.BottomStart),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Blanco ${"%.1f".format(Locale.US, whiteContrast)}:1",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Negro ${"%.1f".format(Locale.US, blackContrast)}:1",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // 4 Color Model Readout Chips (Tap to copy any format)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            copiedBannerText?.let { msg ->
                                Text(
                                    text = "✓ $msg",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ColorFormatValueCard(
                                    modelName = "HEX",
                                    valueText = active.hex,
                                    onClick = { copyValue("HEX", active.hex) },
                                    modifier = Modifier.weight(1f),
                                    tag = "copy_hex_card"
                                )
                                ColorFormatValueCard(
                                    modelName = "RGB",
                                    valueText = "${active.rgb.r}, ${active.rgb.g}, ${active.rgb.b}",
                                    onClick = { copyValue("RGB", active.rgb.formatCss()) },
                                    modifier = Modifier.weight(1f),
                                    tag = "copy_rgb_card"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ColorFormatValueCard(
                                    modelName = "CMYK",
                                    valueText = "${active.cmyk.c}%, ${active.cmyk.m}%, ${active.cmyk.y}%, ${active.cmyk.k}%",
                                    onClick = { copyValue("CMYK", active.cmyk.formatCss()) },
                                    modifier = Modifier.weight(1f),
                                    tag = "copy_cmyk_card"
                                )
                                ColorFormatValueCard(
                                    modelName = "HSL",
                                    valueText = "${active.hsl.h.roundToInt()}°, ${active.hsl.s.roundToInt()}%, ${active.hsl.l.roundToInt()}%",
                                    onClick = { copyValue("HSL", active.hsl.formatCss()) },
                                    modifier = Modifier.weight(1f),
                                    tag = "copy_hsl_card"
                                )
                            }
                        }
                    }
                }
            }

            // 2. Sub-Tab Selector: Ajustes (RGB/HEX/CMYK/HSL) | Combinaciones | Tonos y Sombras
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.colorStudioSubTab == ColorStudioSubTab.CONVERTER_SLIDERS,
                        onClick = { onSelectSubTab(ColorStudioSubTab.CONVERTER_SLIDERS) },
                        label = { Text("RGB • CMYK • HSL") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("color_tab_sliders")
                    )
                    FilterChip(
                        selected = uiState.colorStudioSubTab == ColorStudioSubTab.COMBINATIONS,
                        onClick = { onSelectSubTab(ColorStudioSubTab.COMBINATIONS) },
                        label = { Text("Combinaciones") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("color_tab_combinations")
                    )
                    FilterChip(
                        selected = uiState.colorStudioSubTab == ColorStudioSubTab.TONES_AND_SHADES,
                        onClick = { onSelectSubTab(ColorStudioSubTab.TONES_AND_SHADES) },
                        label = { Text("Tonos") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Gradient,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("color_tab_tones")
                    )
                }
            }

            // 3. Content based on selected sub-tab
            when (uiState.colorStudioSubTab) {
                ColorStudioSubTab.CONVERTER_SLIDERS -> {
                    // Interactive Spectrum Bar & HEX Input
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Espectro Cromático y Código HEX",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Interactive 360° Hue Spectrum Canvas Bar
                                HueSpectrumBar(
                                    currentHue = active.hsl.h,
                                    onSelectHue = { newHue ->
                                        onUpdateHsl(newHue, active.hsl.s.coerceAtLeast(35f), active.hsl.l.coerceIn(20f, 80f))
                                    }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = hexInputDraft,
                                        onValueChange = { input ->
                                            hexInputDraft = input
                                            if (input.trim().removePrefix("#").length == 6) {
                                                onApplyHex(input)
                                            }
                                        },
                                        label = { Text("Código HEX (#RRGGBB)") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hex_color_input")
                                    )
                                    Button(
                                        onClick = { onApplyHex(hexInputDraft) },
                                        modifier = Modifier.testTag("apply_hex_button")
                                    ) {
                                        Text("Aplicar")
                                    }
                                }

                                // Saved/Preset Swatches Row
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.savedColorSwatches) { swatch ->
                                        val sColor = Color(swatch.rgb.r, swatch.rgb.g, swatch.rgb.b)
                                        Surface(
                                            onClick = { onApplyHex(swatch.hex) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.testTag("preset_swatch_${swatch.hex}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(sColor)
                                                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                                )
                                                Text(
                                                    text = swatch.hex,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // RGB Sliders Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Modelo RGB (Rojo, Verde, Azul • 0–255)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                ColorChannelSlider(
                                    label = "R (Rojo)",
                                    valueText = "${active.rgb.r}",
                                    value = active.rgb.r.toFloat(),
                                    valueRange = 0f..255f,
                                    accentColor = Color(0xFFFF5C5C),
                                    onValueChange = { onUpdateRgb(it.roundToInt(), active.rgb.g, active.rgb.b) }
                                )
                                ColorChannelSlider(
                                    label = "G (Verde)",
                                    valueText = "${active.rgb.g}",
                                    value = active.rgb.g.toFloat(),
                                    valueRange = 0f..255f,
                                    accentColor = Color(0xFF4ADE80),
                                    onValueChange = { onUpdateRgb(active.rgb.r, it.roundToInt(), active.rgb.b) }
                                )
                                ColorChannelSlider(
                                    label = "B (Azul)",
                                    valueText = "${active.rgb.b}",
                                    value = active.rgb.b.toFloat(),
                                    valueRange = 0f..255f,
                                    accentColor = Color(0xFF60A5FA),
                                    onValueChange = { onUpdateRgb(active.rgb.r, active.rgb.g, it.roundToInt()) }
                                )
                            }
                        }
                    }

                    // HSL Sliders Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Modelo HSL (Matiz, Saturación, Luminosidad)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                ColorChannelSlider(
                                    label = "H (Matiz)",
                                    valueText = "${active.hsl.h.roundToInt()}°",
                                    value = active.hsl.h,
                                    valueRange = 0f..360f,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    onValueChange = { onUpdateHsl(it, active.hsl.s, active.hsl.l) }
                                )
                                ColorChannelSlider(
                                    label = "S (Saturación)",
                                    valueText = "${active.hsl.s.roundToInt()}%",
                                    value = active.hsl.s,
                                    valueRange = 0f..100f,
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    onValueChange = { onUpdateHsl(active.hsl.h, it, active.hsl.l) }
                                )
                                ColorChannelSlider(
                                    label = "L (Luminosidad)",
                                    valueText = "${active.hsl.l.roundToInt()}%",
                                    value = active.hsl.l,
                                    valueRange = 0f..100f,
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    onValueChange = { onUpdateHsl(active.hsl.h, active.hsl.s, it) }
                                )
                            }
                        }
                    }

                    // CMYK Sliders Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Modelo CMYK (Impresión Cuatricromía • 0–100%)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                ColorChannelSlider(
                                    label = "C (Cian)",
                                    valueText = "${active.cmyk.c}%",
                                    value = active.cmyk.c.toFloat(),
                                    valueRange = 0f..100f,
                                    accentColor = Color(0xFF00E5FF),
                                    onValueChange = {
                                        onUpdateCmyk(it.roundToInt(), active.cmyk.m, active.cmyk.y, active.cmyk.k)
                                    }
                                )
                                ColorChannelSlider(
                                    label = "M (Magenta)",
                                    valueText = "${active.cmyk.m}%",
                                    value = active.cmyk.m.toFloat(),
                                    valueRange = 0f..100f,
                                    accentColor = Color(0xFFFF4081),
                                    onValueChange = {
                                        onUpdateCmyk(active.cmyk.c, it.roundToInt(), active.cmyk.y, active.cmyk.k)
                                    }
                                )
                                ColorChannelSlider(
                                    label = "Y (Amarillo)",
                                    valueText = "${active.cmyk.y}%",
                                    value = active.cmyk.y.toFloat(),
                                    valueRange = 0f..100f,
                                    accentColor = Color(0xFFFFD740),
                                    onValueChange = {
                                        onUpdateCmyk(active.cmyk.c, active.cmyk.m, it.roundToInt(), active.cmyk.k)
                                    }
                                )
                                ColorChannelSlider(
                                    label = "K (Negro / Key)",
                                    valueText = "${active.cmyk.k}%",
                                    value = active.cmyk.k.toFloat(),
                                    valueRange = 0f..100f,
                                    accentColor = Color(0xFFB0BEC5),
                                    onValueChange = {
                                        onUpdateCmyk(active.cmyk.c, active.cmyk.m, active.cmyk.y, it.roundToInt())
                                    }
                                )
                            }
                        }
                    }
                }

                ColorStudioSubTab.COMBINATIONS -> {
                    val harmonies = ColorHarmonyEngine.buildColorHarmonies(active.hsl)
                    items(harmonies) { group ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Armonía ${group.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = group.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Interactive palette bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    group.swatches.forEach { swatch ->
                                        HarmonySwatchCard(
                                            swatch = swatch,
                                            onSelect = { onApplyHex(swatch.hex) },
                                            onCopy = { copyValue(group.title, swatch.hex) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ColorStudioSubTab.TONES_AND_SHADES -> {
                    val luminosityScale = ColorHarmonyEngine.buildLuminosityScale(active.hsl)
                    val saturationTones = ColorHarmonyEngine.buildSaturationTones(active.hsl)

                    item {
                        TonalScaleCard(
                            title = "Matices y Sombras (Escala de Luminosidad L%)",
                            subtitle = "Variación desde tintes claros pastel (92%) hasta sombras profundas (12%). Toca cualquier tono para seleccionarlo.",
                            swatches = luminosityScale,
                            onSelectSwatch = { onApplyHex(it.hex) },
                            onCopySwatch = { copyValue("Tono", "${it.hex} (${it.rgb.formatCss()})") }
                        )
                    }

                    item {
                        TonalScaleCard(
                            title = "Tonos de Saturación (Escala S%)",
                            subtitle = "Variación desde color puro vibrante (100%) hasta gris apagado (10%).",
                            swatches = saturationTones,
                            onSelectSwatch = { onApplyHex(it.hex) },
                            onCopySwatch = { copyValue("Saturación", "${it.hex} (${it.hsl.formatCss()})") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorFormatValueCard(
    modelName: String,
    valueText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copiar $modelName",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun HueSpectrumBar(
    currentHue: Float,
    onSelectHue: (Float) -> Unit
) {
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000)
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width).coerceIn(0f, 1f)
                    onSelectHue(frac * 360f)
                }
            }
            .testTag("hue_spectrum_bar")
    ) {
        drawRect(
            brush = Brush.horizontalGradient(rainbowColors)
        )
        val markerX = ((currentHue / 360f).coerceIn(0f, 1f)) * size.width
        drawCircle(
            color = Color.White,
            radius = size.height * 0.42f,
            center = Offset(markerX, size.height / 2f)
        )
        drawCircle(
            color = Color.Black,
            radius = size.height * 0.22f,
            center = Offset(markerX, size.height / 2f)
        )
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            )
        )
    }
}

@Composable
private fun HarmonySwatchCard(
    swatch: ColorSwatchSpec,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(swatch.rgb.r, swatch.rgb.g, swatch.rgb.b)
    val textColor = if (ColorHarmonyEngine.contrastRatioWithWhite(swatch.rgb) >= 3.0) {
        Color.White
    } else {
        Color(0xFF0B1026)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onSelect)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(bgColor)
        ) {
            Text(
                text = swatch.hex,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopy)
                .padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Text(
                text = swatch.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = "RGB(${swatch.rgb.r},${swatch.rgb.g},${swatch.rgb.b})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TonalScaleCard(
    title: String,
    subtitle: String,
    swatches: List<ColorSwatchSpec>,
    onSelectSwatch: (ColorSwatchSpec) -> Unit,
    onCopySwatch: (ColorSwatchSpec) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                swatches.forEach { swatch ->
                    val c = Color(swatch.rgb.r, swatch.rgb.g, swatch.rgb.b)
                    val onC = if (ColorHarmonyEngine.contrastRatioWithWhite(swatch.rgb) >= 3.0) {
                        Color.White
                    } else {
                        Color(0xFF0B1026)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c)
                            .clickable { onSelectSwatch(swatch) }
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${swatch.label}  •  ${swatch.hex}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = onC
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "RGB(${swatch.rgb.r}, ${swatch.rgb.g}, ${swatch.rgb.b})  •  CMYK(${swatch.cmyk.c}%,${swatch.cmyk.m}%,${swatch.cmyk.y}%,${swatch.cmyk.k}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = onC.copy(alpha = 0.85f)
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar tono",
                                tint = onC,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onCopySwatch(swatch) }
                            )
                        }
                    }
                }
            }
        }
    }
}
