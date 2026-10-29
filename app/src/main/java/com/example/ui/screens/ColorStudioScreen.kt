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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import com.example.ui.components.iconVector
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ColorHarmonyEngine
import com.example.domain.ColorSwatchSpec
import com.example.domain.GradientPreset
import com.example.domain.GradientStyleType
import com.example.ui.ColorStudioSubTab
import com.example.ui.ModuleThemeTarget
import com.example.ui.SeleniaUiState
import com.example.ui.components.SkinGradientCanvas
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    onSetGradientStyle: (GradientStyleType) -> Unit,
    onSetGradientAngle: (Int) -> Unit,
    onSetRadialFocalAndRadius: (Float, Float, Float) -> Unit,
    onCycleGradientVariant: () -> Unit,
    onApplyGradientPreset: (List<ColorSwatchSpec>) -> Unit,
    onSelectThemeTarget: (ModuleThemeTarget) -> Unit,
    onApplySkinToTarget: (ModuleThemeTarget) -> Unit,
    onResetSkinForTarget: (ModuleThemeTarget) -> Unit,
    onNavigateToModule: (ModuleThemeTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedBannerText by remember { mutableStateOf<String?>(null) }
    var hexInputDraft by rememberSaveable(uiState.activeColorSwatch.hex) {
        mutableStateOf(uiState.activeColorSwatch.hex)
    }

    val active = uiState.activeColorSwatch
    val composeColor = remember(active.rgb) { Color(active.rgb.r, active.rgb.g, active.rgb.b) }
    val whiteContrast = remember(active.rgb) { ColorHarmonyEngine.contrastRatioWithWhite(active.rgb) }
    val blackContrast = remember(active.rgb) { ColorHarmonyEngine.contrastRatioWithBlack(active.rgb) }
    val idealOnColor = if (whiteContrast >= blackContrast) Color.White else Color(0xFF0B1026)

    // Memoize harmonies and tonal scales by active.hsl so scrolling the LazyColumn never recomputes them
    val memoizedHarmonies = remember(active.hsl) {
        ColorHarmonyEngine.buildColorHarmonies(active.hsl)
    }
    val memoizedLuminosityScale = remember(active.hsl) {
        ColorHarmonyEngine.buildLuminosityScale(active.hsl)
    }
    val memoizedSaturationTones = remember(active.hsl) {
        ColorHarmonyEngine.buildSaturationTones(active.hsl)
    }

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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(126.dp)
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
                                    color = MaterialTheme.colorScheme.tertiary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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

            // 1.5. Interactive "Aplicar Color y Degradado a la Interfaz por Módulo" Card
            item {
                ModuleSkinApplicationCard(
                    uiState = uiState,
                    onSelectTarget = onSelectThemeTarget,
                    onApplySkin = { target ->
                        onApplySkinToTarget(target)
                        copiedBannerText = "Tema y degradado ${uiState.gradientStyle.spanishTitle} (${active.hex}) aplicados a ${target.spanishTitle}"
                    },
                    onResetSkin = { target ->
                        onResetSkinForTarget(target)
                        copiedBannerText = "Tema original restaurado en ${target.spanishTitle}"
                    },
                    onNavigateToModule = onNavigateToModule
                )
            }

            // 2. 5-Section Sub-Navigation Strip:
            // Conversor | Degradados y Malla | Combinaciones | Tonos | Catálogo (+48 Colores)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
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
                            modifier = Modifier.testTag("color_tab_sliders")
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.colorStudioSubTab == ColorStudioSubTab.GRADIENTS_AND_MESH,
                            onClick = { onSelectSubTab(ColorStudioSubTab.GRADIENTS_AND_MESH) },
                            label = { Text("Degradados y Malla") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("color_tab_gradients")
                        )
                    }
                    item {
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
                            modifier = Modifier.testTag("color_tab_combinations")
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.colorStudioSubTab == ColorStudioSubTab.TONES_AND_SHADES,
                            onClick = { onSelectSubTab(ColorStudioSubTab.TONES_AND_SHADES) },
                            label = { Text("Tonos y Sombras") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Gradient,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("color_tab_tones")
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.colorStudioSubTab == ColorStudioSubTab.COLOR_CATALOG,
                            onClick = { onSelectSubTab(ColorStudioSubTab.COLOR_CATALOG) },
                            label = { Text("Catálogo +48 Colores") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ColorLens,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("color_tab_catalog")
                        )
                    }
                }
            }

            // 3. Content based on selected sub-tab
            when (uiState.colorStudioSubTab) {
                ColorStudioSubTab.CONVERTER_SLIDERS -> {
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

                                HueSpectrumBar(
                                    currentHue = active.hsl.h,
                                    onSelectHue = { newHue ->
                                        onUpdateHsl(
                                            newHue,
                                            active.hsl.s.coerceAtLeast(35f),
                                            active.hsl.l.coerceIn(20f, 80f)
                                        )
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

                ColorStudioSubTab.GRADIENTS_AND_MESH -> {
                    item {
                        GradientAndMeshLabCard(
                            uiState = uiState,
                            onSetGradientStyle = onSetGradientStyle,
                            onSetGradientAngle = onSetGradientAngle,
                            onSetRadialFocalAndRadius = onSetRadialFocalAndRadius,
                            onCycleGradientVariant = onCycleGradientVariant,
                            onSelectStopColor = { onApplyHex(it.hex) },
                            onCopyCss = { css -> copyValue("CSS Degradado", css) }
                        )
                    }

                    // Curated Gradients & Mesh Gallery
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
                                    text = "Galería de Degradados Lineales, Radiantes y Malla",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Toca cualquier colección para cargarla en el lienzo de degradados o copia sus códigos HEX.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                ColorHarmonyEngine.CURATED_GRADIENT_PRESETS.forEach { preset ->
                                    CuratedGradientPresetRow(
                                        preset = preset,
                                        activeStyle = uiState.gradientStyle,
                                        onApplyPreset = { onApplyGradientPreset(preset.stops) },
                                        onCopyPreset = {
                                            val hexes = preset.stops.joinToString(" → ") { it.hex }
                                            copyValue(preset.name, hexes)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                ColorStudioSubTab.COMBINATIONS -> {
                    items(
                        items = memoizedHarmonies,
                        key = { it.title }
                    ) { group ->
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    item(key = "luminosity_scale_card") {
                        TonalScaleCard(
                            title = "Matices y Sombras (Escala de Luminosidad L%)",
                            subtitle = "10 variaciones desde tintes claros pastel (94%) hasta sombras profundas (8%). Toca cualquier tono para seleccionarlo.",
                            swatches = memoizedLuminosityScale,
                            onSelectSwatch = { onApplyHex(it.hex) },
                            onCopySwatch = { copyValue("Tono", "${it.hex} (${it.rgb.formatCss()})") }
                        )
                    }

                    item(key = "saturation_tones_card") {
                        TonalScaleCard(
                            title = "Tonos de Saturación (Escala S%)",
                            subtitle = "8 variaciones desde color puro vibrante (100%) hasta gris neutro (4%).",
                            swatches = memoizedSaturationTones,
                            onSelectSwatch = { onApplyHex(it.hex) },
                            onCopySwatch = { copyValue("Saturación", "${it.hex} (${it.hsl.formatCss()})") }
                        )
                    }
                }

                ColorStudioSubTab.COLOR_CATALOG -> {
                    items(
                        items = ColorHarmonyEngine.NAMED_COLOR_FAMILIES,
                        key = { it.familyName }
                    ) { family ->
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
                                    text = family.familyName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    family.colors.chunked(2).forEach { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pair.forEach { swatch ->
                                                CatalogColorItemCard(
                                                    swatch = swatch,
                                                    onSelect = { onApplyHex(swatch.hex) },
                                                    onCopy = {
                                                        copyValue(
                                                            swatch.label,
                                                            "${swatch.hex} | ${swatch.rgb.formatCss()} | ${swatch.cmyk.formatCss()}"
                                                        )
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (pair.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GradientAndMeshLabCard(
    uiState: SeleniaUiState,
    onSetGradientStyle: (GradientStyleType) -> Unit,
    onSetGradientAngle: (Int) -> Unit,
    onSetRadialFocalAndRadius: (Float, Float, Float) -> Unit,
    onCycleGradientVariant: () -> Unit,
    onSelectStopColor: (ColorSwatchSpec) -> Unit,
    onCopyCss: (String) -> Unit
) {
    val stops = uiState.activeGradientStops
    val cssCode = ColorHarmonyEngine.formatCssGradient(
        style = uiState.gradientStyle,
        angleDeg = uiState.gradientAngleDeg,
        stops = stops
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.gradientStyle.spanishTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lineal, Radiante (Radial), Cónico 360° y Malla Multicapa (Mesh)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onCycleGradientVariant,
                    modifier = Modifier.testTag("cycle_gradient_variant_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Variar")
                }
            }

            // 4 Gradient Style Selector Chips: Lineal | Radiante | Cónico | Malla
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GradientStyleType.entries.forEach { style ->
                    FilterChip(
                        selected = uiState.gradientStyle == style,
                        onClick = { onSetGradientStyle(style) },
                        label = { Text(style.badge) },
                        leadingIcon = if (style == GradientStyleType.MESH) {
                            {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("gradient_style_${style.name.lowercase(Locale.US)}")
                    )
                }
            }

            // Interactive Live Gradient / Mesh Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .pointerInput(uiState.gradientStyle, uiState.radialRadiusFactor) {
                        detectTapGestures { tapOffset ->
                            if (uiState.gradientStyle == GradientStyleType.RADIAL ||
                                uiState.gradientStyle == GradientStyleType.MESH
                            ) {
                                val nx = (tapOffset.x / size.width).coerceIn(0.1f, 0.9f)
                                val ny = (tapOffset.y / size.height).coerceIn(0.1f, 0.9f)
                                onSetRadialFocalAndRadius(nx, ny, uiState.radialRadiusFactor)
                            }
                        }
                    }
                    .testTag("gradient_preview_canvas")
            ) {
                GradientRendererCanvas(
                    style = uiState.gradientStyle,
                    stops = stops,
                    angleDeg = uiState.gradientAngleDeg,
                    focalX = uiState.radialFocalX,
                    focalY = uiState.radialFocalY,
                    radiusFactor = uiState.radialRadiusFactor
                )

                // Overlay badge with hint
                Surface(
                    color = Color.Black.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = when (uiState.gradientStyle) {
                            GradientStyleType.LINEAR -> "Ángulo ${uiState.gradientAngleDeg}°"
                            GradientStyleType.RADIAL -> "Toca el lienzo para mover el foco radiante"
                            GradientStyleType.SWEEP -> "Barrido Cónico 360°"
                            GradientStyleType.MESH -> "Malla de 6 nodos (toca para desplazar foco)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Controls for Angle (Linear) or Radius (Radial/Mesh)
            if (uiState.gradientStyle == GradientStyleType.LINEAR) {
                ColorChannelSlider(
                    label = "Ángulo del Degradado Lineal",
                    valueText = "${uiState.gradientAngleDeg}°",
                    value = uiState.gradientAngleDeg.toFloat(),
                    valueRange = 0f..360f,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onValueChange = { onSetGradientAngle(it.roundToInt()) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 45, 90, 135, 180, 270).forEach { presetAngle ->
                        AssistChip(
                            onClick = { onSetGradientAngle(presetAngle) },
                            label = { Text("${presetAngle}°") }
                        )
                    }
                }
            } else if (uiState.gradientStyle == GradientStyleType.RADIAL ||
                uiState.gradientStyle == GradientStyleType.MESH
            ) {
                ColorChannelSlider(
                    label = "Expansión del Radio Radiante / Malla",
                    valueText = "${(uiState.radialRadiusFactor * 100).roundToInt()}%",
                    value = uiState.radialRadiusFactor,
                    valueRange = 0.35f..1.55f,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onValueChange = {
                        onSetRadialFocalAndRadius(uiState.radialFocalX, uiState.radialFocalY, it)
                    }
                )
            }

            // Color Stops Row (4 stops with HEX, RGB, CMYK)
            Text(
                text = "Nodos de Color del Degradado (${stops.size} paradas)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stops.forEach { stop ->
                    HarmonySwatchCard(
                        swatch = stop,
                        onSelect = { onSelectStopColor(stop) },
                        onCopy = { onCopyCss(stop.hex) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Copy CSS Gradient Button
            Surface(
                onClick = { onCopyCss(cssCode) },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("copy_gradient_css_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Código CSS / Diseño (Toca para copiar)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = cssCode,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar CSS",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GradientRendererCanvas(
    style: GradientStyleType,
    stops: List<ColorSwatchSpec>,
    angleDeg: Int,
    focalX: Float,
    focalY: Float,
    radiusFactor: Float
) {
    val composeColors = remember(stops) {
        if (stops.size >= 2) {
            stops.map { Color(it.rgb.r, it.rgb.g, it.rgb.b) }
        } else {
            listOf(Color(0xFFF5C451), Color(0xFF64D2FF))
        }
    }

    val meshNodes = remember(stops) {
        ColorHarmonyEngine.buildMeshNodes(stops).map {
            Color(it.rgb.r, it.rgb.g, it.rgb.b)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        when (style) {
            GradientStyleType.LINEAR -> {
                val rad = angleDeg * (PI / 180.0)
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                val halfDiag = kotlin.math.sqrt(w * w + h * h) / 2f
                val cx = w / 2f
                val cy = h / 2f
                val start = Offset(cx - cosA * halfDiag, cy - sinA * halfDiag)
                val end = Offset(cx + cosA * halfDiag, cy + sinA * halfDiag)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = composeColors,
                        start = start,
                        end = end
                    )
                )
            }

            GradientStyleType.RADIAL -> {
                val center = Offset(w * focalX, h * focalY)
                val radius = (kotlin.math.max(w, h) * 0.65f * radiusFactor).coerceAtLeast(20f)
                drawRect(color = composeColors.last())
                drawRect(
                    brush = Brush.radialGradient(
                        colors = composeColors,
                        center = center,
                        radius = radius
                    )
                )
            }

            GradientStyleType.SWEEP -> {
                val sweepColors = composeColors + composeColors.first()
                drawRect(
                    brush = Brush.sweepGradient(
                        colors = sweepColors,
                        center = Offset(w * focalX, h * focalY)
                    )
                )
            }

            GradientStyleType.MESH -> {
                // Multi-point organic 2D Mesh Gradient using layered radial fields
                drawRect(color = meshNodes.lastOrNull() ?: Color(0xFF0B1026))

                val anchorPoints = listOf(
                    Offset(w * 0.15f, h * 0.18f),
                    Offset(w * 0.85f, h * 0.20f),
                    Offset(w * 0.20f, h * 0.85f),
                    Offset(w * 0.82f, h * 0.80f),
                    Offset(w * focalX, h * focalY),
                    Offset(w * (1f - focalX), h * (1f - focalY))
                )

                val baseRadius = kotlin.math.max(w, h) * 0.68f * radiusFactor
                meshNodes.forEachIndexed { idx, nodeColor ->
                    val pt = anchorPoints.getOrElse(idx) { Offset(w * 0.5f, h * 0.5f) }
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                nodeColor.copy(alpha = 0.88f),
                                nodeColor.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = pt,
                            radius = (baseRadius * if (idx == 4) 0.9f else 0.78f).coerceAtLeast(20f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CuratedGradientPresetRow(
    preset: GradientPreset,
    activeStyle: GradientStyleType,
    onApplyPreset: () -> Unit,
    onCopyPreset: () -> Unit
) {
    Surface(
        onClick = onApplyPreset,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gradient_preset_${preset.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                GradientRendererCanvas(
                    style = activeStyle,
                    stops = preset.stops,
                    angleDeg = 135,
                    focalX = 0.5f,
                    focalY = 0.5f,
                    radiusFactor = 0.9f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = preset.stops.joinToString(" • ") { it.hex },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar paleta",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onCopyPreset() }
                )
            }
        }
    }
}

@Composable
private fun CatalogColorItemCard(
    swatch: ColorSwatchSpec,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Color(swatch.rgb.r, swatch.rgb.g, swatch.rgb.b)
    Surface(
        onClick = onSelect,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = swatch.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${swatch.hex} • RGB(${swatch.rgb.r},${swatch.rgb.g},${swatch.rgb.b})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "CMYK(${swatch.cmyk.c},${swatch.cmyk.m},${swatch.cmyk.y},${swatch.cmyk.k}) • HSL(${swatch.hsl.h.roundToInt()}°,${swatch.hsl.s.roundToInt()}%,${swatch.hsl.l.roundToInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copiar",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onCopy() }
            )
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
                .height(64.dp)
                .background(bgColor)
        ) {
            Text(
                text = swatch.hex,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopy)
                .padding(vertical = 6.dp, horizontal = 2.dp)
        ) {
            Text(
                text = swatch.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${swatch.rgb.r},${swatch.rgb.g},${swatch.rgb.b}",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleSkinApplicationCard(
    uiState: SeleniaUiState,
    onSelectTarget: (ModuleThemeTarget) -> Unit,
    onApplySkin: (ModuleThemeTarget) -> Unit,
    onResetSkin: (ModuleThemeTarget) -> Unit,
    onNavigateToModule: (ModuleThemeTarget) -> Unit
) {
    val selectedTarget = uiState.selectedThemeTarget
    val activeColor = Color(
        uiState.activeColorSwatch.rgb.r,
        uiState.activeColorSwatch.rgb.g,
        uiState.activeColorSwatch.rgb.b
    )
    val individualModules = listOf(
        ModuleThemeTarget.CALENDAR,
        ModuleThemeTarget.MOON_OBSERVATORY,
        ModuleThemeTarget.CLOCK,
        ModuleThemeTarget.CALCULATOR
    )

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("apply_module_skin_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎨 Aplicar a la Interfaz (Cada Módulo a su Manera)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = activeColor
                    )
                    Text(
                        text = "Personaliza Calendario, Luna, Reloj y Calculadora con su propio color y degradado (Lineal, Radial, Cónico o Malla).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Target selector chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ModuleThemeTarget.entries) { target ->
                    val isSelected = selectedTarget == target
                    val hasCustom = if (target == ModuleThemeTarget.ALL_MODULES) {
                        individualModules.all { uiState.skinFor(it).isCustomApplied }
                    } else {
                        uiState.skinFor(target).isCustomApplied
                    }
                    val targetIcon = target.iconVector()
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTarget(target) },
                        leadingIcon = {
                            Icon(
                                imageVector = targetIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = buildString {
                                    append(target.spanishTitle)
                                    if (hasCustom) append(" • Activo")
                                }
                            )
                        },
                        modifier = Modifier.testTag("skin_target_chip_${target.name.lowercase()}")
                    )
                }
            }

            // Explanation of how the selected module applies the skin + mini live preview
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.5.dp,
                                color = activeColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        SkinGradientCanvas(
                            style = uiState.gradientStyle,
                            stops = uiState.activeGradientStops,
                            angleDeg = uiState.gradientAngleDeg,
                            focalX = uiState.radialFocalX,
                            focalY = uiState.radialFocalY,
                            radiusFactor = uiState.radialRadiusFactor,
                            alphaIntensity = 0.85f,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = selectedTarget.iconVector(),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(26.dp)
                                .align(Alignment.Center)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Cómo se aplica en ${selectedTarget.spanishTitle}:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = selectedTarget.howItAppliesDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Degradado listo: ${uiState.gradientStyle.spanishTitle} • Color: ${uiState.activeColorSwatch.hex}",
                            style = MaterialTheme.typography.labelSmall,
                            color = activeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Per-module status strip showing current color & gradient assigned to each of the 4 modules
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                individualModules.forEach { mod ->
                    val modSkin = uiState.skinFor(mod)
                    val swatchColor = if (modSkin.isCustomApplied) {
                        Color(
                            modSkin.primarySwatch.rgb.r,
                            modSkin.primarySwatch.rgb.g,
                            modSkin.primarySwatch.rgb.b
                        )
                    } else {
                        MaterialTheme.colorScheme.outline
                    }

                    Surface(
                        onClick = { onSelectTarget(mod) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = if (selectedTarget == mod) 0.9f else 0.4f
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (selectedTarget == mod) 1.5.dp else 0.5.dp,
                                color = swatchColor.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                )
                                Text(
                                    text = mod.spanishTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = if (modSkin.isCustomApplied) {
                                    "${modSkin.primarySwatch.hex} • ${modSkin.gradientStyle.spanishTitle.substringBefore(" ")}"
                                } else {
                                    "Original"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (modSkin.isCustomApplied) swatchColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Action Buttons: Apply to selected target, Open target module, Reset target
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onApplySkin(selectedTarget) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apply_skin_to_target_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aplicar a ${selectedTarget.spanishTitle}",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedTarget != ModuleThemeTarget.ALL_MODULES) {
                    FilledTonalButton(
                        onClick = {
                            onApplySkin(selectedTarget)
                            onNavigateToModule(selectedTarget)
                        },
                        modifier = Modifier.testTag("apply_and_open_module_button")
                    ) {
                        Icon(
                            imageVector = selectedTarget.iconVector(),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aplicar y Ver")
                    }
                }

                AssistChip(
                    onClick = { onResetSkin(selectedTarget) },
                    label = { Text("Restaurar") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restaurar tema original",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("reset_skin_target_button")
                )
            }
        }
    }
}

