package com.example.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.example.domain.CalculationRecord
import com.example.domain.CalendarDayCell
import com.example.domain.ColorHarmonyEngine
import com.example.domain.ColorSwatchSpec
import com.example.domain.GradientStyleType
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
    CLOCK,
    CALCULATOR,
    COLORS
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

enum class ColorStudioSubTab {
    CONVERTER_SLIDERS,
    GRADIENTS_AND_MESH,
    COMBINATIONS,
    TONES_AND_SHADES,
    COLOR_CATALOG
}

enum class ModuleThemeTarget(
    val spanishTitle: String,
    val shortBadge: String,
    val emoji: String,
    val howItAppliesDescription: String
) {
    ALL_MODULES(
        spanishTitle = "Toda la App (Global)",
        shortBadge = "Toda la App",
        emoji = "🌐",
        howItAppliesDescription = "Aplica el color y degradado simultáneamente a Calendario, Luna, Reloj y Calculadora."
    ),
    CALENDAR(
        spanishTitle = "Calendario Lunar",
        shortBadge = "Calendario",
        emoji = "📅",
        howItAppliesDescription = "Personaliza la cabecera del mes, el fondo de la ficha del día, los bordes de selección y los hitos lunares."
    ),
    MOON_OBSERVATORY(
        spanishTitle = "Observatorio de la Luna",
        shortBadge = "Luna",
        emoji = "🌕",
        howItAppliesDescription = "Tiñe la superficie iluminada de la Luna, su corona estelar, el fondo del observatorio y la barra sinódica."
    ),
    CLOCK(
        spanishTitle = "Reloj, Cronómetro y Temporizador",
        shortBadge = "Reloj",
        emoji = "⌚",
        howItAppliesDescription = "Personaliza el dial del reloj analógico, las agujas, los dígitos LED, el cronómetro y el anillo del temporizador."
    ),
    CALCULATOR(
        spanishTitle = "Calculadora Científica y Fechas",
        shortBadge = "Calculadora",
        emoji = "🧮",
        howItAppliesDescription = "Aplica el degradado al visor de resultados, la tecla '=' y los botones de operadores y funciones."
    )
}

@Immutable
data class ModuleVisualSkin(
    val target: ModuleThemeTarget,
    val primarySwatch: ColorSwatchSpec,
    val secondarySwatch: ColorSwatchSpec,
    val gradientStops: List<ColorSwatchSpec>,
    val gradientStyle: GradientStyleType = GradientStyleType.MESH,
    val gradientAngleDeg: Int = 135,
    val focalX: Float = 0.5f,
    val focalY: Float = 0.5f,
    val radiusFactor: Float = 0.9f,
    val isCustomApplied: Boolean = false
)

@Immutable
data class CalendarNote(
    val id: Long,
    val date: SimpleDate,
    val title: String,
    val category: String
)

