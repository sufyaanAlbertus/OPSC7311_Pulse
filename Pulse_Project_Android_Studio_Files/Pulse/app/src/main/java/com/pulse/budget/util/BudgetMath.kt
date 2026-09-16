package com.pulse.budget.util

/**
 * Pulled out of ExpenseViewModel so it's a plain, testable function rather than logic buried
 * inside a Flow.combine lambda. A day is flagged "over" on the 7-Day Signal chart when its
 * spend exceeds 1.5x the implied daily budget (monthlyBudget / 30) — there's no per-day limit
 * in the data model, so this is the simplest meaningful threshold.
 */
fun dailyThreshold(monthlyBudget: Float, multiplier: Float = 1.5f): Float {
    if (monthlyBudget <= 0f) return 0f
    return (monthlyBudget / 30f) * multiplier
}

fun isDayOverThreshold(daySpend: Float, monthlyBudget: Float, multiplier: Float = 1.5f): Boolean {
    val threshold = dailyThreshold(monthlyBudget, multiplier)
    if (threshold <= 0f) return false
    return daySpend > threshold
}