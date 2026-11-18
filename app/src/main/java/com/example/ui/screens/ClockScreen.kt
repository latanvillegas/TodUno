package com.example.ui.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import com.example.notifications.SeleniaAlertManager
import com.example.ui.ClockSubTab
import com.example.ui.SeleniaUiState
import com.example.ui.StopwatchLap
import com.example.ui.components.RealisticMoonCanvas
import com.example.ui.components.SeleniaIconBadge
import com.example.ui.components.SeleniaVectorIcons
import com.example.ui.currentMonotonicMs
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Precomputed 60-tick unit circle cosine/sine tables for the analog watch face:
// eliminates 120 trig calls per draw frame.
private val WATCH_TICK_COS = FloatArray(60) { i ->
    cos((i * 6.0 - 90.0) * (PI / 180.0)).toFloat()
}
private val WATCH_TICK_SIN = FloatArray(60) { i ->
    sin((i * 6.0 - 90.0) * (PI / 180.0)).toFloat()
}
private val OUTER_BEZEL_STROKE = Stroke(width = 3.2f)
private val INNER_BEZEL_STROKE = Stroke(width = 1.4f)

private val TIMER_PRESETS = listOf(
    60 to "1 min",
    180 to "3 min",
    300 to "5 min",
    600 to "10 min",
    900 to "15 min",
    1772 to "29.5m (Ciclo)"
)

@Immutable
private data class WorldCitySpec(
    val city: String,
    val region: String,
    val timeZoneId: String
) {
    val timeZone: TimeZone = TimeZone.getTimeZone(timeZoneId)
}

@Immutable
private data class WorldCityFormattedEntry(
    val timeZoneId: String,
    val city: String,
    val region: String,
    val isDaylight: Boolean,
    val timeText: String
)

private val WORLD_CITIES = listOf(
    WorldCitySpec("Tiempo Universal (UTC)", "Referencia Astronómica", "UTC"),
    WorldCitySpec("Madrid / París", "Europa Central", "Europe/Madrid"),
    WorldCitySpec("Londres / Lisboa", "Europa Occidental", "Europe/London"),
    WorldCitySpec("Berlín / Roma", "Europa Continental", "Europe/Berlin"),
    WorldCitySpec("Ciudad de México", "América Central", "America/Mexico_City"),
    WorldCitySpec("Bogotá / Lima / Quito", "Andes / Pacífico", "America/Bogota"),
    WorldCitySpec("Buenos Aires", "Cono Sur Atlántico", "America/Argentina/Buenos_Aires"),
    WorldCitySpec("Santiago de Chile", "Pacífico Austral", "America/Santiago"),
    WorldCitySpec("Caracas / La Paz", "Caribe / Altiplano", "America/Caracas"),
    WorldCitySpec("São Paulo / Río", "Brasil Oriental", "America/Sao_Paulo"),
    WorldCitySpec("Nueva York / Miami", "Costa Este EE. UU.", "America/New_York"),
    WorldCitySpec("Chicago / Dallas", "Centro Norteamérica", "America/Chicago"),
    WorldCitySpec("Los Ángeles / Tijuana", "Costa Oeste Pacífico", "America/Los_Angeles"),
    WorldCitySpec("Honolulú", "Pacífico Central", "Pacific/Honolulu"),
    WorldCitySpec("El Cairo / Atenas", "Mediterráneo Oriental", "Africa/Cairo"),
    WorldCitySpec("Estambul / Riad", "Oriente Próximo", "Europe/Istanbul"),
    WorldCitySpec("Dubái", "Golfo Pérsico", "Asia/Dubai"),
    WorldCitySpec("Nueva Delhi", "Subcontinente Indio", "Asia/Kolkata"),
    WorldCitySpec("Singapur / Pekín", "Sudeste Asiático / China", "Asia/Singapore"),
    WorldCitySpec("Tokio / Seúl", "Asia Oriental", "Asia/Tokyo"),
    WorldCitySpec("Sídney", "Oceanía Oriental", "Australia/Sydney")
)

private val WORLD_CITIES_BY_ID = WORLD_CITIES.associateBy { it.timeZoneId }

/**
 * Observes Android Battery Saver ([PowerManager.isPowerSaveMode]) so high-frequency UI tickers
 * automatically step down frame rates (4Hz -> 1Hz on watch, 30Hz -> 10Hz on stopwatch, 15Hz -> 2Hz on timer)
 * when the user is conserving battery.
 */
@Composable
private fun rememberPowerSaveMode(): Boolean {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    var isPowerSave by remember {
        mutableStateOf(powerManager?.isPowerSaveMode == true)
    }

    DisposableEffect(context, powerManager) {
        if (powerManager == null) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                isPowerSave = powerManager.isPowerSaveMode
            }
        }
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        ContextCompatSafeRegister(context, receiver, filter)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
    return isPowerSave
}

