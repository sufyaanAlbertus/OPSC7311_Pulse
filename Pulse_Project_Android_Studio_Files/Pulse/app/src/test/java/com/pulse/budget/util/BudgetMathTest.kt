package com.pulse.budget.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the 7-Day Signal threshold on the Dashboard. */
class BudgetMathTest {

    @Test
    fun `daily threshold is one and a half times the daily share of the max goal`() {
        assertEquals(500f, dailyThreshold(10000f), 0.01f)
        assertEquals(0f, dailyThreshold(0f), 0.01f)
    }

    @Test
    fun `day is flagged only when it goes over the threshold`() {
        assertTrue(isDayOverThreshold(501f, 10000f))
        assertFalse(isDayOverThreshold(500f, 10000f))
        assertFalse(isDayOverThreshold(9999f, 0f))
    }
}
