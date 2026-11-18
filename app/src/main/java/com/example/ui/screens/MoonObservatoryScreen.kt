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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CelestialEventKind
import com.example.domain.LunarAstronomer
import com.example.domain.LunarEclipseKind
import com.example.domain.MoonPhaseType
import com.example.domain.SimpleDate
import com.example.domain.SolarEclipseKind
import com.example.notifications.SeleniaAlertManager
import com.example.ui.SeleniaUiState
import com.example.ui.components.MoonPhaseBadgeIcon
import com.example.ui.components.RealisticMoonCanvas
import com.example.ui.components.SeleniaVectorIcons
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Astro-photographic selenite regolith palette; shifts automatically only during real umbral eclipses
private val SELENITE_PEARL_LIT = Color(0xFFF3E6C8)
private val SELENITE_PEARL_HIGHLIGHT = Color(0xFFFFFFFF)
private val SELENITE_PEARL_SHADOW = Color(0xFF0F1932)
private val SELENITE_PEARL_CORONA = Color(0xFF80DEEA)

private val UMBRAL_TOTAL_LIT = Color(0xFFD84315)
private val UMBRAL_TOTAL_HIGHLIGHT = Color(0xFFFFAB91)
private val UMBRAL_TOTAL_SHADOW = Color(0xFF1F080C)
private val UMBRAL_TOTAL_CORONA = Color(0xFFFF7043)

