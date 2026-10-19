package com.example.service

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class CalcEvalOutcome(
    val isValid: Boolean,
    val numericValue: Double = 0.0,
    val formattedResult: String = "0",
    val errorMessage: String? = null
)

data class PercentageCalcOutcome(
    val mainResult: Double,
    val secondaryValue: Double = 0.0,
    val summaryLabel: String,
    val formulaExplanation: String
)

data class LoanCalcOutcome(
    val monthlyPayment: Double,
    val totalPayment: Double,
    val totalInterest: Double,
    val principalAmount: Double,
    val isValid: Boolean = true,
    val errorMessage: String? = null
)

data class CompoundInterestOutcome(
    val finalBalance: Double,
    val totalContributions: Double,
    val totalInterestEarned: Double,
    val growthMultiple: Double,
    val isValid: Boolean = true,
    val errorMessage: String? = null
)

data class DiscountTaxOutcome(
    val discountAmount: Double,
    val priceAfterDiscount: Double,
    val taxAmount: Double,
    val finalPayablePrice: Double,
    val totalSavings: Double
)

data class ProfitMarginOutcome(
    val netProfit: Double,
    val grossMarginPercent: Double,
    val markupPercent: Double,
    val isProfit: Boolean
)

data class DateDiffOutcome(
    val totalDays: Long,
    val years: Int,
    val months: Int,
    val days: Int,
    val totalWeeks: Long,
    val remainingWeekDays: Long,
    val totalHours: Long,
    val totalMinutes: Long,
    val isValid: Boolean = true,
    val errorMessage: String? = null
)

data class StatisticsOutcome(
    val count: Int,
    val sum: Double,
    val mean: Double,
    val median: Double,
    val modes: List<Double>,
    val min: Double,
    val max: Double,
    val range: Double,
    val variancePop: Double,
    val varianceSample: Double,
    val stdDevPop: Double,
    val stdDevSample: Double,
    val sortedValues: List<Double>,
    val isValid: Boolean = true,
    val errorMessage: String? = null
)

data class ProgrammerOutcome(
    val decString: String,
    val binString: String,
    val octString: String,
    val hexString: String,
    val rawValue: Long,
    val bitWidth: Int,
    val isValid: Boolean = true,
    val errorMessage: String? = null
)

enum class ConversionCategory(val id: String, val labelId: String, val labelEn: String) {
    LENGTH("LENGTH", "Panjang", "Length"),
    WEIGHT("WEIGHT", "Berat / Massa", "Weight / Mass"),
    TEMPERATURE("TEMPERATURE", "Suhu", "Temperature"),
    TIME("TIME", "Waktu", "Time"),
    DATA("DATA", "Data Digital", "Digital Data"),
    AREA("AREA", "Luas", "Area"),
    VOLUME("VOLUME", "Volume", "Volume"),
    SPEED("SPEED", "Kecepatan", "Speed")
}

data class ConversionUnitSpec(
    val code: String,
    val nameId: String,
    val nameEn: String,
    val toBaseFactor: Double = 1.0
)

object SmartCalculatorEngine {

    private val idDecimalSymbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val usDecimalSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    fun formatNumber(value: Double, useIndonesianFormat: Boolean = false, maxDecimals: Int = 8): String {
        if (value.isNaN()) return "Tidak Terdefinisi (NaN)"
        if (value.isInfinite()) return if (value > 0) "∞ (Tak Hingga)" else "-∞ (Tak Hingga)"
        val cleanVal = if (abs(value) < 1e-12) 0.0 else value
        val absVal = abs(cleanVal)

        if (absVal > 0.0 && (absVal >= 1e12 || absVal < 1e-6)) {
            return String.format(Locale.US, "%.6E", cleanVal)
        }

        val bd = BigDecimal.valueOf(cleanVal)
            .setScale(maxDecimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()

        val pattern = buildString {
            append("#,##0")
            if (maxDecimals > 0) {
                append(".")
                repeat(maxDecimals) { append("#") }
            }
        }
        val df = DecimalFormat(pattern, if (useIndonesianFormat) idDecimalSymbols else usDecimalSymbols)
        return df.format(bd)
    }

    fun formatCurrencyIdr(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Rp 0"
        val rounded = BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP)
        val df = DecimalFormat("#,##0", idDecimalSymbols)
        return "Rp ${df.format(rounded)}"
    }

    /**
     * Parses user number input supporting both Indonesian format (e.g. "10.000.000", "1.250.000,50", "12,5")
     * and international format (e.g. "10,000,000.50", "12.5").
     */
    fun parseSmartNumber(raw: String): Double? {
        val s = raw.trim().replace(" ", "").replace("Rp", "").replace("$", "")
        if (s.isEmpty()) return null
        val dotCount = s.count { it == '.' }
        val commaCount = s.count { it == ',' }

        val normalized = when {
            dotCount > 0 && commaCount > 0 -> {
                val lastDot = s.lastIndexOf('.')
                val lastComma = s.lastIndexOf(',')
                if (lastComma > lastDot) {
                    // Indonesian / European: 1.250.000,75
                    s.replace(".", "").replace(",", ".")
                } else {
                    // US / International: 1,250,000.75
                    s.replace(",", "")
                }
            }
            dotCount > 1 -> {
                // Multiple dots -> thousand separators (e.g. 10.000.000)
                s.replace(".", "")
            }
            commaCount > 1 -> {
                // Multiple commas -> thousand separators (e.g. 10,000,000)
                s.replace(",", "")
            }
            commaCount == 1 -> {
                // Single comma -> decimal separator in ID (e.g. 12,5 or 0,75)
                s.replace(",", ".")
            }
            else -> s
        }
        return normalized.toDoubleOrNull()
    }

