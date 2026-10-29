package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.LunarAstronomer
import com.example.domain.MoonPhaseType
import com.example.domain.SimpleDate
import com.example.ui.ModuleThemeTarget
import com.example.ui.SeleniaUiState
import com.example.ui.components.ModuleSkinBox
import com.example.ui.components.RealisticMoonCanvas
import kotlin.math.roundToInt

@Composable
fun MoonObservatoryScreen(
    uiState: SeleniaUiState,
    onStepDay: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDate: (SimpleDate) -> Unit,
    onToggleHemisphere: () -> Unit,
    onOpenColorCustomizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val info = uiState.selectedDayLunarInfo
    val skin = uiState.skinFor(ModuleThemeTarget.MOON_OBSERVATORY)
    val customMoonLitColor = if (skin.isCustomApplied) {
        Color(skin.primarySwatch.rgb.r, skin.primarySwatch.rgb.g, skin.primarySwatch.rgb.b)
    } else {
        Color(0xFFF7E7B4)
    }
    val customMoonHighlight = if (skin.isCustomApplied) {
        val sec = skin.secondarySwatch.rgb
        Color(
            red = ((sec.r + 255) / 2).coerceIn(0, 255),
            green = ((sec.g + 255) / 2).coerceIn(0, 255),
            blue = ((sec.b + 255) / 2).coerceIn(0, 255)
        )
    } else {
        Color(0xFFFFF8E1)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("moon_observatory_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Lunar Sphere & Interactive Date Stepper with custom Moon Skin
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModuleSkinBox(
                        skin = skin,
                        alphaIntensity = 0.42f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (!skin.isCustomApplied) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.background,
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Transparent)
                                        )
                                    }
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = true,
                                    onClick = onToggleHemisphere,
                                    label = {
                                        Text(
                                            if (uiState.isNorthernHemisphere) {
                                                stringResource(R.string.hemisphere_north)
                                            } else {
                                                stringResource(R.string.hemisphere_south)
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("hemisphere_toggle_chip")
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = onOpenColorCustomizer,
                                        modifier = Modifier.testTag("moon_customize_color_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "Personalizar color de la Luna",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    AssistChip(
                                        onClick = onJumpToToday,
                                        label = { Text(stringResource(R.string.today_button)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Today,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.testTag("moon_today_button")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Large Realistic Moon Canvas tinted with the Moon Module's custom color!
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(220.dp)
                                    .testTag("hero_moon_canvas")
                            ) {
                                RealisticMoonCanvas(
                                    phaseFraction = info.phaseFraction,
                                    isNorthernHemisphere = uiState.isNorthernHemisphere,
                                    showAmbientGlow = true,
                                    litColor = customMoonLitColor,
                                    litHighlightColor = customMoonHighlight
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${info.phaseType.emoji} ${info.phaseType.spanishName}",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("moon_phase_name_text")
                            )

                            Text(
                                text = uiState.selectedDate.formatSpanishLong().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (skin.isCustomApplied) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Luna personalizada: ${skin.primarySwatch.label} (${skin.primarySwatch.hex})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Date Stepper Row: -7d, -1d, +1d, +7d
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = { onStepDay(-7) },
                                    label = { Text(stringResource(R.string.jump_minus_7)) },
                                    modifier = Modifier.testTag("moon_step_minus_7")
                                )
                                FilledTonalIconButton(
                                    onClick = { onStepDay(-1) },
                                    modifier = Modifier.testTag("moon_step_minus_1")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = stringResource(R.string.prev_day_desc)
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "${"%.1f".format(info.illuminationPercent)}% luz",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { onStepDay(1) },
                                    modifier = Modifier.testTag("moon_step_plus_1")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.next_day_desc)
                                    )
                                }
                                AssistChip(
                                    onClick = { onStepDay(7) },
                                    label = { Text(stringResource(R.string.jump_plus_7)) },
                                    modifier = Modifier.testTag("moon_step_plus_7")
                                )
                            }
                        }
                    }
                }
            }

            // 2. 8-Phase Interactive Synodic Cycle Strip
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.synodic_progress),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Día ${"%.1f".format(info.lunarAgeDays)} / 29.5",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (info.phaseFraction).toFloat().coerceIn(0f, 1f) },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MoonPhaseType.entries.forEach { phase ->
                                val isActive = phase == info.phaseType
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isActive) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = phase.emoji,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = phase.shortLabel.take(6),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isActive) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Astronomical Telemetry Grid (4 cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryStatCard(
                            label = stringResource(R.string.lunar_illumination_label),
                            value = "${"%.1f".format(info.illuminationPercent)}%",
                            subtitle = if (info.phaseFraction <= 0.5) "Fase creciente" else "Fase menguante",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatCard(
                            label = stringResource(R.string.lunar_age_label),
                            value = "${"%.2f".format(info.lunarAgeDays)} d",
                            subtitle = "${(info.phaseFraction * 100).roundToInt()}% del mes sinódico",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryStatCard(
                            label = stringResource(R.string.distance_earth_moon),
                            value = "${info.distanceKm} km",
                            subtitle = if (info.distanceKm < 370000) "Cerca del Perigeo" else "Órbita media/Apogeo",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatCard(
                            label = stringResource(R.string.zodiac_constellation),
                            value = info.zodiacSign,
                            subtitle = "Longitud eclíptica",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Natural Almanac & Biorhythm Recommendations
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.biorhythm_activity_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        AlmanacTipRow(
                            badge = "🌱 Agricultura y Jardín",
                            description = info.phaseType.gardeningTip
                        )
                        AlmanacTipRow(
                            badge = "🔭 Observación Astronómica",
                            description = info.phaseType.stargazingTip
                        )
                        AlmanacTipRow(
                            badge = "🌊 Dinámica de Mareas",
                            description = info.phaseType.tideEffect
                        )
                    }
                }
            }

            // 5. Upcoming 4 Major Phases
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.upcoming_major_phases),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        uiState.upcomingMajorPhases.forEach { milestone ->
                            val deltaDays = LunarAstronomer.daysBetween(uiState.selectedDate, milestone.date)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectDate(milestone.date) }
                                    .testTag("upcoming_phase_${milestone.date.toStorageKey()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = milestone.phaseType.emoji,
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                        Column {
                                            Text(
                                                text = milestone.phaseType.spanishName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = milestone.date.formatSpanishLong().replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (deltaDays == 0) "En esta fecha" else "En +$deltaDays d",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
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

@Composable
private fun TelemetryStatCard(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.medium
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun AlmanacTipRow(
    badge: String,
    description: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
