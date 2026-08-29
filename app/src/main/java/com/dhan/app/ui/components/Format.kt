package com.dhan.app.ui.components

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val inrGrouping: NumberFormat by lazy {
    NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
}

/** Indian-grouped currency string, matching components.jsx's formatINR(). */
fun formatINR(n: Number, showSign: Boolean = false, noSymbol: Boolean = false): String {
    val value = n.toDouble()
    val s = inrGrouping.format(abs(value))
    val sign = if (value < 0) "−" else if (showSign) "+" else ""
    val symbol = if (noSymbol) "" else "₹"
    return "$sign$symbol$s"
}
