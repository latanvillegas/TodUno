package com.example.ui

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SeleniaPersistedSettings
import com.example.data.SeleniaRepository
import com.example.domain.CalculationRecord
import com.example.domain.CalendarDayCell
import com.example.domain.CelestialEvent
import com.example.domain.LunarAstronomer
import com.example.domain.LunarDayInfo
import com.example.domain.MajorPhaseMilestone
import com.example.domain.ScientificCalculatorEngine
import com.example.domain.SimpleDate
import com.example.notifications.SeleniaAlertManager
import com.example.widget.SeleniaMoonWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class AppDestination {
    CALENDAR,
    MOON_OBSERVATORY,
    CLOCK,
    CALCULATOR
}

enum class ClockSubTab {
    WORLD_CLOCK,
    STOPWATCH,
    TIMER
}

@Immutable
data class CalendarNote(
    val id: Long,
    val date: SimpleDate,
    val title: String,
    val category: String,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0
) {
    fun formattedReminderTime(): String =
        "%02d:%02d".format(Locale.US, reminderHour.coerceIn(0, 23), reminderMinute.coerceIn(0, 59))
}

@Immutable
data class StopwatchLap(
    val lapNumber: Int,
    val lapDurationMs: Long,
    val totalElapsedMs: Long
)

/**
 * Returns monotonic milliseconds immune to NTP/manual wall-clock changes.
 * Falls back cleanly to JVM [System.nanoTime] in local unit tests where Android's
 * stubbed [SystemClock.elapsedRealtime] returns 0L.
 */
internal fun currentMonotonicMs(): Long {
    val elapsed = runCatching { SystemClock.elapsedRealtime() }.getOrDefault(0L)
    return if (elapsed > 0L) elapsed else (System.nanoTime() / 1_000_000L)
}

@Immutable
data class SeleniaUiState(
    val currentDestination: AppDestination = AppDestination.CALENDAR,
    val today: SimpleDate = LunarAstronomer.today(),
    val selectedDate: SimpleDate = LunarAstronomer.today(),
    val displayYear: Int = LunarAstronomer.today().year,
    val displayMonth: Int = LunarAstronomer.today().month,
    val isNorthernHemisphere: Boolean = true,
    val lunarAlertsEnabled: Boolean = false,
    val calendarCells: List<CalendarDayCell> = emptyList(),
    val monthMilestones: List<MajorPhaseMilestone> = emptyList(),
    val selectedDayLunarInfo: LunarDayInfo = LunarAstronomer.getLunarInfo(LunarAstronomer.today()),
    val upcomingMajorPhases: List<MajorPhaseMilestone> = emptyList(),
    val upcomingCelestialEvents: List<CelestialEvent> = emptyList(),
    val notesByDate: Map<String, List<CalendarNote>> = emptyMap(),
    val backupStatusMessage: String? = null,

    // Clock & World Cities State
    val clockSubTab: ClockSubTab = ClockSubTab.WORLD_CLOCK,
    val is24HourFormat: Boolean = true,
    val selectedWorldCityIds: List<String> = emptyList(),
    val watchDialPresetName: String = SeleniaPersistedSettings.DEFAULT_WATCH_DIAL_PRESET,
    val timeZoneRevision: Int = 0,

    // Persistent Monotonic Stopwatch State (0 background ticks when off-screen!)
    val stopwatchRunning: Boolean = false,
    val stopwatchAccumulatedMs: Long = 0L,
    val stopwatchLastStartMonoMs: Long = 0L,
    val stopwatchLaps: List<StopwatchLap> = emptyList(),

    // Persistent Monotonic Timer State (0 background ticks when off-screen!)
    val timerTotalSeconds: Int = 300,
    val timerRemainingPausedMs: Long = 300_000L,
    val timerEndTargetMonoMs: Long = 0L,
    val timerRunning: Boolean = false,
    val timerFinishedAlert: Boolean = false,

    // Calculator State
    val isRadiansMode: Boolean = false,
    val isInverseTrigMode: Boolean = false,
    val expression: String = "",
    val liveResultPreview: String = "0",
    val lastAnswer: String = "0",
    val calculationHistory: List<CalculationRecord> = emptyList()
) {
    fun computeStopwatchElapsedMs(nowMonoMs: Long = currentMonotonicMs()): Long {
        return if (stopwatchRunning) {
            stopwatchAccumulatedMs + (nowMonoMs - stopwatchLastStartMonoMs).coerceAtLeast(0L)
        } else {
            stopwatchAccumulatedMs
        }
    }

    fun computeTimerRemainingMs(nowMonoMs: Long = currentMonotonicMs()): Long {
        return if (timerRunning) {
            (timerEndTargetMonoMs - nowMonoMs).coerceAtLeast(0L)
        } else {
            timerRemainingPausedMs
        }
    }
}

