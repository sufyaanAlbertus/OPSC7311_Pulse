package com.pulse.budget.util

import java.text.NumberFormat
import java.util.Locale

/** Wraps java.text.NumberFormat for ZAR currency display. */
private val zarFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 2
}

fun formatZar(amount: Double): String = zarFormat.format(amount)
fun formatZar(amount: Float): String = zarFormat.format(amount.toDouble())
