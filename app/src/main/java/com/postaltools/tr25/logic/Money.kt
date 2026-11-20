package com.postaltools.tr25.logic

import java.math.BigDecimal
import java.math.RoundingMode

/** Number parsing and Indian-style formatting. All money is BigDecimal, never Double. */
object Money {

    private val pattern = Regex("^\\d+(\\.\\d+)?$")

    /**
     * Parses a non-negative number typed by the user.
     * Blank -> 0. Commas and spaces are ignored. Returns null if the text is not a valid number
     * or has more than [maxDecimals] decimal places.
     */
    fun parse(text: String, maxDecimals: Int = 2): BigDecimal? {
        val t = text.replace(",", "").replace(" ", "").trim()
        if (t.isEmpty()) return BigDecimal.ZERO
        val clean = if (t.endsWith(".")) t.dropLast(1) else t
        if (!pattern.matches(clean)) return null
        val bd = BigDecimal(clean)
        if (bd.scale() > maxDecimals) return null
        return bd
    }

    fun isBlank(text: String) = text.replace(",", "").isBlank()

    /** Rounds to paise, half-up. */
    fun paise(v: BigDecimal): BigDecimal = v.setScale(2, RoundingMode.HALF_UP)

    /** 1234567.5 -> "12,34,567.50" (Indian digit grouping). */
    fun format(v: BigDecimal): String {
        val r = paise(v)
        val negative = r.signum() < 0
        val plain = r.abs().toPlainString()
        val intPart = plain.substringBefore('.')
        val decPart = plain.substringAfter('.', "00").padEnd(2, '0')
        val grouped = if (intPart.length <= 3) intPart else {
            val last3 = intPart.takeLast(3)
            val rest = intPart.dropLast(3)
            val parts = mutableListOf<String>()
            var s = rest
            while (s.length > 2) {
                parts.add(0, s.takeLast(2)); s = s.dropLast(2)
            }
            if (s.isNotEmpty()) parts.add(0, s)
            parts.joinToString(",") + "," + last3
        }
        return (if (negative) "-" else "") + grouped + "." + decPart
    }

    /** With the rupee sign, for the app screens. The PDF uses "Rs." as the printed form does. */
    fun rupees(v: BigDecimal): String = "₹" + format(v)

    /** Plain number without grouping for quantities like km or days: 12.50 -> "12.5", 3.00 -> "3". */
    fun qty(v: BigDecimal): String {
        val s = v.stripTrailingZeros().toPlainString()
        return if (s == "0.0") "0" else s
    }
}
