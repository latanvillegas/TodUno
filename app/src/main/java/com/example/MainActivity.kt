package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SeleniaRepository
import com.example.notifications.SeleniaAlertManager
import com.example.ui.AppDestination
import com.example.ui.SeleniaUiState
import com.example.ui.SeleniaViewModel
import com.example.ui.components.SeleniaModule3DIcon
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ClockScreen
import com.example.ui.screens.MoonObservatoryScreen
import com.example.ui.theme.ApplicationTheme
import com.example.widget.SeleniaMoonWidgetProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        SeleniaAlertManager.ensureChannels(applicationContext)
        SeleniaMoonWidgetProvider.refreshAllWidgets(applicationContext)

        setContent {
            val appCtx = LocalContext.current.applicationContext
            val repository = remember(appCtx) {
                SeleniaRepository.getInstance(appCtx)
            }
            val viewModel: SeleniaViewModel = viewModel(
                factory = SeleniaViewModel.provideFactory(repository, appCtx)
            )

            ApplicationTheme {
                SeleniaApp(viewModel = viewModel)
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
    )
)

@Composable
fun SeleniaApp(
    viewModel: SeleniaViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                viewModel.onSystemTimeOrZoneChanged()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(timeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(timeReceiver, filter)
            }
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { context.unregisterReceiver(timeReceiver) }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.currentDestination != AppDestination.CALENDAR) {
        BackHandler {
            viewModel.navigateTo(AppDestination.CALENDAR)
        }
    }

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
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.navigateTo(item.destination) },
                            icon = {
                                SeleniaModule3DIcon(
                                    destination = item.destination,
                                    selected = selected,
                                    size = if (selected) 30.dp else 26.dp
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
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val selected = uiState.currentDestination == item.destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(item.destination) },
                                icon = {
                                    SeleniaModule3DIcon(
                                        destination = item.destination,
                                        selected = selected,
                                        size = if (selected) 30.dp else 26.dp
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
                onJumpToYearMonth = viewModel::jumpToYearMonth,
                onJumpToToday = viewModel::jumpToToday,
                onAddNote = viewModel::addNoteForSelectedDate,
                onUpdateNote = viewModel::updateNote,
                onDeleteNote = viewModel::deleteNote,
                onExportBackupJson = viewModel::exportBackupJson,
                onImportBackupJson = viewModel::importBackupJson,
                onDismissBackupMessage = viewModel::dismissBackupStatusMessage
            )

            AppDestination.MOON_OBSERVATORY -> MoonObservatoryScreen(
                uiState = uiState,
                onStepDay = viewModel::stepSelectedDay,
                onJumpToToday = viewModel::jumpToToday,
                onSelectDate = viewModel::selectDate,
                onToggleHemisphere = viewModel::toggleHemisphere,
                onSetLunarAlertsEnabled = viewModel::setLunarAlertsEnabled
            )

            AppDestination.CLOCK -> ClockScreen(
                uiState = uiState,
                onSelectSubTab = viewModel::selectClockSubTab,
                onSelectWatchDialPreset = viewModel::selectWatchDialPreset,
                onToggle24HourFormat = viewModel::toggle24HourFormat,
                onToggleWorldCity = viewModel::toggleWorldCity,
                onToggleStopwatch = viewModel::toggleStopwatch,
                onLapStopwatch = viewModel::lapStopwatch,
                onResetStopwatch = viewModel::resetStopwatch,
                onSelectTimerPreset = viewModel::selectTimerPresetSeconds,
                onSetCustomTimerDuration = viewModel::setCustomTimerDuration,
                onAdjustTimerSeconds = viewModel::adjustTimerSeconds,
                onToggleTimer = viewModel::toggleTimerStartPause,
                onResetTimer = viewModel::resetTimer,
                onTimerReachedZero = viewModel::onForegroundTimerReachedZero
            )

            AppDestination.CALCULATOR -> CalculatorScreen(
                uiState = uiState,
                onToggleRadiansMode = viewModel::toggleRadiansMode,
                onToggleInverseTrigMode = viewModel::toggleInverseTrigMode,
                onInputToken = viewModel::onCalculatorInput,
                onInsertLastAnswer = viewModel::onInsertLastAnswer,
                onClear = viewModel::onCalculatorClear,
                onBackspace = viewModel::onCalculatorBackspace,
                onEvaluate = viewModel::onCalculatorEvaluate,
                onUseHistoryItem = viewModel::useHistoryItem,
                onClearHistory = viewModel::clearHistory
            )
        }
    }
}
