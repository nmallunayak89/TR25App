package com.postaltools.tr25.util

import com.postaltools.tr25.data.Annex
import com.postaltools.tr25.data.JourneyRow
import com.postaltools.tr25.logic.Dates
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** 1 = first fortnight (1–15), 2 = second fortnight (16–end), 0 = whole month. */
data class Period(val ym: YearMonth, val part: Int) {
    val start: LocalDate get() = if (part == 2) ym.atDay(16) else ym.atDay(1)
    val end: LocalDate get() = if (part == 1) ym.atDay(15) else ym.atEndOfMonth()
    val key: String get() = "$ym-F$part"
    val monthText: String get() = ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase() + "-" + ym.year
    val name: String get() = when (part) { 1 -> "FIRST FORTNIGHT"; 2 -> "SECOND FORTNIGHT"; else -> "MONTH" }
    val title: String get() = if (part == 0) "FOR THE MONTH OF $monthText" else "FOR THE $name OF $monthText"
    val days: List<LocalDate> get() = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    fun has(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
}

/** Header lines kept in Settings. */
data class ReportHeader(
    val officeTitle: String,   // GAURIBIDANUR SUB DIVISION GAURIBIDANUR-561 208
    val place: String,         // Gauribidanur-561 208
    val submittedTo: String,   // The SSPOs \n Kolar Dn, Kolar-563 101
    val name: String,
    val designation: String,   // Inspector Posts
    val sigOffice: String,     // Gauribidanur Sub Division, GAURIBIDANUR -561 208
)

object ReportHtml {
    private val DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private fun e(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
    private const val PB = "@@PAGEBREAK@@"

    fun journeysIn(all: List<JourneyRow>, p: Period) = all.filter { j ->
        Dates.parseDateTime(j.depDateTime)?.toLocalDate()?.let { p.has(it) } ?: false
    }.sortedBy { Dates.parseDateTime(it.depDateTime) }

    private fun on(all: List<JourneyRow>, d: LocalDate) =
        all.filter { Dates.parseDateTime(it.depDateTime)?.toLocalDate() == d }

    /** Details for a day: the note typed in the diary, else built from that day's journeys. */
    fun autoDetails(d: LocalDate, journeys: List<JourneyRow>): String {
        val js = on(journeys, d)
        if (js.isNotEmpty()) {
            val parts = js.map { j ->
                listOf(
                    listOf(j.workType, j.office).filter { it.isNotBlank() }.joinToString(" of "),
                    j.purpose.takeUnless { it.equals("Return to headquarters", true) } ?: "",
                    if (j.caseRef.isNotBlank()) "(${j.caseRef})" else "",
                ).filter { it.isNotBlank() }.joinToString(" – ")
            }.filter { it.isNotBlank() }.distinct()
            if (parts.isNotEmpty()) return parts.joinToString(". ")
            return "On tour: " + (listOf(js.first().fromPlace) + js.map { it.toPlace }).joinToString(" – ")
        }
        return if (d.dayOfWeek == DayOfWeek.SUNDAY) "Sunday Off" else "At HQ"
    }

    private fun re(s: String) = Regex(s, RegexOption.IGNORE_CASE)
    private fun inspections(js: List<JourneyRow>) = js.filter { re("inspection").containsMatchIn(it.workType) }
        .map { (Dates.parseDateTime(it.depDateTime)?.toLocalDate()) to it.office.ifBlank { it.toPlace } }.distinct()
    private fun visits(js: List<JourneyRow>) = js.filter { re("surprise|visit").containsMatchIn(it.workType) }
        .distinctBy { (Dates.parseDateTime(it.depDateTime)?.toLocalDate()) to it.office.ifBlank { it.toPlace } }

    fun countInspections(js: List<JourneyRow>) = inspections(js).size
    fun countVisits(js: List<JourneyRow>) = visits(js).size

    private fun officeType(s: String) = when {
        re("\\bBO\\b|branch").containsMatchIn(s) -> "BO"
        re("\\bSO\\b|sub ?office").containsMatchIn(s) -> "SO"
        re("\\bHO\\b|head ?office").containsMatchIn(s) -> "HO"
        else -> ""
    }

    fun autoVisitRows(js: List<JourneyRow>): List<List<String>> = visits(js).map { j ->
        val o = j.office.ifBlank { j.toPlace }
        listOf(o, officeType(o), j.depDateTime.substringBefore(' '), j.purpose, j.remarks)
    }

    // ------------------------------------------------------------------ common
    private fun css(landscape: Boolean) = """
<style>
@font-face{font-family:'Pagella';src:url('fonts/texgyrepagella-regular.otf');font-weight:400;font-style:normal}
@font-face{font-family:'Pagella';src:url('fonts/texgyrepagella-bold.otf');font-weight:700;font-style:normal}
@font-face{font-family:'Pagella';src:url('fonts/texgyrepagella-italic.otf');font-weight:400;font-style:italic}
@font-face{font-family:'Pagella';src:url('fonts/texgyrepagella-bolditalic.otf');font-weight:700;font-style:italic}
@page{size:A4 ${if (landscape) "landscape" else "portrait"};margin:${if (landscape) "12mm 14mm" else "14mm 16mm"}}
@page Section1{size:${if (landscape) "841.9pt 595.3pt;mso-page-orientation:landscape" else "595.3pt 841.9pt"};margin:36pt 40pt}
div.Section1{page:Section1}
body{font-family:'Book Antiqua','Pagella','Palatino Linotype',Palatino,serif;font-size:11pt;color:#000;margin:0}
p{margin:3pt 0}
.c{text-align:center}.r{text-align:right}.b{font-weight:bold}.u{text-decoration:underline}
h1{font-family:'Book Antiqua','Pagella',serif;font-size:13pt;text-align:center;text-decoration:underline;margin:0 0 4pt}
h2{font-family:'Book Antiqua','Pagella',serif;font-size:11pt;text-align:center;text-decoration:underline;margin:4pt 0}
table.t{border-collapse:collapse;width:100%;margin:4pt 0}
table.t td,table.t th{border:1px solid #000;padding:3px 4px;vertical-align:top;font-size:10pt}
table.t th{font-weight:bold;text-align:center;vertical-align:middle}
table.s th,table.s td{font-size:8.5pt;padding:2px}
table.n{border-collapse:collapse;width:100%}table.n td{border:none;padding:1px 2px;vertical-align:top}
.sig{text-align:right;margin-top:28pt;line-height:1.35}
</style>"""

    private fun wrap(body: String, title: String, landscape: Boolean, word: Boolean): String {
        val head = if (word) """<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/TR/REC-html40">
<head><meta charset="utf-8"><title>${e(title)}</title><!--[if gte mso 9]><xml><w:WordDocument><w:View>Print</w:View></w:WordDocument></xml><![endif]-->"""
        else """<!DOCTYPE html><html><head><meta charset="utf-8"><title>${e(title)}</title>"""
        val pb = if (word) "<br clear=all style=\"mso-special-character:line-break;page-break-before:always\">"
        else "<div style=\"page-break-before:always\"></div>"
        val b = if (word) "<div class=\"Section1\">$body</div>" else body
        return (if (word) "﻿" else "") + head + css(landscape) + "</head><body>" + b.replace(PB, pb) + "</body></html>"
    }

    private fun sig(h: ReportHeader) =
        "<div class=\"sig\">(${e(h.name)})<br>${e(h.designation)},<br>${e(h.sigOffice)}</div>"

    private fun table(head: String, rows: List<List<String>>, cols: Int, small: Boolean = false, nil: String = "NIL"): String {
        val sb = StringBuilder("<table class=\"t${if (small) " s" else ""}\">").append(head)
        if (rows.isEmpty()) sb.append("<tr><td class=\"c\">01</td><td colspan=\"${cols - 1}\">${e(nil)}</td></tr>")
        rows.forEachIndexed { i, r ->
            sb.append("<tr><td class=\"c\">${"%02d".format(i + 1)}</td>")
            r.forEach { sb.append("<td>${e(it)}</td>") }
            repeat(cols - 1 - r.size) { sb.append("<td></td>") }
            sb.append("</tr>")
        }
        return sb.append("</table>").toString()
    }

    private fun nonBlank(rows: List<List<String>>) = rows.filter { r -> r.any { it.isNotBlank() } }

    // ------------------------------------------------------------------ DIARY
    fun diary(
        p: Period, h: ReportHeader, journeys: List<JourneyRow>, notes: Map<String, String>, a: Annex, word: Boolean,
    ): String {
        val js = journeysIn(journeys, p)
        val nInsp = a.inspCount.ifBlank { "%02d".format(countInspections(js)) }
        val nVis = a.visitCount.ifBlank { "${countVisits(js)}" }
        val sb = StringBuilder()
        sb.append("<h1>D E P A R T M E N T &nbsp; O F &nbsp; P O S T S</h1>")
        sb.append("<p class=\"c b\">DIARY OF ${e(h.designation.uppercase())}, ${e(h.officeTitle)} ${p.title}</p>")
        sb.append("<table class=\"t\"><tr><th width=\"14%\">Date</th><th width=\"13%\">Day</th><th>Details/Purpose of Journey</th></tr>")
        p.days.forEach { d ->
            val note = notes[d.toString()].orEmpty().ifBlank { autoDetails(d, js) }
            sb.append("<tr><td class=\"c\">${d.format(DMY)}</td><td>${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}</td><td>${e(note)}</td></tr>")
        }
        sb.append("</table>")
        sb.append("<table class=\"n\" style=\"margin-top:6pt\"><tr><td>No of Inspections: ${e(nInsp)}</td><td class=\"r\">Number of visits: ${e(nVis)}</td></tr></table>")
        sb.append(sig(h))

        if (p.part != 0) {
            val fn = "${p.name} OF ${p.monthText}"
            sb.append(PB)
            sb.append("<h2>ANNEXURE TO THE ${p.name} DIARY OF ${p.monthText} OF ${e(h.officeTitle.uppercase())}</h2>")
            sb.append("<p class=\"c b u\">SUMMARY OF WORK DONE FOR THE $fn.</p>")

            // A
            sb.append("<p class=\"b\">A. Surprise Visits:</p>")
            val vrows = nonBlank(a.visits).ifEmpty { autoVisitRows(js) }
            sb.append(table("<tr><th width=\"7%\">Sl. No.</th><th>Offices Visited</th><th width=\"10%\">Type (SO/BO)</th><th width=\"13%\">Date</th><th>Purpose</th><th>Remarks</th></tr>", vrows, 6))
            if (a.reasons.isNotBlank()) sb.append("<p><b>Reasons for not carrying out adequate no. of monthly visits:</b> ${e(a.reasons)}</p>")

            // B
            sb.append("<p class=\"b\">B. Inspections:</p>")
            sb.append("<table class=\"t s\"><tr><th colspan=\"18\" style=\"text-align:left\">Designation of Inspecting Officer : ${e(h.designation)}, ${e(h.sigOffice)}</th></tr>")
            sb.append("<tr><th rowspan=\"3\">Type of Inspections</th><th rowspan=\"3\">Sl. No</th><th colspan=\"2\" rowspan=\"2\">Inspections Approved for the year- ${p.ym.year}</th><th colspan=\"14\">No. of inspections (offices)</th></tr>")
            sb.append("<tr>" + listOf("Done in the FN", "Previous arrears", "Arrears of current month", "Total pending",
                "Total inspections required to be carried out till current month", "Total inspections carried out (Progressive)",
                "IRs pending to be released").joinToString("") { "<th colspan=\"2\">$it</th>" } + "</tr>")
            sb.append("<tr>" + "<th>SO</th><th>BO</th>".repeat(8) + "</tr>")
            a.insp.forEach { r -> sb.append("<tr><td>${e(r.getOrElse(0) { "" })}</td><td></td>" + (1..16).joinToString("") { "<td class=\"c\">${e(r.getOrElse(it) { "" })}</td>" } + "</tr>") }
            sb.append("</table>")

            // C
            sb.append("<p class=\"b\">C. Establishment work:</p>")
            sb.append(table("<tr><th width=\"7%\">Sl. No.</th><th>Items of work</th><th>Pending at the beginning/no. as per yearly Program</th><th>Received in F/N</th><th>Carried out in F/N</th><th>Pending at the end of F/N</th></tr>",
                a.est.map { r -> r.mapIndexed { i, v -> if (i > 0 && v.isBlank() && i == 1) "Nil" else v } }, 6))

            // D
            sb.append("<p class=\"b\">D. Particulars of fraud cases detected/reported/surfaced during the fortnight.</p>")
            sb.append(table("<tr><th width=\"7%\">Sl. No.</th><th>Brief of the cases</th><th>Date</th><th>Amount of fraud reported</th><th>Date of commencement of PWV</th></tr>",
                nonBlank(a.fraud), 5, nil = "No FRAUD CASES WERE DETECTED IN THE $fn"))

            // E
            sb.append("<p class=\"b\">E. Rule 10/14 cases pending in which the ASP/IP is Inquiry Officer / Presenting Officer:</p>")
            sb.append(table("<tr><th rowspan=\"2\" width=\"6%\">Sl. No</th><th rowspan=\"2\">Brief particulars of the cases</th><th rowspan=\"2\">Date of Appointment as IO/PO</th><th rowspan=\"2\">No. of hearings held</th><th colspan=\"2\">Examination of witnesses</th><th rowspan=\"2\">Date by which enquiry would be completed</th><th rowspan=\"2\">IO/PO report would be submitted</th><th rowspan=\"2\">Reasons for delay</th></tr><tr><th>Examined</th><th>Yet to be Examined</th></tr>",
                nonBlank(a.rule), 9, small = true))

            // F
            sb.append("<p class=\"b\">F. Provisional / Regular Engagements made:-</p>")
            sb.append(table("<tr><th width=\"7%\">Sl. No.</th><th>Particulars of post with name of office</th><th>Date of vacancy</th><th>Date of finalization of Engagement</th><th>Reasons for delay in finalization</th></tr>",
                nonBlank(a.engage), 5))

            // Endorsement
            sb.append("<p style=\"margin-top:10pt\">Endt. No. ${e(a.endtNo.ifBlank { "................" })} Dated @ ${e(h.place)}, the ${e(a.endtDate.ifBlank { "............" })}</p>")
            sb.append("<p>Submitted to; through Speed Post</p>")
            sb.append("<p>1. ${e(a.copyTo.ifBlank { h.submittedTo.replace("\n", ", ") + " :- for kind information." })}</p>")
            sb.append("<p>2. Office Copy</p>")
            sb.append(sig(h))
        }
        return wrap(sb.toString(), "Diary ${p.title}", landscape = false, word = word)
    }

    // ------------------------------------------------------------------ TOUR MOMENT
    fun tourMoment(p: Period, h: ReportHeader, journeys: List<JourneyRow>, a: Annex, word: Boolean): String {
        val js = journeysIn(journeys, p)
        val sb = StringBuilder()
        sb.append("<h1>DEPARTMENT OF POSTS, INDIA</h1>")
        sb.append("<p class=\"c b u\">JOURNEY PARTICULARS OF ${e(h.designation.uppercase())}, ${e(h.officeTitle)} ${p.title.replace(p.monthText, p.monthText.lowercase().replaceFirstChar { it.uppercase() })}</p>")
        sb.append("<table class=\"t\">")
        sb.append("<tr><th colspan=\"2\">From</th><th colspan=\"2\">To</th><th colspan=\"2\">Mode of Journey, Class &amp; Distance</th><th rowspan=\"3\" width=\"7%\">Fare paid in Rs</th><th rowspan=\"3\" width=\"30%\">Purpose of journey</th></tr>")
        sb.append("<tr><th colspan=\"2\">(Departure)</th><th colspan=\"2\">(Arrival)</th><th rowspan=\"2\">Mode</th><th rowspan=\"2\">Kms</th></tr>")
        sb.append("<tr><th width=\"11%\">Date and Time</th><th>Station</th><th width=\"11%\">Date and Time</th><th>Station</th></tr>")
        if (js.isEmpty()) sb.append("<tr><td colspan=\"8\" class=\"c\">NIL</td></tr>")
        var i = 0
        while (i < js.size) {
            // Rows with the same purpose share one merged cell, as in the register.
            var k = i + 1
            while (k < js.size && js[k].purpose.isNotBlank() && js[k].purpose == js[i].purpose) k++
            for (x in i until k) {
                val j = js[x]
                sb.append("<tr><td class=\"c\">${e(j.depDateTime)}</td><td>${e(j.fromPlace)}</td><td class=\"c\">${e(j.arrDateTime)}</td><td>${e(j.toPlace)}</td>")
                sb.append("<td class=\"c\">${e(j.modeClass)}</td><td class=\"c\">${e(j.distanceKm)}</td><td class=\"c\">${e(j.fare)}</td>")
                if (x == i) {
                    val purpose = listOf(j.purpose, listOf(j.workType, j.office).filter { it.isNotBlank() }.joinToString(" – ").takeIf { j.purpose.isBlank() } ?: "")
                        .filter { it.isNotBlank() }.joinToString(" ")
                    sb.append("<td rowspan=\"${k - i}\" style=\"vertical-align:middle;text-align:center\">${e(purpose)}${if (j.remarks.isNotBlank()) "<br><i>${e(j.remarks)}</i>" else ""}</td>")
                }
                sb.append("</tr>")
            }
            i = k
        }
        sb.append("</table>")
        sb.append("<p style=\"margin-top:8pt\" class=\"b\">No. ${e(a.refNo.ifBlank { "................" })} &nbsp;&nbsp;&nbsp; dated at ${e(h.place)} the ${e(a.refDate.ifBlank { "............" })}.</p>")
        sb.append("<p>Submitted to,</p><p>${e(h.submittedTo)}</p>")
        sb.append(sig(h))
        return wrap(sb.toString(), "Tour Moment ${p.title}", landscape = true, word = word)
    }
}
