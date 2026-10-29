package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.domain.CalculationRecord
import com.example.domain.CalendarDayCell
import com.example.domain.LunarAstronomer
import com.example.domain.LunarDayInfo
import com.example.domain.MajorPhaseMilestone
import com.example.domain.MoonPhaseType
import com.example.domain.ScientificCalculatorEngine
import com.example.domain.SimpleDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

enum class AppDestination {
    CALENDAR,
    MOON_OBSERVATORY,
    CALCULATOR
}

enum class CalculatorMode {
    SCIENTIFIC,
    DATE_AND_LUNAR
}

data class CalendarNote(
    val id: Long,
    val date: SimpleDate,
    val title: String,
    val category: String
)

data class SeleniaUiState(
    val currentDestination: AppDestination = AppDestination.CALENDAR,
    val today: SimpleDate = LunarAstronomer.today(),
    val selectedDate: SimpleDate = LunarAstronomer.today(),
    val displayYear: Int = LunarAstronomer.today().year,
    val displayMonth: Int = LunarAstronomer.today().month,
    val isNorthernHemisphere: Boolean = true,
    val calendarCells: List<CalendarDayCell> = emptyList(),
    val monthMilestones: List<MajorPhaseMilestone> = emptyList(),
    val selectedDayLunarInfo: LunarDayInfo = LunarAstronomer.getLunarInfo(LunarAstronomer.today()),
    val upcomingMajorPhases: List<MajorPhaseMilestone> = emptyList(),
    val notesByDate: Map<String, List<CalendarNote>> = emptyMap(),

    // Calculator State
    val calculatorMode: CalculatorMode = CalculatorMode.SCIENTIFIC,
    val expression: String = "",
    val liveResultPreview: String = "0",
    val calculationHistory: List<CalculationRecord> = emptyList(),

    // Date & Lunar Cycle Calculator State
    val dateOffsetDays: Int = 29,
    val dateOffsetLunarCycles: Double = 1.0,
    val useLunarCycleOffset: Boolean = false,
    val comparisonEndDate: SimpleDate = LunarAstronomer.addDays(LunarAstronomer.today(), 30)
) {
    val computedTargetDate: SimpleDate
        get() {
            val totalOffset = if (useLunarCycleOffset) {
                (dateOffsetLunarCycles * LunarAstronomer.SYNODIC_MONTH_DAYS).roundToInt()
            } else {
                dateOffsetDays
            }
            return LunarAstronomer.addDays(selectedDate, totalOffset)
        }

    val computedTargetLunarInfo: LunarDayInfo
        get() = LunarAstronomer.getLunarInfo(computedTargetDate)

    val daysBetweenSelectedAndComparison: Int
        get() = LunarAstronomer.daysBetween(selectedDate, comparisonEndDate)

    val synodicCyclesBetweenSelectedAndComparison: Double
        get() = daysBetweenSelectedAndComparison / LunarAstronomer.SYNODIC_MONTH_DAYS
}

class SeleniaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<SeleniaUiState> = _uiState.asStateFlow()

    private fun createInitialState(): SeleniaUiState {
        val today = LunarAstronomer.today()
        val cells = LunarAstronomer.getMonthGrid(today.year, today.month, today)
        val milestones = LunarAstronomer.getMonthMajorMilestones(today.year, today.month)
        val lunarInfo = LunarAstronomer.getLunarInfo(today)
        val upcoming = LunarAstronomer.getUpcomingMajorPhases(today, count = 4)

        // Seed helpful initial astronomical notes on upcoming major phases so the calendar feels alive immediately
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
                    category = "Observatorio"
                )
            )
        }

        // Seed 2 example calculations showing lunar + math capabilities
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
            notesByDate = initialNotes,
            calculationHistory = initialHistory
        )
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
    }

    fun selectDate(date: SimpleDate) {
        _uiState.update { current ->
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
            current.copy(
                selectedDate = date,
                displayYear = date.year,
                displayMonth = date.month,
                calendarCells = newCells,
                monthMilestones = newMilestones,
                selectedDayLunarInfo = LunarAstronomer.getLunarInfo(date),
                upcomingMajorPhases = LunarAstronomer.getUpcomingMajorPhases(date, count = 4)
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
        _uiState.update { it.copy(isNorthernHemisphere = !it.isNorthernHemisphere) }
    }

    fun addNoteForSelectedDate(title: String, category: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        _uiState.update { current ->
            val key = current.selectedDate.toStorageKey()
            val existing = current.notesByDate[key].orEmpty()
            val newNote = CalendarNote(
                id = System.currentTimeMillis(),
                date = current.selectedDate,
                title = cleanTitle,
                category = category.ifBlank { "Nota" }
            )
            current.copy(
                notesByDate = current.notesByDate + (key to (existing + newNote))
            )
        }
    }

    fun deleteNote(date: SimpleDate, noteId: Long) {
        _uiState.update { current ->
            val key = date.toStorageKey()
            val updated = current.notesByDate[key].orEmpty().filterNot { it.id == noteId }
            current.copy(
                notesByDate = current.notesByDate + (key to updated)
            )
        }
    }

    // Calculator Actions
    fun setCalculatorMode(mode: CalculatorMode) {
        _uiState.update { it.copy(calculatorMode = mode) }
    }

    fun onCalculatorInput(token: String) {
        _uiState.update { current ->
            val newExpr = current.expression + token
            val preview = ScientificCalculatorEngine.evaluate(newExpr)
                .map { ScientificCalculatorEngine.formatNumber(it) }
                .getOrDefault(current.liveResultPreview)
            current.copy(
                expression = newExpr,
                liveResultPreview = preview
            )
        }
    }

    fun onCalculatorClear() {
        _uiState.update {
            it.copy(expression = "", liveResultPreview = "0")
        }
    }

    fun onCalculatorBackspace() {
        _uiState.update { current ->
            val newExpr = if (current.expression.isNotEmpty()) {
                current.expression.dropLast(1)
            } else {
                ""
            }
            val preview = if (newExpr.isEmpty()) {
                "0"
            } else {
                ScientificCalculatorEngine.evaluate(newExpr)
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
        _uiState.update { current ->
            if (current.expression.isBlank()) return@update current
            val evalResult = ScientificCalculatorEngine.evaluate(current.expression)
            evalResult.fold(
                onSuccess = { num ->
                    val formatted = ScientificCalculatorEngine.formatNumber(num)
                    val record = CalculationRecord(
                        id = System.currentTimeMillis(),
                        expression = current.expression,
                        result = formatted,
                        badgeLabel = customBadge
                    )
                    current.copy(
                        expression = formatted,
                        liveResultPreview = formatted,
                        calculationHistory = listOf(record) + current.calculationHistory.take(19)
                    )
                },
                onFailure = {
                    current.copy(liveResultPreview = "Error")
                }
            )
        }
    }

    fun convertCurrentValueToSynodicCycles() {
        _uiState.update { current ->
            val baseExpr = current.expression.ifBlank { current.liveResultPreview }
            val currentVal = ScientificCalculatorEngine.evaluate(baseExpr).getOrNull() ?: return@update current
            val cycles = currentVal / LunarAstronomer.SYNODIC_MONTH_DAYS
            val formatted = ScientificCalculatorEngine.formatNumber(cycles)
            val record = CalculationRecord(
                id = System.currentTimeMillis(),
                expression = "${ScientificCalculatorEngine.formatNumber(currentVal)} días ÷ 29.5306",
                result = formatted,
                badgeLabel = "Ciclos Lunares"
            )
            current.copy(
                expression = formatted,
                liveResultPreview = formatted,
                calculationHistory = listOf(record) + current.calculationHistory.take(19)
            )
        }
    }

    fun multiplyCurrentValueBySynodicCycle() {
        _uiState.update { current ->
            val baseExpr = current.expression.ifBlank { "1" }
            val currentVal = ScientificCalculatorEngine.evaluate(baseExpr).getOrNull() ?: 1.0
            val days = currentVal * LunarAstronomer.SYNODIC_MONTH_DAYS
            val formatted = ScientificCalculatorEngine.formatNumber(days)
            val record = CalculationRecord(
                id = System.currentTimeMillis(),
                expression = "${ScientificCalculatorEngine.formatNumber(currentVal)} × 29.5306 d",
                result = formatted,
                badgeLabel = "Días en ${ScientificCalculatorEngine.formatNumber(currentVal)} lunas"
            )
            current.copy(
                expression = formatted,
                liveResultPreview = formatted,
                calculationHistory = listOf(record) + current.calculationHistory.take(19)
            )
        }
    }

    fun sendSelectedDayLunarAgeToCalculator() {
        val ageFormatted = "%.2f".format(java.util.Locale.US, _uiState.value.selectedDayLunarInfo.lunarAgeDays)
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
}
