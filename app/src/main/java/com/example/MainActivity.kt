package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.ColorHarmonyEngine
import com.example.ui.AppDestination
import com.example.ui.ModuleThemeTarget
import com.example.ui.ModuleVisualSkin
import com.example.ui.SeleniaUiState
import com.example.ui.SeleniaViewModel
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ClockScreen
import com.example.ui.screens.ColorStudioScreen
import com.example.ui.screens.MoonObservatoryScreen
import com.example.ui.theme.ApplicationTheme

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
    val testTag: String
)

private val NAV_ITEMS = listOf(
    NavItemSpec(
        destination = AppDestination.CALENDAR,
        labelRes = R.string.nav_calendar,
        testTag = "nav_tab_calendar"
    ),
    NavItemSpec(
        destination = AppDestination.MOON_OBSERVATORY,
        labelRes = R.string.nav_moon,
        testTag = "nav_tab_moon"
    ),
    NavItemSpec(
        destination = AppDestination.CLOCK,
        labelRes = R.string.nav_clock,
        testTag = "nav_tab_clock"
    ),
    NavItemSpec(
        destination = AppDestination.CALCULATOR,
        labelRes = R.string.nav_calculator,
        testTag = "nav_tab_calculator"
    ),
    NavItemSpec(
        destination = AppDestination.COLORS,
        labelRes = R.string.nav_colors,
        testTag = "nav_tab_colors"
    )
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
                        NAV_ITEMS.forEach { item ->
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

                    SeleniaMainContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NAV_ITEMS.forEach { item ->
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