class SeleniaViewModel(
    private val repository: SeleniaRepository? = null,
    appContext: Context? = null
) : ViewModel() {

    private val appContext: Context? = appContext?.applicationContext
    private val idCounter = AtomicLong(System.currentTimeMillis())
    private var hasHydratedClockState = false
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<SeleniaUiState> = _uiState.asStateFlow()

    init {
        if (repository != null) {
            viewModelScope.launch {
                repository.notesByDateFlow.collect { persistedNotes ->
                    _uiState.update { it.copy(notesByDate = persistedNotes) }
                }
            }
            viewModelScope.launch {
                repository.calculationHistoryFlow.collect { history ->
                    _uiState.update { current ->
                        current.copy(
                            calculationHistory = history,
                            lastAnswer = history.firstOrNull()?.result ?: current.lastAnswer
                        )
                    }
                }
            }
            viewModelScope.launch {
                repository.settingsFlow.collect { settings ->
                    val shouldHydrateClock = !hasHydratedClockState
                    hasHydratedClockState = true
                    var expiredPersistedTimerSec: Int? = null
                    var activePersistedTimerRemainingMs: Long? = null

                    _uiState.update { current ->
                        val preview = if (current.expression.isBlank()) {
                            current.liveResultPreview
                        } else {
                            ScientificCalculatorEngine.evaluate(current.expression, settings.isRadiansMode)
                                .map { ScientificCalculatorEngine.formatNumber(it) }
                                .getOrDefault(current.liveResultPreview)
                        }

                        var next = current.copy(
                            isNorthernHemisphere = settings.isNorthernHemisphere,
                            is24HourFormat = settings.is24HourFormat,
                            isRadiansMode = settings.isRadiansMode,
                            lunarAlertsEnabled = settings.lunarAlertsEnabled,
                            selectedWorldCityIds = settings.selectedWorldCityIds,
                            watchDialPresetName = settings.watchDialPresetName,
                            liveResultPreview = preview
                        )

                        if (shouldHydrateClock) {
                            val nowWall = System.currentTimeMillis()
                            val nowMono = currentMonotonicMs()

                            val hydratedSwAccumulated = if (
                                settings.stopwatchRunning && settings.stopwatchStartWallEpochMs > 0L
                            ) {
                                settings.stopwatchAccumulatedMs +
                                    (nowWall - settings.stopwatchStartWallEpochMs).coerceAtLeast(0L)
                            } else {
                                settings.stopwatchAccumulatedMs
                            }
                            val hydratedSwStartMono = if (settings.stopwatchRunning) nowMono else 0L

                            var hydratedTimerRunning = false
                            var hydratedTimerPausedMs = settings.timerRemainingPausedMs
                            var hydratedTimerEndMono = 0L
                            var hydratedTimerFinished = false

                            if (settings.timerRunning && settings.timerEndWallEpochMs > 0L) {
                                val remainingMs = settings.timerEndWallEpochMs - nowWall
                                if (remainingMs > 0L) {
                                    hydratedTimerRunning = true
                                    hydratedTimerPausedMs = remainingMs
                                    hydratedTimerEndMono = nowMono + remainingMs
                                    activePersistedTimerRemainingMs = remainingMs
                                } else {
                                    hydratedTimerRunning = false
                                    hydratedTimerPausedMs = 0L
                                    hydratedTimerFinished = true
                                    expiredPersistedTimerSec = settings.timerTotalSeconds
                                }
                            }

                            next = next.copy(
                                stopwatchRunning = settings.stopwatchRunning,
                                stopwatchAccumulatedMs = hydratedSwAccumulated,
                                stopwatchLastStartMonoMs = hydratedSwStartMono,
                                stopwatchLaps = settings.stopwatchLaps,
                                timerTotalSeconds = settings.timerTotalSeconds,
                                timerRunning = hydratedTimerRunning,
                                timerRemainingPausedMs = hydratedTimerPausedMs,
                                timerEndTargetMonoMs = hydratedTimerEndMono,
                                timerFinishedAlert = hydratedTimerFinished
                            )
                        } else if (!next.timerRunning && next.timerTotalSeconds != settings.timerTotalSeconds) {
                            next = next.copy(
                                timerTotalSeconds = settings.timerTotalSeconds,
                                timerRemainingPausedMs = settings.timerRemainingPausedMs
                            )
                        }
                        next
                    }

                    expiredPersistedTimerSec?.let { totalSec ->
                        repository.saveTimerSnapshot(
                            totalSeconds = totalSec,
                            running = false,
                            remainingPausedMs = 0L,
                            endWallEpochMs = 0L
                        )
                    }
                    val activeRem = activePersistedTimerRemainingMs
                    if (activeRem != null && this@SeleniaViewModel.appContext != null) {
                        SeleniaAlertManager.scheduleBackgroundTimerAlert(
                            context = this@SeleniaViewModel.appContext,
                            remainingDurationMs = activeRem,
                            totalSeconds = settings.timerTotalSeconds
                        )
                    }
                }
            }
        }
    }

    private fun createInitialState(): SeleniaUiState {
        val today = LunarAstronomer.today()
        val cells = LunarAstronomer.getMonthGrid(today.year, today.month, today)
        val milestones = LunarAstronomer.getMonthMajorMilestones(today.year, today.month)
        val lunarInfo = LunarAstronomer.getLunarInfo(today)
        val upcoming = LunarAstronomer.getUpcomingMajorPhases(today, count = 4)
        val celestialEvents = LunarAstronomer.getUpcomingCelestialEvents(today, count = 6)

        return SeleniaUiState(
            today = today,
            selectedDate = today,
            displayYear = today.year,
            displayMonth = today.month,
            calendarCells = cells,
            monthMilestones = milestones,
            selectedDayLunarInfo = lunarInfo,
            upcomingMajorPhases = upcoming,
            upcomingCelestialEvents = celestialEvents,
            notesByDate = emptyMap(),
            calculationHistory = emptyList(),
            lastAnswer = "0"
        )
    }

    /**
     * Called on Activity ON_RESUME:
     * 1. Detects midnight calendar rollover if the app remained in memory overnight.
     * 2. Reconciles timer completion if a background countdown finished while stopped.
     */
    fun onAppResumed() {
        val currentToday = LunarAstronomer.today()
        val nowMono = currentMonotonicMs()
        var dayChanged = false
        var timerCompletedWhileStopped = false
        var completedTimerSec = 300

        _uiState.update { current ->
            var next = current
            if (current.today != currentToday) {
                dayChanged = true
                val wasSelectedOnOldToday = current.selectedDate == current.today
                val nextSelected = if (wasSelectedOnOldToday) currentToday else current.selectedDate
                next = next.copy(
                    today = currentToday,
                    selectedDate = nextSelected,
                    calendarCells = LunarAstronomer.getMonthGrid(next.displayYear, next.displayMonth, currentToday),
                    selectedDayLunarInfo = LunarAstronomer.getLunarInfo(nextSelected),
                    upcomingMajorPhases = LunarAstronomer.getUpcomingMajorPhases(nextSelected, count = 4),
                    upcomingCelestialEvents = LunarAstronomer.getUpcomingCelestialEvents(nextSelected, count = 6)
                )
            }
            if (next.timerRunning && nowMono >= next.timerEndTargetMonoMs) {
                timerCompletedWhileStopped = true
                completedTimerSec = next.timerTotalSeconds
                next = next.copy(
                    timerRunning = false,
                    timerRemainingPausedMs = 0L,
                    timerFinishedAlert = true
                )
            }
            next
        }

        if (dayChanged) {
            appContext?.let {
                SeleniaMoonWidgetProvider.refreshAllWidgets(
                    context = it,
                    isNorthernHemisphereOverride = _uiState.value.isNorthernHemisphere
                )
            }
        }
        if (timerCompletedWhileStopped && repository != null) {
            viewModelScope.launch {
                repository.saveTimerSnapshot(
                    totalSeconds = completedTimerSec,
                    running = false,
                    remainingPausedMs = 0L,
                    endWallEpochMs = 0L
                )
            }
        }
    }

    /**
     * Triggered dynamically when the OS broadcasts ACTION_TIMEZONE_CHANGED, ACTION_TIME_CHANGED,
     * or ACTION_DATE_CHANGED while the app is open.
     */
    fun onSystemTimeOrZoneChanged() {
        _uiState.update { it.copy(timeZoneRevision = it.timeZoneRevision + 1) }
        onAppResumed()
        val ctx = appContext
        if (ctx != null) {
            SeleniaMoonWidgetProvider.refreshAllWidgets(
                context = ctx,
                isNorthernHemisphereOverride = _uiState.value.isNorthernHemisphere
            )
            if (repository != null) {
                viewModelScope.launch {
                    if (_uiState.value.lunarAlertsEnabled) {
                        SeleniaAlertManager.syncUpcomingLunarPhaseAlerts(ctx, true)
                    }
                    repository.getReminderNotes().forEach { note ->
                        SeleniaAlertManager.scheduleNoteReminder(
                            context = ctx,
                            noteId = note.id,
                            date = note.date,
                            title = note.title,
                            category = note.category,
                            reminderHour = note.reminderHour,
                            reminderMinute = note.reminderMinute
                        )
                    }
                }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { current ->
            if (current.currentDestination == destination) current
            else current.copy(currentDestination = destination)
        }
    }

    fun selectClockSubTab(subTab: ClockSubTab) {
        _uiState.update { current ->
            if (current.clockSubTab == subTab) current
            else current.copy(clockSubTab = subTab)
        }
    }

    fun selectWatchDialPreset(presetName: String) {
        if (presetName.isBlank()) return
        _uiState.update { current ->
            if (current.watchDialPresetName == presetName) current
            else current.copy(watchDialPresetName = presetName)
        }
        if (repository != null) {
            viewModelScope.launch { repository.setWatchDialPresetName(presetName) }
        }
    }

    fun toggle24HourFormat() {
        val next = !_uiState.value.is24HourFormat
        _uiState.update { it.copy(is24HourFormat = next) }
        if (repository != null) {
            viewModelScope.launch { repository.set24HourFormat(next) }
        }
    }

    fun toggleWorldCity(timeZoneId: String) {
        if (timeZoneId.isBlank()) return
        var updatedIds: List<String> = emptyList()
        _uiState.update { current ->
            val next = if (timeZoneId in current.selectedWorldCityIds) {
                current.selectedWorldCityIds - timeZoneId
            } else {
                (current.selectedWorldCityIds + timeZoneId).distinct()
            }
            updatedIds = next
            current.copy(selectedWorldCityIds = next)
        }
        if (repository != null) {
            viewModelScope.launch { repository.setSelectedWorldCityIds(updatedIds) }
        }
    }

    // --- Stopwatch Actions (Monotonic in RAM + Persisted Across Process Death) ---

    private fun persistStopwatchState(state: SeleniaUiState, nowMono: Long) {
        val repo = repository ?: return
        val nowWall = System.currentTimeMillis()
        val startWall = if (state.stopwatchRunning) {
            val activeDelta = (nowMono - state.stopwatchLastStartMonoMs).coerceAtLeast(0L)
            (nowWall - activeDelta).coerceAtLeast(1L)
        } else {
            0L
        }
        viewModelScope.launch {
            repo.saveStopwatchSnapshot(
                running = state.stopwatchRunning,
                accumulatedMs = state.stopwatchAccumulatedMs,
                startWallEpochMs = startWall,
                laps = state.stopwatchLaps
            )
        }
    }

    fun toggleStopwatch() {
        val nowMono = currentMonotonicMs()
        lateinit var updated: SeleniaUiState
        _uiState.update { current ->
            val next = if (current.stopwatchRunning) {
                val total = current.stopwatchAccumulatedMs +
                    (nowMono - current.stopwatchLastStartMonoMs).coerceAtLeast(0L)
                current.copy(
                    stopwatchRunning = false,
                    stopwatchAccumulatedMs = total
                )
            } else {
                current.copy(
                    stopwatchRunning = true,
                    stopwatchLastStartMonoMs = nowMono
                )
            }
            updated = next
            next
        }
        persistStopwatchState(updated, nowMono)
    }

    fun lapStopwatch() {
        val nowMono = currentMonotonicMs()
        var updated: SeleniaUiState? = null
        _uiState.update { current ->
            val currentTotal = current.computeStopwatchElapsedMs(nowMono)
            if (currentTotal <= 0L) return@update current
            val previousTotal = current.stopwatchLaps.firstOrNull()?.totalElapsedMs ?: 0L
            val lapDiff = (currentTotal - previousTotal).coerceAtLeast(0L)
            val nextLapNumber = (current.stopwatchLaps.firstOrNull()?.lapNumber ?: 0) + 1
            val newLap = StopwatchLap(
                lapNumber = nextLapNumber,
                lapDurationMs = lapDiff,
                totalElapsedMs = currentTotal
            )
            val next = current.copy(
                stopwatchLaps = (listOf(newLap) + current.stopwatchLaps).take(200)
            )
            updated = next
            next
        }
        updated?.let { persistStopwatchState(it, nowMono) }
    }

    fun resetStopwatch() {
        val nowMono = currentMonotonicMs()
        lateinit var updated: SeleniaUiState
        _uiState.update { current ->
            val next = current.copy(
                stopwatchRunning = false,
                stopwatchAccumulatedMs = 0L,
                stopwatchLastStartMonoMs = 0L,
                stopwatchLaps = emptyList()
            )
            updated = next
            next
        }
        persistStopwatchState(updated, nowMono)
    }

    // --- Timer Actions (Monotonic in RAM + Persisted Across Process Death, up to 23:59:59) ---

    private fun persistTimerState(state: SeleniaUiState, nowMono: Long) {
        val repo = repository ?: return
        val nowWall = System.currentTimeMillis()
        val endWall = if (state.timerRunning) {
            val remaining = (state.timerEndTargetMonoMs - nowMono).coerceAtLeast(0L)
            nowWall + remaining
        } else {
            0L
        }
        viewModelScope.launch {
            repo.saveTimerSnapshot(
                totalSeconds = state.timerTotalSeconds,
                running = state.timerRunning,
                remainingPausedMs = state.timerRemainingPausedMs,
                endWallEpochMs = endWall
            )
        }
    }

    fun selectTimerPresetSeconds(seconds: Int) {
        val nowMono = currentMonotonicMs()
        val clamped = seconds.coerceIn(
            SeleniaRepository.MIN_TIMER_SECONDS,
            SeleniaRepository.MAX_TIMER_SECONDS
        )
        appContext?.let { SeleniaAlertManager.cancelBackgroundTimerAlert(it) }
        lateinit var updated: SeleniaUiState
        _uiState.update { current ->
            val next = current.copy(
                timerRunning = false,
                timerFinishedAlert = false,
                timerTotalSeconds = clamped,
                timerRemainingPausedMs = clamped * 1000L,
                timerEndTargetMonoMs = 0L
            )
            updated = next
            next
        }
        persistTimerState(updated, nowMono)
    }

    fun setCustomTimerDuration(hours: Int, minutes: Int, seconds: Int) {
        val total = (hours.coerceIn(0, 23) * 3600) +
            (minutes.coerceIn(0, 59) * 60) +
            seconds.coerceIn(0, 59)
        selectTimerPresetSeconds(total.coerceAtLeast(SeleniaRepository.MIN_TIMER_SECONDS))
    }

    fun adjustTimerSeconds(deltaSec: Int) {
        val nowMono = currentMonotonicMs()
        var scheduleRemainingMs: Long? = null
        var scheduleTotalSec = 0
        lateinit var updated: SeleniaUiState

        _uiState.update { current ->
            val nextSec = (current.timerTotalSeconds + deltaSec).coerceIn(
                SeleniaRepository.MIN_TIMER_SECONDS,
                SeleniaRepository.MAX_TIMER_SECONDS
            )
            scheduleTotalSec = nextSec
            val next = if (!current.timerRunning) {
                current.copy(
                    timerTotalSeconds = nextSec,
                    timerFinishedAlert = false,
                    timerRemainingPausedMs = nextSec * 1000L
                )
            } else {
                val adjustedEnd = (current.timerEndTargetMonoMs + deltaSec * 1000L)
                    .coerceAtLeast(nowMono + 1_000L)
                scheduleRemainingMs = adjustedEnd - nowMono
                current.copy(
                    timerTotalSeconds = nextSec,
                    timerEndTargetMonoMs = adjustedEnd
                )
            }
            updated = next
            next
        }

        val rem = scheduleRemainingMs
        if (rem != null && appContext != null) {
            SeleniaAlertManager.scheduleBackgroundTimerAlert(appContext, rem, scheduleTotalSec)
        }
        persistTimerState(updated, nowMono)
    }

    fun toggleTimerStartPause() {
        val nowMono = currentMonotonicMs()
        var scheduleRemainingMs: Long? = null
        var scheduleTotalSec = 0
        var shouldCancel = false
        lateinit var updated: SeleniaUiState

        _uiState.update { current ->
            val next = if (current.timerRunning) {
                shouldCancel = true
                val remaining = (current.timerEndTargetMonoMs - nowMono).coerceAtLeast(0L)
                current.copy(
                    timerRunning = false,
                    timerFinishedAlert = false,
                    timerRemainingPausedMs = remaining
                )
            } else {
                val durationMs = if (current.timerRemainingPausedMs <= 0L) {
                    current.timerTotalSeconds * 1000L
                } else {
                    current.timerRemainingPausedMs
                }
                scheduleRemainingMs = durationMs
                scheduleTotalSec = current.timerTotalSeconds
                current.copy(
                    timerRunning = true,
                    timerFinishedAlert = false,
                    timerRemainingPausedMs = durationMs,
                    timerEndTargetMonoMs = nowMono + durationMs
                )
            }
            updated = next
            next
        }

        if (shouldCancel) {
            appContext?.let { SeleniaAlertManager.cancelBackgroundTimerAlert(it) }
        } else {
            val rem = scheduleRemainingMs
            if (rem != null && appContext != null) {
                SeleniaAlertManager.scheduleBackgroundTimerAlert(appContext, rem, scheduleTotalSec)
            }
        }
        persistTimerState(updated, nowMono)
    }

    fun resetTimer() {
        val nowMono = currentMonotonicMs()
        appContext?.let { SeleniaAlertManager.cancelBackgroundTimerAlert(it) }
        lateinit var updated: SeleniaUiState
        _uiState.update { current ->
            val next = current.copy(
                timerRunning = false,
                timerFinishedAlert = false,
                timerRemainingPausedMs = current.timerTotalSeconds * 1000L,
                timerEndTargetMonoMs = 0L
            )
            updated = next
            next
        }
        persistTimerState(updated, nowMono)
    }

    fun onForegroundTimerReachedZero() {
        val nowMono = currentMonotonicMs()
        var didTransitionToFinished = false
        var updated: SeleniaUiState? = null
        _uiState.update { current ->
            if (!current.timerRunning) return@update current
            didTransitionToFinished = true
            val next = current.copy(
                timerRunning = false,
                timerRemainingPausedMs = 0L,
                timerFinishedAlert = true
            )
            updated = next
            next
        }
        if (didTransitionToFinished) {
            appContext?.let { SeleniaAlertManager.triggerTimerFinishedFeedback(it) }
            updated?.let { persistTimerState(it, nowMono) }
        }
    }

    // --- Calendar, Notes & JSON Backup/Restore ---

    fun selectDate(date: SimpleDate) {
        _uiState.update { current ->
            if (date == current.selectedDate &&
                date.year == current.displayYear &&
                date.month == current.displayMonth
            ) {
                return@update current
            }
            val needMonthRefresh = date.year != current.displayYear || date.month != current.displayMonth
            val newCells = if (needMonthRefresh) {
                LunarAstronomer.getMonthGrid(date.year, date.month, current.today)
            } else {
                current.calendarCells
            }
            val newMilestones = if (needMonthRefresh) {
                LunarAstronomer.getMonthMajorMilestones(date.year, date.month)
            } else {
                current.monthMilestones
            }
            val firstCachedEventDate = current.upcomingCelestialEvents.firstOrNull()?.date
            val newCelestialEvents = if (
                !needMonthRefresh &&
                firstCachedEventDate != null &&
                date <= firstCachedEventDate &&
                date >= current.selectedDate
            ) {
                current.upcomingCelestialEvents
            } else {
                LunarAstronomer.getUpcomingCelestialEvents(date, count = 6)
            }

            current.copy(
                selectedDate = date,
                displayYear = date.year,
                displayMonth = date.month,
                calendarCells = newCells,
                monthMilestones = newMilestones,
                selectedDayLunarInfo = LunarAstronomer.getLunarInfo(date),
                upcomingMajorPhases = LunarAstronomer.getUpcomingMajorPhases(date, count = 4),
                upcomingCelestialEvents = newCelestialEvents
            )
        }
    }

    fun stepSelectedDay(deltaDays: Int) {
        val nextDate = LunarAstronomer.addDays(_uiState.value.selectedDate, deltaDays)
        selectDate(nextDate)
    }

    fun jumpToToday() {
        selectDate(LunarAstronomer.today())
    }

    fun jumpToYearMonth(year: Int, month: Int) {
        val safeYear = year.coerceIn(1900, 2150)
        val safeMonth = month.coerceIn(1, 12)
        val currentDay = _uiState.value.selectedDate.day
        val clampedDay = currentDay.coerceAtMost(LunarAstronomer.daysInMonth(safeYear, safeMonth))
        selectDate(SimpleDate(safeYear, safeMonth, clampedDay))
    }

    fun changeMonth(deltaMonths: Int) {
        _uiState.update { current ->
            var targetMonth = current.displayMonth + deltaMonths
            var targetYear = current.displayYear
            while (targetMonth > 12) {
                targetMonth -= 12
                targetYear += 1
            }
            while (targetMonth < 1) {
                targetMonth += 12
                targetYear -= 1
            }
            targetYear = targetYear.coerceIn(1900, 2150)
            val newCells = LunarAstronomer.getMonthGrid(targetYear, targetMonth, current.today)
            val newMilestones = LunarAstronomer.getMonthMajorMilestones(targetYear, targetMonth)
            current.copy(
                displayYear = targetYear,
                displayMonth = targetMonth,
                calendarCells = newCells,
                monthMilestones = newMilestones
            )
        }
    }

    fun toggleHemisphere() {
        val next = !_uiState.value.isNorthernHemisphere
        _uiState.update { it.copy(isNorthernHemisphere = next) }
        if (repository != null) {
            viewModelScope.launch { repository.setNorthernHemisphere(next) }
        }
        appContext?.let {
            SeleniaMoonWidgetProvider.refreshAllWidgets(
                context = it,
                isNorthernHemisphereOverride = next
            )
        }
    }

    fun setLunarAlertsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(lunarAlertsEnabled = enabled) }
        if (repository != null) {
            viewModelScope.launch { repository.setLunarAlertsEnabled(enabled) }
        }
        appContext?.let { SeleniaAlertManager.syncUpcomingLunarPhaseAlerts(it, enabled) }
    }

    fun addNoteForSelectedDate(
        title: String,
        category: String,
        reminderEnabled: Boolean = false,
        reminderHour: Int = 9,
        reminderMinute: Int = 0
    ) {
        val cleanTitle = title.trim().take(240)
        if (cleanTitle.isEmpty()) return
        val cleanCategory = category.trim().ifBlank { "Nota" }.take(48)
        val safeHour = reminderHour.coerceIn(0, 23)
        val safeMinute = reminderMinute.coerceIn(0, 59)
        val targetDate = _uiState.value.selectedDate
        val key = targetDate.toStorageKey()
        val alreadyExists = _uiState.value.notesByDate[key].orEmpty().any {
            it.title.equals(cleanTitle, ignoreCase = true) &&
                it.category.equals(cleanCategory, ignoreCase = true)
        }
        if (alreadyExists) return

        if (repository != null) {
            viewModelScope.launch {
                val insertedId = repository.insertNote(
                    date = targetDate,
                    title = cleanTitle,
                    category = cleanCategory,
                    reminderEnabled = reminderEnabled,
                    reminderHour = safeHour,
                    reminderMinute = safeMinute
                )
                if (reminderEnabled && appContext != null) {
                    SeleniaAlertManager.scheduleNoteReminder(
                        context = appContext,
                        noteId = insertedId,
                        date = targetDate,
                        title = cleanTitle,
                        category = cleanCategory,
                        reminderHour = safeHour,
                        reminderMinute = safeMinute
                    )
                }
            }
        } else {
            _uiState.update { current ->
                val existing = current.notesByDate[key].orEmpty()
                if (existing.any {
                        it.title.equals(cleanTitle, ignoreCase = true) &&
                            it.category.equals(cleanCategory, ignoreCase = true)
                    }
                ) {
                    return@update current
                }
                val newNote = CalendarNote(
                    id = idCounter.incrementAndGet(),
                    date = targetDate,
                    title = cleanTitle,
                    category = cleanCategory,
                    reminderEnabled = reminderEnabled,
                    reminderHour = safeHour,
                    reminderMinute = safeMinute
                )
                current.copy(
                    notesByDate = current.notesByDate + (key to (existing + newNote))
                )
            }
        }
    }

    fun updateNote(
        date: SimpleDate,
        noteId: Long,
        title: String,
        category: String,
        reminderEnabled: Boolean,
        reminderHour: Int = 9,
        reminderMinute: Int = 0
    ) {
        val cleanTitle = title.trim().take(240)
        if (cleanTitle.isEmpty()) return
        val cleanCategory = category.trim().ifBlank { "Nota" }.take(48)
        val safeHour = reminderHour.coerceIn(0, 23)
        val safeMinute = reminderMinute.coerceIn(0, 59)

        if (repository != null) {
            viewModelScope.launch {
                repository.updateNote(
                    noteId = noteId,
                    title = cleanTitle,
                    category = cleanCategory,
                    reminderEnabled = reminderEnabled,
                    reminderHour = safeHour,
                    reminderMinute = safeMinute
                )
                if (appContext != null) {
                    if (reminderEnabled) {
                        SeleniaAlertManager.scheduleNoteReminder(
                            context = appContext,
                            noteId = noteId,
                            date = date,
                            title = cleanTitle,
                            category = cleanCategory,
                            reminderHour = safeHour,
                            reminderMinute = safeMinute
                        )
                    } else {
                        SeleniaAlertManager.cancelNoteReminder(appContext, noteId)
                    }
                }
            }
        } else {
            _uiState.update { current ->
                val key = date.toStorageKey()
                val updatedList = current.notesByDate[key].orEmpty().map { existing ->
                    if (existing.id == noteId) {
                        existing.copy(
                            title = cleanTitle,
                            category = cleanCategory,
                            reminderEnabled = reminderEnabled,
                            reminderHour = safeHour,
                            reminderMinute = safeMinute
                        )
                    } else {
                        existing
                    }
                }
                current.copy(notesByDate = current.notesByDate + (key to updatedList))
            }
        }
    }

    fun deleteNote(date: SimpleDate, noteId: Long) {
        appContext?.let { SeleniaAlertManager.cancelNoteReminder(it, noteId) }
        if (repository != null) {
            viewModelScope.launch { repository.deleteNote(noteId) }
        } else {
            _uiState.update { current ->
                val key = date.toStorageKey()
                val updated = current.notesByDate[key].orEmpty().filterNot { it.id == noteId }
                current.copy(
                    notesByDate = current.notesByDate + (key to updated)
                )
            }
        }
    }

    fun dismissBackupStatusMessage() {
        _uiState.update { it.copy(backupStatusMessage = null) }
    }

    fun exportBackupJson(onReady: (String) -> Unit) {
        val repo = repository
        if (repo != null) {
            viewModelScope.launch {
                runCatching { repo.exportBackupJson() }
                    .onSuccess { json ->
                        onReady(json)
                        _uiState.update {
                            it.copy(backupStatusMessage = "Respaldo JSON exportado correctamente.")
                        }
                    }
                    .onFailure {
                        _uiState.update {
                            it.copy(backupStatusMessage = "No se pudo generar el respaldo JSON.")
                        }
                    }
            }
        } else {
            _uiState.update {
                it.copy(backupStatusMessage = "Almacenamiento local no disponible en modo de prueba.")
            }
        }
    }

    fun importBackupJson(rawJson: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            runCatching { repo.importBackupJson(rawJson) }
                .onSuccess { summary ->
                    val ctx = appContext
                    if (ctx != null) {
                        val settings = repo.getPersistedSettingsSnapshot()
                        SeleniaAlertManager.syncUpcomingLunarPhaseAlerts(ctx, settings.lunarAlertsEnabled)
                        repo.getReminderNotes().forEach { note ->
                            SeleniaAlertManager.scheduleNoteReminder(
                                context = ctx,
                                noteId = note.id,
                                date = note.date,
                                title = note.title,
                                category = note.category,
                                reminderHour = note.reminderHour,
                                reminderMinute = note.reminderMinute
                            )
                        }
                        SeleniaMoonWidgetProvider.refreshAllWidgets(
                            context = ctx,
                            isNorthernHemisphereOverride = settings.isNorthernHemisphere
                        )
                    }
                    _uiState.update {
                        it.copy(
                            backupStatusMessage = "Restaurado: ${summary.notesImported} notas nuevas, " +
                                "${summary.calculationsImported} cálculos y ${summary.worldCitiesCount} ciudades."
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(backupStatusMessage = "Archivo JSON inválido o incompatible.")
                    }
                }
        }
    }

    // --- Scientific Calculator Actions ---

    fun toggleRadiansMode() {
        val nextRad = !_uiState.value.isRadiansMode
        _uiState.update { current ->
            val preview = if (current.expression.isBlank()) {
                "0"
            } else {
                ScientificCalculatorEngine.evaluate(current.expression, nextRad)
                    .map { ScientificCalculatorEngine.formatNumber(it) }
                    .getOrDefault(current.liveResultPreview)
            }
            current.copy(
                isRadiansMode = nextRad,
                liveResultPreview = preview
            )
        }
        if (repository != null) {
            viewModelScope.launch { repository.setRadiansMode(nextRad) }
        }
    }

    fun toggleInverseTrigMode() {
        _uiState.update { it.copy(isInverseTrigMode = !it.isInverseTrigMode) }
    }

    fun onCalculatorInput(token: String) {
        if (token.isEmpty()) return
        _uiState.update { current ->
            val base = if (current.expression == "Error") "" else current.expression
            if (base.length + token.length > ScientificCalculatorEngine.MAX_EXPRESSION_LENGTH) {
                return@update current
            }

            val isBinaryOp = token in BINARY_OPERATORS
            val newExpr = when {
                base.isEmpty() && (token == "×" || token == "÷" || token == "^" || token == "!") ->
                    "${current.lastAnswer.takeIf { it != "Error" } ?: "0"}$token"
                isBinaryOp && base.isNotEmpty() && base.last().toString() in BINARY_OPERATORS ->
                    base.dropLast(1) + token
                else -> base + token
            }

            val preview = ScientificCalculatorEngine.evaluate(newExpr, current.isRadiansMode)
                .map { ScientificCalculatorEngine.formatNumber(it) }
                .getOrDefault(current.liveResultPreview)
            current.copy(
                expression = newExpr,
                liveResultPreview = preview
            )
        }
    }

    fun onInsertLastAnswer() {
        val ans = _uiState.value.lastAnswer.takeIf { it.isNotBlank() && it != "Error" } ?: return
        onCalculatorInput(ans)
    }

    fun onCalculatorClear() {
        _uiState.update {
            it.copy(expression = "", liveResultPreview = "0")
        }
    }

    fun onCalculatorBackspace() {
        _uiState.update { current ->
            val newExpr = ScientificCalculatorEngine.smartBackspace(current.expression)
            val preview = if (newExpr.isEmpty()) {
                "0"
            } else {
                ScientificCalculatorEngine.evaluate(newExpr, current.isRadiansMode)
                    .map { ScientificCalculatorEngine.formatNumber(it) }
                    .getOrDefault(current.liveResultPreview)
            }
            current.copy(
                expression = newExpr,
                liveResultPreview = preview
            )
        }
    }

    fun onCalculatorEvaluate(customBadge: String? = null) {
        var recordToPersist: Triple<String, String, String?>? = null
        _uiState.update { current ->
            val trimmedExpr = current.expression.trim()
            if (trimmedExpr.isEmpty()) return@update current
            val evalResult = ScientificCalculatorEngine.evaluate(trimmedExpr, current.isRadiansMode)
            evalResult.fold(
                onSuccess = { num ->
                    val formatted = ScientificCalculatorEngine.formatNumber(num)
                    val topRecord = current.calculationHistory.firstOrNull()
                    val isTrivialIdentity = customBadge == null && trimmedExpr == formatted
                    val isConsecutiveDuplicate = topRecord != null &&
                        topRecord.expression == trimmedExpr &&
                        topRecord.result == formatted

                    if (isTrivialIdentity || isConsecutiveDuplicate) {
                        current.copy(
                            expression = formatted,
                            liveResultPreview = formatted,
                            lastAnswer = formatted
                        )
                    } else {
                        val hasTrig = trimmedExpr.contains("sin") ||
                            trimmedExpr.contains("cos") ||
                            trimmedExpr.contains("tan")
                        val modeTag = customBadge ?: if (current.isRadiansMode && hasTrig) {
                            "Modo RAD"
                        } else {
                            null
                        }
                        recordToPersist = Triple(trimmedExpr, formatted, modeTag)
                        val record = CalculationRecord(
                            id = idCounter.incrementAndGet(),
                            expression = trimmedExpr,
                            result = formatted,
                            badgeLabel = modeTag
                        )
                        current.copy(
                            expression = formatted,
                            liveResultPreview = formatted,
                            lastAnswer = formatted,
                            calculationHistory = listOf(record) + current.calculationHistory.take(29)
                        )
                    }
                },
                onFailure = {
                    current.copy(liveResultPreview = "Error")
                }
            )
        }
        val persisted = recordToPersist
        if (repository != null && persisted != null) {
            viewModelScope.launch {
                repository.insertCalculation(persisted.first, persisted.second, persisted.third)
            }
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(calculationHistory = emptyList()) }
        if (repository != null) {
            viewModelScope.launch { repository.clearCalculations() }
        }
    }

    fun useHistoryItem(record: CalculationRecord) {
        _uiState.update {
            it.copy(
                expression = record.result,
                liveResultPreview = record.result
            )
        }
    }

    companion object {
        private val BINARY_OPERATORS = setOf("+", "-", "−", "×", "÷", "^")

        fun provideFactory(
            repository: SeleniaRepository,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SeleniaViewModel(repository = repository, appContext = appContext) as T
            }
        }
    }
}
