package com.postaltools.tr25.logic

import com.postaltools.tr25.data.JourneyRow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** One tour: departure from HQ, any connecting journeys, arrival back at HQ. */
data class Tour(
    val no: Int,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val returnedToHq: Boolean,
    val startsFromHq: Boolean,
    val legs: List<JourneyRow>,
) {
    val minutes: Long get() = Duration.between(start, end).toMinutes()
}

/** Absence from HQ on one calendar day and the DA percentage for it. */
data class DaDay(val date: LocalDate, val minutes: Long, val pct: Int, val tours: List<Int>)

object Tours {

    private val words = Regex("\\b(ho|so|bo|po|hq|sub ?office|head ?office)\\b")
    private fun norm(s: String) = s.lowercase().replace(words, "").replace(Regex("[^a-z]"), "")

    /** "Gauribidanur", "Gauribidanur HO", "gauribidanur S.O." all count as the same HQ. */
    fun isHq(place: String, hq: String): Boolean {
        val a = norm(place); val b = norm(hq)
        if (a.isEmpty() || b.isEmpty()) return false
        return a == b || (a.length >= 4 && b.startsWith(a)) || (b.length >= 4 && a.startsWith(b))
    }

    /** Groups journeys into tours. A tour ends on arrival at HQ. */
    fun tours(journeys: List<JourneyRow>, hq: String): List<Tour> {
        data class Leg(val j: JourneyRow, val dep: LocalDateTime, val arr: LocalDateTime)
        val legs = journeys.mapNotNull { j ->
            val d = Dates.parseDateTime(j.depDateTime)
            val a = Dates.parseDateTime(j.arrDateTime)
            if (d != null && a != null && !a.isBefore(d)) Leg(j, d, a) else null
        }.sortedBy { it.dep }
        val out = mutableListOf<Tour>()
        var cur = mutableListOf<Leg>()
        fun close(ok: Boolean) {
            if (cur.isEmpty()) return
            out += Tour(out.size + 1, cur.first().dep, cur.last().arr, ok, isHq(cur.first().j.fromPlace, hq), cur.map { it.j })
            cur = mutableListOf()
        }
        for (l in legs) {
            if (cur.isNotEmpty() && isHq(l.j.fromPlace, hq)) close(false) // return to HQ was not entered
            cur.add(l)
            if (isHq(l.j.toPlace, hq)) close(true)
        }
        close(false)
        return out
    }

    /** Own motor cycle / scooter / car: road mileage is claimed instead of fare. */
    fun isOwnVehicle(mode: String): Boolean =
        Regex("own|bike|motor ?cycle|scooter|two.?wheeler|rma", RegexOption.IGNORE_CASE).containsMatchIn(mode)

    fun isPublicTransport(mode: String): Boolean = !isOwnVehicle(mode) &&
        Regex("bus|rail|train|air|flight|taxi|auto|steamer|ferry|ksrtc|metro", RegexOption.IGNORE_CASE).containsMatchIn(mode)

    /** Halt at the place reached by each journey within a tour (time until the next departure). */
    fun halts(tours: List<Tour>): Map<JourneyRow, Long> {
        val out = mutableMapOf<JourneyRow, Long>()
        tours.forEach { t ->
            t.legs.zipWithNext { a, b ->
                val arr = Dates.parseDateTime(a.arrDateTime); val dep = Dates.parseDateTime(b.depDateTime)
                if (arr != null && dep != null && dep.isAfter(arr)) out[a] = Duration.between(arr, dep).toMinutes()
            }
        }
        return out
    }

    fun pct(minutes: Long): Int = when {
        minutes > 12 * 60 -> 100
        minutes >= 6 * 60 -> 70
        else -> 30
    }

    /** Absence per calendar day (midnight to midnight). */
    fun daDays(tours: List<Tour>): List<DaDay> {
        val mins = sortedMapOf<LocalDate, Long>()
        val which = mutableMapOf<LocalDate, MutableList<Int>>()
        for (t in tours) {
            var c = t.start
            while (c.isBefore(t.end)) {
                val mid = c.toLocalDate().plusDays(1).atStartOfDay()
                val e = if (t.end.isBefore(mid)) t.end else mid
                val d = c.toLocalDate()
                mins[d] = (mins[d] ?: 0L) + Duration.between(c, e).toMinutes()
                val l = which.getOrPut(d) { mutableListOf() }
                if (!l.contains(t.no)) l.add(t.no)
                c = e
            }
        }
        return mins.map { (d, m) -> DaDay(d, m, pct(m), which[d] ?: emptyList()) }
    }

    fun hm(minutes: Long): String = "${minutes / 60} h ${"%02d".format(minutes % 60)} m"
}

/** Rupees in words, Indian system: "Rupees One Thousand Two Hundred Only". */
object Words {
    private val a = listOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven",
        "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
    private val b = listOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
    private fun two(x: Int) = if (x < 20) a[x] else b[x / 10] + if (x % 10 > 0) " " + a[x % 10] else ""
    private fun three(x: Int) = (if (x >= 100) a[x / 100] + " Hundred" + (if (x % 100 > 0) " " else "") else "") +
        (if (x % 100 > 0) two(x % 100) else "")

    fun rupees(v: java.math.BigDecimal): String {
        val p = Money.paise(v.abs())
        var r = p.toBigInteger().toLong()
        val paise = p.subtract(java.math.BigDecimal(r)).movePointRight(2).toInt()
        val sb = StringBuilder()
        if (r == 0L) sb.append("Zero")
        val cr = r / 10_000_000; r %= 10_000_000
        val lk = r / 100_000; r %= 100_000
        val th = r / 1000; r %= 1000
        if (cr > 0) sb.append(three(cr.toInt())).append(" Crore ")
        if (lk > 0) sb.append(two(lk.toInt())).append(" Lakh ")
        if (th > 0) sb.append(two(th.toInt())).append(" Thousand ")
        if (r > 0) sb.append(three(r.toInt()))
        var s = "Rupees " + sb.toString().trim()
        if (paise > 0) s += " and " + two(paise) + " Paise"
        return "$s Only"
    }
}
