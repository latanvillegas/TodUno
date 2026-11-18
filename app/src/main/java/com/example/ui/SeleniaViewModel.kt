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
import com.example.domain.MoonPhaseType
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
import kotlin.math.roundToInt

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

enum class CalculatorMode {
    SCIENTIFIC,
    DATE_AND_LUNAR
}

@Immutable
data class CalendarNote(
    val id: Long,
    val date: SimpleDate,
    val title: String,
    val category: String,
    val reminderEnabled: Boolean = false
)

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

    // Clock & World Cities State
    val clockSubTab: ClockSubTab = ClockSubTab.WORLD_CLOCK,
    val is24HourFormat: Boolean = true,
    val selectedWorldCityIds: List<String> = SeleniaPersistedSettings.DEFAULT_WORLD_CITY_IDS,

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
    val calculatorMode: CalculatorMode = CalculatorMode.SCIENTIFIC,
    val isRadiansMode: Boolean = false,
    val expression: String = "",
    val liveResultPreview: String = "0",
    val lastAnswer: String = "0",
    val calculationHistory: List<CalculationRecord> = emptyList(),

    // Date & Lunar Cycle Calculator State
    val dateOffsetDays: Int = 29,
    val dateOffsetLunarCycles: Double = 1.0,
    val useLunarCycleOffset: Boolean = false,
    val comparisonStartDate: SimpleDate = LunarAstronomer.today(),
    val comparisonEndDate: SimpleDate = LunarAstronomer.addDays(LunarAstronomer.today(), 30)
) {
    val computedTargetDate: SimpleDate by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val totalOffset = if (useLunarCycleOffset) {
            (dateOffsetLunarCycles * LunarAstronomer.SYNODIC_MONTH_DAYS).roundToInt()
        } else {
            dateOffsetDays
        }
        LunarAstronomer.addDays(selectedDate, totalOffset)
    }

    val computedTargetLunarInfo: LunarDayInfo by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LunarAstronomer.getLunarInfo(computedTargetDate)
    }

    val daysBetweenSelectedAndComparison: Int by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LunarAstronomer.daysBetween(comparisonStartDate, comparisonEndDate)
    }

    val synodicCyclesBetweenSelectedAndComparison: Double
        get() = daysBetweenSelectedAndComparison / LunarAstronomer.SYNODIC_MONTH_DAYS

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
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<SeleniaUiState> = _uiState.asStateFlow()

    init {
        if (repository != null) {
            viewModelScope.launch {
                repository.ensureSeeded()
            }
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
                    _uiState.update { current ->
                        val preview = if (current.expression.isBlank()) {
                            current.liveResultPreview
                        } else {
                            ScientificCalculatorEngine.evaluate(current.expression, settings.isRadiansMode)
                                .map { ScientificCalculatorEngine.formatNumber(it) }
                                .getOrDefault(current.liveResultPreview)
                        }
                        current.copy(
                            isNorthernHemisphere = settings.isNorthernHemisphere,
                            is24HourFormat = settings.is24HourFormat,
                            isRadiansMode = settings.isRadiansMode,
                            lunarAlertsEnabled = settings.lunarAlertsEnabled,
                            selectedWorldCityIds = settings.selectedWorldCityIds,
                            liveResultPreview = preview
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

        val initialNotes = mutableMapOf<String, List<CalendarNote>>()
        upcoming.firstOrNull { it.phaseType == MoonPhaseType.FULL_MOON }?.let { fullMoon ->
            val key = fullMoon.date.toStorageKey()
            initialNotes[key] = listOf(
                CalendarNote(
                    id = 1L,
                    date = fullMoon.date,
                    title = "Observación de Plenilunio (100% iluminada)",
                    category = "Astronomía"
                )
            )
        }
        upcoming.firstOrNull { it.phaseType == MoonPhaseType.NEW_MOON }?.let { newMoon ->
            val key = newMoon.date.toStorageKey()
            initialNotes[key] = listOf(
                CalendarNote(
                    id = 2L,
                    date = newMoon.date,
                    title = "Noche sin luna: ideal para cielo profundo",
                    category = "Astronomía"
                )
            )
        }

        val initialHistory = listOf(
            CalculationRecord(
                id = 1L,
                expression = "12 × 29.5306",
                result = "354.3672",
                badgeLabel = "Año Lunar (12 lunas)"
            ),
            CalculationRecord(
                id = 2L,
                expression = "365.2422 − (12 × 29.5306)",
                result = "10.875",
                badgeLabel = "Epacta anual (días)"
            )
        )

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
            notesByDate = initialNotes,
            calculationHistory = initialHistory,
            lastAnswer = initialHistory.first().result
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
                next = next.copy(
                    timerRunning = false,
                    timerRemainingPausedMs = 0L,
                    timerFinishedAlert = true
                )
            }
            next
        }

        if (dayChanged) {
            appContext?.let { SeleniaMoonWidgetProvider.refreshAllWidgets(it) }
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

    fun toggle24HourFormat() {
        val next = !_uiState.value.is24HourFormat
        _uiState.update { it.copy(is24HourFormat = next) }
        if (repository != null) {
            viewModelScope.launch { repository.set24HourFormat(next) }
        }
    }

    fun toggleWorldCity(timeZoneId: String) {
        var updatedIds: List<String> = emptyList()
        _uiState.update { current ->
            val next = if (timeZoneId in current.selectedWorldCityIds) {
                if (current.selectedWorldCityIds.size > 1) {
                    current.selectedWorldCityIds - timeZoneId
                } else {
                    current.selectedWorldCityIds
                }
            } else {
                (current.selectedWorldCityIds + timeZoneId).distinct()
            }
            updatedIds = next
            current.copy(selectedWorldCityIds = next)
        }
        if (repository != null && updatedIds.isNotEmpty()) {
            viewModelScope.launch { repository.setSelectedWorldCityIds(updatedIds) }
        }
    }

    // --- Stopwatch Actions (Monotonic & Zero-Tick in ViewModel) ---

    fun toggleStopwatch() {
        val nowMono = currentMonotonicMs()
        _uiState.update { current ->
            if (current.stopwatchRunning) {
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
        }
    }

    fun lapStopwatch() {
        val nowMono = currentMonotonicMs()
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
            current.copy(
                stopwatchLaps = (listOf(newLap) + current.stopwatchLaps).take(200)
            )
        }
    }

    fun resetStopwatch() {
        _uiState.update { current ->
            current.copy(
                stopwatchRunning = false,
                stopwatchAccumulatedMs = 0L,
                stopwatchLastStartMonoMs = 0L,
                stopwatchLaps = emptyList()
            )
        }
    }

    // --- Timer Actions (Monotonic & Zero-Tick in ViewModel) ---

    fun selectTimerPresetSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(10, 7200)
        appContext?.let { SeleniaAlertManager.cancelBackgroundTimerAlert(it) }
        _uiState.update { current ->
            current.copy(
                timerRunning = false,
                timerFinishedAlert = false,
                timerTotalSeconds = clamped,
                timerRemainingPausedMs = clamped * 1000L,
                timerEndTargetMonoMs = 0L
            )
        }
    }

    fun adjustTimerSeconds(deltaSec: Int) {
        val nowMono = currentMonotonicMs()
        var scheduleRemainingMs: Long? = null
        var scheduleTotalSec = 0

        _uiState.update { current ->
            val nextSec = (current.timerTotalSeconds + deltaSec).coerceIn(10, 7200)
            scheduleTotalSec = nextSec
            if (!current.timerRunning) {
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
        }

        val rem = scheduleRemainingMs
        if (rem != null && appContext != null) {
            SeleniaAlertManager.scheduleBackgroundTimerAlert(appContext, rem, scheduleTotalSec)
        }
    }

    fun toggleTimerStartPause() {
        val nowMono = currentMonotonicMs()
        var scheduleRemainingMs: Long? = null
        var scheduleTotalSec = 0
        var shouldCancel = false

        _uiState.update { current ->
            if (current.timerRunning) {
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
        }

        if (shouldCancel) {
            appContext?.let { SeleniaAlertManager.cancelBackgroundTimerAlert(it) }
        } else {
            val rem = scheduleRemainingMs
            if (rem != null && appContext != null) {
                SeleniaAlertManager.scheduleBackgroundTimerAlert(appContext, rem, scheduleTotalSec)
            }
        }
    }

    fun resetTimer() {
        appContext?.let { SeleniaAlertManager.cancelBackgroundTimerAlert(it) }
        _uiState.update { current ->
            current.copy(
                timerRunning = false,
                timerFinishedAlert = false,
                timerRemainingPausedMs = current.timerTotalSeconds * 1000L,
                timerEndTargetMonoMs = 0L
            )
        }
    }

    fun onForegroundTimerReachedZero() {
        var didTransitionToFinished = false
        _uiState.update { current ->
            if (!current.timerRunning) return@update current
            didTransitionToFinished = true
            current.copy(
                timerRunning = false,
                timerRemainingPausedMs = 0L,
                timerFinishedAlert = true
            )
        }
        if (didTransitionToFinished) {
            appContext?.let { SeleniaAlertManager.triggerTimerFinishedFeedback(it) }
        }
    }

    // --- Calendar & Lunar Navigation ---

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
            // Reuse celestial events if start date is still before the earliest cached celestial event
            // and within the same month, avoiding redundant list rebuilds on single-day taps!
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
        appContext?.let { SeleniaMoonWidgetProvider.refreshAllWidgets(it) }
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
        reminderEnabled: Boolean = false
    ) {
        val cleanTitle = title.trim().take(240)
        if (cleanTitle.isEmpty()) return
        val cleanCategory = category.trim().ifBlank { "Nota" }.take(48)
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
                    reminderEnabled = reminderEnabled
                )
                if (reminderEnabled && appContext != null) {
                    SeleniaAlertManager.scheduleNoteReminder(
                        context = appContext,
                        noteId = insertedId,
                        date = targetDate,
                        title = cleanTitle,
                        category = cleanCategory
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
                    reminderEnabled = reminderEnabled
                )
                current.copy(
                    notesByDate = current.notesByDate + (key to (existing + newNote))
                )
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

    // --- Calculator Actions ---

    fun setCalculatorMode(mode: CalculatorMode) {
        _uiState.update { current ->
            if (current.calculatorMode == mode) current else current.copy(calculatorMode = mode)
        }
    }

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

    fun onCalculatorInput(token: String) {
        if (token.isEmpty()) return
        _uiState.update { current ->
            val base = if (current.expression == "Error") "" else current.expression
            if (base.length + token.length > ScientificCalculatorEngine.MAX_EXPRESSION_LENGTH) {
                return@update current
            }

            val isBinaryOp = token in BINARY_OPERATORS
            val newExpr = when {
                base.isEmpty() && (token == "×" || token == "÷" || token == "^") ->
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
                    // Avoid inserting trivial "123 = 123" or exact duplicate of top history item
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
                        val modeTag = customBadge ?: if (current.isRadiansMode &&
                            (trimmedExpr.contains("sin") || trimmedExpr.contains("cos") || trimmedExpr.contains("tan"))
                        ) {
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

    fun convertCurrentValueToSynodicCycles() {
        var recordToPersist: Triple<String, String, String?>? = null
        _uiState.update { current ->
            val baseExpr = current.expression.ifBlank { current.liveResultPreview }
            val currentVal = ScientificCalculatorEngine.evaluate(baseExpr, current.isRadiansMode).getOrNull()
                ?: return@update current
            val cycles = currentVal / LunarAstronomer.SYNODIC_MONTH_DAYS
            val formatted = ScientificCalculatorEngine.formatNumber(cycles)
            val exprText = "${ScientificCalculatorEngine.formatNumber(currentVal)} días ÷ 29.5306"
            val topRecord = current.calculationHistory.firstOrNull()
            val isDuplicate = topRecord != null && topRecord.expression == exprText && topRecord.result == formatted
            val nextHistory = if (isDuplicate) {
                current.calculationHistory
            } else {
                recordToPersist = Triple(exprText, formatted, "Ciclos Lunares")
                val record = CalculationRecord(
                    id = idCounter.incrementAndGet(),
                    expression = exprText,
                    result = formatted,
                    badgeLabel = "Ciclos Lunares"
                )
                listOf(record) + current.calculationHistory.take(29)
            }
            current.copy(
                expression = formatted,
                liveResultPreview = formatted,
                lastAnswer = formatted,
                calculationHistory = nextHistory
            )
        }
        val persisted = recordToPersist
        if (repository != null && persisted != null) {
            viewModelScope.launch {
                repository.insertCalculation(persisted.first, persisted.second, persisted.third)
            }
        }
    }

    fun multiplyCurrentValueBySynodicCycle() {
        var recordToPersist: Triple<String, String, String?>? = null
        _uiState.update { current ->
            val baseExpr = current.expression.ifBlank { "1" }
            val currentVal = ScientificCalculatorEngine.evaluate(baseExpr, current.isRadiansMode).getOrNull() ?: 1.0
            val days = currentVal * LunarAstronomer.SYNODIC_MONTH_DAYS
            val formatted = ScientificCalculatorEngine.formatNumber(days)
            val exprText = "${ScientificCalculatorEngine.formatNumber(currentVal)} × 29.5306 d"
            val badge = "Días en ${ScientificCalculatorEngine.formatNumber(currentVal)} lunas"
            val topRecord = current.calculationHistory.firstOrNull()
            val isDuplicate = topRecord != null && topRecord.expression == exprText && topRecord.result == formatted
            val nextHistory = if (isDuplicate) {
                current.calculationHistory
            } else {
                recordToPersist = Triple(exprText, formatted, badge)
                val record = CalculationRecord(
                    id = idCounter.incrementAndGet(),
                    expression = exprText,
                    result = formatted,
                    badgeLabel = badge
                )
                listOf(record) + current.calculationHistory.take(29)
            }
            current.copy(
                expression = formatted,
                liveResultPreview = formatted,
                lastAnswer = formatted,
                calculationHistory = nextHistory
            )
        }
        val persisted = recordToPersist
        if (repository != null && persisted != null) {
            viewModelScope.launch {
                repository.insertCalculation(persisted.first, persisted.second, persisted.third)
            }
        }
    }

    fun sendSelectedDayLunarAgeToCalculator() {
        val ageFormatted = "%.2f".format(Locale.US, _uiState.value.selectedDayLunarInfo.lunarAgeDays)
        _uiState.update { current ->
            current.copy(
                currentDestination = AppDestination.CALCULATOR,
                calculatorMode = CalculatorMode.SCIENTIFIC,
                expression = ageFormatted,
                liveResultPreview = ageFormatted
            )
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

    // Date & Lunar Calculator controls
    fun setUseLunarCycleOffset(useCycles: Boolean) {
        _uiState.update { it.copy(useLunarCycleOffset = useCycles) }
    }

    fun updateDateOffsetDays(days: Int) {
        _uiState.update { it.copy(dateOffsetDays = days.coerceIn(-3650, 3650)) }
    }

    fun updateDateOffsetLunarCycles(cycles: Double) {
        _uiState.update { it.copy(dateOffsetLunarCycles = cycles.coerceIn(-120.0, 120.0)) }
    }

    fun stepComparisonEndDate(deltaDays: Int) {
        _uiState.update { current ->
            current.copy(
                comparisonEndDate = LunarAstronomer.addDays(current.comparisonEndDate, deltaDays)
            )
        }
    }

    fun setComparisonStartDate(date: SimpleDate) {
        _uiState.update { it.copy(comparisonStartDate = date) }
    }

    fun setComparisonEndDate(date: SimpleDate) {
        _uiState.update { it.copy(comparisonEndDate = date) }
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
