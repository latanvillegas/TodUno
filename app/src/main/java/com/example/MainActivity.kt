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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NightlightRound
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppDestination
import com.example.ui.SeleniaUiState
import com.example.ui.SeleniaViewModel
import com.example.ui.components.RealisticMoonCanvas
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ClockScreen
import com.example.ui.screens.MoonObservatoryScreen
import com.example.ui.theme.ApplicationTheme
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
        )
    )

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
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.navigateTo(item.destination) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelRes)
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
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun SeleniaTopHeader(
    uiState: SeleniaUiState,
    onHeaderMoonClick: () -> Unit,
    onHeaderClockClick: () -> Unit
) {
    var liveTimeText by remember { mutableStateOf("") }
    LaunchedEffect(uiState.is24HourFormat) {
        while (true) {
            val cal = Calendar.getInstance()
            val h24 = cal.get(Calendar.HOUR_OF_DAY)
            val m = cal.get(Calendar.MINUTE)
            val s = cal.get(Calendar.SECOND)
            liveTimeText = if (uiState.is24HourFormat) {
                "%02d:%02d:%02d".format(Locale.US, h24, m, s)
            } else {
                val h12 = if (h24 % 12 == 0) 12 else h24 % 12
                val amPm = if (h24 < 12) "AM" else "PM"
                "%02d:%02d %s".format(Locale.US, h12, m, amPm)
            }
            delay(1000L)
        }
    }

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
            Column(modifier = Modifier.weight(1f)) {
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Clock Pill in Header
                Surface(
                    onClick = onHeaderClockClick,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.testTag("header_live_clock_badge")
                ) {
                    Text(
                        text = "⌚ $liveTimeText",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                // Live Moon Badge showing active date's moon phase
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
                                showAmbientGlow = false
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
        when (destination) {
            AppDestination.CALENDAR -> CalendarScreen(
                uiState = uiState,
                onSelectDate = viewModel::selectDate,
                onChangeMonth = viewModel::changeMonth,
                onJumpToToday = viewModel::jumpToToday,
                onOpenMoonObservatory = { viewModel.navigateTo(AppDestination.MOON_OBSERVATORY) },
                onSendAgeToCalculator = viewModel::sendSelectedDayLunarAgeToCalculator,
                onAddNote = viewModel::addNoteForSelectedDate,
                onDeleteNote = viewModel::deleteNote
            )

            AppDestination.MOON_OBSERVATORY -> MoonObservatoryScreen(
                uiState = uiState,
                onStepDay = viewModel::stepSelectedDay,
                onJumpToToday = viewModel::jumpToToday,
                onSelectDate = viewModel::selectDate,
                onToggleHemisphere = viewModel::toggleHemisphere
            )

            AppDestination.CLOCK -> ClockScreen(
                uiState = uiState,
                onSelectSubTab = viewModel::selectClockSubTab,
                onToggle24HourFormat = viewModel::toggle24HourFormat
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
                }
            )
        }
    }
}
