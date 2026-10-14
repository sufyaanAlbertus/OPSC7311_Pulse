package com.pulse.budget.util

import com.pulse.budget.data.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Tests for own feature #2 — automatic recurring expenses. */
class RecurrenceMathTest {

    private val anchor = LocalDate.of(2026, 1, 31)

    @Test
    fun `weekly occurrences are every 7 days after the anchor`() {
        val dates = recurrenceOccurrences(anchor, RecurrenceType.WEEKLY, anchor, LocalDate.of(2026, 2, 21))
        assertEquals(listOf(LocalDate.of(2026, 2, 7), LocalDate.of(2026, 2, 14), LocalDate.of(2026, 2, 21)), dates)
    }

    @Test
    fun `monthly occurrences do not drift after a short month`() {
        val dates = recurrenceOccurrences(anchor, RecurrenceType.MONTHLY, anchor, LocalDate.of(2026, 4, 30))
        assertEquals(listOf(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30)), dates)
    }

    @Test
    fun `only occurrences after the last generated date are returned`() {
        val dates = recurrenceOccurrences(anchor, RecurrenceType.WEEKLY, LocalDate.of(2026, 2, 7), LocalDate.of(2026, 2, 14))
        assertEquals(listOf(LocalDate.of(2026, 2, 14)), dates)
    }

    @Test
    fun `nothing is due for NONE or before the next date`() {
        assertTrue(recurrenceOccurrences(anchor, RecurrenceType.NONE, anchor, LocalDate.of(2027, 1, 1)).isEmpty())
        assertTrue(recurrenceOccurrences(anchor, RecurrenceType.MONTHLY, anchor, LocalDate.of(2026, 2, 27)).isEmpty())
    }

    @Test
    fun `a very old template is capped`() {
        val dates = recurrenceOccurrences(LocalDate.of(2000, 1, 1), RecurrenceType.WEEKLY, LocalDate.of(2000, 1, 1), LocalDate.of(2026, 1, 1))
        assertEquals(MAX_OCCURRENCES_PER_RUN, dates.size)
    }
}
