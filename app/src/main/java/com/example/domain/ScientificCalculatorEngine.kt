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
     * +, -, ×, ÷, %, ^, parentheses, π, e, √, sin, cos, tan, ln, log, and implicit multiplication.
     * Trigonometric functions accept degrees for intuitive everyday scientific calculator usage.
     */
    fun evaluate(rawExpression: String): Result<Double> {
        return runCatching {
            val normalized = normalizeExpression(rawExpression)
            if (normalized.isEmpty()) return Result.failure(IllegalArgumentException("Empty"))
            val parser = ExpressionParser(normalized)
            val value = parser.parse()
            if (value.isNaN() || value.isInfinite()) {
                throw ArithmeticException("Invalid result")
            }
            value
        }
    }

    /**
     * Normalizes operators, symbols, and inserts explicit '*' where implicit multiplication is written
     * (e.g., "2π" -> "2*π", "2(3+4)" -> "2*(3+4)", "(2)(3)" -> "(2)*(3)", "3sin(30)" -> "3*sin(30)").
     */
    private fun normalizeExpression(raw: String): String {
        val preReplaced = if (raw.contains("🌙")) raw.replace("🌙", "L") else raw
        val compact = buildString(preReplaced.length) {
            for (ch in preReplaced) {
                when (ch) {
                    ' ' -> Unit
                    '×', '⋅' -> append('*')
                    '÷' -> append('/')
                    '−', '–' -> append('-')
                    else -> append(ch)
                }
            }
        }
        if (compact.isEmpty()) return ""

        return buildString(compact.length + 8) {
            for (i in compact.indices) {
                val curr = compact[i]
                if (i > 0) {
                    val prev = compact[i - 1]
                    if (needsImplicitMultiply(prev, curr)) {
                        append('*')
                    }
                }
                append(curr)
            }
        }
    }

    private fun needsImplicitMultiply(prev: Char, curr: Char): Boolean {
        val prevIsValueEnd = prev.isDigit() || prev == ')' || prev == '%' || prev == 'π' || prev == 'e' || prev == 'L'
        val currIsValueStart = curr == '(' || curr == '√' || curr == 'π' || curr == 'L' || curr.isLetter()
        val prevIsConstantOrClose = prev == ')' || prev == '%' || prev == 'π' || prev == 'e' || prev == 'L'
        val currIsNumber = curr.isDigit() || curr == '.'

        // Avoid splitting function names like "sin", "cos", "tan", "log", "ln", "sqrt"
        if (prev.isLetter() && prev != 'π' && prev != 'L' && curr.isLetter() && curr != 'π' && curr != 'L') {
            return false
        }
        if (prev.isLetter() && prev != 'π' && prev != 'e' && prev != 'L' && curr == '(') {
            return false
        }
        return (prevIsValueEnd && currIsValueStart) || (prevIsConstantOrClose && currIsNumber)
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
            } else if (ch == 'π'.code) {
                nextChar()
                x = PI
            } else if (ch == 'L'.code) {
                nextChar()
                x = LunarAstronomer.SYNODIC_MONTH_DAYS
            } else if (ch == '√'.code) {
                nextChar()
                val arg = parseFactor()
                if (arg < 0.0) throw ArithmeticException("Negative square root")
                x = sqrt(arg)
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val func = input.substring(startPos, pos).lowercase(Locale.ROOT)
                x = if (func == "e") {
                    E
                } else {
                    val arg = parseFactor()
                    when (func) {
                        "sqrt" -> {
                            if (arg < 0.0) throw ArithmeticException("Negative square root")
                            sqrt(arg)
                        }
                        "sin" -> {
                            val normDeg = ((arg % 360.0) + 360.0) % 360.0
                            if (normDeg == 0.0 || normDeg == 180.0) 0.0 else sin(Math.toRadians(arg))
                        }
                        "cos" -> {
                            val normDeg = ((arg % 360.0) + 360.0) % 360.0
                            if (normDeg == 90.0 || normDeg == 270.0) 0.0 else cos(Math.toRadians(arg))
                        }
                        "tan" -> {
                            val normDeg = ((arg % 180.0) + 180.0) % 180.0
                            if (kotlin.math.abs(normDeg - 90.0) < 1e-9) {
                                throw ArithmeticException("Tangent undefined at 90°")
                            }
                            if (normDeg == 0.0) 0.0 else tan(Math.toRadians(arg))
                        }
                        "ln" -> {
                            if (arg <= 0.0) throw ArithmeticException("Non-positive logarithm")
                            ln(arg)
                        }
                        "log" -> {
                            if (arg <= 0.0) throw ArithmeticException("Non-positive logarithm")
                            log10(arg)
                        }
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
