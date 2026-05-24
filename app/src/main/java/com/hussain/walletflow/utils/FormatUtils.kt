package com.hussain.walletflow.utils

import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    fun formatIndianAmount(amount: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale("en", "IN"))
        // If the fractional part is zero, don't show it (e.g., 100.00 -> 100)
        // Otherwise, show 2 decimal places (e.g., 100.50 -> 100.50)
        if (amount % 1.0 == 0.0) {
            nf.minimumFractionDigits = 0
            nf.maximumFractionDigits = 0
        } else {
            nf.minimumFractionDigits = 2
            nf.maximumFractionDigits = 2
        }
        return nf.format(amount)
    }
}