    /**
     * Evaluates a mathematical expression supporting Basic & Scientific operators:
     * +, -, *, ×, /, ÷, %, ^, parentheses (), unary +/-, postfix factorial !,
     * constants pi (π) and e, and functions:
     * sin, cos, tan, asin, acos, atan, ln, log, sqrt (√), cbrt (∛), abs, inv.
     */
    fun evaluateExpression(
        rawExpr: String,
        isDegreeMode: Boolean = true,
        memoryValue: Double = 0.0
    ): CalcEvalOutcome {
        val trimmed = rawExpr.trim()
        if (trimmed.isEmpty()) {
            return CalcEvalOutcome(isValid = true, numericValue = 0.0, formattedResult = "0")
        }

        val normalized = normalizeExpression(trimmed, memoryValue)
        return try {
            val tokens = tokenize(normalized)
            if (tokens.isEmpty()) {
                return CalcEvalOutcome(isValid = true, numericValue = 0.0, formattedResult = "0")
            }
            val parser = RecursiveParser(tokens, isDegreeMode)
            val result = parser.parseExpression()
            if (parser.hasRemainingTokens()) {
                return CalcEvalOutcome(
                    isValid = false,
                    errorMessage = "Format ekspresi tidak valid"
                )
            }
            if (result.isNaN()) {
                return CalcEvalOutcome(
                    isValid = false,
                    errorMessage = "Hasil tidak terdefinisi (NaN)"
                )
            }
            if (result.isInfinite()) {
                return CalcEvalOutcome(
                    isValid = false,
                    errorMessage = "Tidak dapat dibagi dengan nol (Overflow/∞)"
                )
            }
            val clean = if (abs(result) < 1e-12) 0.0 else result
            CalcEvalOutcome(
                isValid = true,
                numericValue = clean,
                formattedResult = formatNumber(clean, useIndonesianFormat = false, maxDecimals = 8)
            )
        } catch (e: CalcDomainException) {
            CalcEvalOutcome(isValid = false, errorMessage = e.message ?: "Domain matematika tidak valid")
        } catch (_: Exception) {
            CalcEvalOutcome(isValid = false, errorMessage = "Ekspresi belum lengkap atau tidak valid")
        }
    }

    private class CalcDomainException(message: String) : Exception(message)

    private fun normalizeExpression(expr: String, memoryValue: Double): String {
        var s = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("–", "-")
            .replace("π", "pi")
            .replace("√(", "sqrt(")
            .replace("∛(", "cbrt(")
            .replace("√", "sqrt")
            .replace("∛", "cbrt")
            .replace("MEM", memoryValue.toString())
            .replace(Regex("(?i)\\bmod\\b"), "§")
            .replace(" ", "")

        // Strip trailing incomplete binary operators for graceful evaluation
        while (s.isNotEmpty() && s.last() in listOf('+', '-', '*', '/', '^', '§', '.')) {
            if (s.length == 1) break
            s = s.dropLast(1)
        }

        // Auto-close unclosed parentheses for seamless live evaluation
        val openCount = s.count { it == '(' }
        val closeCount = s.count { it == ')' }
        if (openCount > closeCount) {
            s += ")".repeat(openCount - closeCount)
        }

        return s
    }

    private sealed class Token {
        data class NumberToken(val value: Double) : Token()
        data class OpToken(val op: Char) : Token()
        data class FuncToken(val name: String) : Token()
        data object LParen : Token()
        data object RParen : Token()
    }

