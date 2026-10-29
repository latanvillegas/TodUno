package com.example.domain

import androidx.compose.runtime.Immutable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

@Immutable
data class CalculationRecord(
    val id: Long,
    val expression: String,
    val result: String,
    val badgeLabel: String? = null
)

object ScientificCalculatorEngine {
    private val symbols = DecimalFormatSymbols(Locale.US)
    private val decimalFormat = DecimalFormat("#,##0.########", symbols).apply {
        isGroupingUsed = false
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val rounded = if (kotlin.math.abs(value) < 1e-12) 0.0 else value
        synchronized(decimalFormat) {
            return decimalFormat.format(rounded)
        }
    }

    /**
     * Evaluates a mathematical expression string supporting:
     * +, -, ×, ÷, %, ^, parentheses, π, e, √, sin, cos, tan, ln, log
     * Trigonometric functions accept degrees for intuitive everyday scientific calculator usage.
     */
    fun evaluate(rawExpression: String): Result<Double> {
        return runCatching {
            val cleaned = rawExpression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", PI.toString())
                .replace("🌙", LunarAstronomer.SYNODIC_MONTH_DAYS.toString())
                .replace(" ", "")
                .trim()

            if (cleaned.isEmpty()) return Result.failure(IllegalArgumentException("Empty"))
            val parser = ExpressionParser(cleaned)
            val value = parser.parse()
            if (value.isNaN() || value.isInfinite()) {
                throw ArithmeticException("Invalid result")
            }
            value
        }
    }

    private class ExpressionParser(private val input: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) throw IllegalArgumentException("Unexpected char: ${ch.toChar()}")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code) // Auto-close trailing parenthesis if omitted
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = input.substring(startPos, pos).toDouble()
            } else if (ch == '√'.code) {
                nextChar()
                x = sqrt(parseFactor())
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val func = input.substring(startPos, pos).lowercase(Locale.ROOT)
                x = if (func == "e") {
                    E
                } else {
                    val arg = parseFactor()
                    when (func) {
                        "sqrt" -> sqrt(arg)
                        "sin" -> sin(Math.toRadians(arg))
                        "cos" -> cos(Math.toRadians(arg))
                        "tan" -> tan(Math.toRadians(arg))
                        "ln" -> ln(arg)
                        "log" -> log10(arg)
                        else -> throw IllegalArgumentException("Unknown function: $func")
                    }
                }
            } else {
                throw IllegalArgumentException("Unexpected character")
            }

            if (eat('^'.code)) {
                x = x.pow(parseFactor())
            }
            while (eat('%'.code)) {
                x /= 100.0
            }

            return x
        }
    }
}
