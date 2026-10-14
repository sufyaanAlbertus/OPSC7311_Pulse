package com.pulse.budget.util

/*
 * References:
 *  - Oracle (2023) java.time.LocalDate.plusMonths. https://docs.oracle.com/javase/8/docs/api/java/time/LocalDate.html
 *  - Anthropic (2026) Claude [Large language model] — used as a coding assistant; see AI declaration in README.
 */

import com.pulse.budget.data.model.RecurrenceType
import java.time.LocalDate

// Own feature #2: recurring expenses.

/** Safety cap so a very old template can never create an unbounded number of copies at once. */
const val MAX_OCCURRENCES_PER_RUN = 100

/** Occurrence dates of a recurring expense after [afterExclusive] up to [untilInclusive]. */
fun recurrenceOccurrences(
    anchor: LocalDate,
    type: RecurrenceType,
    afterExclusive: LocalDate,
    untilInclusive: LocalDate
): List<LocalDate> {
    if (type == RecurrenceType.NONE || untilInclusive.isBefore(anchor)) return emptyList()
    val result = mutableListOf<LocalDate>()
    var n = 1L
    while (result.size < MAX_OCCURRENCES_PER_RUN) {
        val next = when (type) {
            RecurrenceType.WEEKLY -> anchor.plusWeeks(n)
            RecurrenceType.MONTHLY -> anchor.plusMonths(n)
            RecurrenceType.NONE -> return result
        }
        if (next.isAfter(untilInclusive)) break
        if (next.isAfter(afterExclusive)) result.add(next)
        n++
    }
    return result
}
