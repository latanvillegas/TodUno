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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.domain.LunarAlmanacCategory
import com.example.domain.LunarAstronomer
import com.example.domain.LunarDayInfo
import com.example.domain.LunarTraditionalGoal
import com.example.domain.MoonPhaseType
import com.example.domain.SimpleDate
import com.example.ui.ModuleThemeTarget
import com.example.ui.SeleniaUiState
import com.example.ui.components.ModuleSkinBox
import com.example.ui.components.MoonPhaseBadgeIcon
import com.example.ui.components.RealisticMoonCanvas
import com.example.ui.components.SeleniaIconBadge
import com.example.ui.components.accentColor
import com.example.ui.components.iconVector
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
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                                    com.example.ui.components.MoonPhaseBadgeIcon(
                                        phaseType = phase,
                                        size = 24.dp,
                                        litColor = customMoonLitColor,
                                        southernHemisphere = !uiState.isNorthernHemisphere
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

            // 4. Complete Traditional Lunar Guide & Almanac (Beauty, Hair, Wellness, Gardening, Astronomy, Tides, Fishing, Home)
            item {
                TraditionalLunarAlmanacSection(
                    info = info,
                    selectedDate = uiState.selectedDate,
                    isNorthernHemisphere = uiState.isNorthernHemisphere,
                    moonLitColor = customMoonLitColor,
                    onSelectDate = onSelectDate
                )
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
                                        com.example.ui.components.MoonPhaseBadgeIcon(
                                            phaseType = milestone.phaseType,
                                            size = 34.dp,
                                            litColor = customMoonLitColor,
                                            southernHemisphere = !uiState.isNorthernHemisphere
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
private fun TraditionalLunarAlmanacSection(
    info: LunarDayInfo,
    selectedDate: SimpleDate,
    isNorthernHemisphere: Boolean,
    moonLitColor: Color,
    onSelectDate: (SimpleDate) -> Unit
) {
    var selectedCategory by rememberSaveable {
        mutableStateOf(LunarAlmanacCategory.ALL)
    }
    var selectedGoal by rememberSaveable {
        mutableStateOf(LunarTraditionalGoal.FAST_HAIR_GROWTH)
    }

    val allGuides = info.phaseType.almanacGuides()
    val visibleGuides = if (selectedCategory == LunarAlmanacCategory.ALL) {
        allGuides
    } else {
        allGuides.filter { it.category == selectedCategory }
    }

    val upcomingGoalDates = remember(selectedDate, selectedGoal) {
        LunarAstronomer.getUpcomingIdealDatesForGoal(selectedDate, selectedGoal, count = 4)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Card A: Complete Traditional Guide for the Active Moon Phase
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("traditional_almanac_card")
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
                            text = stringResource(R.string.biorhythm_activity_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Belleza, cabello, bienestar, agricultura, astronomía, mareas, pesca y hogar en ${info.phaseType.spanishName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Summary Pill Strip for Hair + Beauty on this day with custom vector badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAlmanacHighlightPill(
                        icon = LunarAlmanacCategory.HAIR_CARE.iconVector(),
                        tint = LunarAlmanacCategory.HAIR_CARE.accentColor(),
                        label = "Cabello",
                        value = info.phaseType.hairActionBadge,
                        modifier = Modifier.weight(1f)
                    )
                    QuickAlmanacHighlightPill(
                        icon = LunarAlmanacCategory.BEAUTY_SKIN.iconVector(),
                        tint = LunarAlmanacCategory.BEAUTY_SKIN.accentColor(),
                        label = "Belleza y Piel",
                        value = info.phaseType.beautyActionBadge,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Category Filter Row with vector leading icons
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(com.example.domain.LunarAlmanacCategory.entries.size) { index ->
                        val cat = com.example.domain.LunarAlmanacCategory.entries[index]
                        val catIcon = cat.iconVector()
                        val catColor = cat.accentColor()
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            leadingIcon = {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = if (selectedCategory == cat) MaterialTheme.colorScheme.primary else catColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(cat.shortChipLabel) },
                            modifier = Modifier.testTag("almanac_cat_${cat.name.lowercase()}")
                        )
                    }
                }

                // Detailed Guide Cards with glowing SeleniaIconBadge
                visibleGuides.forEach { guide ->
                    AlmanacTipRow(
                        category = guide.category,
                        badge = guide.badgeTitle,
                        actionHighlight = guide.actionHighlight,
                        description = guide.description,
                        scoreOutOfFive = guide.scoreOutOfFive
                    )
                }
            }
        }

        // Card B: Interactive Best Lunar Dates Finder for Beauty, Haircuts, Depilation, Planting & Timber
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("lunar_beauty_goal_finder_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Buscador de Fechas Ideales (Belleza, Corte y Tradición)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Elige tu objetivo y toca cualquier fecha recomendada para inspeccionar su Luna:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(com.example.domain.LunarTraditionalGoal.entries.size) { idx ->
                        val goal = com.example.domain.LunarTraditionalGoal.entries[idx]
                        val goalIcon = goal.iconVector()
                        val goalColor = goal.accentColor()
                        FilterChip(
                            selected = selectedGoal == goal,
                            onClick = { selectedGoal = goal },
                            leadingIcon = {
                                Icon(
                                    imageVector = goalIcon,
                                    contentDescription = null,
                                    tint = if (selectedGoal == goal) MaterialTheme.colorScheme.primary else goalColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(goal.title) },
                            modifier = Modifier.testTag("goal_chip_${goal.name.lowercase()}")
                        )
                    }
                }

                val isSelectedDateIdealForGoal = info.phaseType in selectedGoal.idealPhases
                val activeGoalIcon = selectedGoal.iconVector()
                val activeGoalColor = selectedGoal.accentColor()

                Surface(
                    color = if (isSelectedDateIdealForGoal) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                com.example.ui.components.SeleniaIconBadge(
                                    icon = activeGoalIcon,
                                    tint = activeGoalColor,
                                    size = 34.dp,
                                    iconSize = 18.dp
                                )
                                Text(
                                    text = selectedGoal.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                color = if (isSelectedDateIdealForGoal) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isSelectedDateIdealForGoal) "¡Óptimo en esta fecha!" else "Ver próximas fechas",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelectedDateIdealForGoal) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = selectedGoal.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Fases ideales: " + selectedGoal.idealPhases.joinToString(" • ") { it.spanishName },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Upcoming recommended dates row with MoonPhaseBadgeIcon medallions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    upcomingGoalDates.forEach { dayInfo ->
                        val isCurrent = dayInfo.date == selectedDate
                        Surface(
                            onClick = { onSelectDate(dayInfo.date) },
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (isCurrent) 1.5.dp else 0.5.dp,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                com.example.ui.components.MoonPhaseBadgeIcon(
                                    phaseType = dayInfo.phaseType,
                                    size = 26.dp,
                                    litColor = moonLitColor,
                                    southernHemisphere = !isNorthernHemisphere
                                )
                                Text(
                                    text = dayInfo.date.formatSpanishShort().substringBeforeLast(" "),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = dayInfo.phaseType.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAlmanacHighlightPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.example.ui.components.SeleniaIconBadge(
                icon = icon,
                tint = tint,
                size = 32.dp,
                iconSize = 16.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
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
    category: LunarAlmanacCategory,
    badge: String,
    actionHighlight: String,
    description: String,
    scoreOutOfFive: Int
) {
    val catIcon = category.iconVector()
    val catColor = category.accentColor()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    com.example.ui.components.SeleniaIconBadge(
                        icon = catIcon,
                        tint = catColor,
                        size = 34.dp,
                        iconSize = 18.dp
                    )
                    Text(
                        text = category.spanishTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = actionHighlight,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Afinidad lunar: " + "★".repeat(scoreOutOfFive) + "☆".repeat((5 - scoreOutOfFive).coerceAtLeast(0)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

