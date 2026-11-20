package com.postaltools.tr25.logic

import com.postaltools.tr25.data.TaBill
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object Dates {
    val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT)
    val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT)
    val FILE: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")

    fun parseDate(s: String): LocalDate? = runCatching { LocalDate.parse(s.trim(), DATE) }.getOrNull()
    fun parseDateTime(s: String): LocalDateTime? = runCatching { LocalDateTime.parse(s.trim(), DATE_TIME) }.getOrNull()
    fun today(): String = LocalDate.now().format(DATE)
}

/** One problem found before a bill can be finalised. [key] matches the Calculator error keys where relevant. */
data class Issue(val key: String, val message: String)

object Validator {

    /** Checks needed before generating the final bill. Drafts can be saved with any of these open. */
    fun validateForFinal(b: TaBill, totals: Totals = Calculator.compute(b)): List<Issue> {
        val out = mutableListOf<Issue>()
        fun req(key: String, value: String, label: String) {
            if (value.isBlank()) out += Issue(key, "$label is required")
        }

        req("name", b.name, "1. Name")
        req("designation", b.designation, "2. Designation")
        req("pay", b.pay, "3. Pay")
        req("headquarters", b.headquarters, "4. Headquarters")

        val filled = b.journeys.filterNot { isEmptyJourney(it) }
        if (filled.isEmpty()) out += Issue("journeys", "5. Enter at least one journey")
        b.journeys.forEachIndexed { i, j ->
            if (isEmptyJourney(j)) return@forEachIndexed
            val n = i + 1
            req("journey.$i.fromPlace", j.fromPlace, "Journey $n: Departure From")
            req("journey.$i.toPlace", j.toPlace, "Journey $n: Arrival To")
            val dep = j.depDateTime.takeIf { it.isNotBlank() }
            val arr = j.arrDateTime.takeIf { it.isNotBlank() }
            if (dep == null) out += Issue("journey.$i.depDateTime", "Journey $n: Departure date and time is required")
            val depT = dep?.let { Dates.parseDateTime(it) }
            val arrT = arr?.let { Dates.parseDateTime(it) }
            if (dep != null && depT == null) out += Issue("journey.$i.depDateTime", "Journey $n: Departure date/time should be dd-MM-yyyy HH:mm")
            if (arr != null && arrT == null) out += Issue("journey.$i.arrDateTime", "Journey $n: Arrival date/time should be dd-MM-yyyy HH:mm")
            if (depT != null && arrT != null && arrT.isBefore(depT)) {
                out += Issue("journey.$i.arrDateTime", "Journey $n: Arrival is before departure")
            }
        }

        b.hotels.forEachIndexed { i, h ->
            val f = Dates.parseDate(h.fromDate); val t = Dates.parseDate(h.toDate)
            if (f != null && t != null && t.isBefore(f)) out += Issue("hotel.$i.toDate", "Hotel ${i + 1}: 'To' date is before 'From' date")
        }

        if (b.claimDate.isNotBlank() && Dates.parseDate(b.claimDate) == null) {
            out += Issue("claimDate", "Date under the certificate should be dd-MM-yyyy")
        }

        totals.errors.forEach { (k, m) -> out += Issue(k, m) }
        return out
    }

    fun isEmptyJourney(j: com.postaltools.tr25.data.JourneyRow): Boolean =
        listOf(j.depDateTime, j.fromPlace, j.arrDateTime, j.toPlace, j.modeClass, j.fare,
            j.distanceKm, j.haltDays, j.haltHours, j.purpose, j.remarks).all { it.isBlank() }
}
