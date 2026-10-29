package com.example

import com.example.domain.LunarAstronomer
import com.example.domain.MoonPhaseType
import com.example.domain.ScientificCalculatorEngine
import com.example.domain.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun referenceNewMoon_isNewMoonPhase() {
        // Jan 6, 2000 is the astronomical reference New Moon
        val info = LunarAstronomer.getLunarInfo(SimpleDate(2000, 1, 6))
        assertEquals(MoonPhaseType.NEW_MOON, info.phaseType)
        assertTrue(info.illuminationPercent < 2.0)
    }

    @Test
    fun scientificCalculator_evaluatesArithmeticAndLunarConstants() {
        val res1 = ScientificCalculatorEngine.evaluate("12 × 29.5306").getOrThrow()
        assertEquals(354.3672, res1, 0.001)

        val res2 = ScientificCalculatorEngine.evaluate("√(144) + 8").getOrThrow()
        assertEquals(20.0, res2, 0.0001)
    }
}
