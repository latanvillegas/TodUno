package com.example.domain

import androidx.compose.runtime.Immutable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
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
    const val MAX_EXPRESSION_LENGTH = 256
    private const val MAX_PARSE_DEPTH = 64

    private val symbols = DecimalFormatSymbols(Locale.US)
    private val standardFormat = DecimalFormat("#,##0.########", symbols).apply {
        isGroupingUsed = false
    }
    private val scientificFormat = DecimalFormat("0.######E0", symbols)

    private val MULTI_CHAR_TOKENS = listOf(
        "Error",
        "sqrt(",
        "asin(",
        "acos(",
        "atan(",
        "sin(",
        "cos(",
        "tan(",
        "log(",
        "ln(",
        "√("
    )

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val absVal = abs(value)
        if (absVal < 1e-12) return "0"
        return if (absVal >= 1e12 || absVal < 1e-6) {
            synchronized(scientificFormat) {
                scientificFormat.format(value)
            }
        } else {
            synchronized(standardFormat) {
                val formatted = standardFormat.format(value)
                if (formatted == "-0") "0" else formatted
            }
        }
    }

    /**
     * Deletes an entire function token (e.g., `asin(`, `sin(`, `log(`, `ln(`, `√(`) or `Error`
     * in one press, falling back to single-character deletion for digits and operators.
     */
    fun smartBackspace(expression: String): String {
        if (expression.isEmpty()) return ""
        for (token in MULTI_CHAR_TOKENS) {
            if (expression.endsWith(token)) {
                return expression.dropLast(token.length)
            }
        }
        return expression.dropLast(1)
    }

    /**
     * Evaluates a mathematical expression string supporting:
     * +, -, ×, ÷, %, !, ^, parentheses, π, e, √, sin, cos, tan, asin, acos, atan, ln, log,
     * scientific E notation, and implicit multiplication.
     * Supports both Degrees ([isRadians] = false) and Radians ([isRadians] = true).
     */
    fun evaluate(rawExpression: String, isRadians: Boolean = false): Result<Double> {
        return runCatching {
            if (rawExpression.length > MAX_EXPRESSION_LENGTH) {
                throw IllegalArgumentException("Expression exceeds maximum safe length")
            }
            val normalized = normalizeExpression(rawExpression)
            if (normalized.isEmpty()) return Result.failure(IllegalArgumentException("Empty"))
            val parser = ExpressionParser(normalized, isRadians)
            val value = parser.parse()
            if (value.isNaN() || value.isInfinite()) {
                throw ArithmeticException("Invalid result")
            }
            value
        }
    }

    private fun normalizeExpression(raw: String): String {
        val compact = buildString(raw.length) {
            for (ch in raw) {
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
                    if (needsImplicitMultiply(compact, i, prev, curr)) {
                        append('*')
                    }
                }
                append(curr)
            }
        }
    }

    private fun needsImplicitMultiply(compact: String, index: Int, prev: Char, curr: Char): Boolean {
        // Preserve scientific notation such as 1.5E6 or 1.5E-7 (uppercase E after digit/dot)
        if (curr == 'E' && (prev.isDigit() || prev == '.')) {
            val next = compact.getOrNull(index + 1)
            if (next != null && (next.isDigit() || next == '+' || next == '-')) {
                return false
            }
        }

        val prevIsValueEnd = prev.isDigit() || prev == ')' || prev == '%' || prev == '!' || prev == 'π' || prev == 'e'
        val currIsValueStart = curr == '(' || curr == '√' || curr == 'π' || curr.isLetter()
        val prevIsConstantOrClose = prev == ')' || prev == '%' || prev == '!' || prev == 'π' || prev == 'e'
        val currIsNumber = curr.isDigit() || curr == '.'

        if (prev.isLetter() && prev != 'π' && curr.isLetter() && curr != 'π') {
            return false
        }
        if (prev.isLetter() && prev != 'π' && prev != 'e' && curr == '(') {
            return false
        }
        return (prevIsValueEnd && currIsValueStart) || (prevIsConstantOrClose && currIsNumber)
    }

    private fun computeFactorial(value: Double): Double {
        if (value.isNaN() || value < -1e-9 || value > 170.0) {
            throw ArithmeticException("Factorial out of domain")
        }
        val rounded = value.roundToLong()
        if (abs(value - rounded.toDouble()) > 1e-9 || rounded < 0L) {
            throw ArithmeticException("Factorial requires non-negative integer")
        }
        var acc = 1.0
        for (i in 2L..rounded) {
            acc *= i.toDouble()
        }
        return acc
    }

    private class ExpressionParser(
        private val input: String,
        private val isRadians: Boolean
    ) {
        private var pos = -1
        private var ch = 0
        private var depth = 0

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
            if (++depth > MAX_PARSE_DEPTH) {
                throw IllegalArgumentException("Expression nesting too deep")
            }
            try {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            } finally {
                depth--
            }
        }

        private fun parseTerm(): Double {
            var x = parseUnary()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseUnary()
                    eat('/'.code) -> {
                        val divisor = parseUnary()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parseUnary(): Double {
            if (++depth > MAX_PARSE_DEPTH) {
                throw IllegalArgumentException("Expression nesting too deep")
            }
            try {
                if (eat('+'.code)) return parseUnary()
                if (eat('-'.code)) return -parseUnary()
                return parsePower()
            } finally {
                depth--
            }
        }

        private fun parsePower(): Double {
            var x = parsePostfix()
            if (eat('^'.code)) {
                x = x.pow(parseUnary())
            }
            return x
        }

        private fun parsePostfix(): Double {
            var x = parsePrimary()
            while (true) {
                when {
                    eat('%'.code) -> x /= 100.0
                    eat('!'.code) -> x = computeFactorial(x)
                    else -> return x
                }
            }
        }

        private fun parsePrimary(): Double {
            val startPos = pos
            return if (eat('('.code)) {
                val inner = parseExpression()
                if (!eat(')'.code) && pos < input.length) {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                inner
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                // Scientific exponent part (e.g., 1.25E-8 or 3E12)
                if (ch == 'E'.code) {
                    nextChar()
                    if (ch == '+'.code || ch == '-'.code) nextChar()
                    if (ch !in '0'.code..'9'.code) {
                        throw IllegalArgumentException("Invalid scientific exponent")
                    }
                    while (ch in '0'.code..'9'.code) nextChar()
                }
                input.substring(startPos, pos).toDouble()
            } else if (ch == 'π'.code) {
                nextChar()
                PI
            } else if (ch == '√'.code) {
                nextChar()
                val arg = parseUnary()
                if (arg < 0.0) throw ArithmeticException("Negative square root")
                sqrt(arg)
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val func = input.substring(startPos, pos).lowercase(Locale.ROOT)
                if (func == "e") {
                    E
                } else {
                    val arg = parseUnary()
                    val degEquiv = if (isRadians) Math.toDegrees(arg) else arg
                    val radEquiv = if (isRadians) arg else Math.toRadians(arg)
                    when (func) {
                        "sqrt" -> {
                            if (arg < 0.0) throw ArithmeticException("Negative square root")
                            sqrt(arg)
                        }
                        "sin" -> {
                            val normDeg = ((degEquiv % 360.0) + 360.0) % 360.0
                            if (abs(normDeg) < 1e-9 || abs(normDeg - 180.0) < 1e-9) {
                                0.0
                            } else {
                                sin(radEquiv)
                            }
                        }
                        "cos" -> {
                            val normDeg = ((degEquiv % 360.0) + 360.0) % 360.0
                            if (abs(normDeg - 90.0) < 1e-9 || abs(normDeg - 270.0) < 1e-9) {
                                0.0
                            } else {
                                cos(radEquiv)
                            }
                        }
                        "tan" -> {
                            val normDeg = ((degEquiv % 180.0) + 180.0) % 180.0
                            if (abs(normDeg - 90.0) < 1e-7) {
                                throw ArithmeticException("Tangent undefined at 90°")
                            }
                            if (abs(normDeg) < 1e-9) 0.0 else tan(radEquiv)
                        }
                        "asin" -> {
                            if (arg < -1.0 || arg > 1.0) {
                                throw ArithmeticException("Arcsine domain [-1, 1]")
                            }
                            val rad = asin(arg)
                            if (isRadians) rad else Math.toDegrees(rad)
                        }
                        "acos" -> {
                            if (arg < -1.0 || arg > 1.0) {
                                throw ArithmeticException("Arccosine domain [-1, 1]")
                            }
                            val rad = acos(arg)
                            if (isRadians) rad else Math.toDegrees(rad)
                        }
                        "atan" -> {
                            val rad = atan(arg)
                            if (isRadians) rad else Math.toDegrees(rad)
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
        }
    }
}