private fun ContextCompatSafeRegister(
    context: Context,
    receiver: BroadcastReceiver,
    filter: IntentFilter
) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }
}

@Composable
fun ClockScreen(
    uiState: SeleniaUiState,
    onSelectSubTab: (ClockSubTab) -> Unit,
    onToggle24HourFormat: () -> Unit,
    onToggleWorldCity: (String) -> Unit = {},
    onToggleStopwatch: () -> Unit = {},
    onLapStopwatch: () -> Unit = {},
    onResetStopwatch: () -> Unit = {},
    onSelectTimerPreset: (Int) -> Unit = {},
    onAdjustTimerSeconds: (Int) -> Unit = {},
    onToggleTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    onTimerReachedZero: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPowerSaveMode = rememberPowerSaveMode()

    val timerNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sub-tab selector: Reloj y Husos | Cronómetro | Temporizador
            // Zero ticker reads here -> sub-tab bar never recomposes on clock/stopwatch/timer ticks!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.clockSubTab == ClockSubTab.WORLD_CLOCK,
                    onClick = { onSelectSubTab(ClockSubTab.WORLD_CLOCK) },
                    label = { Text(stringResource(R.string.clock_tab_world)) },
                    leadingIcon = {
                        Icon(
                            imageVector = SeleniaVectorIcons.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clock_subtab_world")
                )
                FilterChip(
                    selected = uiState.clockSubTab == ClockSubTab.STOPWATCH,
                    onClick = { onSelectSubTab(ClockSubTab.STOPWATCH) },
                    label = { Text(stringResource(R.string.clock_tab_stopwatch)) },
                    leadingIcon = {
                        Icon(
                            imageVector = SeleniaVectorIcons.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clock_subtab_stopwatch")
                )
                FilterChip(
                    selected = uiState.clockSubTab == ClockSubTab.TIMER,
                    onClick = { onSelectSubTab(ClockSubTab.TIMER) },
                    label = { Text(stringResource(R.string.clock_tab_timer)) },
                    leadingIcon = {
                        Icon(
                            imageVector = SeleniaVectorIcons.HourglassBottom,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clock_subtab_timer")
                )
            }

            when (uiState.clockSubTab) {
                ClockSubTab.WORLD_CLOCK -> AstronomicalAndWorldClockContent(
                    isPowerSaveMode = isPowerSaveMode,
                    is24Hour = uiState.is24HourFormat,
                    selectedWorldCityIds = uiState.selectedWorldCityIds,
                    moonPhaseFraction = uiState.selectedDayLunarInfo.phaseFraction,
                    moonPhaseName = uiState.selectedDayLunarInfo.phaseType.spanishName,
                    isNorthernHemisphere = uiState.isNorthernHemisphere,
                    onToggle24Hour = onToggle24HourFormat,
                    onToggleWorldCity = onToggleWorldCity
                )

                ClockSubTab.STOPWATCH -> StopwatchContent(
                    isPowerSaveMode = isPowerSaveMode,
                    isRunning = uiState.stopwatchRunning,
                    accumulatedMs = uiState.stopwatchAccumulatedMs,
                    lastStartMonoMs = uiState.stopwatchLastStartMonoMs,
                    laps = uiState.stopwatchLaps,
                    onToggleStartPause = onToggleStopwatch,
                    onLap = onLapStopwatch,
                    onReset = onResetStopwatch
                )

                ClockSubTab.TIMER -> TimerContent(
                    isPowerSaveMode = isPowerSaveMode,
                    totalSeconds = uiState.timerTotalSeconds,
                    remainingPausedMs = uiState.timerRemainingPausedMs,
                    endTargetMonoMs = uiState.timerEndTargetMonoMs,
                    isRunning = uiState.timerRunning,
                    isFinishedAlert = uiState.timerFinishedAlert,
                    onSelectPresetSeconds = onSelectTimerPreset,
                    onAdjustSeconds = onAdjustTimerSeconds,
                    onToggleStartPause = {
                        if (!uiState.timerRunning &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !SeleniaAlertManager.hasNotificationPermission(context)
                        ) {
                            timerNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onToggleTimer()
                    },
                    onReset = onResetTimer,
                    onReachedZero = onTimerReachedZero
                )
            }
        }
    }
}

@Immutable
enum class WatchDialPreset(
    val spanishTitle: String,
    val dialCenterColor: Color,
    val dialEdgeColor: Color,
    val bezelGoldColor: Color,
    val innerRingColor: Color,
    val hourHandColor: Color,
    val minuteHandColor: Color,
    val secondHandColor: Color
) {
    SAPPHIRE_ASTRAL(
        spanishTitle = "Zafiro Astral",
        dialCenterColor = Color(0xFF1F387A),
        dialEdgeColor = Color(0xFF0A1228),
        bezelGoldColor = Color(0xFFFFD54F),
        innerRingColor = Color(0xFF4DD0E1),
        hourHandColor = Color(0xFFFFD54F),
        minuteHandColor = Color(0xFFF4F7FF),
        secondHandColor = Color(0xFFFF5252)
    ),
    IMPERIAL_GOLD(
        spanishTitle = "Oro Imperial",
        dialCenterColor = Color(0xFF4C3510),
        dialEdgeColor = Color(0xFF1A1105),
        bezelGoldColor = Color(0xFFFFCA28),
        innerRingColor = Color(0xFFFFE082),
        hourHandColor = Color(0xFFFFD54F),
        minuteHandColor = Color(0xFFFFF8E1),
        secondHandColor = Color(0xFFFF6E40)
    ),
    EMERALD_BOREAL(
        spanishTitle = "Esmeralda Boreal",
        dialCenterColor = Color(0xFF0E443A),
        dialEdgeColor = Color(0xFF061B18),
        bezelGoldColor = Color(0xFF69F0AE),
        innerRingColor = Color(0xFF4DD0E1),
        hourHandColor = Color(0xFF69F0AE),
        minuteHandColor = Color(0xFFE0F2F1),
        secondHandColor = Color(0xFFFFD54F)
    ),
    RUBY_METEORITE(
        spanishTitle = "Rubí Meteorito",
        dialCenterColor = Color(0xFF4F132A),
        dialEdgeColor = Color(0xFF1E0711),
        bezelGoldColor = Color(0xFFFF8A80),
        innerRingColor = Color(0xFFFFB300),
        hourHandColor = Color(0xFFFFCA28),
        minuteHandColor = Color(0xFFFCE4EC),
        secondHandColor = Color(0xFF40C4FF)
    ),
    SIDEREAL_CYAN(
        spanishTitle = "Cian Sideral",
        dialCenterColor = Color(0xFF0B3752),
        dialEdgeColor = Color(0xFF051624),
        bezelGoldColor = Color(0xFF40C4FF),
        innerRingColor = Color(0xFFB388FF),
        hourHandColor = Color(0xFF80D8FF),
        minuteHandColor = Color(0xFFE1F5FE),
        secondHandColor = Color(0xFFFF5252)
    )
}

@Composable
private fun AstronomicalAndWorldClockContent(
    isPowerSaveMode: Boolean,
    is24Hour: Boolean,
    selectedWorldCityIds: List<String>,
    moonPhaseFraction: Double,
    moonPhaseName: String,
    isNorthernHemisphere: Boolean,
    onToggle24Hour: () -> Unit,
    onToggleWorldCity: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Aligned battery-smart ticker:
    // - Normal mode: 250ms aligned ticks (only Canvas draw phase reads currentMillis; digital text uses derivedStateOf 1Hz!)
    // - Battery Saver mode: 1000ms aligned ticks (cuts CPU wakeups by 75%)
    LaunchedEffect(lifecycleOwner, isPowerSaveMode) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val stepMs = if (isPowerSaveMode) 1_000L else 250L
            while (true) {
                val now = System.currentTimeMillis()
                currentMillis = now
                val waitMs = (stepMs - (now % stepMs)).coerceIn(40L, stepMs)
                delay(waitMs)
            }
        }
    }

    // Derived epochs prevent parent LazyColumn from recomposing at 4 Hz!
    val currentSecondEpoch by remember { derivedStateOf { currentMillis / 1_000L } }
    val currentMinuteEpoch by remember { derivedStateOf { currentMillis / 60_000L } }

    var selectedDialPreset by rememberSaveable { mutableStateOf(WatchDialPreset.SAPPHIRE_ASTRAL) }
    var showCitySelectorDialog by rememberSaveable { mutableStateOf(false) }
    var citySearchQuery by rememberSaveable { mutableStateOf("") }

    if (showCitySelectorDialog) {
        val filteredCities = remember(citySearchQuery) {
            val q = citySearchQuery.trim().lowercase(Locale.ROOT)
            if (q.isEmpty()) WORLD_CITIES
            else WORLD_CITIES.filter {
                it.city.lowercase(Locale.ROOT).contains(q) ||
                    it.region.lowercase(Locale.ROOT).contains(q) ||
                    it.timeZoneId.lowercase(Locale.ROOT).contains(q)
            }
        }
        AlertDialog(
            onDismissRequest = { showCitySelectorDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.manage_world_cities),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = citySearchQuery,
                        onValueChange = { citySearchQuery = it },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        label = { Text("Buscar ciudad o región") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("world_city_search_input")
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCities, key = { it.timeZoneId }) { spec ->
                            val isSelected = spec.timeZoneId in selectedWorldCityIds
                            Surface(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleWorldCity(spec.timeZoneId) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = spec.city,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${spec.region} • ${spec.timeZoneId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seleccionada",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCitySelectorDialog = false }) {
                    Text("Listo")
                }
            }
        )
    }

    val activeCities = remember(selectedWorldCityIds) {
        val resolved = selectedWorldCityIds.mapNotNull { WORLD_CITIES_BY_ID[it] }
        resolved.ifEmpty { WORLD_CITIES.take(6) }
    }

    // World city entries only recompute once per minute (1/60 Hz)!
    val worldCityEntries = remember(currentMinuteEpoch, is24Hour, activeCities) {
        val minuteMs = currentMinuteEpoch * 60_000L
        val cityCal = Calendar.getInstance()
        activeCities.map { citySpec ->
            cityCal.timeZone = citySpec.timeZone
            cityCal.timeInMillis = minuteMs
            val cHour = cityCal.get(Calendar.HOUR_OF_DAY)
            val cMin = cityCal.get(Calendar.MINUTE)
            val isDaylight = cHour in 6..18
            val timeText = if (is24Hour) {
                "%02d:%02d".format(Locale.US, cHour, cMin)
            } else {
                val h12 = if (cHour % 12 == 0) 12 else cHour % 12
                val amPm = if (cHour < 12) "AM" else "PM"
                "%02d:%02d %s".format(Locale.US, h12, cMin, amPm)
            }
            WorldCityFormattedEntry(
                timeZoneId = citySpec.timeZoneId,
                city = citySpec.city,
                region = citySpec.region,
                isDaylight = isDaylight,
                timeText = timeText
            )
        }
    }

    val watchLitColor = selectedDialPreset.bezelGoldColor
    val localZoneId = remember { TimeZone.getDefault().id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("world_clock_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // 1. Hero Moonphase Astronomical Watch Card with 3D Dial Presets
        item(key = "hero_watch_card") {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reloj Astronómico con Fase Lunar",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        AssistChip(
                            onClick = onToggle24Hour,
                            label = {
                                Text(
                                    if (is24Hour) {
                                        stringResource(R.string.clock_format_24h)
                                    } else {
                                        stringResource(R.string.clock_format_12h)
                                    }
                                )
                            },
                            modifier = Modifier.testTag("toggle_clock_format_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5-Mode 3D Horology Dial Preset Selector
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("watch_dial_preset_row")
                    ) {
                        items(WatchDialPreset.entries, key = { it.name }) { preset ->
                            val isSelected = selectedDialPreset == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDialPreset = preset },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(preset.bezelGoldColor, preset.dialCenterColor)
                                                )
                                            )
                                            .border(1.dp, preset.innerRingColor, CircleShape)
                                    )
                                },
                                label = {
                                    Text(
                                        text = preset.spanishTitle,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("watch_dial_chip_${preset.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Analog Astrolabe Watch Face: deferred state read in draw phase -> 0 recompositions at 4 Hz!
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(230.dp)
                            .testTag("analog_astronomical_clock")
                    ) {
                        AstronomicalAnalogClockCanvas(
                            timeMillisProvider = { currentMillis },
                            dialPreset = selectedDialPreset
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 74.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(1.dp, selectedDialPreset.bezelGoldColor.copy(alpha = 0.75f), CircleShape)
                                    .padding(2.dp)
                            ) {
                                RealisticMoonCanvas(
                                    phaseFraction = moonPhaseFraction,
                                    isNorthernHemisphere = isNorthernHemisphere,
                                    showAmbientGlow = false,
                                    litColor = watchLitColor
                                )
                            }
                            Text(
                                text = moonPhaseName,
                                style = MaterialTheme.typography.labelSmall,
                                color = selectedDialPreset.bezelGoldColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Leaf 1-Hz Digital, UTC, and Sidereal Readout
                    OneHertzAstronomicalReadout(
                        secondEpoch = currentSecondEpoch,
                        is24Hour = is24Hour,
                        localZoneId = localZoneId,
                        dialPreset = selectedDialPreset
                    )
                }
            }
        }

        // 2. World Clocks Section
        item(key = "world_clocks_card") {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.world_clocks_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AssistChip(
                            onClick = { showCitySelectorDialog = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Ciudades (${activeCities.size})") },
                            modifier = Modifier.testTag("manage_world_cities_button")
                        )
                    }

                    worldCityEntries.forEach { entry ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SeleniaIconBadge(
                                        icon = if (entry.isDaylight) {
                                            SeleniaVectorIcons.WbSunny
                                        } else {
                                            SeleniaVectorIcons.NightsStay
                                        },
                                        tint = if (entry.isDaylight) {
                                            Color(0xFFFFD54F)
                                        } else {
                                            Color(0xFF4DD0E1)
                                        },
                                        size = 36.dp,
                                        iconSize = 18.dp,
                                        isCircular = true
                                    )
                                    Column {
                                        Text(
                                            text = entry.city,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = entry.region,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = entry.timeText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.isDaylight) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    }
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
private fun OneHertzAstronomicalReadout(
    secondEpoch: Long,
    is24Hour: Boolean,
    localZoneId: String,
    dialPreset: WatchDialPreset
) {
    val localCal = remember { Calendar.getInstance() }
    val utcCal = remember { Calendar.getInstance(TimeZone.getTimeZone("UTC")) }

    val formattedDigitalTime = remember(secondEpoch, is24Hour) {
        localCal.timeInMillis = secondEpoch * 1_000L
        val hour24 = localCal.get(Calendar.HOUR_OF_DAY)
        val minute = localCal.get(Calendar.MINUTE)
        val second = localCal.get(Calendar.SECOND)
        if (is24Hour) {
            "%02d:%02d:%02d".format(Locale.US, hour24, minute, second)
        } else {
            val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
            val amPm = if (hour24 < 12) "AM" else "PM"
            "%02d:%02d:%02d %s".format(Locale.US, h12, minute, second, amPm)
        }
    }

    val utcFormatted = remember(secondEpoch) {
        utcCal.timeInMillis = secondEpoch * 1_000L
        "%02d:%02d:%02d UTC".format(
            Locale.US,
            utcCal.get(Calendar.HOUR_OF_DAY),
            utcCal.get(Calendar.MINUTE),
            utcCal.get(Calendar.SECOND)
        )
    }

    val gmstString = remember(secondEpoch) {
        val jd = ((secondEpoch * 1_000L) / 86400000.0) + 2440587.5
        val d = jd - 2451545.0
        var gmstHours = (18.697374558 + 24.06570982441908 * d) % 24.0
        if (gmstHours < 0) gmstHours += 24.0
        val gmstH = gmstHours.toInt()
        val gmstM = ((gmstHours - gmstH) * 60.0).toInt()
        val gmstS = ((((gmstHours - gmstH) * 60.0) - gmstM) * 60.0).toInt()
        "%02dh %02dm %02ds".format(Locale.US, gmstH, gmstM, gmstS)
    }

    Text(
        text = formattedDigitalTime,
        style = MaterialTheme.typography.displayMedium,
        color = dialPreset.bezelGoldColor,
        modifier = Modifier.testTag("digital_clock_display")
    )

    Text(
        text = "Zona local: $localZoneId • Esfera: ${dialPreset.spanishTitle}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = "Tiempo Universal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = utcFormatted,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = dialPreset.innerRingColor
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = "Tiempo Sideral (GMST)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = gmstString,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = dialPreset.bezelGoldColor
                )
            }
        }
    }
}

/**
 * Analog watch face using [drawWithCache] for all static shaders/geometry and reading
 * [timeMillisProvider] strictly inside [onDrawBehind], eliminating 100% of Compose recompositions
 * and shader allocations on sub-second hand updates.
 */
@Composable
private fun AstronomicalAnalogClockCanvas(
    timeMillisProvider: () -> Long,
    dialPreset: WatchDialPreset
) {
    val drawCalendar = remember { Calendar.getInstance() }
    val bezelGold = dialPreset.bezelGoldColor
    val innerCyan = dialPreset.innerRingColor
    val dialCenterColor = dialPreset.dialCenterColor
    val dialEdgeColor = dialPreset.dialEdgeColor
    val hourHandColor = dialPreset.hourHandColor
    val minuteHandColor = dialPreset.minuteHandColor
    val secondHandColor = dialPreset.secondHandColor

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                val haloBrush = Brush.radialGradient(
                    colors = listOf(
                        bezelGold.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                )

                val dialBrush = Brush.radialGradient(
                    colors = listOf(
                        dialCenterColor,
                        dialEdgeColor
                    ),
                    center = Offset(center.x - radius * 0.22f, center.y - radius * 0.22f),
                    radius = radius * 1.15f
                )

                val glareBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        innerCyan.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    start = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
                    end = center
                )

                val glareTopLeft = Offset(center.x - radius * 0.91f, center.y - radius * 0.91f)
                val glareSize = Size(radius * 1.82f, radius * 1.82f)
                val bezelRingColor = bezelGold.copy(alpha = 0.86f)
                val innerRingStrokeColor = innerCyan.copy(alpha = 0.42f)
                val minorTickColor = minuteHandColor.copy(alpha = 0.35f)
                val shadowColor = Color.Black.copy(alpha = 0.45f)
                val hourSpineColor = Color.White.copy(alpha = 0.7f)
                val minSpineColor = innerCyan.copy(alpha = 0.85f)

                onDrawBehind {
                    drawCalendar.timeInMillis = timeMillisProvider()
                    val hour = drawCalendar.get(Calendar.HOUR_OF_DAY)
                    val minute = drawCalendar.get(Calendar.MINUTE)
                    val second = drawCalendar.get(Calendar.SECOND)
                    val millis = drawCalendar.get(Calendar.MILLISECOND)

                    // 1. 3D Outer Metallic Bezel Halo
                    drawCircle(
                        brush = haloBrush,
                        radius = radius,
                        center = center
                    )

                    // 2. 3D Sunburst Dial Background
                    drawCircle(
                        brush = dialBrush,
                        radius = radius * 0.95f,
                        center = center
                    )

                    // 3. 3D Sapphire Crystal Specular Reflection Glare Arc
                    drawArc(
                        brush = glareBrush,
                        startAngle = 195f,
                        sweepAngle = 125f,
                        useCenter = true,
                        topLeft = glareTopLeft,
                        size = glareSize
                    )

                    // 4. Outer & Inner Astrolabe Bezel Rings
                    drawCircle(
                        color = bezelRingColor,
                        radius = radius * 0.95f,
                        center = center,
                        style = OUTER_BEZEL_STROKE
                    )
                    drawCircle(
                        color = innerRingStrokeColor,
                        radius = radius * 0.86f,
                        center = center,
                        style = INNER_BEZEL_STROKE
                    )

                    // 5. 60 minute / 12 hour ticks + 4 Cardinal Luminous Jewels
                    val outerR = radius * 0.90f
                    val majorInnerR = radius * 0.77f
                    val minorInnerR = radius * 0.85f
                    val cardinalInnerR = radius * 0.74f
                    for (i in 0 until 60) {
                        val cosA = WATCH_TICK_COS[i]
                        val sinA = WATCH_TICK_SIN[i]
                        val isCardinal = i % 15 == 0
                        val isMajorHour = i % 5 == 0
                        val innerR = when {
                            isCardinal -> cardinalInnerR
                            isMajorHour -> majorInnerR
                            else -> minorInnerR
                        }
                        drawLine(
                            color = when {
                                isCardinal -> innerCyan
                                isMajorHour -> bezelGold
                                else -> minorTickColor
                            },
                            start = Offset(center.x + innerR * cosA, center.y + innerR * sinA),
                            end = Offset(center.x + outerR * cosA, center.y + outerR * sinA),
                            strokeWidth = when {
                                isCardinal -> 4.8f
                                isMajorHour -> 3.4f
                                else -> 1.4f
                            },
                            cap = StrokeCap.Round
                        )
                    }

                    val smoothSecond = second + (millis / 1000f)
                    val smoothMinute = minute + (smoothSecond / 60f)
                    val smoothHour = (hour % 12) + (smoothMinute / 60f)

                    // 6. 3D Hour Hand
                    val hourAngle = (smoothHour * 30.0 - 90.0) * (PI / 180.0)
                    val hourCos = cos(hourAngle).toFloat()
                    val hourSin = sin(hourAngle).toFloat()
                    val hourEnd = Offset(
                        x = center.x + radius * 0.49f * hourCos,
                        y = center.y + radius * 0.49f * hourSin
                    )
                    drawLine(
                        color = shadowColor,
                        start = Offset(center.x + 2f, center.y + 3f),
                        end = Offset(hourEnd.x + 2f, hourEnd.y + 3f),
                        strokeWidth = 8.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = hourHandColor,
                        start = center,
                        end = hourEnd,
                        strokeWidth = 7.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = hourSpineColor,
                        start = center,
                        end = Offset(
                            x = center.x + radius * 0.36f * hourCos,
                            y = center.y + radius * 0.36f * hourSin
                        ),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )

                    // 7. 3D Minute Hand
                    val minAngle = (smoothMinute * 6.0 - 90.0) * (PI / 180.0)
                    val minCos = cos(minAngle).toFloat()
                    val minSin = sin(minAngle).toFloat()
                    val minEnd = Offset(
                        x = center.x + radius * 0.69f * minCos,
                        y = center.y + radius * 0.69f * minSin
                    )
                    drawLine(
                        color = shadowColor,
                        start = Offset(center.x + 2f, center.y + 3f),
                        end = Offset(minEnd.x + 2f, minEnd.y + 3f),
                        strokeWidth = 5.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = minuteHandColor,
                        start = center,
                        end = minEnd,
                        strokeWidth = 4.8f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = minSpineColor,
                        start = center,
                        end = Offset(
                            x = center.x + radius * 0.52f * minCos,
                            y = center.y + radius * 0.52f * minSin
                        ),
                        strokeWidth = 1.6f,
                        cap = StrokeCap.Round
                    )

                    // 8. 3D Chronograph Second Hand + Jewel Cap
                    val secAngle = (smoothSecond * 6.0 - 90.0) * (PI / 180.0)
                    val secCos = cos(secAngle).toFloat()
                    val secSin = sin(secAngle).toFloat()
                    drawLine(
                        color = secondHandColor,
                        start = Offset(
                            x = center.x - radius * 0.16f * secCos,
                            y = center.y - radius * 0.16f * secSin
                        ),
                        end = Offset(
                            x = center.x + radius * 0.78f * secCos,
                            y = center.y + radius * 0.78f * secSin
                        ),
                        strokeWidth = 2.4f,
                        cap = StrokeCap.Round
                    )

                    drawCircle(color = bezelGold, radius = 7.5f, center = center)
                    drawCircle(color = secondHandColor, radius = 4.2f, center = center)
                    drawCircle(
                        color = Color.White,
                        radius = 1.6f,
                        center = Offset(center.x - 1.2f, center.y - 1.2f)
                    )
                }
            }
    )
}

@Composable
private fun StopwatchContent(
    isPowerSaveMode: Boolean,
    isRunning: Boolean,
    accumulatedMs: Long,
    lastStartMonoMs: Long,
    laps: List<StopwatchLap>,
    onToggleStartPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit
) {
    val hasProgress = isRunning || accumulatedMs > 0L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stopwatch_content_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item(key = "stopwatch_main_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = stringResource(R.string.clock_tab_stopwatch),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Isolated leaf ticker: buttons and lap list never recompose on 33ms ticks!
                    LiveStopwatchReadoutText(
                        isPowerSaveMode = isPowerSaveMode,
                        isRunning = isRunning,
                        accumulatedMs = accumulatedMs,
                        lastStartMonoMs = lastStartMonoMs
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        Button(
                            onClick = onToggleStartPause,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                            modifier = Modifier.testTag("stopwatch_start_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isRunning) SeleniaVectorIcons.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isRunning) {
                                    stringResource(R.string.stopwatch_pause)
                                } else {
                                    stringResource(R.string.stopwatch_start)
                                }
                            )
                        }

                        FilledTonalButton(
                            onClick = onLap,
                            enabled = hasProgress,
                            modifier = Modifier.testTag("stopwatch_lap_button")
                        ) {
                            Icon(imageVector = SeleniaVectorIcons.Flag, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.stopwatch_lap))
                        }

                        FilledTonalButton(
                            onClick = onReset,
                            enabled = hasProgress,
                            modifier = Modifier.testTag("stopwatch_reset_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.stopwatch_reset))
                        }
                    }
                }
            }
        }

        if (laps.isNotEmpty()) {
            item(key = "stopwatch_laps_card") {
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
                            text = "Registro de Vueltas (${laps.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        laps.forEach { lap ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Vuelta #${lap.lapNumber}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "+${formatStopwatchMs(lap.lapDurationMs)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatStopwatchMs(lap.totalElapsedMs),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
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

@Composable
private fun LiveStopwatchReadoutText(
    isPowerSaveMode: Boolean,
    isRunning: Boolean,
    accumulatedMs: Long,
    lastStartMonoMs: Long
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var displayMs by remember(isRunning, accumulatedMs, lastStartMonoMs) {
        mutableLongStateOf(
            if (isRunning) {
                accumulatedMs + (currentMonotonicMs() - lastStartMonoMs).coerceAtLeast(0L)
            } else {
                accumulatedMs
            }
        )
    }

    LaunchedEffect(isRunning, accumulatedMs, lastStartMonoMs, isPowerSaveMode, lifecycleOwner) {
        if (isRunning) {
            val delayMs = if (isPowerSaveMode) 100L else 33L
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    displayMs = accumulatedMs + (currentMonotonicMs() - lastStartMonoMs).coerceAtLeast(0L)
                    delay(delayMs)
                }
            }
        } else {
            displayMs = accumulatedMs
        }
    }

    Text(
        text = formatStopwatchMs(displayMs),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("stopwatch_time_display")
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerContent(
    isPowerSaveMode: Boolean,
    totalSeconds: Int,
    remainingPausedMs: Long,
    endTargetMonoMs: Long,
    isRunning: Boolean,
    isFinishedAlert: Boolean,
    onSelectPresetSeconds: (Int) -> Unit,
    onAdjustSeconds: (Int) -> Unit,
    onToggleStartPause: () -> Unit,
    onReset: () -> Unit,
    onReachedZero: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("timer_content_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item(key = "timer_main_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Isolated leaf countdown dial: preset chips & buttons never recompose during ticks!
                    LiveTimerCountdownDial(
                        isPowerSaveMode = isPowerSaveMode,
                        totalSeconds = totalSeconds,
                        remainingPausedMs = remainingPausedMs,
                        endTargetMonoMs = endTargetMonoMs,
                        isRunning = isRunning,
                        isFinishedAlert = isFinishedAlert,
                        onReachedZero = onReachedZero
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { onAdjustSeconds(-60) },
                            label = { Text("-1 min") }
                        )
                        AssistChip(
                            onClick = { onAdjustSeconds(30) },
                            label = { Text("+30 seg") }
                        )
                        AssistChip(
                            onClick = { onAdjustSeconds(60) },
                            label = { Text("+1 min") }
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TIMER_PRESETS.forEach { (sec, label) ->
                            FilterChip(
                                selected = totalSeconds == sec,
                                onClick = { onSelectPresetSeconds(sec) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onToggleStartPause,
                            modifier = Modifier.testTag("timer_start_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isRunning) SeleniaVectorIcons.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isRunning) {
                                    stringResource(R.string.stopwatch_pause)
                                } else {
                                    stringResource(R.string.stopwatch_start)
                                }
                            )
                        }

                        FilledTonalButton(
                            onClick = onReset,
                            modifier = Modifier.testTag("timer_reset_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.stopwatch_reset))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveTimerCountdownDial(
    isPowerSaveMode: Boolean,
    totalSeconds: Int,
    remainingPausedMs: Long,
    endTargetMonoMs: Long,
    isRunning: Boolean,
    isFinishedAlert: Boolean,
    onReachedZero: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var liveRemainingMs by remember(isRunning, remainingPausedMs, endTargetMonoMs) {
        mutableLongStateOf(
            if (isRunning) {
                (endTargetMonoMs - currentMonotonicMs()).coerceAtLeast(0L)
            } else {
                remainingPausedMs
            }
        )
    }

    LaunchedEffect(isRunning, endTargetMonoMs, remainingPausedMs, isPowerSaveMode, lifecycleOwner) {
        if (isRunning) {
            val delayMs = if (isPowerSaveMode) 350L else 66L
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val left = (endTargetMonoMs - currentMonotonicMs()).coerceAtLeast(0L)
                    liveRemainingMs = left
                    if (left == 0L) {
                        onReachedZero()
                        break
                    }
                    delay(delayMs)
                }
            }
        } else {
            liveRemainingMs = remainingPausedMs
        }
    }

    val totalMs = (totalSeconds * 1000L).coerceAtLeast(1000L)
    val displaySeconds by remember(totalSeconds) {
        derivedStateOf { ((liveRemainingMs + 999L) / 1000L).toInt() }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 10.dp.toPx()
            val arcSize = Size(size.width - strokeW, size.height - strokeW)
            val topLeft = Offset(strokeW / 2f, strokeW / 2f)
            val strokeStyle = Stroke(width = strokeW, cap = StrokeCap.Round)
            val progress = (liveRemainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = strokeStyle
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = strokeStyle
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val mins = displaySeconds / 60
            val secs = displaySeconds % 60
            Text(
                text = "%02d:%02d".format(Locale.US, mins, secs),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("timer_countdown_display")
            )
            if (isFinishedAlert) {
                Text(
                    text = stringResource(R.string.timer_finished_message),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatStopwatchMs(ms: Long): String {
    val totalCentiseconds = (ms / 10L).coerceAtLeast(0L)
    val centiseconds = (totalCentiseconds % 100).toInt()
    val totalSeconds = totalCentiseconds / 100
    val seconds = (totalSeconds % 60).toInt()
    val minutes = (totalSeconds / 60).toInt()
    if (minutes < 100) {
        val buf = CharArray(8)
        buf[0] = ('0'.code + (minutes / 10)).toChar()
        buf[1] = ('0'.code + (minutes % 10)).toChar()
        buf[2] = ':'
        buf[3] = ('0'.code + (seconds / 10)).toChar()
        buf[4] = ('0'.code + (seconds % 10)).toChar()
        buf[5] = '.'
        buf[6] = ('0'.code + (centiseconds / 10)).toChar()
        buf[7] = ('0'.code + (centiseconds % 10)).toChar()
        return String(buf)
    }
    return "%02d:%02d.%02d".format(Locale.US, minutes, seconds, centiseconds)
}
