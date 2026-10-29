package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.ClockSubTab
import com.example.ui.ModuleThemeTarget
import com.example.ui.ModuleVisualSkin
import com.example.ui.SeleniaUiState
import com.example.ui.components.ModuleSkinBox
import com.example.ui.components.RealisticMoonCanvas
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class StopwatchLap(
    val lapNumber: Int,
    val lapDurationMs: Long,
    val totalElapsedMs: Long
)

private data class WorldCitySpec(
    val city: String,
    val region: String,
    val timeZoneId: String
)

private val WORLD_CITIES = listOf(
    WorldCitySpec("Tiempo Universal (UTC)", "Referencia Astronómica", "UTC"),
    WorldCitySpec("Madrid / París", "Europa Central", "Europe/Madrid"),
    WorldCitySpec("Ciudad de México", "América Central", "America/Mexico_City"),
    WorldCitySpec("Bogotá / Lima", "Andes / Pacífico", "America/Bogota"),
    WorldCitySpec("Buenos Aires / Santiago", "Cono Sur", "America/Argentina/Buenos_Aires"),
    WorldCitySpec("Nueva York / Miami", "Costa Este EE. UU.", "America/New_York"),
    WorldCitySpec("Tokio", "Asia Oriental", "Asia/Tokyo")
)

@Composable
fun ClockScreen(
    uiState: SeleniaUiState,
    onSelectSubTab: (ClockSubTab) -> Unit,
    onToggle24HourFormat: () -> Unit,
    onOpenColorCustomizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val skin = uiState.skinFor(ModuleThemeTarget.CLOCK)

    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(200L)
        }
    }

    // Stopwatch state
    var stopwatchRunning by rememberSaveable { mutableStateOf(false) }
    var stopwatchAccumulatedMs by rememberSaveable { mutableLongStateOf(0L) }
    var stopwatchLastStartMs by rememberSaveable { mutableLongStateOf(0L) }
    var stopwatchDisplayMs by remember { mutableLongStateOf(0L) }
    val stopwatchLaps = remember { mutableStateListOf<StopwatchLap>() }

    LaunchedEffect(stopwatchRunning, stopwatchAccumulatedMs, stopwatchLastStartMs) {
        while (stopwatchRunning) {
            stopwatchDisplayMs = stopwatchAccumulatedMs + (System.currentTimeMillis() - stopwatchLastStartMs)
            delay(30L)
        }
        stopwatchDisplayMs = stopwatchAccumulatedMs
    }

    // Timer state
    var timerTotalSeconds by rememberSaveable { mutableIntStateOf(300) }
    var timerRemainingMs by rememberSaveable { mutableLongStateOf(300_000L) }
    var timerRunning by rememberSaveable { mutableStateOf(false) }
    var timerEndTargetMs by rememberSaveable { mutableLongStateOf(0L) }
    var timerFinishedAlert by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(timerRunning, timerEndTargetMs) {
        while (timerRunning) {
            val left = (timerEndTargetMs - System.currentTimeMillis()).coerceAtLeast(0L)
            timerRemainingMs = left
            if (left == 0L) {
                timerRunning = false
                timerFinishedAlert = true
            }
            delay(50L)
        }
    }

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
                            imageVector = Icons.Default.AccessTime,
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
                            imageVector = Icons.Default.Timer,
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
                            imageVector = Icons.Default.HourglassBottom,
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
                    currentMillis = currentMillis,
                    is24Hour = uiState.is24HourFormat,
                    moonPhaseFraction = uiState.selectedDayLunarInfo.phaseFraction,
                    moonPhaseName = uiState.selectedDayLunarInfo.phaseType.spanishName,
                    isNorthernHemisphere = uiState.isNorthernHemisphere,
                    skin = skin,
                    onToggle24Hour = onToggle24HourFormat,
                    onOpenColorCustomizer = onOpenColorCustomizer
                )

                ClockSubTab.STOPWATCH -> StopwatchContent(
                    elapsedMs = stopwatchDisplayMs,
                    isRunning = stopwatchRunning,
                    laps = stopwatchLaps,
                    skin = skin,
                    onToggleStartPause = {
                        if (stopwatchRunning) {
                            stopwatchAccumulatedMs += (System.currentTimeMillis() - stopwatchLastStartMs)
                            stopwatchRunning = false
                        } else {
                            stopwatchLastStartMs = System.currentTimeMillis()
                            stopwatchRunning = true
                        }
                    },
                    onLap = {
                        val currentTotal = stopwatchDisplayMs
                        val previousTotal = stopwatchLaps.firstOrNull()?.totalElapsedMs ?: 0L
                        val lapDiff = (currentTotal - previousTotal).coerceAtLeast(0L)
                        stopwatchLaps.add(
                            0,
                            StopwatchLap(
                                lapNumber = stopwatchLaps.size + 1,
                                lapDurationMs = lapDiff,
                                totalElapsedMs = currentTotal
                            )
                        )
                    },
                    onReset = {
                        stopwatchRunning = false
                        stopwatchAccumulatedMs = 0L
                        stopwatchDisplayMs = 0L
                        stopwatchLaps.clear()
                    }
                )

                ClockSubTab.TIMER -> TimerContent(
                    totalSeconds = timerTotalSeconds,
                    remainingMs = timerRemainingMs,
                    isRunning = timerRunning,
                    isFinishedAlert = timerFinishedAlert,
                    skin = skin,
                    onSelectPresetSeconds = { seconds ->
                        timerRunning = false
                        timerFinishedAlert = false
                        timerTotalSeconds = seconds
                        timerRemainingMs = seconds * 1000L
                    },
                    onAdjustSeconds = { deltaSec ->
                        val nextSec = (timerTotalSeconds + deltaSec).coerceIn(10, 7200)
                        timerTotalSeconds = nextSec
                        if (!timerRunning) {
                            timerFinishedAlert = false
                            timerRemainingMs = nextSec * 1000L
                        } else {
                            timerEndTargetMs += deltaSec * 1000L
                        }
                    },
                    onToggleStartPause = {
                        timerFinishedAlert = false
                        if (timerRunning) {
                            timerRemainingMs = (timerEndTargetMs - System.currentTimeMillis()).coerceAtLeast(0L)
                            timerRunning = false
                        } else {
                            if (timerRemainingMs <= 0L) {
                                timerRemainingMs = timerTotalSeconds * 1000L
                            }
                            timerEndTargetMs = System.currentTimeMillis() + timerRemainingMs
                            timerRunning = true
                        }
                    },
                    onReset = {
                        timerRunning = false
                        timerFinishedAlert = false
                        timerRemainingMs = timerTotalSeconds * 1000L
                    }
                )
            }
        }
    }
}