private val UMBRAL_PARTIAL_LIT = Color(0xFFE67E22)
private val UMBRAL_PARTIAL_HIGHLIGHT = Color(0xFFFFE0B2)
private val UMBRAL_PARTIAL_SHADOW = Color(0xFF1A1016)
private val UMBRAL_PARTIAL_CORONA = Color(0xFFFFAB40)

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

    // Realistic lunar appearance: authentic selenite pearl regolith by default,
    // shifting physically to copper-red Rayleigh scattering ONLY during a real umbral lunar eclipse.
    val moonLitColor = when (info.lunarEclipseKind) {
        LunarEclipseKind.TOTAL -> UMBRAL_TOTAL_LIT
        LunarEclipseKind.PARTIAL -> UMBRAL_PARTIAL_LIT
        else -> SELENITE_PEARL_LIT
    }
    val moonHighlightColor = when (info.lunarEclipseKind) {
        LunarEclipseKind.TOTAL -> UMBRAL_TOTAL_HIGHLIGHT
        LunarEclipseKind.PARTIAL -> UMBRAL_PARTIAL_HIGHLIGHT
        else -> SELENITE_PEARL_HIGHLIGHT
    }
    val moonShadowColor = when (info.lunarEclipseKind) {
        LunarEclipseKind.TOTAL -> UMBRAL_TOTAL_SHADOW
        LunarEclipseKind.PARTIAL -> UMBRAL_PARTIAL_SHADOW
        else -> SELENITE_PEARL_SHADOW
    }
    val moonCoronaColor = when (info.lunarEclipseKind) {
        LunarEclipseKind.TOTAL -> UMBRAL_TOTAL_CORONA
        LunarEclipseKind.PARTIAL -> UMBRAL_PARTIAL_CORONA
        else -> SELENITE_PEARL_CORONA
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
            // 1. Hero Realistic Selenite Lunar Sphere & Interactive Date Stepper
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

                        // Large 3D Realistic Moon Canvas (Selenite Pearl, or Umbral Copper on Lunar Eclipse)
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
                                litColor = moonLitColor,
                                litHighlightColor = moonHighlightColor,
                                shadowColor = moonShadowColor,
                                coronaSecondaryColor = moonCoronaColor
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
                                litColor = moonLitColor,
                                southernHemisphere = !uiState.isNorthernHemisphere
                            )
                            Text(
                                text = info.phaseType.spanishName,
                                style = MaterialTheme.typography.displaySmall,
                                color = moonLitColor,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = uiState.selectedDate.formatSpanishLong().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        // Real astronomical phenomenon badges (only shown when physically occurring on this date)
                        if (info.lunarEclipseKind != LunarEclipseKind.NONE) {
                            val eclipseBadgeText = when (info.lunarEclipseKind) {
                                LunarEclipseKind.TOTAL -> "● ECLIPSE TOTAL DE LUNA (UMBRA TERRESTRE)"
                                LunarEclipseKind.PARTIAL -> "◐ ECLIPSE PARCIAL DE LUNA"
                                LunarEclipseKind.PENUMBRAL -> "◌ ECLIPSE LUNAR PENUMBRAL"
                                LunarEclipseKind.NONE -> ""
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            AstronomicalPhenomenonBadge(
                                text = eclipseBadgeText,
                                accentColor = Color(0xFFFF7043),
                                modifier = Modifier.testTag("active_lunar_eclipse_badge")
                            )
                        }

                        if (info.solarEclipseKind != SolarEclipseKind.NONE) {
                            val solarBadgeText = when (info.solarEclipseKind) {
                                SolarEclipseKind.TOTAL -> "☀ ECLIPSE SOLAR TOTAL"
                                SolarEclipseKind.ANNULAR -> "◎ ECLIPSE SOLAR ANULAR"
                                SolarEclipseKind.HYBRID -> "◎ ECLIPSE SOLAR HÍBRIDO"
                                SolarEclipseKind.PARTIAL -> "◐ ECLIPSE SOLAR PARCIAL"
                                SolarEclipseKind.NONE -> ""
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            AstronomicalPhenomenonBadge(
                                text = solarBadgeText,
                                accentColor = Color(0xFF4DD0E1),
                                modifier = Modifier.testTag("active_solar_eclipse_badge")
                            )
                        }

                        if (info.isSupermoon) {
                            Spacer(modifier = Modifier.height(6.dp))
                            AstronomicalPhenomenonBadge(
                                text = "★ SUPERLUNA DE PERIGEO (${info.distanceKm} km)",
                                accentColor = Color(0xFFFFD54F),
                                modifier = Modifier.testTag("active_supermoon_badge")
                            )
                        }

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
                                    MoonPhaseBadgeIcon(
                                        phaseType = phase,
                                        size = 24.dp,
                                        litColor = moonLitColor,
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

            // 3. 100% Real Global Astronomical Telemetry Grid (6 cards — zero simulated horizon guesses)
            item(key = "telemetry_grid") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("moon_telemetry_grid")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryStatCard(
                            label = stringResource(R.string.lunar_illumination_label),
                            value = "${"%.1f".format(Locale.US, info.illuminationPercent)}%",
                            subtitle = if (info.phaseFraction <= 0.5) "Elongación creciente" else "Elongación menguante",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatCard(
                            label = stringResource(R.string.lunar_age_label),
                            value = "${"%.2f".format(Locale.US, info.lunarAgeDays)} d",
                            subtitle = "Desde última Luna Nueva real",
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
                            subtitle = when {
                                info.distanceKm <= 363500 -> "Perigeo cercano"
                                info.distanceKm >= 401000 -> "Apogeo lejano"
                                else -> "Órbita elíptica media"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatCard(
                            label = stringResource(R.string.zodiac_constellation),
                            value = info.zodiacSign,
                            subtitle = "λ ${"%.1f".format(Locale.US, info.eclipticLongitudeDeg)}° (${"%.1f".format(Locale.US, info.zodiacDegreeInSign)}°)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val arcSeconds = (info.angularDiameterArcmin * 60.0).roundToInt()
                        TelemetryStatCard(
                            label = stringResource(R.string.angular_diameter_label),
                            value = "${"%.1f".format(Locale.US, info.angularDiameterArcmin)}′",
                            subtitle = "$arcSeconds″ disco aparente",
                            modifier = Modifier.weight(1f)
                        )
                        val nodeSubtitle = when {
                            abs(info.eclipticLatitudeDeg) < 1.35 -> "Cerca de nodo dracónico"
                            info.eclipticLatitudeDeg > 0.0 -> "Norte de la eclíptica"
                            else -> "Sur de la eclíptica"
                        }
                        TelemetryStatCard(
                            label = stringResource(R.string.ecliptic_latitude_label),
                            value = "${"%+.2f".format(Locale.US, info.eclipticLatitudeDeg)}°",
                            subtitle = nodeSubtitle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Upcoming 4 Major Phases (exact Meeus Ch. 49 dates)
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
                                        MoonPhaseBadgeIcon(
                                            phaseType = milestone.phaseType,
                                            size = 34.dp,
                                            litColor = SELENITE_PEARL_LIT,
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

            // 5. Dynamically Computed Supermoons & Solar/Lunar Eclipses (Meeus Ch. 54 — any year/century)
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
                                text = "Calculados astronómicamente a partir de la fecha seleccionada. Toca cualquiera para inspeccionarlo:",
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
                                                        deltaDays == 0 -> "En fecha"
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
                                            text = event.visibilityRegion,
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
private fun AstronomicalPhenomenonBadge(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.18f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(
            width = 1.dp,
            color = accentColor,
            shape = RoundedCornerShape(10.dp)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
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