    private fun tokenize(s: String): List<Token> {
        val list = mutableListOf<Token>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c == '(' -> {
                    list.add(Token.LParen)
                    i++
                }
                c == ')' -> {
                    list.add(Token.RParen)
                    i++
                }
                c in listOf('+', '-', '*', '/', '^', '%', '!', '§') -> {
                    list.add(Token.OpToken(c))
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val start = i
                    var hasDot = (c == '.')
                    i++
                    while (i < s.length) {
                        val ch = s[i]
                        if (ch.isDigit()) {
                            i++
                        } else if (ch == '.' && !hasDot) {
                            hasDot = true
                            i++
                        } else if ((ch == 'E' || ch == 'e') && i + 1 < s.length &&
                            (s[i + 1].isDigit() || ((s[i + 1] == '+' || s[i + 1] == '-') && i + 2 < s.length && s[i + 2].isDigit()))
                        ) {
                            i++ // consume E
                            if (s[i] == '+' || s[i] == '-') i++
                        } else {
                            break
                        }
                    }
                    val numStr = s.substring(start, i)
                    val parsed = numStr.toDoubleOrNull()
                        ?: throw CalcDomainException("Format angka tidak valid: $numStr")
                    list.add(Token.NumberToken(parsed))
                }
                c.isLetter() -> {
                    val start = i
                    while (i < s.length && (s[i].isLetter() || (s[i] == '2' && s.substring(start, i).equals("log", ignoreCase = true)))) {
                        i++
                    }
                    val ident = s.substring(start, i).lowercase(Locale.US)
                    when (ident) {
                        "pi" -> list.add(Token.NumberToken(PI))
                        "e" -> list.add(Token.NumberToken(Math.E))
                        "phi" -> list.add(Token.NumberToken(1.618033988749895))
                        "mod" -> list.add(Token.OpToken('§'))
                        else -> list.add(Token.FuncToken(ident))
                    }
                }
                else -> throw CalcDomainException("Simbol tidak dikenali: $c")
            }
        }
        return insertImplicitMultiplication(list)
    }

    private fun insertImplicitMultiplication(rawTokens: List<Token>): List<Token> {
        if (rawTokens.size < 2) return rawTokens
        val out = mutableListOf<Token>()
        for (idx in rawTokens.indices) {
            val curr = rawTokens[idx]
            out.add(curr)
            if (idx + 1 < rawTokens.size) {
                val next = rawTokens[idx + 1]
                val currCanImplicitMultiply = curr is Token.NumberToken ||
                    curr is Token.RParen ||
                    (curr is Token.OpToken && (curr.op == '!' || curr.op == '%'))
                val nextCanImplicitMultiply = next is Token.NumberToken ||
                    next is Token.LParen ||
                    next is Token.FuncToken
                if (currCanImplicitMultiply && nextCanImplicitMultiply) {
                    out.add(Token.OpToken('*'))
                }
            }
        }
        return out
    }

    private class RecursiveParser(
        private val tokens: List<Token>,
        private val isDegreeMode: Boolean
    ) {
        private var pos = 0

        fun hasRemainingTokens(): Boolean = pos < tokens.size

        private fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null
        private fun consume(): Token = tokens[pos++]

        // Grammar:
        // Expr -> Term (( '+' | '-' ) Term)*
        // Special handling: A + B% => A + (A * B / 100), A - B% => A - (A * B / 100)
        fun parseExpression(): Double {
            var left = parseTerm()
            while (true) {
                val t = peek()
                if (t is Token.OpToken && (t.op == '+' || t.op == '-')) {
                    consume()
                    val savedPos = pos
                    val right = parseTerm()
                    // Check if the right term ended with a direct percentage token (e.g. 200 + 10% -> 220)
                    val endedWithPercent = pos > savedPos &&
                        (tokens.getOrNull(pos - 1) == Token.OpToken('%')) &&
                        !subSliceContainsMulDiv(savedPos, pos - 1)
                    left = if (endedWithPercent) {
                        // In parsePostfix, % already divided `right` by 100, so `right` is B/100.
                        // Therefore A + B% = left + (left * right), and A - B% = left - (left * right).
                        if (t.op == '+') left + (left * right) else left - (left * right)
                    } else {
                        if (t.op == '+') left + right else left - right
                    }
                } else {
                    break
                }
            }
            return left
        }

        private fun subSliceContainsMulDiv(start: Int, endExclusive: Int): Boolean {
            for (idx in start until endExclusive) {
                val tk = tokens[idx]
                if (tk is Token.OpToken && (tk.op == '*' || tk.op == '/' || tk.op == '§')) return true
            }
            return false
        }

        // Term -> Power (( '*' | '/' | '§' ) Power)*
        private fun parseTerm(): Double {
            var left = parseUnary()
            while (true) {
                val t = peek()
                if (t is Token.OpToken && (t.op == '*' || t.op == '/' || t.op == '§')) {
                    consume()
                    val right = parseUnary()
                    when (t.op) {
                        '*' -> left *= right
                        '/' -> {
                            if (abs(right) < 1e-15) {
                                throw CalcDomainException("Tidak dapat dibagi dengan nol (÷ 0)")
                            }
                            left /= right
                        }
                        '§' -> {
                            if (abs(right) < 1e-15) {
                                throw CalcDomainException("Modulo tidak dapat dibagi dengan nol (mod 0)")
                            }
                            left %= right
                        }
                    }
                } else {
                    break
                }
            }
            return left
        }

        // Unary -> ('+' | '-') Unary | Power
        private fun parseUnary(): Double {
            val t = peek()
            if (t is Token.OpToken && (t.op == '+' || t.op == '-')) {
                consume()
                val v = parseUnary()
                return if (t.op == '-') -v else v
            }
            return parsePower()
        }

        // Power -> Postfix ('^' Unary)? (Right-associative)
        private fun parsePower(): Double {
            val base = parsePostfix()
            val t = peek()
            if (t is Token.OpToken && t.op == '^') {
                consume()
                val exp = parseUnary()
                val res = base.pow(exp)
                if (res.isNaN()) {
                    throw CalcDomainException("Pangkat menghasilkan bilangan imajiner")
                }
                return res
            }
            return base
        }

        // Postfix -> Primary ( '!' | '%' )*
        private fun parsePostfix(): Double {
            var v = parsePrimary()
            while (true) {
                val t = peek()
                if (t is Token.OpToken && t.op == '!') {
                    consume()
                    v = factorial(v)
                } else if (t is Token.OpToken && t.op == '%') {
                    consume()
                    v /= 100.0
                } else {
                    break
                }
            }
            return v
        }

        private fun parsePrimary(): Double {
            val t = peek() ?: throw CalcDomainException("Ekspresi belum selesai")
            return when (t) {
                is Token.NumberToken -> {
                    consume()
                    t.value
                }
                is Token.LParen -> {
                    consume()
                    val inside = parseExpression()
                    if (peek() is Token.RParen) {
                        consume()
                    }
                    inside
                }
                is Token.FuncToken -> {
                    consume()
                    val arg = if (peek() is Token.LParen) {
                        consume()
                        val inside = parseExpression()
                        if (peek() is Token.RParen) consume()
                        inside
                    } else {
                        parsePrimary()
                    }
                    applyFunction(t.name, arg)
                }
                else -> throw CalcDomainException("Urutan operator tidak valid")
            }
        }

        private fun applyFunction(name: String, arg: Double): Double {
            return when (name) {
                "sin" -> {
                    val rad = if (isDegreeMode) Math.toRadians(arg) else arg
                    // Snap clean multiples of 180 deg to 0
                    if (isDegreeMode && abs(arg % 180.0) == 0.0) 0.0 else sin(rad)
                }
                "cos" -> {
                    val rad = if (isDegreeMode) Math.toRadians(arg) else arg
                    if (isDegreeMode && abs((abs(arg) - 90.0) % 180.0) == 0.0) 0.0 else cos(rad)
                }
                "tan" -> {
                    if (isDegreeMode && abs((abs(arg) - 90.0) % 180.0) == 0.0) {
                        throw CalcDomainException("tan(${arg.toInt()}°) tidak terdefinisi (Asimtot)")
                    }
                    val rad = if (isDegreeMode) Math.toRadians(arg) else arg
                    if (isDegreeMode && abs(arg % 180.0) == 0.0) 0.0 else tan(rad)
                }
                "asin" -> {
                    if (arg < -1.0 || arg > 1.0) throw CalcDomainException("Domain asin harus antara -1 dan 1")
                    val rad = asin(arg)
                    if (isDegreeMode) Math.toDegrees(rad) else rad
                }
                "acos" -> {
                    if (arg < -1.0 || arg > 1.0) throw CalcDomainException("Domain acos harus antara -1 dan 1")
                    val rad = acos(arg)
                    if (isDegreeMode) Math.toDegrees(rad) else rad
                }
                "atan" -> {
                    val rad = atan(arg)
                    if (isDegreeMode) Math.toDegrees(rad) else rad
                }
                "sqrt" -> {
                    if (arg < 0.0) throw CalcDomainException("Akar kuadrat negatif tidak valid (√ < 0)")
                    sqrt(arg)
                }
                "cbrt" -> cbrt(arg)
                "ln" -> {
                    if (arg <= 0.0) throw CalcDomainException("Domain ln(x) harus > 0")
                    ln(arg)
                }
                "log" -> {
                    if (arg <= 0.0) throw CalcDomainException("Domain log10(x) harus > 0")
                    log10(arg)
                }
                "log2" -> {
                    if (arg <= 0.0) throw CalcDomainException("Domain log2(x) harus > 0")
                    ln(arg) / ln(2.0)
                }
                "sinh" -> kotlin.math.sinh(arg)
                "cosh" -> kotlin.math.cosh(arg)
                "tanh" -> kotlin.math.tanh(arg)
                "abs" -> abs(arg)
                "inv" -> {
                    if (abs(arg) < 1e-15) throw CalcDomainException("1/x tidak dapat membagi nol")
                    1.0 / arg
                }
                else -> throw CalcDomainException("Fungsi tidak dikenal: $name")
            }
        }

        private fun factorial(n: Double): Double {
            if (n < 0.0 || n != kotlin.math.floor(n)) {
                throw CalcDomainException("Faktorial (n!) hanya untuk bilangan bulat ≥ 0")
            }
            val intN = n.toInt()
            if (intN > 170) {
                throw CalcDomainException("Angka faktorial terlalu besar (> 170!)")
            }
            var acc = 1.0
            for (k in 2..intN) {
                acc *= k.toDouble()
            }
            return acc
        }
    }

    // =========================================================================
    // 3. PERCENTAGE CALCULATOR
    // =========================================================================
    fun calculatePercentage(modeIndex: Int, inputA: Double, inputB: Double, isId: Boolean = true): PercentageCalcOutcome {
        return when (modeIndex) {
            0 -> {
                // X% dari Y
                val res = (inputA / 100.0) * inputB
                PercentageCalcOutcome(
                    mainResult = res,
                    secondaryValue = inputB - res,
                    summaryLabel = if (isId) "${formatNumber(inputA)}% dari ${formatNumber(inputB)}" else "${formatNumber(inputA)}% of ${formatNumber(inputB)}",
                    formulaExplanation = "(${formatNumber(inputA)} ÷ 100) × ${formatNumber(inputB)} = ${formatNumber(res)}"
                )
            }
            1 -> {
                // X adalah berapa % dari Y
                if (abs(inputB) < 1e-12) {
                    PercentageCalcOutcome(
                        mainResult = 0.0,
                        summaryLabel = if (isId) "Pembagi Y tidak boleh 0" else "Base Y cannot be 0",
                        formulaExplanation = "Error: ÷ 0"
                    )
                } else {
                    val pct = (inputA / inputB) * 100.0
                    PercentageCalcOutcome(
                        mainResult = pct,
                        secondaryValue = inputB - inputA,
                        summaryLabel = if (isId) "${formatNumber(inputA)} adalah ${formatNumber(pct)}% dari ${formatNumber(inputB)}" else "${formatNumber(inputA)} is ${formatNumber(pct)}% of ${formatNumber(inputB)}",
                        formulaExplanation = "(${formatNumber(inputA)} ÷ ${formatNumber(inputB)}) × 100 = ${formatNumber(pct)}%"
                    )
                }
            }
            2 -> {
                // Kenaikan / Penurunan % dari A ke B
                if (abs(inputA) < 1e-12) {
                    PercentageCalcOutcome(
                        mainResult = 0.0,
                        summaryLabel = if (isId) "Nilai awal tidak boleh 0" else "Initial value cannot be 0",
                        formulaExplanation = "Error: ÷ 0"
                    )
                } else {
                    val diff = inputB - inputA
                    val pctChange = (diff / abs(inputA)) * 100.0
                    val dirLabel = if (diff >= 0) {
                        if (isId) "Kenaikan" else "Increase"
                    } else {
                        if (isId) "Penurunan" else "Decrease"
                    }
                    PercentageCalcOutcome(
                        mainResult = pctChange,
                        secondaryValue = diff,
                        summaryLabel = "$dirLabel ${formatNumber(abs(pctChange))}% (Selisih: ${formatNumber(diff)})",
                        formulaExplanation = "((${formatNumber(inputB)} - ${formatNumber(inputA)}) ÷ ${formatNumber(inputA)}) × 100 = ${formatNumber(pctChange)}%"
                    )
                }
            }
            else -> {
                // Nilai Awal +/- X% (Tambah & Kurang Persentase)
                val delta = inputA * (inputB / 100.0)
                val added = inputA + delta
                val subtracted = inputA - delta
                PercentageCalcOutcome(
                    mainResult = added,
                    secondaryValue = subtracted,
                    summaryLabel = if (isId) "Tambah (+${formatNumber(inputB)}%): ${formatNumber(added)} • Diskon (-${formatNumber(inputB)}%): ${formatNumber(subtracted)}"
                    else "After +${formatNumber(inputB)}%: ${formatNumber(added)} • After -${formatNumber(inputB)}%: ${formatNumber(subtracted)}",
                    formulaExplanation = "Δ = ${formatNumber(inputA)} × ${formatNumber(inputB)}% = ${formatNumber(delta)}"
                )
            }
        }
    }

    // =========================================================================
    // 4. FINANCIAL CALCULATOR
    // =========================================================================
    fun calculateLoanInstallment(
        principal: Double,
        annualRatePercent: Double,
        tenureMonths: Int
    ): LoanCalcOutcome {
        if (principal <= 0.0 || tenureMonths <= 0) {
            return LoanCalcOutcome(0.0, 0.0, 0.0, 0.0, isValid = false, errorMessage = "Pokok pinjaman & tenor harus > 0")
        }
        if (annualRatePercent < 0.0) {
            return LoanCalcOutcome(0.0, 0.0, 0.0, 0.0, isValid = false, errorMessage = "Suku bunga tidak boleh negatif")
        }
        val monthlyRate = (annualRatePercent / 100.0) / 12.0
        val monthlyPayment = if (monthlyRate < 1e-9) {
            principal / tenureMonths.toDouble()
        } else {
            val factor = (1.0 + monthlyRate).pow(tenureMonths.toDouble())
            principal * (monthlyRate * factor) / (factor - 1.0)
        }
        val totalPayment = monthlyPayment * tenureMonths
        val totalInterest = maxOf(0.0, totalPayment - principal)
        return LoanCalcOutcome(
            monthlyPayment = monthlyPayment,
            totalPayment = totalPayment,
            totalInterest = totalInterest,
            principalAmount = principal,
            isValid = true
        )
    }

    fun calculateCompoundInterest(
        initialDeposit: Double,
        monthlyContribution: Double,
        annualRatePercent: Double,
        years: Double,
        compoundsPerYear: Int = 12
    ): CompoundInterestOutcome {
        if (initialDeposit < 0.0 || monthlyContribution < 0.0 || years <= 0.0) {
            return CompoundInterestOutcome(0.0, 0.0, 0.0, 0.0, isValid = false, errorMessage = "Input tidak valid")
        }
        val totalMonths = (years * 12.0).roundToLong().toInt().coerceAtLeast(1)
        val monthlyRate = (annualRatePercent / 100.0) / 12.0
        var balance = initialDeposit
        for (m in 1..totalMonths) {
            balance = balance * (1.0 + monthlyRate) + monthlyContribution
        }
        val totalContrib = initialDeposit + (monthlyContribution * totalMonths)
        val totalInterest = maxOf(0.0, balance - totalContrib)
        val multiple = if (totalContrib > 0) balance / totalContrib else 1.0
        return CompoundInterestOutcome(
            finalBalance = balance,
            totalContributions = totalContrib,
            totalInterestEarned = totalInterest,
            growthMultiple = multiple,
            isValid = true
        )
    }

    fun calculateDiscountAndTax(
        originalPrice: Double,
        discountPercent: Double,
        extraDiscountPercent: Double = 0.0,
        taxPercent: Double = 11.0
    ): DiscountTaxOutcome {
        val base = maxOf(0.0, originalPrice)
        val disc1 = base * (discountPercent.coerceIn(0.0, 100.0) / 100.0)
        val afterFirst = base - disc1
        val disc2 = afterFirst * (extraDiscountPercent.coerceIn(0.0, 100.0) / 100.0)
        val priceAfterDiscount = afterFirst - disc2
        val totalDiscount = disc1 + disc2
        val taxAmount = priceAfterDiscount * (maxOf(0.0, taxPercent) / 100.0)
        val finalPayable = priceAfterDiscount + taxAmount
        return DiscountTaxOutcome(
            discountAmount = totalDiscount,
            priceAfterDiscount = priceAfterDiscount,
            taxAmount = taxAmount,
            finalPayablePrice = finalPayable,
            totalSavings = totalDiscount
        )
    }

    fun calculateProfitMargin(costPrice: Double, sellingPrice: Double): ProfitMarginOutcome {
        val profit = sellingPrice - costPrice
        val marginPct = if (abs(sellingPrice) > 1e-9) (profit / sellingPrice) * 100.0 else 0.0
        val markupPct = if (abs(costPrice) > 1e-9) (profit / costPrice) * 100.0 else 0.0
        return ProfitMarginOutcome(
            netProfit = profit,
            grossMarginPercent = marginPct,
            markupPercent = markupPct,
            isProfit = profit >= 0.0
        )
    }

    // =========================================================================
    // 5. UNIT CONVERSION CALCULATOR
    // =========================================================================
    fun getUnitsForCategory(category: ConversionCategory): List<ConversionUnitSpec> {
        return when (category) {
            ConversionCategory.LENGTH -> listOf(
                ConversionUnitSpec("mm", "Milimeter (mm)", "Millimeter (mm)", 0.001),
                ConversionUnitSpec("cm", "Sentimeter (cm)", "Centimeter (cm)", 0.01),
                ConversionUnitSpec("m", "Meter (m)", "Meter (m)", 1.0),
                ConversionUnitSpec("km", "Kilometer (km)", "Kilometer (km)", 1000.0),
                ConversionUnitSpec("in", "Inci (in)", "Inch (in)", 0.0254),
                ConversionUnitSpec("ft", "Kaki (ft)", "Foot (ft)", 0.3048),
                ConversionUnitSpec("yd", "Yard (yd)", "Yard (yd)", 0.9144),
                ConversionUnitSpec("mi", "Mil (mi)", "Mile (mi)", 1609.344)
            )
            ConversionCategory.WEIGHT -> listOf(
                ConversionUnitSpec("mg", "Miligram (mg)", "Milligram (mg)", 0.001),
                ConversionUnitSpec("g", "Gram (g)", "Gram (g)", 1.0),
                ConversionUnitSpec("kg", "Kilogram (kg)", "Kilogram (kg)", 1000.0),
                ConversionUnitSpec("ton", "Ton Metrik (t)", "Metric Ton (t)", 1_000_000.0),
                ConversionUnitSpec("oz", "Ons (oz)", "Ounce (oz)", 28.349523125),
                ConversionUnitSpec("lb", "Pon (lb)", "Pound (lb)", 453.59237)
            )
            ConversionCategory.TEMPERATURE -> listOf(
                ConversionUnitSpec("C", "Celsius (°C)", "Celsius (°C)"),
                ConversionUnitSpec("F", "Fahrenheit (°F)", "Fahrenheit (°F)"),
                ConversionUnitSpec("K", "Kelvin (K)", "Kelvin (K)")
            )
            ConversionCategory.TIME -> listOf(
                ConversionUnitSpec("ms", "Milidetik (ms)", "Millisecond (ms)", 0.001),
                ConversionUnitSpec("s", "Detik (s)", "Second (s)", 1.0),
                ConversionUnitSpec("min", "Menit (min)", "Minute (min)", 60.0),
                ConversionUnitSpec("h", "Jam (h)", "Hour (h)", 3600.0),
                ConversionUnitSpec("d", "Hari (d)", "Day (d)", 86400.0),
                ConversionUnitSpec("wk", "Minggu (wk)", "Week (wk)", 604800.0)
            )
            ConversionCategory.DATA -> listOf(
                ConversionUnitSpec("B", "Byte (B)", "Byte (B)", 1.0),
                ConversionUnitSpec("KB", "Kilobyte (KB)", "Kilobyte (KB)", 1024.0),
                ConversionUnitSpec("MB", "Megabyte (MB)", "Megabyte (MB)", 1024.0 * 1024.0),
                ConversionUnitSpec("GB", "Gigabyte (GB)", "Gigabyte (GB)", 1024.0 * 1024.0 * 1024.0),
                ConversionUnitSpec("TB", "Terabyte (TB)", "Terabyte (TB)", 1024.0 * 1024.0 * 1024.0 * 1024.0)
            )
            ConversionCategory.AREA -> listOf(
                ConversionUnitSpec("cm2", "cm²", "cm²", 0.0001),
                ConversionUnitSpec("m2", "Meter Persegi (m²)", "Square Meter (m²)", 1.0),
                ConversionUnitSpec("ha", "Hektare (ha)", "Hectare (ha)", 10_000.0),
                ConversionUnitSpec("km2", "km²", "km²", 1_000_000.0),
                ConversionUnitSpec("ft2", "ft²", "ft²", 0.09290304),
                ConversionUnitSpec("ac", "Acre (ac)", "Acre (ac)", 4046.8564224)
            )
            ConversionCategory.VOLUME -> listOf(
                ConversionUnitSpec("ml", "Mililiter (mL)", "Milliliter (mL)", 0.001),
                ConversionUnitSpec("l", "Liter (L)", "Liter (L)", 1.0),
                ConversionUnitSpec("m3", "Meter Kubik (m³)", "Cubic Meter (m³)", 1000.0),
                ConversionUnitSpec("cup", "Cup (US)", "Cup (US)", 0.2365882365),
                ConversionUnitSpec("gal", "Galon (US gal)", "Gallon (US gal)", 3.785411784)
            )
            ConversionCategory.SPEED -> listOf(
                ConversionUnitSpec("ms", "Meter/detik (m/s)", "Meter/second (m/s)", 1.0),
                ConversionUnitSpec("kmh", "Kilometer/jam (km/j)", "Kilometer/hour (km/h)", 1.0 / 3.6),
                ConversionUnitSpec("mph", "Mil/jam (mph)", "Mile/hour (mph)", 0.44704),
                ConversionUnitSpec("kn", "Knot Maritim (kn)", "Maritime Knot (kn)", 0.514444)
            )
        }
    }

    fun convertUnit(
        category: ConversionCategory,
        value: Double,
        fromCode: String,
        toCode: String
    ): Double {
        if (fromCode == toCode) return value
        if (category == ConversionCategory.TEMPERATURE) {
            val celsius = when (fromCode) {
                "F" -> (value - 32.0) * 5.0 / 9.0
                "K" -> value - 273.15
                else -> value
            }
            return when (toCode) {
                "F" -> (celsius * 9.0 / 5.0) + 32.0
                "K" -> celsius + 273.15
                else -> celsius
            }
        }
        val units = getUnitsForCategory(category)
        val fromUnit = units.find { it.code == fromCode } ?: return value
        val toUnit = units.find { it.code == toCode } ?: return value
        // Exact BigDecimal conversion to avoid floating-point drift (e.g., 1000 m -> 1 km)
        val bdVal = BigDecimal.valueOf(value)
        val bdFrom = BigDecimal.valueOf(fromUnit.toBaseFactor)
        val bdTo = BigDecimal.valueOf(toUnit.toBaseFactor)
        val baseVal = bdVal.multiply(bdFrom, MathContext.DECIMAL128)
        return baseVal.divide(bdTo, MathContext.DECIMAL128).toDouble()
    }

    // =========================================================================
    // 6. DATE & TIME CALCULATOR
    // =========================================================================
    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun calculateDateDifference(startIso: String, endIso: String): DateDiffOutcome {
        return try {
            val start = LocalDate.parse(startIso.trim(), isoDateFormatter)
            val end = LocalDate.parse(endIso.trim(), isoDateFormatter)
            val orderedStart = if (start.isBefore(end)) start else end
            val orderedEnd = if (start.isBefore(end)) end else start
            val period = Period.between(orderedStart, orderedEnd)
            val totalDays = abs(ChronoUnit.DAYS.between(start, end))
            val weeks = totalDays / 7
            val remDays = totalDays % 7
            DateDiffOutcome(
                totalDays = totalDays,
                years = period.years,
                months = period.months,
                days = period.days,
                totalWeeks = weeks,
                remainingWeekDays = remDays,
                totalHours = totalDays * 24L,
                totalMinutes = totalDays * 1440L,
                isValid = true
            )
        } catch (_: Exception) {
            DateDiffOutcome(
                totalDays = 0,
                years = 0,
                months = 0,
                days = 0,
                totalWeeks = 0,
                remainingWeekDays = 0,
                totalHours = 0,
                totalMinutes = 0,
                isValid = false,
                errorMessage = "Format tanggal harus YYYY-MM-DD (contoh: 2026-10-19)"
            )
        }
    }

    fun addOrSubtractDaysFromDate(
        baseIso: String,
        daysDelta: Long,
        weeksDelta: Long = 0L,
        monthsDelta: Long = 0L
    ): String {
        return try {
            val base = LocalDate.parse(baseIso.trim(), isoDateFormatter)
            val result = base
                .plusMonths(monthsDelta)
                .plusWeeks(weeksDelta)
                .plusDays(daysDelta)
            val dayOfWeekId = when (result.dayOfWeek.value) {
                1 -> "Senin"
                2 -> "Selasa"
                3 -> "Rabu"
                4 -> "Kamis"
                5 -> "Jumat"
                6 -> "Sabtu"
                else -> "Minggu"
            }
            "${result.format(isoDateFormatter)} ($dayOfWeekId)"
        } catch (_: Exception) {
            "Format tanggal harus YYYY-MM-DD"
        }
    }

    fun calculateTimeDuration(startHm: String, endHm: String): String {
        return try {
            val p1 = startHm.trim().split(":")
            val p2 = endHm.trim().split(":")
            val h1 = p1.getOrNull(0)?.toInt() ?: 0
            val m1 = p1.getOrNull(1)?.toInt() ?: 0
            val s1 = p1.getOrNull(2)?.toInt() ?: 0

            val h2 = p2.getOrNull(0)?.toInt() ?: 0
            val m2 = p2.getOrNull(1)?.toInt() ?: 0
            val s2 = p2.getOrNull(2)?.toInt() ?: 0

            val t1 = LocalTime.of(h1.coerceIn(0, 23), m1.coerceIn(0, 59), s1.coerceIn(0, 59))
            val t2 = LocalTime.of(h2.coerceIn(0, 23), m2.coerceIn(0, 59), s2.coerceIn(0, 59))

            var diffSecs = ChronoUnit.SECONDS.between(t1, t2)
            if (diffSecs < 0) {
                diffSecs += 24 * 3600L // Cross midnight support
            }
            val hours = diffSecs / 3600L
            val mins = (diffSecs % 3600L) / 60L
            val secs = diffSecs % 60L
            val totalMins = diffSecs / 60.0
            "%02d jam %02d menit %02d detik (Total: %s menit / %,d detik)".format(
                Locale.US,
                hours,
                mins,
                secs,
                formatNumber(totalMins, maxDecimals = 2),
                diffSecs
            )
        } catch (_: Exception) {
            "Format waktu harus HH:MM atau HH:MM:SS (contoh 08:30)"
        }
    }

    // =========================================================================
    // 7. STATISTICS CALCULATOR
    // =========================================================================
    fun calculateStatistics(rawInput: String): StatisticsOutcome {
        val tokens = rawInput
            .split(Regex("[,;\\s\\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (tokens.isEmpty()) {
            return StatisticsOutcome(
                0, 0.0, 0.0, 0.0, emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList(),
                isValid = false,
                errorMessage = "Masukkan minimal 1 angka (pisahkan dengan koma atau spasi)"
            )
        }

        val values = mutableListOf<Double>()
        for (tk in tokens) {
            val normalized = tk.replace(",", ".")
            val v = normalized.toDoubleOrNull()
                ?: return StatisticsOutcome(
                    0, 0.0, 0.0, 0.0, emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList(),
                    isValid = false,
                    errorMessage = "Data bukan angka valid: \"$tk\""
                )
            values.add(v)
        }

        val sorted = values.sorted()
        val count = sorted.size
        val sum = sorted.sum()
        val mean = sum / count.toDouble()
        val median = if (count % 2 == 1) {
            sorted[count / 2]
        } else {
            (sorted[(count / 2) - 1] + sorted[count / 2]) / 2.0
        }

        val freqMap = sorted.groupingBy { it }.eachCount()
        val maxFreq = freqMap.values.maxOrNull() ?: 1
        val modes = if (maxFreq > 1) {
            freqMap.filterValues { it == maxFreq }.keys.toList()
        } else {
            emptyList()
        }

        val minVal = sorted.first()
        val maxVal = sorted.last()
        val range = maxVal - minVal

        val sqDiffSum = sorted.sumOf { (it - mean) * (it - mean) }
        val variancePop = sqDiffSum / count.toDouble()
        val varianceSample = if (count > 1) sqDiffSum / (count - 1).toDouble() else 0.0

        return StatisticsOutcome(
            count = count,
            sum = sum,
            mean = mean,
            median = median,
            modes = modes,
            min = minVal,
            max = maxVal,
            range = range,
            variancePop = variancePop,
            varianceSample = varianceSample,
            stdDevPop = sqrt(variancePop),
            stdDevSample = sqrt(varianceSample),
            sortedValues = sorted,
            isValid = true
        )
    }

    // =========================================================================
    // 8. PROGRAMMER CALCULATOR
    // =========================================================================
    fun parseProgrammerValue(raw: String, radix: Int): Long? {
        val clean = raw.trim()
            .replace("_", "")
            .replace(" ", "")
            .removePrefix("0x")
            .removePrefix("0X")
            .removePrefix("0b")
            .removePrefix("0B")
        if (clean.isEmpty()) return 0L
        return try {
            java.lang.Long.parseUnsignedLong(clean, radix)
        } catch (_: Exception) {
            clean.toLongOrNull(radix)
        }
    }

    fun evaluateProgrammerOperation(
        inputA: String,
        inputB: String,
        inputRadix: Int,
        operation: String,
        bitWidth: Int = 64
    ): ProgrammerOutcome {
        val mask: Long = when (bitWidth) {
            8 -> 0xFFL
            16 -> 0xFFFFL
            32 -> 0xFFFFFFFFL
            else -> -1L // All 64 bits
        }
        val aVal = parseProgrammerValue(inputA, inputRadix)
            ?: return ProgrammerOutcome("0", "0", "0", "0", 0L, bitWidth, false, "Input A tidak sesuai basis $inputRadix")
        val bVal = parseProgrammerValue(inputB, inputRadix)
            ?: return ProgrammerOutcome("0", "0", "0", "0", 0L, bitWidth, false, "Input B tidak sesuai basis $inputRadix")

        val aMasked = aVal and mask
        val bMasked = bVal and mask

        val rawRes: Long = when (operation) {
            "AND" -> aMasked and bMasked
            "OR" -> aMasked or bMasked
            "XOR" -> aMasked xor bMasked
            "NOT" -> (aMasked.inv()) and mask
            "SHL" -> (aMasked shl (bMasked.toInt().coerceIn(0, 63))) and mask
            "SHR" -> (aMasked ushr (bMasked.toInt().coerceIn(0, 63))) and mask
            "ADD" -> (aMasked + bMasked) and mask
            "SUB" -> (aMasked - bMasked) and mask
            else -> aMasked
        }

        val finalVal = rawRes and mask
        return ProgrammerOutcome(
            decString = finalVal.toString(10),
            binString = java.lang.Long.toUnsignedString(finalVal, 2)
                .chunked(4)
                .joinToString(" "),
            octString = java.lang.Long.toUnsignedString(finalVal, 8),
            hexString = java.lang.Long.toUnsignedString(finalVal, 16).uppercase(Locale.US),
            rawValue = finalVal,
            bitWidth = bitWidth,
            isValid = true
        )
    }

    /**
     * Converts a number string from [fromRadix] to [toRadix] when switching bases in Programmer mode.
     * Returns the converted representation, or cleans invalid characters if conversion fails.
     */
    fun convertProgrammerBaseInput(raw: String, fromRadix: Int, toRadix: Int, bitWidth: Int = 64): String {
        if (fromRadix == toRadix) return raw
        val parsed = parseProgrammerValue(raw, fromRadix) ?: return "0"
        val mask: Long = when (bitWidth) {
            8 -> 0xFFL
            16 -> 0xFFFFL
            32 -> 0xFFFFFFFFL
            else -> -1L
        }
        val masked = parsed and mask
        return when (toRadix) {
            10 -> masked.toString(10)
            16 -> java.lang.Long.toUnsignedString(masked, 16).uppercase(Locale.US)
            8 -> java.lang.Long.toUnsignedString(masked, 8)
            2 -> java.lang.Long.toUnsignedString(masked, 2)
            else -> masked.toString(10)
        }
    }

    /**
     * Toggles a specific bit [bitIndex] (0 = LSB) in the value represented by [raw] in [radix].
     */
    fun toggleBitInBase(raw: String, radix: Int, bitIndex: Int, bitWidth: Int = 64): String {
        val current = parseProgrammerValue(raw, radix) ?: 0L
        val mask: Long = when (bitWidth) {
            8 -> 0xFFL
            16 -> 0xFFFFL
            32 -> 0xFFFFFFFFL
            else -> -1L
        }
        val toggled = (current xor (1L shl bitIndex.coerceIn(0, 63))) and mask
        return convertProgrammerBaseInput(java.lang.Long.toUnsignedString(toggled, 16), 16, radix, bitWidth)
    }
}
