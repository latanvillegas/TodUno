package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.ColorHarmonyEngine
import com.example.ui.AppDestination
import com.example.ui.ModuleThemeTarget
import com.example.ui.ModuleVisualSkin
import com.example.ui.SeleniaUiState
import com.example.ui.SeleniaViewModel
import com.example.ui.components.RealisticMoonCanvas
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ClockScreen
import com.example.ui.screens.ColorStudioScreen
import com.example.ui.screens.MoonObservatoryScreen
import com.example.ui.theme.ApplicationTheme
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApplicationTheme {
                SeleniaApp()
            }
        }
    }
}

private data class NavItemSpec(
    val destination: AppDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * Dynamically overrides MaterialTheme's primary/secondary/tertiary palette for the active module
 * when the user has applied a custom color & gradient skin to that module in Estudio de Colores.
 */
@Composable
private fun ModuleThemedSurface(
    skin: ModuleVisualSkin?,
    content: @Composable () -> Unit
) {
    val baseScheme = MaterialTheme.colorScheme
    val effectiveScheme = remember(skin, baseScheme) {
        if (skin == null || !skin.isCustomApplied) {
            baseScheme
        } else {
            val pRgb = skin.primarySwatch.rgb
            val sRgb = skin.secondarySwatch.rgb
            val primaryColor = Color(pRgb.r, pRgb.g, pRgb.b)
            val secondaryColor = Color(sRgb.r, sRgb.g, sRgb.b)
            val onPrimary = if (ColorHarmonyEngine.contrastRatioWithWhite(pRgb) >=
                ColorHarmonyEngine.contrastRatioWithBlack(pRgb)
            ) {
                Color.White
            } else {
                Color(0xFF090D1E)
            }
            val onSecondary = if (ColorHarmonyEngine.contrastRatioWithWhite(sRgb) >=
                ColorHarmonyEngine.contrastRatioWithBlack(sRgb)
            ) {
                Color.White
            } else {
                Color(0xFF090D1E)
            }
            val thirdStop = skin.gradientStops.getOrElse(2) { skin.secondarySwatch }.rgb
            val tertiaryColor = Color(thirdStop.r, thirdStop.g, thirdStop.b)

            baseScheme.copy(
                primary = primaryColor,
                onPrimary = onPrimary,
                primaryContainer = primaryColor.copy(alpha = 0.24f),
                onPrimaryContainer = Color.White,
                secondary = secondaryColor,
                onSecondary = onSecondary,
                secondaryContainer = secondaryColor.copy(alpha = 0.24f),
                onSecondaryContainer = Color.White,
                tertiary = tertiaryColor
            )
        }
    }

    MaterialTheme(
        colorScheme = effectiveScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

@Composable
fun SeleniaApp(
    viewModel: SeleniaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.currentDestination != AppDestination.CALENDAR) {
        BackHandler {
            viewModel.navigateTo(AppDestination.CALENDAR)
        }
    }

    val navItems = listOf(
        NavItemSpec(
            destination = AppDestination.CALENDAR,
            labelRes = R.string.nav_calendar,
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth,
            testTag = "nav_tab_calendar"
        ),
        NavItemSpec(
            destination = AppDestination.MOON_OBSERVATORY,
            labelRes = R.string.nav_moon,
            selectedIcon = Icons.Filled.NightlightRound,
            unselectedIcon = Icons.Outlined.NightlightRound,
            testTag = "nav_tab_moon"
        ),
        NavItemSpec(
            destination = AppDestination.CLOCK,
            labelRes = R.string.nav_clock,
            selectedIcon = Icons.Filled.AccessTime,
            unselectedIcon = Icons.Outlined.AccessTime,
            testTag = "nav_tab_clock"
        ),
        NavItemSpec(
            destination = AppDestination.CALCULATOR,
            labelRes = R.string.nav_calculator,
            selectedIcon = Icons.Filled.Calculate,
            unselectedIcon = Icons.Outlined.Calculate,
            testTag = "nav_tab_calculator"
        ),
        NavItemSpec(
            destination = AppDestination.COLORS,
            labelRes = R.string.nav_colors,
            selectedIcon = Icons.Filled.Palette,
            unselectedIcon = Icons.Outlined.Palette,
            testTag = "nav_tab_colors"
        )
    )

    val activeModuleSkin = uiState.skinForDestination(uiState.currentDestination)

    ModuleThemedSurface(skin = activeModuleSkin) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val selected = uiState.currentDestination == item.destination
                            val itemSkin = uiState.skinForDestination(item.destination)
                            val customAccent = if (itemSkin?.isCustomApplied == true) {
                                Color(
                                    itemSkin.primarySwatch.rgb.r,
                                    itemSkin.primarySwatch.rgb.g,
                                    itemSkin.primarySwatch.rgb.b
                                )
                            } else null
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    com.example.ui.components.SeleniaModule3DIcon(
                                        destination = item.destination,
                                        selected = selected,
                                        size = if (selected) 30.dp else 26.dp,
                                        customAccent = customAccent
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        SeleniaTopHeader(
                            uiState = uiState,
                            onHeaderMoonClick = { viewModel.navigateTo(AppDestination.MOON_OBSERVATORY) },
                            onHeaderClockClick = { viewModel.navigateTo(AppDestination.CLOCK) }
                        )
                        SeleniaMainContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Scaffold(
                    topBar = {
                        SeleniaTopHeader(
                            uiState = uiState,
                            onHeaderMoonClick = { viewModel.navigateTo(AppDestination.MOON_OBSERVATORY) },
                            onHeaderClockClick = { viewModel.navigateTo(AppDestination.CLOCK) }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            navItems.forEach { item ->
                                val selected = uiState.currentDestination == item.destination
                                val itemSkin = uiState.skinForDestination(item.destination)
                                val customAccent = if (itemSkin?.isCustomApplied == true) {
                                    Color(
                                        itemSkin.primarySwatch.rgb.r,
                                        itemSkin.primarySwatch.rgb.g,
                                        itemSkin.primarySwatch.rgb.b
                                    )
                                } else null
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateTo(item.destination) },
                                    icon = {
                                        com.example.ui.components.SeleniaModule3DIcon(
                                            destination = item.destination,
                                            selected = selected,
                                            size = if (selected) 30.dp else 26.dp,
                                            customAccent = customAccent
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Unspecified,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                        unselectedIconColor = Color.Unspecified,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    SeleniaMainContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeleniaTopHeader(
    uiState: SeleniaUiState,
    onHeaderMoonClick: () -> Unit,
    onHeaderClockClick: () -> Unit
) {
    val moonSkin = uiState.skinFor(ModuleThemeTarget.MOON_OBSERVATORY)
    val litTint = if (moonSkin.isCustomApplied) {
        Color(moonSkin.primarySwatch.rgb.r, moonSkin.primarySwatch.rgb.g, moonSkin.primarySwatch.rgb.b)
    } else null

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                com.example.ui.components.SeleniaBrandEmblem(
                    size = 38.dp,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.secondary
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.subtitle_observatory),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderLiveClockBadge(
                    is24HourFormat = uiState.is24HourFormat,
                    onClick = onHeaderClockClick
                )

                Surface(
                    onClick = onHeaderMoonClick,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.testTag("header_live_moon_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        ) {
                            RealisticMoonCanvas(
                                phaseFraction = uiState.selectedDayLunarInfo.phaseFraction,
                                isNorthernHemisphere = uiState.isNorthernHemisphere,
                                showAmbientGlow = false,
                                litColor = litTint ?: Color(0xFFF7E7B4)
                            )
                        }
                        Text(
                            text = "${uiState.selectedDayLunarInfo.illuminationPercent.roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Isolated composable for the 1Hz header clock badge:
 * - Uses lifecycle-aware repeatOnLifecycle(STARTED) so 0% battery is used in background.
 * - Aligns sleep duration to exact second boundary.
 * - Isolates state reads so the 1Hz tick only recomposes this badge and never recomposes
 *   the header's RealisticMoonCanvas or title.
 */
@Composable
private fun HeaderLiveClockBadge(
    is24HourFormat: Boolean,
    onClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val reusableCal = remember { Calendar.getInstance() }
    var liveTimeText by remember { mutableStateOf("") }

    LaunchedEffect(is24HourFormat, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                reusableCal.timeInMillis = now
                val h24 = reusableCal.get(Calendar.HOUR_OF_DAY)
                val m = reusableCal.get(Calendar.MINUTE)
                val s = reusableCal.get(Calendar.SECOND)
                liveTimeText = if (is24HourFormat) {
                    "%02d:%02d:%02d".format(Locale.US, h24, m, s)
                } else {
                    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
                    val amPm = if (h24 < 12) "AM" else "PM"
                    "%02d:%02d %s".format(Locale.US, h12, m, amPm)
                }
                val sleepMs = (1000L - (now % 1000L)).coerceIn(100L, 1000L)
                delay(sleepMs)
            }
        }
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.testTag("header_live_clock_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = liveTimeText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun SeleniaMainContent(
    uiState: SeleniaUiState,
    viewModel: SeleniaViewModel,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = uiState.currentDestination,
        label = "SeleniaDestinationCrossfade",
        modifier = modifier.fillMaxSize()
    ) { destination ->
        val destinationSkin = uiState.skinForDestination(destination)
        ModuleThemedSurface(skin = destinationSkin) {
            when (destination) {
                AppDestination.CALENDAR -> CalendarScreen(
                    uiState = uiState,
                    onSelectDate = viewModel::selectDate,
                    onChangeMonth = viewModel::changeMonth,
                    onJumpToToday = viewModel::jumpToToday,
                    onOpenMoonObservatory = { viewModel.navigateTo(AppDestination.MOON_OBSERVATORY) },
                    onSendAgeToCalculator = viewModel::sendSelectedDayLunarAgeToCalculator,
                    onAddNote = viewModel::addNoteForSelectedDate,
                    onDeleteNote = viewModel::deleteNote,
                    onOpenColorCustomizer = {
                        viewModel.openColorCustomizerForModule(ModuleThemeTarget.CALENDAR)
                    }
                )

                AppDestination.MOON_OBSERVATORY -> MoonObservatoryScreen(
                    uiState = uiState,
                    onStepDay = viewModel::stepSelectedDay,
                    onJumpToToday = viewModel::jumpToToday,
                    onSelectDate = viewModel::selectDate,
                    onToggleHemisphere = viewModel::toggleHemisphere,
                    onOpenColorCustomizer = {
                        viewModel.openColorCustomizerForModule(ModuleThemeTarget.MOON_OBSERVATORY)
                    }
                )

                AppDestination.CLOCK -> ClockScreen(
                    uiState = uiState,
                    onSelectSubTab = viewModel::selectClockSubTab,
                    onToggle24HourFormat = viewModel::toggle24HourFormat,
                    onOpenColorCustomizer = {
                        viewModel.openColorCustomizerForModule(ModuleThemeTarget.CLOCK)
                    }
                )

                AppDestination.CALCULATOR -> CalculatorScreen(
                    uiState = uiState,
                    onSetMode = viewModel::setCalculatorMode,
                    onInputToken = viewModel::onCalculatorInput,
                    onClear = viewModel::onCalculatorClear,
                    onBackspace = viewModel::onCalculatorBackspace,
                    onEvaluate = viewModel::onCalculatorEvaluate,
                    onConvertDaysToSynodic = viewModel::convertCurrentValueToSynodicCycles,
                    onMultiplyBySynodic = viewModel::multiplyCurrentValueBySynodicCycle,
                    onUseHistoryItem = viewModel::useHistoryItem,
                    onClearHistory = viewModel::clearHistory,
                    onSetUseLunarCycleOffset = viewModel::setUseLunarCycleOffset,
                    onUpdateOffsetDays = viewModel::updateDateOffsetDays,
                    onUpdateOffsetLunarCycles = viewModel::updateDateOffsetLunarCycles,
                    onStepComparisonDate = viewModel::stepComparisonEndDate,
                    onJumpToDateInCalendar = { targetDate ->
                        viewModel.selectDate(targetDate)
                        viewModel.navigateTo(AppDestination.CALENDAR)
                    },
                    onOpenColorCustomizer = {
                        viewModel.openColorCustomizerForModule(ModuleThemeTarget.CALCULATOR)
                    }
                )

                AppDestination.COLORS -> ColorStudioScreen(
                    uiState = uiState,
                    onSelectSubTab = viewModel::selectColorStudioSubTab,
                    onUpdateRgb = viewModel::updateColorFromRgb,
                    onUpdateHsl = viewModel::updateColorFromHsl,
                    onUpdateCmyk = viewModel::updateColorFromCmyk,
                    onApplyHex = viewModel::updateColorFromHex,
                    onSaveFavoriteColor = viewModel::saveFavoriteColor,
                    onSetGradientStyle = viewModel::setGradientStyle,
                    onSetGradientAngle = viewModel::setGradientAngle,
                    onSetRadialFocalAndRadius = viewModel::setRadialFocalAndRadius,
                    onCycleGradientVariant = viewModel::cycleGradientHarmonyVariant,
                    onApplyGradientPreset = viewModel::applyGradientPreset,
                    onSelectThemeTarget = viewModel::selectThemeTarget,
                    onApplySkinToTarget = viewModel::applyActiveColorAndGradientToTarget,
                    onResetSkinForTarget = viewModel::resetSkinForTarget,
                    onNavigateToModule = { target ->
                        when (target) {
                            ModuleThemeTarget.CALENDAR -> viewModel.navigateTo(AppDestination.CALENDAR)
                            ModuleThemeTarget.MOON_OBSERVATORY -> viewModel.navigateTo(AppDestination.MOON_OBSERVATORY)
                            ModuleThemeTarget.CLOCK -> viewModel.navigateTo(AppDestination.CLOCK)
                            ModuleThemeTarget.CALCULATOR -> viewModel.navigateTo(AppDestination.CALCULATOR)
                            ModuleThemeTarget.ALL_MODULES -> viewModel.navigateTo(AppDestination.CALENDAR)
                        }
                    }
                )
            }
        }
    }
}

