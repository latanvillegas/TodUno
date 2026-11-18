package com.example

import com.example.domain.CelestialEventKind
import com.example.domain.LunarAstronomer
import com.example.domain.LunarEclipseKind
import com.example.domain.MoonPhaseType
import com.example.domain.ScientificCalculatorEngine
import com.example.domain.SimpleDate
import com.example.domain.SolarEclipseKind
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
        assertTrue(info1.isExactMajorPhaseDay)
        assertTrue(info1.illuminationPercent < 1.0)
        // Jan 6, 2000 was a Thursday ("Jueves")
        assertEquals("Jueves", LunarAstronomer.getDayOfWeekSpanish(2000, 1, 6))
    }

    @Test
    fun meeusEphemeris_computesRealJ2000EclipticLongitudeAndAngularDiameter() {
        // At J2000.0 (Jan 1, 2000 12:00 UTC), real JPL Horizons lunar longitude is ~223.3° (Escorpio)
        val j2000 = LunarAstronomer.getLunarInfo(SimpleDate(2000, 1, 1))
        assertEquals("Escorpio ♏", j2000.zodiacSign)
        assertEquals(223.3, j2000.eclipticLongitudeDeg, 0.5)
        assertTrue(j2000.angularDiameterArcmin in 29.0..34.0)
        assertTrue(j2000.eclipticLatitudeDeg in -5.5..5.5)
    }

    @Test
    fun dynamicMeeusEclipseEngine_detectsRealSolarAndLunarEclipsesAcrossDecades() {
        // 1. April 8, 2024: Great North American Total Solar Eclipse (pre-2026)
        val apr2024 = LunarAstronomer.getLunarInfo(SimpleDate(2024, 4, 8))
        assertEquals(SolarEclipseKind.TOTAL, apr2024.solarEclipseKind)

        // 2. March 3, 2026: Total Lunar Eclipse
        val mar2026 = LunarAstronomer.getLunarInfo(SimpleDate(2026, 3, 3))
        assertEquals(LunarEclipseKind.TOTAL, mar2026.lunarEclipseKind)

        // 3. August 12, 2026: Total Solar Eclipse
        val aug12_2026 = LunarAstronomer.getLunarInfo(SimpleDate(2026, 8, 12))
        assertEquals(SolarEclipseKind.TOTAL, aug12_2026.solarEclipseKind)

        // 4. August 28, 2026: Deep Partial Lunar Eclipse
        val aug28_2026 = LunarAstronomer.getLunarInfo(SimpleDate(2026, 8, 28))
        assertEquals(LunarEclipseKind.PARTIAL, aug28_2026.lunarEclipseKind)

        // 5. October 8, 2033: Total Lunar Eclipse (post-2029, proving zero hardcoded year cap)
        val oct2033 = LunarAstronomer.getLunarInfo(SimpleDate(2033, 10, 8))
        assertEquals(LunarEclipseKind.TOTAL, oct2033.lunarEclipseKind)

        val upcomingFrom2033 = LunarAstronomer.getUpcomingCelestialEvents(SimpleDate(2033, 9, 1), count = 4)
        assertTrue(upcomingFrom2033.any {
            it.date == SimpleDate(2033, 10, 8) && it.kind == CelestialEventKind.LUNAR_ECLIPSE
        })
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
    fun lunarAstronomer_ordersUpcomingPhasesAndCelestialEventsChronologically() {
        val phases = LunarAstronomer.getUpcomingMajorPhases(SimpleDate(2026, 1, 1), count = 4)
        assertEquals(4, phases.size)
        for (i in 1 until phases.size) {
            assertTrue(LunarAstronomer.daysBetween(phases[i - 1].date, phases[i].date) > 0)
        }

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

        assertTrue(vm.uiState.value.selectedWorldCityIds.isEmpty())
        vm.toggleWorldCity("Europe/London")
        assertTrue("Europe/London" in vm.uiState.value.selectedWorldCityIds)
        vm.toggleWorldCity("Europe/London")
        assertTrue(vm.uiState.value.selectedWorldCityIds.isEmpty())
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

        // 2. Fresh ViewModel starts with ZERO pre-seeded notes, ZERO pre-seeded history, and ZERO pre-populated cities
        val defaults = com.example.data.SeleniaPersistedSettings.DEFAULT_WORLD_CITY_IDS
        assertTrue(defaults.isEmpty())

        val vm = SeleniaViewModel()
        assertTrue(vm.uiState.value.notesByDate.isEmpty())
        assertTrue(vm.uiState.value.calculationHistory.isEmpty())
        assertEquals("0", vm.uiState.value.lastAnswer)
        assertTrue(vm.uiState.value.selectedWorldCityIds.isEmpty())
        vm.toggleWorldCity("America/Lima")
        vm.toggleWorldCity("America/Lima")
        assertTrue(vm.uiState.value.selectedWorldCityIds.isEmpty())

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

        // 5. Watch dial preset & stopwatch lap serialization round-trip cleanly
        vm.selectWatchDialPreset("EMERALD_BOREAL")
        assertEquals("EMERALD_BOREAL", vm.uiState.value.watchDialPresetName)

        val sampleLaps = listOf(
            com.example.ui.StopwatchLap(lapNumber = 2, lapDurationMs = 1450L, totalElapsedMs = 3200L),
            com.example.ui.StopwatchLap(lapNumber = 1, lapDurationMs = 1750L, totalElapsedMs = 1750L)
        )
        val encoded = com.example.data.SeleniaRepository.encodeStopwatchLaps(sampleLaps)
        val decoded = com.example.data.SeleniaRepository.decodeStopwatchLaps(encoded)
        assertEquals(sampleLaps, decoded)
    }

    @Test
    fun scientificCalculator_supportsInverseTrigLogEulerAndFactorial() {
        // Degree mode inverse trig
        assertEquals(30.0, ScientificCalculatorEngine.evaluate("asin(0.5)", isRadians = false).getOrThrow(), 1e-6)
        assertEquals(60.0, ScientificCalculatorEngine.evaluate("acos(0.5)", isRadians = false).getOrThrow(), 1e-6)
        assertEquals(45.0, ScientificCalculatorEngine.evaluate("atan(1)", isRadians = false).getOrThrow(), 1e-6)

        // Radian mode inverse trig
        assertEquals(Math.PI / 6.0, ScientificCalculatorEngine.evaluate("asin(0.5)", isRadians = true).getOrThrow(), 1e-9)
        assertTrue(ScientificCalculatorEngine.evaluate("asin(1.5)").isFailure)

        // Base-10 log, Euler's e, and postfix factorial
        assertEquals(3.0, ScientificCalculatorEngine.evaluate("log(1000)").getOrThrow(), 1e-9)
        assertEquals(1.0, ScientificCalculatorEngine.evaluate("ln(e)").getOrThrow(), 1e-9)
        assertEquals(120.0, ScientificCalculatorEngine.evaluate("5!").getOrThrow(), 1e-9)
        assertEquals(1.0, ScientificCalculatorEngine.evaluate("0!").getOrThrow(), 1e-9)
        assertEquals(240.0, ScientificCalculatorEngine.evaluate("2(5!)").getOrThrow(), 1e-9)
        assertTrue(ScientificCalculatorEngine.evaluate("(-3)!").isFailure)

        // Smart backspace removes inverse trig tokens in one press
        assertEquals("2+", ScientificCalculatorEngine.smartBackspace("2+asin("))
        assertEquals("", ScientificCalculatorEngine.smartBackspace("atan("))
    }

    @Test
    fun viewModel_supportsNoteEditingCustomReminderTime24hTimerAndTimezoneSync() {
        val vm = SeleniaViewModel()
        val targetDate = vm.uiState.value.selectedDate
        vm.addNoteForSelectedDate(
            title = "Conjunción Lunar",
            category = "Astronomía",
            reminderEnabled = true,
            reminderHour = 21,
            reminderMinute = 30
        )
        val created = vm.uiState.value.notesByDate[targetDate.toStorageKey()].orEmpty().single()
        assertEquals("21:30", created.formattedReminderTime())

        vm.updateNote(
            date = targetDate,
            noteId = created.id,
            title = "Conjunción Lunar y Júpiter",
            category = "Astronomía",
            reminderEnabled = true,
            reminderHour = 22,
            reminderMinute = 15
        )
        val edited = vm.uiState.value.notesByDate[targetDate.toStorageKey()].orEmpty().single()
        assertEquals("Conjunción Lunar y Júpiter", edited.title)
        assertEquals("22:15", edited.formattedReminderTime())

        // Custom multi-hour timer (2h 15m 30s = 8130s)
        vm.setCustomTimerDuration(hours = 2, minutes = 15, seconds = 30)
        assertEquals(8_130, vm.uiState.value.timerTotalSeconds)

        // Inverse trig mode toggle & live timezone revision increment
        assertFalse(vm.uiState.value.isInverseTrigMode)
        vm.toggleInverseTrigMode()
        assertTrue(vm.uiState.value.isInverseTrigMode)

        val prevTzRev = vm.uiState.value.timeZoneRevision
        vm.onSystemTimeOrZoneChanged()
        assertEquals(prevTzRev + 1, vm.uiState.value.timeZoneRevision)
    }
}

