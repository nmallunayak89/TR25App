package com.postaltools.tr25.logic

import com.postaltools.tr25.data.JourneyRow
import java.time.format.DateTimeFormatter

/** Rows for the annexures (Tour Movement, Tour Diary), shared by the PDF and the Word file. */
object TourReport {
    private val DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    private val D = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    val tourHead = listOf("Tour", "Route (places visited)", "Left HQ", "Reached HQ", "Hours away", "DA (date – %)")
    val tourWeights = listOf(6f, 34f, 14f, 14f, 10f, 22f)

    fun tourRows(t: Totals): List<List<String>> = t.tours.map { tr ->
        val route = (listOf(tr.legs.first().fromPlace) + tr.legs.map { it.toPlace }).joinToString(" – ")
        val days = t.daDays.filter { tr.no in it.tours }.joinToString("; ") { "${it.date.format(D)} – ${it.pct}%" }
        listOf("${tr.no}", route, tr.start.format(DT), if (tr.returnedToHq) tr.end.format(DT) else "Not entered",
            Tours.hm(tr.minutes), days)
    }

    val diaryHead = listOf("Date", "Journey (time)", "Mode", "Work type", "Office / Establishment", "Case No. / Ref.", "Work done", "Remarks")
    val diaryWeights = listOf(10f, 18f, 9f, 12f, 14f, 9f, 15f, 13f)

    private fun sorted(journeys: List<JourneyRow>) =
        journeys.filter { it.fromPlace.isNotBlank() || it.toPlace.isNotBlank() }.sortedBy { Dates.parseDateTime(it.depDateTime) }

    fun diaryRows(journeys: List<JourneyRow>): List<List<String>> = sorted(journeys).map { j ->
        val dep = j.depDateTime.trim(); val arr = j.arrDateTime.trim()
        val sameDay = dep.substringBefore(' ') == arr.substringBefore(' ')
        listOf(
            dep.substringBefore(' '),
            "${j.fromPlace} ${dep.substringAfter(' ', "")} → ${j.toPlace} ${if (sameDay) arr.substringAfter(' ', "") else arr}".trim(),
            j.modeClass, j.workType, j.office, j.caseRef, j.purpose, j.remarks,
        )
    }

    /** One-line counts for the diary summary. */
    fun summary(journeys: List<JourneyRow>, t: Totals): List<String> {
        val js = sorted(journeys)
        fun has(j: JourneyRow, re: String) = Regex(re, RegexOption.IGNORE_CASE).containsMatchIn(j.workType)
        val insp = js.filter { has(it, "inspection|surprise|verification") }
        val cases = js.filter { has(it, "case|inquiry|enquiry|complaint") || it.caseRef.isNotBlank() }
        val out = mutableListOf<String>()
        out += "Tours: ${t.tours.size}   ·   Days on tour (DA days): ${t.daDays.size}   ·   Journeys: ${js.size}"
        out += "Inspections / visits: ${insp.size}" +
            (if (insp.isNotEmpty()) " — " + insp.map { it.office.ifBlank { it.toPlace } }.distinct().joinToString(", ") else "")
        out += "Cases / inquiries: ${cases.size}" +
            (if (cases.isNotEmpty()) " — " + cases.map { listOf(it.caseRef, it.office.ifBlank { it.toPlace }).filter { s -> s.isNotBlank() }.joinToString(" ") }.distinct().joinToString("; ") else "")
        return out
    }
}
