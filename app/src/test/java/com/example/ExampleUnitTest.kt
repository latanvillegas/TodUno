package com.example

import com.example.domain.ColorHarmonyEngine
import com.example.domain.GradientStyleType
import com.example.domain.LunarAlmanacCategory
import com.example.domain.LunarAstronomer
import com.example.domain.LunarTraditionalGoal
import com.example.domain.MoonPhaseType
import com.example.domain.ScientificCalculatorEngine
import com.example.domain.SimpleDate
import com.example.ui.ModuleThemeTarget
import com.example.ui.SeleniaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun colorHarmonyEngine_convertsRgbHexCmykHslAndBuildsGradientsAndMesh() {
        val swatch = ColorHarmonyEngine.fromRgb(245, 196, 81)
        assertEquals("#F5C451", swatch.hex)

        val parsed = ColorHarmonyEngine.parseHexToRgb("#F5C451")
        assertNotNull(parsed)
        assertEquals(245, parsed?.r)
        assertEquals(196, parsed?.g)
        assertEquals(81, parsed?.b)

        val harmonies = ColorHarmonyEngine.buildColorHarmonies(swatch.hsl)
        assertEquals(7, harmonies.size)

        val tones = ColorHarmonyEngine.buildLuminosityScale(swatch.hsl)
        assertEquals(10, tones.size)

        val stops = ColorHarmonyEngine.buildDynamicGradientStops(swatch, 0)
        assertEquals(4, stops.size)

        val meshNodes = ColorHarmonyEngine.buildMeshNodes(stops)
        assertEquals(6, meshNodes.size)

        val cssMesh = ColorHarmonyEngine.formatCssGradient(GradientStyleType.MESH, 135, stops)
        assertTrue(cssMesh.contains("radial-gradient"))
    }

    @Test
    fun viewModel_appliesAndResetsPerModuleVisualSkinIndependently() {
        val vm = SeleniaViewModel()
        vm.updateColorFromHex("#EC4899")
        vm.setGradientStyle(GradientStyleType.RADIAL)
        vm.applyActiveColorAndGradientToTarget(ModuleThemeTarget.MOON_OBSERVATORY)

        val stateAfterMoon = vm.uiState.value
        val moonSkin = stateAfterMoon.skinFor(ModuleThemeTarget.MOON_OBSERVATORY)
        val calendarSkin = stateAfterMoon.skinFor(ModuleThemeTarget.CALENDAR)

        assertTrue(moonSkin.isCustomApplied)
        assertEquals("#EC4899", moonSkin.primarySwatch.hex)
        assertEquals(GradientStyleType.RADIAL, moonSkin.gradientStyle)
        assertFalse(calendarSkin.isCustomApplied)

        vm.resetSkinForTarget(ModuleThemeTarget.MOON_OBSERVATORY)
        assertFalse(vm.uiState.value.skinFor(ModuleThemeTarget.MOON_OBSERVATORY).isCustomApplied)
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
        vm.addNoteForSelectedDate("Nota 1", "Astro")
        vm.addNoteForSelectedDate("Nota 2", "Personal")
        val notes = vm.uiState.value.notesByDate.values.flatten()
        assertTrue(notes.size >= 2)
        assertEquals(notes.size, notes.map { it.id }.distinct().size)
    }
}