@Composable
private fun AstronomicalAndWorldClockContent(
    currentMillis: Long,
    is24Hour: Boolean,
    moonPhaseFraction: Double,
    moonPhaseName: String,
    isNorthernHemisphere: Boolean,
    skin: ModuleVisualSkin,
    onToggle24Hour: () -> Unit,
    onOpenColorCustomizer: () -> Unit
) {
    val localCal = remember(currentMillis) {
        Calendar.getInstance().apply { timeInMillis = currentMillis }
    }
    val utcCal = remember(currentMillis) {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = currentMillis }
    }

    val hour24 = localCal.get(Calendar.HOUR_OF_DAY)
    val minute = localCal.get(Calendar.MINUTE)
    val second = localCal.get(Calendar.SECOND)
    val millis = localCal.get(Calendar.MILLISECOND)

    val formattedDigitalTime = if (is24Hour) {
        "%02d:%02d:%02d".format(Locale.US, hour24, minute, second)
    } else {
        val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        val amPm = if (hour24 < 12) "AM" else "PM"
        "%02d:%02d:%02d %s".format(Locale.US, h12, minute, second, amPm)
    }

    val utcFormatted = "%02d:%02d:%02d UTC".format(
        Locale.US,
        utcCal.get(Calendar.HOUR_OF_DAY),
        utcCal.get(Calendar.MINUTE),
        utcCal.get(Calendar.SECOND)
    )

    val jd = (currentMillis / 86400000.0) + 2440587.5
    val d = jd - 2451545.0
    var gmstHours = (18.697374558 + 24.06570982441908 * d) % 24.0
    if (gmstHours < 0) gmstHours += 24.0
    val gmstH = gmstHours.toInt()
    val gmstM = ((gmstHours - gmstH) * 60.0).toInt()
    val gmstS = ((((gmstHours - gmstH) * 60.0) - gmstM) * 60.0).toInt()
    val gmstString = "%02dh %02dm %02ds".format(Locale.US, gmstH, gmstM, gmstS)

    val watchLitColor = if (skin.isCustomApplied) {
        Color(skin.primarySwatch.rgb.r, skin.primarySwatch.rgb.g, skin.primarySwatch.rgb.b)
    } else {
        Color(0xFFF7E7B4)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("world_clock_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // 1. Hero Moonphase Astronomical Watch Card with custom Clock Skin
        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                        listOf(
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
                            Text(
                                text = "Reloj Astronómico con Fase Lunar",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = onOpenColorCustomizer,
                                    modifier = Modifier.testTag("clock_customize_color_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Personalizar color del Reloj",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
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
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Analog Astrolabe Watch Face with custom Dial Gradient & Hands
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(230.dp)
                                .testTag("analog_astronomical_clock")
                        ) {
                            AstronomicalAnalogClockCanvas(
                                hour = hour24,
                                minute = minute,
                                second = second,
                                millis = millis,
                                skin = skin
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
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
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
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = formattedDigitalTime,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("digital_clock_display")
                        )

                        Text(
                            text = "Zona local: ${TimeZone.getDefault().id}",
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
                                        color = MaterialTheme.colorScheme.secondary
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
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. World Clocks Section
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
                        text = stringResource(R.string.world_clocks_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    WORLD_CITIES.forEach { citySpec ->
                        val tz = TimeZone.getTimeZone(citySpec.timeZoneId)
                        val cityCal = Calendar.getInstance(tz).apply { timeInMillis = currentMillis }
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (isDaylight) "☀️" else "🌙",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Column {
                                        Text(
                                            text = citySpec.city,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = citySpec.region,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDaylight) {
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
private fun AstronomicalAnalogClockCanvas(
    hour: Int,
    minute: Int,
    second: Int,
    millis: Int,
    skin: ModuleVisualSkin
) {
    val goldColor = MaterialTheme.colorScheme.primary
    val cyanColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.background
    val markerColor = MaterialTheme.colorScheme.onSurface

    val dialCenterColor = if (skin.isCustomApplied) {
        val p = skin.primarySwatch.rgb
        Color(p.r, p.g, p.b).copy(alpha = 0.35f)
    } else {
        Color(0xFF1A2550)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Dial background with custom skin tint
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    dialCenterColor,
                    surfaceColor
                ),
                center = center,
                radius = radius
            ),
            radius = radius * 0.95f,
            center = center
        )

        // Outer bezel rings
        drawCircle(
            color = goldColor.copy(alpha = 0.72f),
            radius = radius * 0.95f,
            center = center,
            style = Stroke(width = 3.2f)
        )
        drawCircle(
            color = cyanColor.copy(alpha = 0.32f),
            radius = radius * 0.86f,
            center = center,
            style = Stroke(width = 1.4f)
        )

        // 60 minute / 12 hour ticks
        for (i in 0 until 60) {
            val angleRad = (i * 6.0 - 90.0) * (PI / 180.0)
            val isMajorHour = i % 5 == 0
            val outerR = radius * 0.90f
            val innerR = if (isMajorHour) radius * 0.78f else radius * 0.85f
            val start = Offset(
                x = center.x + (innerR * cos(angleRad)).toFloat(),
                y = center.y + (innerR * sin(angleRad)).toFloat()
            )
            val end = Offset(
                x = center.x + (outerR * cos(angleRad)).toFloat(),
                y = center.y + (outerR * sin(angleRad)).toFloat()
            )
            drawLine(
                color = if (isMajorHour) goldColor else markerColor.copy(alpha = 0.35f),
                start = start,
                end = end,
                strokeWidth = if (isMajorHour) 3.5f else 1.5f,
                cap = StrokeCap.Round
            )
        }

        val smoothSecond = second + (millis / 1000f)
        val smoothMinute = minute + (smoothSecond / 60f)
        val smoothHour = (hour % 12) + (smoothMinute / 60f)

        // Hour hand
        val hourAngle = (smoothHour * 30.0 - 90.0) * (PI / 180.0)
        drawLine(
            color = goldColor,
            start = center,
            end = Offset(
                x = center.x + (radius * 0.48f * cos(hourAngle)).toFloat(),
                y = center.y + (radius * 0.48f * sin(hourAngle)).toFloat()
            ),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )

        // Minute hand
        val minAngle = (smoothMinute * 6.0 - 90.0) * (PI / 180.0)
        drawLine(
            color = markerColor,
            start = center,
            end = Offset(
                x = center.x + (radius * 0.68f * cos(minAngle)).toFloat(),
                y = center.y + (radius * 0.68f * sin(minAngle)).toFloat()
            ),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
        )

        // Second hand
        val secAngle = (smoothSecond * 6.0 - 90.0) * (PI / 180.0)
        drawLine(
            color = cyanColor,
            start = Offset(
                x = center.x - (radius * 0.14f * cos(secAngle)).toFloat(),
                y = center.y - (radius * 0.14f * sin(secAngle)).toFloat()
            ),
            end = Offset(
                x = center.x + (radius * 0.76f * cos(secAngle)).toFloat(),
                y = center.y + (radius * 0.76f * sin(secAngle)).toFloat()
            ),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = goldColor,
            radius = 6.5f,
            center = center
        )
        drawCircle(
            color = surfaceColor,
            radius = 2.5f,
            center = center
        )
    }
}

@Composable
private fun StopwatchContent(
    elapsedMs: Long,
    isRunning: Boolean,
    laps: List<StopwatchLap>,
    skin: ModuleVisualSkin,
    onToggleStartPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stopwatch_content_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                ModuleSkinBox(
                    skin = skin,
                    alphaIntensity = 0.38f,
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

                        Text(
                            text = formatStopwatchMs(elapsedMs),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("stopwatch_time_display")
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
                                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
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
                                enabled = elapsedMs > 0L,
                                modifier = Modifier.testTag("stopwatch_lap_button")
                            ) {
                                Icon(imageVector = Icons.Default.Flag, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.stopwatch_lap))
                            }

                            FilledTonalButton(
                                onClick = onReset,
                                enabled = elapsedMs > 0L,
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
        }

        if (laps.isNotEmpty()) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerContent(
    totalSeconds: Int,
    remainingMs: Long,
    isRunning: Boolean,
    isFinishedAlert: Boolean,
    skin: ModuleVisualSkin,
    onSelectPresetSeconds: (Int) -> Unit,
    onAdjustSeconds: (Int) -> Unit,
    onToggleStartPause: () -> Unit,
    onReset: () -> Unit
) {
    val totalMs = (totalSeconds * 1000L).coerceAtLeast(1000L)
    val progress = (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("timer_content_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                ModuleSkinBox(
                    skin = skin,
                    alphaIntensity = 0.38f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = primaryColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val totSecLeft = ((remainingMs + 999L) / 1000L).toInt()
                                val mins = totSecLeft / 60
                                val secs = totSecLeft % 60
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
                            listOf(
                                60 to "1 min",
                                180 to "3 min",
                                300 to "5 min",
                                600 to "10 min",
                                900 to "15 min",
                                1772 to "29.5m (Ciclo)"
                            ).forEach { (sec, label) ->
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
                                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
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
}

private fun formatStopwatchMs(ms: Long): String {
    val totalCentiseconds = (ms / 10L).coerceAtLeast(0L)
    val centiseconds = totalCentiseconds % 100
    val totalSeconds = totalCentiseconds / 100
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60
    return "%02d:%02d.%02d".format(Locale.US, minutes, seconds, centiseconds)
}
