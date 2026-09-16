package com.pulse.budget.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit test (no Android framework dependency, so it runs under ./gradlew test in CI
 * without an emulator) covering the NumberFormat-based currency utility.
 */
class CurrencyFormatTest {

    @Test
    fun `formatZar includes the rand symbol`() {
        val result = formatZar(1000.0)
        assertTrue("Expected a currency symbol in '$result'", result.contains("R"))
    }

    @Test
    fun `formatZar rounds to two decimal places`() {
        val result = formatZar(45.5)
        // en-ZA NumberFormat pads to 2 decimals, e.g. "R45.50" or "R 45.50" depending on locale data.
        assertTrue("Expected two decimal places in '$result'", result.matches(Regex(".*\\d+\\.\\d{2}$")))
    }

    @Test
    fun `formatZar handles zero`() {
        val result = formatZar(0.0)
        assertTrue(result.contains("0"))
    }

    @Test
    fun `Float overload matches Double overload for the same value`() {
        assertEquals(formatZar(250.0), formatZar(250.0f))
    }
}
