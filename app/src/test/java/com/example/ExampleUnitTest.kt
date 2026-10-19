package com.example

import com.example.service.ConversionCategory
import com.example.service.SmartCalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit tests for Kalkulator Smart (SmartCalculatorEngine)
 * covering Section 6 mandatory test cases:
 * 1. Basic arithmetic & operator precedence
 * 2. Scientific calculations (sin, cos, tan, sqrt, powers, logs, factorial)
 * 3. Percentage calculations
 * 4. Financial calculations
 * 5. Unit conversions
 * 6. Date & Time calculations
 * 7. Statistics calculations
 * 8. Programmer calculations (DEC, BIN, OCT, HEX & Bitwise)
 * 9. Error handling (division by zero, negative sqrt, invalid syntax)
 */
class ExampleUnitTest {

    @Test
    fun `basic arithmetic and operator precedence are accurate`() {
        val addRes = SmartCalculatorEngine.evaluateExpression("12 + 8")
        assertTrue(addRes.isValid)
        assertEquals(20.0, addRes.numericValue, 1e-9)
        assertEquals("20", addRes.formattedResult)

        val subRes = SmartCalculatorEngine.evaluateExpression("100 - 37")
        assertTrue(subRes.isValid)
        assertEquals(63.0, subRes.numericValue, 1e-9)

        val mulRes = SmartCalculatorEngine.evaluateExpression("9 × 7")
        assertTrue(mulRes.isValid)
        assertEquals(63.0, mulRes.numericValue, 1e-9)

        val divRes = SmartCalculatorEngine.evaluateExpression("144 ÷ 12")
        assertTrue(divRes.isValid)
        assertEquals(12.0, divRes.numericValue, 1e-9)

        val precedence1 = SmartCalculatorEngine.evaluateExpression("2 + 3 × 4")
        assertTrue(precedence1.isValid)
        assertEquals(14.0, precedence1.numericValue, 1e-9)

        val precedence2 = SmartCalculatorEngine.evaluateExpression("(2 + 3) × 4")
        assertTrue(precedence2.isValid)
        assertEquals(20.0, precedence2.numericValue, 1e-9)
    }

    @Test
    fun `scientific calculations are accurate in degree and radian modes`() {
        val sqrtRes = SmartCalculatorEngine.evaluateExpression("√(81)")
        assertTrue(sqrtRes.isValid)
        assertEquals(9.0, sqrtRes.numericValue, 1e-9)

        val powRes = SmartCalculatorEngine.evaluateExpression("2^10")
        assertTrue(powRes.isValid)
        assertEquals(1024.0, powRes.numericValue, 1e-9)

        val sinDeg = SmartCalculatorEngine.evaluateExpression("sin(30)", isDegreeMode = true)
        assertTrue(sinDeg.isValid)
        assertEquals(0.5, sinDeg.numericValue, 1e-9)

        val cosDeg = SmartCalculatorEngine.evaluateExpression("cos(60)", isDegreeMode = true)
        assertTrue(cosDeg.isValid)
        assertEquals(0.5, cosDeg.numericValue, 1e-9)

        val factRes = SmartCalculatorEngine.evaluateExpression("5!")
        assertTrue(factRes.isValid)
        assertEquals(120.0, factRes.numericValue, 1e-9)

        val logRes = SmartCalculatorEngine.evaluateExpression("log(1000)")
        assertTrue(logRes.isValid)
        assertEquals(3.0, logRes.numericValue, 1e-9)
    }

    @Test
    fun `percentage and financial calculations are accurate`() {
        // 15% of 200 = 30
        val pct = SmartCalculatorEngine.calculatePercentage(0, 15.0, 200.0)
        assertEquals(30.0, pct.mainResult, 1e-9)

        // 20% discount from 500,000 with 0% tax = 400,000
        val disc = SmartCalculatorEngine.calculateDiscountAndTax(
            originalPrice = 500_000.0,
            discountPercent = 20.0,
            extraDiscountPercent = 0.0,
            taxPercent = 0.0
        )
        assertEquals(100_000.0, disc.discountAmount, 1e-6)
        assertEquals(400_000.0, disc.finalPayablePrice, 1e-6)

        // Loan calculation check
        val loan = SmartCalculatorEngine.calculateLoanInstallment(12_000_000.0, 0.0, 12)
        assertTrue(loan.isValid)
        assertEquals(1_000_000.0, loan.monthlyPayment, 1e-6)
    }

