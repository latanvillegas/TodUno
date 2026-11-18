package com.example

import com.example.domain.LunarAlmanacCategory
import com.example.domain.LunarAstronomer
import com.example.domain.LunarTraditionalGoal
import com.example.domain.MoonPhaseType
import com.example.domain.ScientificCalculatorEngine
import com.example.domain.SimpleDate
import com.example.ui.AppDestination
import com.example.ui.SeleniaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun referenceNewMoon_isNewMoonPhase_andLruCacheReturnsSameInstance() {
        val date = SimpleDate(2000, 1, 6)
        val info1 = LunarAstronomer.getLunarInfo(date)
        val info2 = LunarAstronomer.getLunarInfo(date)
        assertSame(info1, info2)
        assertEquals(MoonPhaseType.NEW_MOON, info1.phaseType)
        assertTrue(info1.illuminationPercent < 2.0)
        // Jan 6, 2000 was a Thursday ("Jueves")
        assertEquals("Jueves", LunarAstronomer.getDayOfWeekSpanish(2000, 1, 6))
    }

    @Test
    fun traditionalLunarAlmanac_providesAllEightCategoriesAndIdealDates() {
        MoonPhaseType.entries.forEach { phase ->
            val guides = phase.almanacGuides()
            assertEquals(8, guides.size)
            assertTrue(guides.any { it.category == LunarAlmanacCategory.BEAUTY_SKIN })
            assertTrue(guides.any { it.category == LunarAlmanacCategory.HAIR_CARE })
            assertTrue(guides.any { it.category == LunarAlmanacCategory.WELLNESS })
        }

        val start = SimpleDate(2026, 10, 29)
        LunarTraditionalGoal.entries.forEach { goal ->
            val upcoming = LunarAstronomer.getUpcomingIdealDatesForGoal(start, goal, count = 4)
            assertTrue(upcoming.isNotEmpty())
            assertTrue(upcoming.all { it.phaseType in goal.idealPhases })
        }
    }

    @Test
    fun scientificCalculator_evaluatesArithmeticAndLunarConstants() {
        val res1 = ScientificCalculatorEngine.evaluate("12 × 29.5306").getOrThrow()
        assertEquals(354.3672, res1, 0.001)

        val res2 = ScientificCalculatorEngine.evaluate("√(144) + 8").getOrThrow()
        assertEquals(20.0, res2, 0.0001)
    }

    @Test
    fun appDestinations_hasFourFocusedModulesWithoutColorStudio() {
        assertEquals(
            listOf(
                AppDestination.CALENDAR,
                AppDestination.MOON_OBSERVATORY,
                AppDestination.CLOCK,
                AppDestination.CALCULATOR
            ),
            AppDestination.entries
        )
    }

    @Test
    fun scientificCalculator_supportsImplicitMultiplicationAndExactDegreeBoundaries() {
        val twoPi = ScientificCalculatorEngine.evaluate("2π").getOrThrow()
        assertEquals(2.0 * Math.PI, twoPi, 1e-6)

        val implicitParens = ScientificCalculatorEngine.evaluate("2(3 + 4)").getOrThrow()
        assertEquals(14.0, implicitParens, 1e-6)

        val doubleParens = ScientificCalculatorEngine.evaluate("(2 + 3)(4 + 5)").getOrThrow()
        assertEquals(45.0, doubleParens, 1e-6)

        val implicitTrig = ScientificCalculatorEngine.evaluate("4sin(30)").getOrThrow()
        assertEquals(2.0, implicitTrig, 1e-6)

        val cos90 = ScientificCalculatorEngine.evaluate("cos(90)").getOrThrow()
        assertEquals(0.0, cos90, 1e-12)

        val sin180 = ScientificCalculatorEngine.evaluate("sin(180)").getOrThrow()
        assertEquals(0.0, sin180, 1e-12)

        val tan90 = ScientificCalculatorEngine.evaluate("tan(90)")
        assertTrue(tan90.isFailure)
    }

    @Test
    fun lunarAstronomer_computesCalendarMonthGridWithoutCalendarAllocations() {
        // February 2024 is a leap year (29 days) starting on Thursday (index 3 from Monday)
        val feb2024 = LunarAstronomer.getMonthGrid(2024, 2)
        assertEquals(42, feb2024.size)
        val currentMonthDays = feb2024.filter { it.isCurrentMonth }
        assertEquals(29, currentMonthDays.size)
        assertEquals(3, feb2024.indexOfFirst { it.isCurrentMonth })
        assertEquals("29 Feb 2024", currentMonthDays.last().date.formatSpanishShort())
    }

    @Test
    fun viewModel_generatesUniqueMonotonicIdsForNotesAndHistory() {
        val vm = SeleniaViewModel()
        vm.addNoteForSelectedDate("Nota 1", "Astro", reminderEnabled = true)
        vm.addNoteForSelectedDate("Nota 2", "Personal", reminderEnabled = false)
        val notes = vm.uiState.value.notesByDate.values.flatten()
        assertTrue(notes.size >= 2)
        assertEquals(notes.size, notes.map { it.id }.distinct().size)
    }

    @Test
    fun scientificCalculator_supportsRadiansModeAndSmartBackspace() {
        val sinHalfPiRad = ScientificCalculatorEngine.evaluate("sin(π / 2)", isRadians = true).getOrThrow()
        assertEquals(1.0, sinHalfPiRad, 1e-9)

        val cosPiRad = ScientificCalculatorEngine.evaluate("cos(π)", isRadians = true).getOrThrow()
        assertEquals(-1.0, cosPiRad, 1e-9)

        assertEquals("45+", ScientificCalculatorEngine.smartBackspace("45+sin("))
        assertEquals("", ScientificCalculatorEngine.smartBackspace("√("))
        assertEquals("12", ScientificCalculatorEngine.smartBackspace("123"))
    }

    @Test
    fun lunarAstronomer_providesMoonriseMoonsetAndUpcomingCelestialEvents() {
        val date = SimpleDate(2026, 3, 3)
        val info = LunarAstronomer.getLunarInfo(date)
        assertTrue(info.moonriseTime.matches(Regex("\\d{2}:\\d{2}")))
        assertTrue(info.moonsetTime.matches(Regex("\\d{2}:\\d{2}")))
        assertTrue(info.meridianTransitTime.matches(Regex("\\d{2}:\\d{2}")))

        val events = LunarAstronomer.getUpcomingCelestialEvents(SimpleDate(2026, 1, 1), count = 5)
        assertEquals(5, events.size)
        for (i in 1 until events.size) {
            val diff = LunarAstronomer.daysBetween(events[i - 1].date, events[i].date)
            assertTrue(diff >= 0)
        }
    }

    @Test
    fun viewModel_supportsDegRadAnsYearMonthJumpAndWorldCities() {
        val vm = SeleniaViewModel()
        assertFalse(vm.uiState.value.isRadiansMode)
        vm.toggleRadiansMode()
        assertTrue(vm.uiState.value.isRadiansMode)

        vm.onCalculatorClear()
        vm.onInsertLastAnswer()
        assertTrue(vm.uiState.value.expression.isNotEmpty())

        vm.jumpToYearMonth(2028, 8)
        assertEquals(2028, vm.uiState.value.displayYear)
        assertEquals(8, vm.uiState.value.displayMonth)

        vm.toggleWorldCity("Europe/London")
        assertTrue("Europe/London" in vm.uiState.value.selectedWorldCityIds)
    }

    @Test
    fun scientificCalculator_handlesPostfixPercentPowerScientificNotationAndDepthGuard() {
        val halfSquared = ScientificCalculatorEngine.evaluate("(50%)^2").getOrThrow()
        assertEquals(0.25, halfSquared, 1e-9)

        val unparenthesizedPercentPower = ScientificCalculatorEngine.evaluate("50%^2").getOrThrow()
        assertEquals(0.25, unparenthesizedPercentPower, 1e-9)

        val tinyFormatted = ScientificCalculatorEngine.formatNumber(1.5e-8)
        assertTrue(tinyFormatted.contains("E", ignoreCase = true))
        val tinyRoundTrip = ScientificCalculatorEngine.evaluate("$tinyFormatted × 2").getOrThrow()
        assertEquals(3.0e-8, tinyRoundTrip, 1e-15)

        val deepNesting = "(".repeat(90) + "1" + ")".repeat(90)
        assertTrue(ScientificCalculatorEngine.evaluate(deepNesting).isFailure)

        val hugeExpr = "1+".repeat(160) + "1"
        assertTrue(ScientificCalculatorEngine.evaluate(hugeExpr).isFailure)
    }

    @Test
    fun simpleDate_fastAsciiStorageKey_formatsZeroPaddedDates() {
        assertEquals("2026-03-03", SimpleDate(2026, 3, 3).storageKey)
        assertEquals("0999-01-05", SimpleDate(999, 1, 5).storageKey)
        assertEquals("2028-12-31", SimpleDate(2028, 12, 31).toStorageKey())
    }

    @Test
    fun viewModel_preservesMonotonicStopwatchAndTimerAcrossNavigationAndDeduplicatesOperators() {
        val vm = SeleniaViewModel()

        // Smart binary operator replacement & Ans prefix
        vm.onCalculatorClear()
        vm.onCalculatorInput("5")
        vm.onCalculatorInput("+")
        vm.onCalculatorInput("×")
        assertEquals("5×", vm.uiState.value.expression)

        // Start Stopwatch and Timer on Clock tab, switch to Calendar & Moon, verify still running
        vm.navigateTo(AppDestination.CLOCK)
        vm.toggleStopwatch()
        vm.selectTimerPresetSeconds(120)
        vm.toggleTimerStartPause()

        assertTrue(vm.uiState.value.stopwatchRunning)
        assertTrue(vm.uiState.value.timerRunning)

        vm.navigateTo(AppDestination.CALENDAR)
        vm.navigateTo(AppDestination.MOON_OBSERVATORY)
        vm.navigateTo(AppDestination.CLOCK)

        assertTrue(vm.uiState.value.stopwatchRunning)
        assertTrue(vm.uiState.value.timerRunning)
        assertTrue(vm.uiState.value.computeTimerRemainingMs() in 1L..120_000L)

        vm.resetStopwatch()
        vm.resetTimer()
        assertFalse(vm.uiState.value.stopwatchRunning)
        assertFalse(vm.uiState.value.timerRunning)
    }

    @Test
    fun antiDuplication_preventsRepeatedEventsNotesCalculationsAndWorldClocks() {
        // 1. Celestial events never repeat the same date even when Eclipse + Supermoon coincide
        val events = LunarAstronomer.getUpcomingCelestialEvents(SimpleDate(2026, 2, 1), count = 6)
        val uniqueDates = events.map { it.date.storageKey }.distinct()
        assertEquals(events.size, uniqueDates.size)

        // 2. Default world cities exclude UTC and local timezone, with zero duplicates
        val defaults = com.example.data.SeleniaPersistedSettings.DEFAULT_WORLD_CITY_IDS
        assertFalse("UTC" in defaults)
        assertEquals(defaults.size, defaults.distinct().size)

        val vm = SeleniaViewModel()

        // 3. Duplicate notes on the same date (case-insensitive) are ignored
        val selectedKey = vm.uiState.value.selectedDate.toStorageKey()
        val beforeCount = vm.uiState.value.notesByDate[selectedKey].orEmpty().size
        vm.addNoteForSelectedDate("Observación Lunar", "Astronomía")
        vm.addNoteForSelectedDate("observación lunar", "astronomía")
        val afterCount = vm.uiState.value.notesByDate[selectedKey].orEmpty().size
        assertEquals(beforeCount + 1, afterCount)

        // 4. Repeated '=' or identical consecutive calculations do not duplicate history
        vm.clearHistory()
        vm.onCalculatorClear()
        vm.onCalculatorInput("6×7")
        vm.onCalculatorEvaluate()
        assertEquals(1, vm.uiState.value.calculationHistory.size)
        // Pressing '=' again on resolved "42" should not add "42 = 42"
        vm.onCalculatorEvaluate()
        assertEquals(1, vm.uiState.value.calculationHistory.size)
        // Re-evaluating the exact same "6×7" immediately should not duplicate top entry
        vm.onCalculatorClear()
        vm.onCalculatorInput("6×7")
        vm.onCalculatorEvaluate()
        assertEquals(1, vm.uiState.value.calculationHistory.size)

        // 5. Interval comparison start date is independent from base selectedDate
        val customStart = SimpleDate(2027, 5, 10)
        val originalBase = vm.uiState.value.selectedDate
        vm.setComparisonStartDate(customStart)
        assertEquals(customStart, vm.uiState.value.comparisonStartDate)
        assertEquals(originalBase, vm.uiState.value.selectedDate)
    }
}