@Immutable
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

    // Clock State
    val clockSubTab: ClockSubTab = ClockSubTab.WORLD_CLOCK,
    val is24HourFormat: Boolean = true,

    // Calculator State
    val calculatorMode: CalculatorMode = CalculatorMode.SCIENTIFIC,
    val expression: String = "",
    val liveResultPreview: String = "0",
    val calculationHistory: List<CalculationRecord> = emptyList(),

    // Date & Lunar Cycle Calculator State
    val dateOffsetDays: Int = 29,
    val dateOffsetLunarCycles: Double = 1.0,
    val useLunarCycleOffset: Boolean = false,
    val comparisonEndDate: SimpleDate = LunarAstronomer.addDays(LunarAstronomer.today(), 30),

    // Color Studio & Gradients State
    val colorStudioSubTab: ColorStudioSubTab = ColorStudioSubTab.CONVERTER_SLIDERS,
    val activeColorSwatch: ColorSwatchSpec = ColorHarmonyEngine.fromRgb(245, 196, 81),
    val gradientStyle: GradientStyleType = GradientStyleType.MESH,
    val gradientAngleDeg: Int = 135,
    val radialFocalX: Float = 0.5f,
    val radialFocalY: Float = 0.5f,
    val radialRadiusFactor: Float = 0.85f,
    val gradientVariantIndex: Int = 0,
    val customGradientPresetStops: List<ColorSwatchSpec>? = null,
    val selectedThemeTarget: ModuleThemeTarget = ModuleThemeTarget.ALL_MODULES,
    val moduleSkins: Map<ModuleThemeTarget, ModuleVisualSkin> = defaultModuleSkins(),
    val savedColorSwatches: List<ColorSwatchSpec> = listOf(
        ColorHarmonyEngine.fromRgb(245, 196, 81),  // Oro Lunar #F5C451
        ColorHarmonyEngine.fromRgb(100, 210, 255), // Cian Celestial #64D2FF
        ColorHarmonyEngine.fromRgb(92, 225, 166),  // Menta Aurora #5CE1A6
        ColorHarmonyEngine.fromRgb(255, 122, 107), // Coral Eclipse #FF7A6B
        ColorHarmonyEngine.fromRgb(138, 92, 246),  // Violeta Nebulosa #8A5CF6
        ColorHarmonyEngine.fromRgb(236, 72, 153),  // Magenta Neón #EC4899
        ColorHarmonyEngine.fromRgb(16, 185, 129),  // Esmeralda #10B981
        ColorHarmonyEngine.fromRgb(28, 39, 82)     // Índigo Astrolabio #1C2752
    )
) {
    val computedTargetDate: SimpleDate by lazy(LazyThreadSafetyMode.NONE) {
        val totalOffset = if (useLunarCycleOffset) {
            (dateOffsetLunarCycles * LunarAstronomer.SYNODIC_MONTH_DAYS).roundToInt()
        } else {
            dateOffsetDays
        }
        LunarAstronomer.addDays(selectedDate, totalOffset)
    }

    val computedTargetLunarInfo: LunarDayInfo by lazy(LazyThreadSafetyMode.NONE) {
        LunarAstronomer.getLunarInfo(computedTargetDate)
    }

    val daysBetweenSelectedAndComparison: Int by lazy(LazyThreadSafetyMode.NONE) {
        LunarAstronomer.daysBetween(selectedDate, comparisonEndDate)
    }

    val synodicCyclesBetweenSelectedAndComparison: Double
        get() = daysBetweenSelectedAndComparison / LunarAstronomer.SYNODIC_MONTH_DAYS

    val activeGradientStops: List<ColorSwatchSpec> by lazy(LazyThreadSafetyMode.NONE) {
        customGradientPresetStops
            ?: ColorHarmonyEngine.buildDynamicGradientStops(activeColorSwatch, gradientVariantIndex)
    }

    private val colorStudioPreviewSkin: ModuleVisualSkin by lazy(LazyThreadSafetyMode.NONE) {
        ModuleVisualSkin(
            target = ModuleThemeTarget.ALL_MODULES,
            primarySwatch = activeColorSwatch,
            secondarySwatch = activeGradientStops.getOrElse(2) { activeColorSwatch },
            gradientStops = activeGradientStops,
            gradientStyle = gradientStyle,
            gradientAngleDeg = gradientAngleDeg,
            focalX = radialFocalX,
            focalY = radialFocalY,
            radiusFactor = radialRadiusFactor,
            isCustomApplied = true
        )
    }

    fun skinFor(target: ModuleThemeTarget): ModuleVisualSkin {
        return moduleSkins[target] ?: defaultSkinForTarget(target)
    }

    fun skinForDestination(destination: AppDestination): ModuleVisualSkin {
        return when (destination) {
            AppDestination.CALENDAR -> skinFor(ModuleThemeTarget.CALENDAR)
            AppDestination.MOON_OBSERVATORY -> skinFor(ModuleThemeTarget.MOON_OBSERVATORY)
            AppDestination.CLOCK -> skinFor(ModuleThemeTarget.CLOCK)
            AppDestination.CALCULATOR -> skinFor(ModuleThemeTarget.CALCULATOR)
            AppDestination.COLORS -> colorStudioPreviewSkin
        }
    }

    companion object {
        private val DEFAULT_PRIMARY = ColorHarmonyEngine.fromRgb(245, 196, 81)
        private val DEFAULT_SECONDARY = ColorHarmonyEngine.fromRgb(100, 210, 255)
        private val DEFAULT_STOPS = ColorHarmonyEngine.buildDynamicGradientStops(DEFAULT_PRIMARY, 0)
        private val DEFAULT_SKINS_BY_TARGET: Map<ModuleThemeTarget, ModuleVisualSkin> =
            ModuleThemeTarget.entries.associateWith { target ->
                ModuleVisualSkin(
                    target = target,
                    primarySwatch = DEFAULT_PRIMARY,
                    secondarySwatch = DEFAULT_SECONDARY,
                    gradientStops = DEFAULT_STOPS,
                    gradientStyle = GradientStyleType.MESH,
                    gradientAngleDeg = 135,
                    isCustomApplied = false
                )
            }

        private val DEFAULT_MODULE_SKINS: Map<ModuleThemeTarget, ModuleVisualSkin> = listOf(
            ModuleThemeTarget.CALENDAR,
            ModuleThemeTarget.MOON_OBSERVATORY,
            ModuleThemeTarget.CLOCK,
            ModuleThemeTarget.CALCULATOR
        ).associateWith { DEFAULT_SKINS_BY_TARGET.getValue(it) }

        fun defaultSkinForTarget(target: ModuleThemeTarget): ModuleVisualSkin {
            return DEFAULT_SKINS_BY_TARGET.getValue(target)
        }

        fun defaultModuleSkins(): Map<ModuleThemeTarget, ModuleVisualSkin> {
            return DEFAULT_MODULE_SKINS
        }
    }
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

    fun openColorCustomizerForModule(target: ModuleThemeTarget) {
        _uiState.update { current ->
            val existingSkin = if (target == ModuleThemeTarget.ALL_MODULES) {
                current.skinFor(ModuleThemeTarget.CALENDAR)
            } else {
                current.skinFor(target)
            }
            current.copy(
                currentDestination = AppDestination.COLORS,
                selectedThemeTarget = target,
                activeColorSwatch = existingSkin.primarySwatch
            )
        }
    }

    fun selectThemeTarget(target: ModuleThemeTarget) {
        _uiState.update { it.copy(selectedThemeTarget = target) }
    }

    fun applyActiveColorAndGradientToTarget(target: ModuleThemeTarget = _uiState.value.selectedThemeTarget) {
        _uiState.update { current ->
            val stops = current.activeGradientStops
            val primary = current.activeColorSwatch
            val secondary = stops.getOrElse(2) {
                ColorHarmonyEngine.fromHsl(primary.hsl.h + 150f, primary.hsl.s.coerceAtLeast(60f), 65f)
            }

            fun createSkinFor(t: ModuleThemeTarget) = ModuleVisualSkin(
                target = t,
                primarySwatch = primary,
                secondarySwatch = secondary,
                gradientStops = stops,
                gradientStyle = current.gradientStyle,
                gradientAngleDeg = current.gradientAngleDeg,
                focalX = current.radialFocalX,
                focalY = current.radialFocalY,
                radiusFactor = current.radialRadiusFactor,
                isCustomApplied = true
            )

            val updatedSkins = current.moduleSkins.toMutableMap()
            if (target == ModuleThemeTarget.ALL_MODULES) {
                listOf(
                    ModuleThemeTarget.CALENDAR,
                    ModuleThemeTarget.MOON_OBSERVATORY,
                    ModuleThemeTarget.CLOCK,
                    ModuleThemeTarget.CALCULATOR
                ).forEach { moduleTarget ->
                    updatedSkins[moduleTarget] = createSkinFor(moduleTarget)
                }
            } else {
                updatedSkins[target] = createSkinFor(target)
            }

            current.copy(
                selectedThemeTarget = target,
                moduleSkins = updatedSkins
            )
        }
    }

    fun resetSkinForTarget(target: ModuleThemeTarget = _uiState.value.selectedThemeTarget) {
        _uiState.update { current ->
            val updatedSkins = if (target == ModuleThemeTarget.ALL_MODULES) {
                SeleniaUiState.defaultModuleSkins()
            } else {
                current.moduleSkins + (target to SeleniaUiState.defaultSkinForTarget(target))
            }
            current.copy(moduleSkins = updatedSkins)
        }
    }

    fun selectClockSubTab(subTab: ClockSubTab) {
        _uiState.update { it.copy(clockSubTab = subTab) }
    }

    fun toggle24HourFormat() {
        _uiState.update { it.copy(is24HourFormat = !it.is24HourFormat) }
    }

    fun selectColorStudioSubTab(subTab: ColorStudioSubTab) {
        _uiState.update { it.copy(colorStudioSubTab = subTab) }
    }

    fun updateColorFromRgb(r: Int, g: Int, b: Int) {
        _uiState.update {
            it.copy(
                activeColorSwatch = ColorHarmonyEngine.fromRgb(r, g, b),
                customGradientPresetStops = null
            )
        }
    }

    fun updateColorFromHsl(h: Float, s: Float, l: Float) {
        _uiState.update {
            it.copy(
                activeColorSwatch = ColorHarmonyEngine.fromHsl(h, s, l),
                customGradientPresetStops = null
            )
        }
    }

    fun updateColorFromCmyk(c: Int, m: Int, y: Int, k: Int) {
        _uiState.update {
            it.copy(
                activeColorSwatch = ColorHarmonyEngine.fromCmyk(c, m, y, k),
                customGradientPresetStops = null
            )
        }
    }

    fun updateColorFromHex(hexInput: String) {
        val parsed = ColorHarmonyEngine.parseHexToRgb(hexInput) ?: return
        _uiState.update {
            it.copy(
                activeColorSwatch = ColorHarmonyEngine.fromRgb(parsed.r, parsed.g, parsed.b),
                customGradientPresetStops = null
            )
        }
    }

    fun setGradientStyle(style: GradientStyleType) {
        _uiState.update { it.copy(gradientStyle = style) }
    }

    fun setGradientAngle(angleDeg: Int) {
        _uiState.update { it.copy(gradientAngleDeg = angleDeg.coerceIn(0, 360)) }
    }

    fun setRadialFocalAndRadius(focalX: Float, focalY: Float, radiusFactor: Float) {
        _uiState.update {
            it.copy(
                radialFocalX = focalX.coerceIn(0.1f, 0.9f),
                radialFocalY = focalY.coerceIn(0.1f, 0.9f),
                radialRadiusFactor = radiusFactor.coerceIn(0.3f, 1.6f)
            )
        }
    }

    fun cycleGradientHarmonyVariant() {
        _uiState.update {
            it.copy(
                gradientVariantIndex = (it.gradientVariantIndex + 1) % 5,
                customGradientPresetStops = null
            )
        }
    }

    fun applyGradientPreset(stops: List<ColorSwatchSpec>) {
        if (stops.isEmpty()) return
        _uiState.update {
            it.copy(
                activeColorSwatch = stops.first(),
                customGradientPresetStops = stops
            )
        }
    }

    fun saveFavoriteColor() {
        _uiState.update { current ->
            val active = current.activeColorSwatch
            if (current.savedColorSwatches.any { it.hex == active.hex }) {
                current
            } else {
                current.copy(savedColorSwatches = listOf(active) + current.savedColorSwatches.take(15))
            }
        }
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