    @Test
    fun `unit conversions are accurate`() {
        // 1000 meters = 1 kilometer
        val km = SmartCalculatorEngine.convertUnit(ConversionCategory.LENGTH, 1000.0, "m", "km")
        assertEquals(1.0, km, 1e-9)

        // 100 Celsius = 212 Fahrenheit
        val fahr = SmartCalculatorEngine.convertUnit(ConversionCategory.TEMPERATURE, 100.0, "C", "F")
        assertEquals(212.0, fahr, 1e-9)

        // 60 minutes = 1 hour
        val hr = SmartCalculatorEngine.convertUnit(ConversionCategory.TIME, 60.0, "min", "h")
        assertEquals(1.0, hr, 1e-9)

        // 1024 MB = 1 GB
        val gb = SmartCalculatorEngine.convertUnit(ConversionCategory.DATA, 1024.0, "MB", "GB")
        assertEquals(1.0, gb, 1e-9)
    }

    @Test
    fun `date time statistics and programmer calculations are accurate`() {
        // Date difference
        val dateDiff = SmartCalculatorEngine.calculateDateDifference("2026-10-01", "2026-10-19")
        assertTrue(dateDiff.isValid)
        assertEquals(18L, dateDiff.totalDays)

        // Statistics: [10, 20, 30, 40, 50] -> mean = 30, median = 30, min = 10, max = 50, sum = 150
        val stats = SmartCalculatorEngine.calculateStatistics("10, 20, 30, 40, 50")
        assertTrue(stats.isValid)
        assertEquals(5, stats.count)
        assertEquals(150.0, stats.sum, 1e-9)
        assertEquals(30.0, stats.mean, 1e-9)
        assertEquals(30.0, stats.median, 1e-9)
        assertEquals(10.0, stats.min, 1e-9)
        assertEquals(50.0, stats.max, 1e-9)

        // Programmer: 255 decimal = FF hex = 1111 1111 binary = 377 octal
        val prog = SmartCalculatorEngine.evaluateProgrammerOperation(
            inputA = "255",
            inputB = "255",
            inputRadix = 10,
            operation = "AND",
            bitWidth = 32
        )
        assertTrue(prog.isValid)
        assertEquals("255", prog.decString)
        assertEquals("FF", prog.hexString)
        assertEquals("377", prog.octString)
        assertEquals("1111 1111", prog.binString)
    }

    @Test
    fun `error handling prevents crashes on invalid math operations`() {
        val divZero = SmartCalculatorEngine.evaluateExpression("10 ÷ 0")
        assertFalse(divZero.isValid)
        assertTrue(divZero.errorMessage?.contains("nol", ignoreCase = true) == true)

        val negSqrt = SmartCalculatorEngine.evaluateExpression("√(-9)")
        assertFalse(negSqrt.isValid)

        val asymTan = SmartCalculatorEngine.evaluateExpression("tan(90)", isDegreeMode = true)
        assertFalse(asymTan.isValid)
    }

    @Test
    fun `v1_67_00 smart percentage modulo number parsing and programmer base conversions work`() {
        // 200 + 10% = 220, 200 - 10% = 180, 200 * 10% = 20, 50% = 0.5
        val addPct = SmartCalculatorEngine.evaluateExpression("200 + 10%")
        assertTrue(addPct.isValid)
        assertEquals(220.0, addPct.numericValue, 1e-9)

        val subPct = SmartCalculatorEngine.evaluateExpression("200 - 10%")
        assertTrue(subPct.isValid)
        assertEquals(180.0, subPct.numericValue, 1e-9)

        val mulPct = SmartCalculatorEngine.evaluateExpression("200 × 10%")
        assertTrue(mulPct.isValid)
        assertEquals(20.0, mulPct.numericValue, 1e-9)

        val singlePct = SmartCalculatorEngine.evaluateExpression("50%")
        assertTrue(singlePct.isValid)
        assertEquals(0.5, singlePct.numericValue, 1e-9)

        // Modulo operator
        val modRes = SmartCalculatorEngine.evaluateExpression("17 mod 5")
        assertTrue(modRes.isValid)
        assertEquals(2.0, modRes.numericValue, 1e-9)

        // Smart Indonesian & International number parsing
        assertEquals(100_000_000.0, SmartCalculatorEngine.parseSmartNumber("100.000.000") ?: 0.0, 1e-6)
        assertEquals(1_250_000.5, SmartCalculatorEngine.parseSmartNumber("1.250.000,50") ?: 0.0, 1e-6)
        assertEquals(1_250_000.5, SmartCalculatorEngine.parseSmartNumber("1,250,000.50") ?: 0.0, 1e-6)

        // Programmer base conversion & bit toggle
        assertEquals("FF", SmartCalculatorEngine.convertProgrammerBaseInput("255", 10, 16, 32))
        assertEquals("255", SmartCalculatorEngine.convertProgrammerBaseInput("FF", 16, 10, 32))
        assertEquals("8", SmartCalculatorEngine.toggleBitInBase("0", 10, 3, 32))
    }
}
