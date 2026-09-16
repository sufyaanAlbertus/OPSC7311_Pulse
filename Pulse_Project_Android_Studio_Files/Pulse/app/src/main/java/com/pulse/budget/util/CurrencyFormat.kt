package com.pulse.budget.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Wraps java.text.NumberFormat for ZAR currency display, satisfying the Part 2 brief's explicit
 * requirement to use NumberFormat rather than hand-rolled string formatting. en-ZA gives the
 * correct "R" symbol and thousands separators.
 */
private val zarFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 2
}

fun formatZar(amount: Double): String = zarFormat.format(amount)
fun formatZar(amount: Float): String = zarFormat.format(amount.toDouble())
