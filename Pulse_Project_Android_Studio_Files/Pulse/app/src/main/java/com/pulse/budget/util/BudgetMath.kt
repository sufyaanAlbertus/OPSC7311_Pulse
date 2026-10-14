package com.pulse.budget.util

fun dailyThreshold(monthlyBudget: Float, multiplier: Float = 1.5f): Float {
    if (monthlyBudget <= 0f) return 0f
    return (monthlyBudget / 30f) * multiplier
}

fun isDayOverThreshold(daySpend: Float, monthlyBudget: Float, multiplier: Float = 1.5f): Boolean {
    val threshold = dailyThreshold(monthlyBudget, multiplier)
    if (threshold <= 0f) return false
    return daySpend > threshold
}