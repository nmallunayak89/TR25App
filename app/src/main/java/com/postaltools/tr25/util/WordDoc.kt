package com.postaltools.tr25.util

import com.postaltools.tr25.data.TaBill
import com.postaltools.tr25.logic.Calculator
import com.postaltools.tr25.logic.Money
import com.postaltools.tr25.logic.Validator
import com.postaltools.tr25.logic.Words
import java.math.BigDecimal

/**
 * Editable Word version of the bill (HTML that MS Word / WPS Office open as a .doc):
 * Part A items 1–12, blank Part B for the Bill Section, and the Calculation of Claim page.
 */
object WordDoc {

    private fun e(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    private fun nil(s: String) = if (s.isBlank()) "Nil" else e(s)
    private fun amt(s: String) = if (s.isBlank()) "" else Money.parse(s)?.let { Money.format(it) } ?: e(s)
    private const val T = "border=\"1\" cellspacing=\"0\" cellpadding=\"3\" width=\"100%\" style=\"border-collapse:collapse;width:100%\""
    private const val N = "border=\"0\" cellspacing=\"0\" cellpadding=\"2\" width=\"100%\""
    private const val BR = "<br clear=all style=\"mso-special-character:line-break;page-break-before:always\">"

    private fun kv(label: String, value: String) = "<tr><td width=\"60%\">$label</td><td>: <b>$value</b></td></tr>"

    private fun table(head: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder("<table $T><tr>")
        head.forEach { sb.append("<th>$it</th>") }
        sb.append("</tr>")
        if (rows.isEmpty()) sb.append("<tr><td colspan=\"${head.size}\" align=\"center\">Nil</td></tr>")
        rows.forEach { r -> sb.append("<tr>"); r.forEach { sb.append("<td>$it</td>") }; sb.append("</tr>") }
        return sb.append("</table>").toString()
    }

    fun build(b: TaBill): String {
        val t = Calculator.compute(b)
        val j = b.journeys.filterNot { Validator.isEmptyJourney(it) }
        val halts = com.postaltools.tr25.logic.Tours.halts(t.tours)
        fun haltD(r: com.postaltools.tr25.data.JourneyRow) = r.haltDays.trim().ifBlank { halts[r]?.let { m -> if (m >= 1440) "${m / 1440}" else "" } ?: "" }
        fun haltH(r: com.postaltools.tr25.data.JourneyRow) = r.haltHours.trim().ifBlank { halts[r]?.let { m -> "%d:%02d".format((m % 1440) / 60, m % 60) } ?: "" }
        val sb = StringBuilder()
        sb.append("""<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/TR/REC-html40">
<head><meta charset="utf-8"><title>TR-25A</title>
<!--[if gte mso 9]><xml><w:WordDocument><w:View>Print</w:View></w:WordDocument></xml><![endif]-->
<style>@page Section1{size:595.3pt 841.9pt;margin:30pt 28pt 30pt 28pt}div.Section1{page:Section1}
body{font-family:"Times New Roman";font-size:10.5pt}p{margin:2pt 0}th{font-size:8.5pt}td{font-size:9pt;vertical-align:top}
h2{text-align:center;font-size:14pt;margin:4pt 0}</style></head><body><div class="Section1">""")

        // -------- Page 1: Part A, items 1–5
        sb.append("<table $N><tr><td>Sub-Bill No. ${e(b.subBillNo).ifBlank { ".........." }}</td><td align=\"right\">T.R.-25A<br>G.A.R.-14A</td></tr></table>")
        sb.append("<h2>TRAVELLING ALLOWANCE BILL FOR TOUR</h2>")
        sb.append("<p align=\"center\"><i>Note— This bill should be prepared in duplicate, one for payment and the other as office copy.</i></p>")
        sb.append("<p align=\"center\"><b>PART—A</b> (To be filled up by Government Servant)</p><table $N>")
        sb.append(kv("1. Name", e(b.name))).append(kv("2. Designation", e(b.designation)))
            .append(kv("3. Pay", if (b.pay.isBlank()) "" else "Rs. " + amt(b.pay))).append(kv("4. Headquarters", e(b.headquarters)))
        sb.append("</table><p><b>5. Details and purpose of journey(s) performed :-</b></p>")
        sb.append(table(
            listOf("Departure Date &amp; Time", "From", "Arrival Date &amp; Time", "To", "Mode of travel and class",
                "Fare paid Rs.", "Distance Km for mileage", "Halt Days", "Halt Hours", "Purpose of Journey / Remarks"),
            j.map {
                listOf(e(it.depDateTime), e(it.fromPlace), e(it.arrDateTime), e(it.toPlace), e(it.modeClass), amt(it.fare),
                    e(it.distanceKm), e(haltD(it)), e(haltH(it)),
                    e(it.purpose) + if (it.remarks.isNotBlank()) " (${e(it.remarks)})" else "")
            },
        ))

        // -------- Page 2: items 6–9
        sb.append(BR).append("<p><b>6. Mode of Journey :-</b></p><table $N>")
        sb.append("<tr><td colspan=2><b>(i) Air</b></td></tr>")
        sb.append(kv("(a) Exchange voucher arranged by officer (Yes/No)", nil(b.airVoucherByOfficer)))
        sb.append(kv("(b) Ticket/Exchange voucher arranged by", nil(b.airTicketArrangedBy)))
        sb.append("<tr><td colspan=2><b>(ii) Rail</b></td></tr>")
        sb.append(kv("(a) Whether travelled by mail/express/ordinary train?", nil(b.railTrainType)))
        sb.append(kv("(b) Whether return tickets available?", nil(b.railReturnAvailable)))
        sb.append(kv("(c) If available, whether return tickets purchased? If not, state reasons.",
            nil(listOf(b.railReturnPurchased, b.railNotPurchasedReason).filter { it.isNotBlank() }.joinToString(" — "))))
        sb.append("<tr><td colspan=2><b>(iii) Road</b></td></tr>")
        sb.append(kv("Mode of conveyance used i.e. by Govt. transport/by taking a taxi, a single seat in a bus or other public conveyance/by sharing with another Govt. servant in a car belonging to him or to a third person to be specified.", nil(b.roadConveyance)))
        sb.append("<tr><td colspan=2><b>7. Dates of absence from place of halt on account of :-</b></td></tr>")
        sb.append(kv("(a) R.H. and C.L.", nil(b.absenceRhCl))).append(kv("(b) Not being actually in camp on Sundays and Holidays", nil(b.absenceNotInCamp)))
        sb.append("<tr><td colspan=2><b>8. Dates on which free board and/or lodging provided by the State or any organisation financed by State funds :-</b></td></tr>")
        sb.append(kv("(a) Board only", nil(b.freeBoardOnly))).append(kv("(b) Lodging only", nil(b.freeLodgingOnly)))
            .append(kv("(c) Board and lodging", nil(b.freeBoardAndLodging)))
        sb.append("</table><p><b>9. Particulars to be furnished along with hotel receipts etc. in cases where higher rate of D.A. is claimed for stay in hotel/other establishments providing board and/or lodging at scheduled tariffs :-</b></p>")
        sb.append(table(listOf("Period of stay From", "To", "Name of Hotel", "Daily rate of lodging Rs.", "Total amount paid Rs."),
            b.hotels.map { listOf(e(it.fromDate), e(it.toDate), e(it.hotelName), amt(it.dailyRate), amt(it.totalPaid)) }))

        // -------- Page 3: items 10–12
        sb.append(BR).append("<p><b>10. Particulars of journey(s) for which higher class of accommodation than the one to which the Govt. servant is entitled was used :-</b></p>")
        sb.append(table(listOf("Date", "From", "To", "Mode of conveyance used", "Class entitled", "Class travelled", "Fare of entitled class Rs."),
            b.higherClass.map { listOf(e(it.date), e(it.fromPlace), e(it.toPlace), e(it.mode), e(it.classEntitled), e(it.classTravelled), amt(it.entitledFare)) }))
        sb.append("<p>If the journey(s) by higher class of accommodation had been performed with the approval of the competent authority, No. and date of the sanction may be quoted: <b>${nil(b.higherClassSanction)}</b></p>")
        sb.append("<p><b>11. Details of journey(s) performed by road between places connected by rail :-</b></p>")
        sb.append(table(listOf("Date", "From", "To", "Fare paid Rs."),
            b.roadJourneys.map { listOf(e(it.date), e(it.fromPlace), e(it.toPlace), amt(it.farePaid)) }))
        sb.append("<table $N>").append(kv("12. Amount of T.A. advance, if any, drawn", if (b.advanceAmount.isBlank()) "Nil" else "Rs. " + amt(b.advanceAmount))).append("</table>")
        sb.append("<p>Certified that the information given above is true to the best of my knowledge and belief.</p><br><br>")
        sb.append("<table $N><tr><td>Date: ${e(b.claimDate)}</td><td align=\"right\">Signature of the Govt. Servant<br>${e(b.name)}, ${e(b.designation)}</td></tr></table>")

        // -------- Page 4: Part B (blank)
        val dots = ".........."
        sb.append(BR).append("<p align=\"center\"><b>PART—B</b> (To be filled in the Bill Section)</p>")
        sb.append("<p>The net entitlement on account of travelling allowance works out to Rs. $dots$dots as detailed below :-</p>")
        sb.append(table(listOf("Particulars", "Rs.", "P."), listOf(
            "(a) Railways/Air/Bus/Steamer fare", "(b) Road mileage for $dots Kms. @ Rs. $dots per Km.", "(c) Daily allowance",
            "&nbsp;&nbsp;(i) $dots days @ Rs. $dots per day", "&nbsp;&nbsp;(ii) $dots days @ Rs. $dots per day",
            "&nbsp;&nbsp;(iii) $dots days @ Rs. $dots per day", "(d) Actual expenses", "<b>Gross amount</b>",
            "(e) Less amount of T.A. advance, if any, drawn vide Voucher No. $dots date $dots", "<b>Net amount</b>",
        ).map { listOf(it, "", "") }))
        sb.append("<p>The Expenditures debitable to $dots$dots$dots</p><br><br>")
        sb.append("<table $N><tr><td>Initials of Bill Clerk</td><td align=\"right\">Signature of D.D.O.</td></tr>")
        sb.append("<tr><td><br>Countersigned</td><td></td></tr><tr><td></td><td align=\"right\"><br><br>Signature of Controlling Officer</td></tr></table>")

        // -------- Page 5: Calculation of claim
        sb.append(BR).append("<p align=\"right\"><b>ANNEXURE-I</b></p><h2>CALCULATION OF CLAIM</h2>")
        sb.append("<p align=\"center\">T.A. Bill for Tour — ${e(b.name)}, ${e(b.designation)}, HQ: ${e(b.headquarters)}</p>")
        val rows = mutableListOf<List<String>>()
        rows += listOf("1. Fare by rail / air / bus / other conveyance (actuals)", Money.format(t.fare))
        rows += listOf("2. Road mileage: ${Money.qty(t.km)} Km × Rs. ${Money.format(t.mileageRate)} per Km" +
            (if (t.kmCapped) " (limited to ${e(b.mileageCapKm)} Km per day)" else ""), Money.format(t.mileage))
        val daText = if (t.autoDa) "3. Daily allowance @ Rs. ${Money.format(t.daRate)} per day" +
            t.autoDaLines.joinToString(" + ", prefix = if (t.autoDaLines.isEmpty()) "" else ": ") {
                "${it.days} day${if (it.days > 1) "s" else ""} @ Rs. ${Money.format(it.perDay)} (${it.pct}%)"
            }
        else "3. Daily allowance" + b.daSlabs.mapIndexedNotNull { i, s ->
            if (s.days.isBlank() && s.rate.isBlank()) null
            else "${e(s.days)} days @ Rs. ${amt(s.rate)} = ${Money.format(t.daLines.getOrElse(i) { BigDecimal.ZERO })}"
        }.joinToString("; ", prefix = ": ")
        rows += listOf(daText, Money.format(t.daTotal))
        rows += listOf("4. Actual expenses", Money.format(t.actualExpenses))
        rows += listOf("<b>Gross total</b>", "<b>${Money.format(t.gross)}</b>")
        rows += listOf("Less: T.A. advance drawn" + (if (b.advanceVoucherNo.isNotBlank()) " (Voucher No. ${e(b.advanceVoucherNo)} ${e(b.advanceVoucherDate)})" else ""),
            Money.format(t.advance))
        rows += listOf("<b>Net amount claimed</b>", "<b>${Money.format(t.net)}</b>")
        sb.append(table(listOf("Particulars", "Amount (Rs.)"), rows))
        sb.append("<p>(${Words.rupees(t.net)})</p><br><br>")
        sb.append("<table $N><tr><td>Date: ${e(b.claimDate)}</td><td align=\"right\">Signature of the Govt. Servant</td></tr></table>")
        // -------- Annexure-II: Tour movement, Annexure-III: Tour diary
        val tr = com.postaltools.tr25.logic.TourReport
        val diary = tr.diaryRows(b.journeys)
        if (diary.isNotEmpty()) {
            val who = "<p align=\"center\">${e(b.name)}, ${e(b.designation)}, HQ: ${e(b.headquarters)}</p>"
            val sign = "<br><br><table $N><tr><td>Date: ${e(b.claimDate)}</td><td align=\"right\">Signature of the Govt. Servant</td></tr></table>"
            sb.append(BR).append("<p align=\"right\"><b>ANNEXURE-II</b></p><h2>TOUR MOVEMENT</h2>").append(who)
            sb.append("<p><i>Departure from headquarters to arrival back at headquarters</i></p>")
            sb.append(table(tr.tourHead, tr.tourRows(t).map { r -> r.map { e(it) } }))
            sb.append("<p><i>DA per calendar day of absence from HQ: up to 6 hours 30%, 6 to 12 hours 70%, above 12 hours 100%.</i></p>").append(sign)
            sb.append(BR).append("<p align=\"right\"><b>ANNEXURE-III</b></p><h2>TOUR DIARY</h2>").append(who)
            sb.append("<p><i>Inspections, cases and other work done on tour</i></p>")
            sb.append(table(tr.diaryHead, diary.map { r -> r.map { e(it) } }))
            sb.append("<p><b>Summary</b></p>")
            tr.summary(b.journeys, t).forEach { sb.append("<p>${e(it)}</p>") }
            sb.append("<p>Certified that the above tours were performed and the work noted was done on duty.</p>").append(sign)
        }
        sb.append("</div></body></html>")
        return "﻿" + sb.toString()
    }
}
