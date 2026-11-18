package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CelestialEventKind
import com.example.domain.LunarAlmanacCategory
import com.example.domain.LunarAstronomer
import com.example.domain.LunarDayInfo
import com.example.domain.LunarTraditionalGoal
import com.example.domain.MoonPhaseType
import com.example.domain.SimpleDate
import com.example.notifications.SeleniaAlertManager
import com.example.ui.SeleniaUiState
import com.example.ui.components.LunarAtmospherePreset
import com.example.ui.components.MoonPhaseBadgeIcon
import com.example.ui.components.RealisticMoonCanvas
import com.example.ui.components.SeleniaIconBadge
import com.example.ui.components.SeleniaVectorIcons
import com.example.ui.components.accentColor
import com.example.ui.components.iconVector
import androidx.compose.ui.semantics.Role
import com.example.ui.components.recommendedCalendarLitColor
import kotlin.math.roundToInt

private val AFFINITY_STARS = arrayOf(
    "☆☆☆☆☆",
    "★☆☆☆☆",
    "★★☆☆☆",
    "★★★☆☆",
    "★★★★☆",
    "★★★★★"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoonObservatoryScreen(
    uiState: SeleniaUiState,
    onStepDay: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDate: (SimpleDate) -> Unit,
    onToggleHemisphere: () -> Unit,
    onSetLunarAlertsEnabled: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var dragAccumulatedX by remember { mutableFloatStateOf(0f) }

    val lunarAlertsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSetLunarAlertsEnabled(true)
        }
    }

    val info = uiState.selectedDayLunarInfo

    var selectedAtmosphere by rememberSaveable {
        mutableStateOf(LunarAtmospherePreset.AUTO_PHASE)
    }

    val phaseDynamicColor = info.phaseType.recommendedCalendarLitColor()

    val customMoonLitColor = if (selectedAtmosphere == LunarAtmospherePreset.AUTO_PHASE) {
        phaseDynamicColor
    } else {
        selectedAtmosphere.litColor
    }

    val customMoonHighlight = if (selectedAtmosphere == LunarAtmospherePreset.AUTO_PHASE) {
        Color(0xFFFFFDE7)
    } else {
        selectedAtmosphere.highlightColor
    }

    val customMoonShadow = selectedAtmosphere.shadowColor
    val customCoronaSecondary = selectedAtmosphere.coronaSecondaryColor

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
            // 1. Hero Lunar Sphere & Interactive Date Stepper with custom Moon Skin + 6 3D Atmospheres
            item(key = "hero_moon") {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(uiState.selectedDate) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccumulatedX = 0f },
                                onDragEnd = {
                                    if (dragAccumulatedX > 75f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onStepDay(-1)
                                    } else if (dragAccumulatedX < -75f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onStepDay(1)
                                    }
                                    dragAccumulatedX = 0f
                                }
                            ) { _, dragAmount ->
                                dragAccumulatedX += dragAmount
                            }
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background,
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                                imageVector = SeleniaVectorIcons.Public,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.testTag("hemisphere_toggle_chip")
                                    )

                                    FilterChip(
                                        selected = uiState.lunarAlertsEnabled,
                                        onClick = {
                                            if (!uiState.lunarAlertsEnabled) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                    !SeleniaAlertManager.hasNotificationPermission(context)
                                                ) {
                                                    lunarAlertsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    onSetLunarAlertsEnabled(true)
                                                }
                                            } else {
                                                onSetLunarAlertsEnabled(false)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = { Text("Alertas") },
                                        modifier = Modifier.testTag("lunar_alerts_toggle_chip")
                                    )
                                }

                                AssistChip(
                                    onClick = onJumpToToday,
                                    label = { Text(stringResource(R.string.today_button)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = SeleniaVectorIcons.Today,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("moon_today_button")
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Large 3D Realistic Moon Canvas with craters, Tycho rays & astrolabe degree ring
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(228.dp)
                                    .testTag("hero_moon_canvas")
                            ) {
                                RealisticMoonCanvas(
                                    phaseFraction = info.phaseFraction,
                                    isNorthernHemisphere = uiState.isNorthernHemisphere,
                                    showAmbientGlow = true,
                                    litColor = customMoonLitColor,
                                    litHighlightColor = customMoonHighlight,
                                    shadowColor = customMoonShadow,
                                    coronaSecondaryColor = customCoronaSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.testTag("moon_phase_name_text")
                            ) {
                                MoonPhaseBadgeIcon(
                                    phaseType = info.phaseType,
                                    size = 30.dp,
                                    litColor = customMoonLitColor,
                                    southernHemisphere = !uiState.isNorthernHemisphere
                                )
                                Text(
                                    text = info.phaseType.spanishName,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = customMoonLitColor,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = uiState.selectedDate.formatSpanishLong().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (info.isSupermoon) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = Color(0xFFFFD54F).copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.border(
                                        width = 1.dp,
                                        color = Color(0xFFFFD54F),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                ) {
                                    Text(
                                        text = "★ SUPERLUNA DE PERIGEO (${info.distanceKm} km)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD54F),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 6-Mode 3D Lunar Atmosphere Selector Strip
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(
                                    items = LunarAtmospherePreset.entries,
                                    key = { it.name }
                                ) { preset ->
                                    val isSelected = selectedAtmosphere == preset
                                    val dotColor = if (preset == LunarAtmospherePreset.AUTO_PHASE) {
                                        phaseDynamicColor
                                    } else {
                                        preset.litColor
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedAtmosphere = preset },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        },
                                        label = { Text(preset.shortChip) },
                                        modifier = Modifier.testTag("moon_atmosphere_${preset.name.lowercase()}")
                                    )
                                }
                            }

                            Text(
                                text = "${selectedAtmosphere.spanishName}: ${selectedAtmosphere.description}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Date Stepper Row: -7d, -1d, [Relative Day Offset], +1d, +7d
                            val dayDeltaFromToday = remember(uiState.today, uiState.selectedDate) {
                                LunarAstronomer.daysBetween(uiState.today, uiState.selectedDate)
                            }
                            val relativeDayLabel = when {
                                dayDeltaFromToday == 0 -> "Hoy"
                                dayDeltaFromToday > 0 -> "+$dayDeltaFromToday d"
                                else -> "$dayDeltaFromToday d"
                            }
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
                                        text = relativeDayLabel,
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

            // 2. 8-Phase Interactive Synodic Cycle Strip
            item(key = "synodic_strip") {
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
                                text = "${(info.phaseFraction * 100).roundToInt()}% del ciclo",
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
            item(key = "telemetry_grid") {
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
                            subtitle = "Mes sinódico de 29.53 días",
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryStatCard(
                            label = stringResource(R.string.moonrise_moonset_label),
                            value = "↑ ${info.moonriseTime} • ↓ ${info.moonsetTime}",
                            subtitle = "Efeméride estimada local",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatCard(
                            label = stringResource(R.string.meridian_transit_label),
                            value = info.meridianTransitTime,
                            subtitle = if (info.isSupermoon) "Superluna en perigeo" else "Culminación lunar",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Complete Traditional Lunar Guide & Almanac (Beauty, Hair, Wellness, Gardening, Astronomy, Tides, Fishing, Home)
            item(key = "almanac_section") {
                TraditionalLunarAlmanacSection(
                    info = info,
                    selectedDate = uiState.selectedDate,
                    isNorthernHemisphere = uiState.isNorthernHemisphere,
                    moonLitColor = customMoonLitColor,
                    onSelectDate = onSelectDate
                )
            }

            // 5. Upcoming 4 Major Phases
            item(key = "upcoming_phases") {
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
                                    .clickable(role = Role.Button) { onSelectDate(milestone.date) }
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

            // 6. Upcoming Supermoons & Solar/Lunar Eclipses (2026–2029)
            if (uiState.upcomingCelestialEvents.isNotEmpty()) {
                item(key = "celestial_events") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("celestial_events_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.upcoming_eclipses_supermoons),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Toca cualquier eclipse o superluna para viajar directamente a esa fecha en el observatorio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            uiState.upcomingCelestialEvents.forEach { event ->
                                val deltaDays = LunarAstronomer.daysBetween(uiState.selectedDate, event.date)
                                val badgeColor = when (event.kind) {
                                    CelestialEventKind.SUPERMOON -> Color(0xFFFFD54F)
                                    CelestialEventKind.LUNAR_ECLIPSE -> Color(0xFFFF7043)
                                    CelestialEventKind.SOLAR_ECLIPSE -> Color(0xFF4DD0E1)
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = badgeColor.copy(alpha = 0.45f),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .clickable(role = Role.Button) { onSelectDate(event.date) }
                                        .testTag("celestial_event_${event.date.toStorageKey()}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
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
                                                MoonPhaseBadgeIcon(
                                                    phaseType = event.phaseType,
                                                    size = 28.dp,
                                                    litColor = badgeColor,
                                                    southernHemisphere = !uiState.isNorthernHemisphere
                                                )
                                                Column {
                                                    Text(
                                                        text = event.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeColor
                                                    )
                                                    Text(
                                                        text = event.date.formatSpanishLong().replaceFirstChar { it.uppercase() },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                            Surface(
                                                color = badgeColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        deltaDays == 0 -> "Hoy"
                                                        deltaDays > 0 -> "+${deltaDays}d"
                                                        else -> "${deltaDays}d"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = event.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Visibilidad: ${event.visibilityRegion}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
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

    val visibleGuides = remember(info.phaseType, selectedCategory) {
        val allGuides = info.phaseType.almanacGuides()
        if (selectedCategory == LunarAlmanacCategory.ALL) {
            allGuides
        } else {
            allGuides.filter { it.category == selectedCategory }
        }
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

                // Category Filter Row with vector leading icons
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = LunarAlmanacCategory.entries,
                        key = { it.name }
                    ) { cat ->
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

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = LunarTraditionalGoal.entries,
                        key = { it.name }
                    ) { goal ->
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
                text = "Afinidad lunar: ${AFFINITY_STARS[scoreOutOfFive.coerceIn(0, 5)]}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

