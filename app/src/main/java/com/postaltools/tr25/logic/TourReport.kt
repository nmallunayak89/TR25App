package com.postaltools.tr25.logic

import com.postaltools.tr25.data.JourneyRow
import java.time.format.DateTimeFormatter

/** Rows for the "Tour Movement & Diary" page, shared by the PDF and the Word file. */
object TourReport {
    private val DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    private val D = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    val tourHead = listOf("Tour", "Route (places visited)", "Left HQ", "Reached HQ", "Hours away", "DA (date – %)")
    val tourWeights = listOf(6f, 34f, 14f, 14f, 10f, 22f)

    fun tourRows(t: Totals): List<List<String>> = t.tours.map { tr ->
        val route = (listOf(tr.legs.first().fromPlace) + tr.legs.map { it.toPlace }).joinToString(" – ")
        val days = t.daDays.filter { tr.no in it.tours }.joinToString("; ") { "${it.date.format(D)} – ${it.pct}%" }
        listOf(
            "${tr.no}", route, tr.start.format(DT),
            if (tr.returnedToHq) tr.end.format(DT) else "Not entered",
            Tours.hm(tr.minutes), days,
        )
    }

    val diaryHead = listOf("Date", "Dep.", "From", "Arr.", "To", "Mode", "Purpose / work done", "Remarks")
    val diaryWeights = listOf(11f, 7f, 13f, 7f, 13f, 12f, 20f, 17f)

    fun diaryRows(journeys: List<JourneyRow>): List<List<String>> =
        journeys.filter { it.fromPlace.isNotBlank() || it.toPlace.isNotBlank() }
            .sortedBy { Dates.parseDateTime(it.depDateTime) }
            .map { j ->
                val dep = j.depDateTime.trim(); val arr = j.arrDateTime.trim()
                val sameDay = dep.substringBefore(' ') == arr.substringBefore(' ')
                listOf(
                    dep.substringBefore(' '), dep.substringAfter(' ', ""), j.fromPlace,
                    if (sameDay) arr.substringAfter(' ', "") else arr, j.toPlace, j.modeClass, j.purpose, j.remarks,
                )
            }
}
