package com.postaltools.tr25.pdf

import android.graphics.pdf.PdfDocument
import android.text.Layout
import com.postaltools.tr25.data.TaBill
import com.postaltools.tr25.logic.Calculator
import com.postaltools.tr25.logic.Money
import com.postaltools.tr25.logic.Totals
import com.postaltools.tr25.logic.Validator
import java.io.File
import java.io.FileOutputStream

/**
 * Prints a bill on A4 following the TR-25A / G.A.R.-14A form
 * ("Travelling Allowance Bill for Tour"), item by item, in English.
 */
object Tr25aPdf {

    private val RIGHT = Layout.Alignment.ALIGN_OPPOSITE
    private val CENTER = Layout.Alignment.ALIGN_CENTER

    /** Writes the PDF to [out]. Returns the number of pages. */
    fun write(bill: TaBill, out: File): Int {
        val totals = Calculator.compute(bill)
        // Pass 1 counts pages so the footer can say "Page x of N".
        val countDoc = PdfDocument()
        val pages = try { render(countDoc, bill, totals, 0) } finally { countDoc.close() }

        val doc = PdfDocument()
        try {
            render(doc, bill, totals, pages)
            out.parentFile?.mkdirs()
            FileOutputStream(out).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        return pages
    }

    /** Formats a typed amount for printing; leaves it as typed if it is not a valid number. */
    private fun amt(s: String): String =
        if (s.isBlank()) "" else Money.parse(s)?.let { Money.format(it) } ?: s

    private fun rs(s: String): String = amt(s).let { if (it.isEmpty()) "" else "Rs. $it" }

    private fun dt(s: String) = s.trim().replaceFirst(" ", "\n")

    private fun render(doc: PdfDocument, b: TaBill, t: Totals, totalPages: Int): Int {
        val cont = "T.R.-25A (continued)  —  ${b.name.ifBlank { "" }}" +
            (if (b.subBillNo.isNotBlank()) "  —  Sub-Bill No. ${b.subBillNo}" else "")
        val w = PdfWriter(doc, totalPages, cont)
        w.newPage()

        // ---------------- Heading ----------------
        w.rightText("Sub-Bill No. ${b.subBillNo.ifBlank { "............" }}", 9f, bold = true)
        w.paragraph("T.R.-25A\nG.A.R.-14A", 8f, bold = true, after = 4f)
        w.paragraph("TRAVELLING ALLOWANCE BILL FOR TOUR", 13f, bold = true, align = CENTER, after = 3f)
        w.paragraph(
            "Note— This bill should be prepared in duplicate, one for payment and the other as office copy.",
            7.5f, italic = true, align = CENTER, after = 6f,
        )
        w.rule(thick = true)
        w.paragraph("PART—A (To be filled up by Government Servant)", 9.5f, bold = true, align = CENTER, after = 6f)

        // ---------------- Items 1–4 ----------------
        w.field("1. Name", b.name, 110f)
        w.field("2. Designation", b.designation, 110f)
        w.field("3. Pay", rs(b.pay), 110f)
        w.field("4. Headquarters", b.headquarters, 110f)
        w.gap(2f)

        // ---------------- Item 5 ----------------
        w.paragraph("5. Details and purpose of journey(s) performed :-", 8.5f, bold = true, after = 3f)
        val journeys = b.journeys.filterNot { Validator.isEmptyJourney(it) }
        val halts = com.postaltools.tr25.logic.Tours.halts(t.tours)
        fun haltD(j: com.postaltools.tr25.data.JourneyRow) = j.haltDays.trim().ifBlank { halts[j]?.let { m -> if (m >= 1440) "${m / 1440}" else "" } ?: "" }
        fun haltH(j: com.postaltools.tr25.data.JourneyRow) = j.haltHours.trim().ifBlank { halts[j]?.let { m -> "%d:%02d".format((m % 1440) / 60, m % 60) } ?: "" }
        w.table(
            listOf(
                PdfWriter.Col("Date and Time", 58f, CENTER, "Departure", "1"),
                PdfWriter.Col("From", 60f, group = "Departure", num = "2"),
                PdfWriter.Col("Date and Time", 58f, CENTER, "Arrival", "3"),
                PdfWriter.Col("To", 60f, group = "Arrival", num = "4"),
                PdfWriter.Col("Mode of travel and class of accommodation", 64f, num = "5"),
                PdfWriter.Col("Fare paid\nRs.", 46f, RIGHT, num = "6"),
                PdfWriter.Col("Distance in Km. for mileage", 40f, RIGHT, num = "7"),
                PdfWriter.Col("Days", 25f, CENTER, "Duration of halt", "8"),
                PdfWriter.Col("Hours", 27f, CENTER, "Duration of halt", "8"),
                PdfWriter.Col("Purpose of Journey", 85f, num = "9"),
            ),
            journeys.map {
                listOf(dt(it.depDateTime), it.fromPlace, dt(it.arrDateTime), it.toPlace, it.modeClass,
                    amt(it.fare), it.distanceKm.trim(), haltD(it), haltH(it),
                    it.purpose + (if (it.remarks.isNotBlank()) " (${it.remarks.trim()})" else ""))
            },
        )

        // ---------------- Item 6 ----------------
        w.paragraph("6. Mode of Journey :-", 8.5f, bold = true, after = 2f)
        w.paragraph("(i) Air", 8.5f, bold = true, indent = 10f, after = 2f)
        w.field("(a) Exchange voucher arranged by officer (Yes/No)", b.airVoucherByOfficer, 240f, 20f)
        w.field("(b) Ticket/Exchange voucher arranged by", b.airTicketArrangedBy, 240f, 20f)
        w.paragraph("(ii) Rail", 8.5f, bold = true, indent = 10f, after = 2f)
        w.field("(a) Whether travelled by mail/express/ordinary train?", b.railTrainType, 240f, 20f)
        w.field("(b) Whether return tickets available? (Yes/No)", b.railReturnAvailable, 240f, 20f)
        w.field(
            "(c) If available, whether return tickets purchased? If not, state reasons.",
            listOf(b.railReturnPurchased, b.railNotPurchasedReason).filter { it.isNotBlank() }.joinToString(" — "),
            240f, 20f,
        )
        w.paragraph("(iii) Road", 8.5f, bold = true, indent = 10f, after = 2f)
        w.field(
            "Mode of conveyance used i.e. by Govt. transport/by taking a taxi, a single seat in a bus or other " +
                "public conveyance/by sharing with another Govt. servant in a car belonging to him or to a " +
                "third person to be specified.",
            b.roadConveyance, 260f, 20f,
        )

        // ---------------- Item 7 ----------------
        w.paragraph("7. Dates of absence from place of halt on account of :-", 8.5f, bold = true, after = 2f)
        w.field("(a) R.H. and C.L.", b.absenceRhCl, 240f, 20f)
        w.field("(b) Not being actually in camp on Sundays and Holidays", b.absenceNotInCamp, 240f, 20f)

        // ---------------- Item 8 ----------------
        w.paragraph(
            "8. Dates on which free board and/or lodging provided by the State or any organisation " +
                "financed by State funds :-", 8.5f, bold = true, after = 2f,
        )
        w.field("(a) Board only", b.freeBoardOnly, 240f, 20f)
        w.field("(b) Lodging only", b.freeLodgingOnly, 240f, 20f)
        w.field("(c) Board and lodging", b.freeBoardAndLodging, 240f, 20f)

        // ---------------- Item 9 ----------------
        w.paragraph(
            "9. Particulars to be furnished along with hotel receipts etc. in cases where higher rate of " +
                "D.A. is claimed for stay in hotel/other establishments providing board and/or lodging at " +
                "scheduled tariffs :-", 8.5f, bold = true, after = 3f,
        )
        if (b.hotels.isEmpty()) w.paragraph("Nil", 8.5f, indent = 20f, after = 4f) else w.table(
            listOf(
                PdfWriter.Col("From", 70f, CENTER, "Period of stay", "1"),
                PdfWriter.Col("To", 70f, CENTER, "Period of stay", "1"),
                PdfWriter.Col("Name of Hotel", 203f, num = "2"),
                PdfWriter.Col("Daily rate of lodging charged\nRs.", 85f, RIGHT, num = "3"),
                PdfWriter.Col("Total amount paid\nRs.", 95f, RIGHT, num = "4"),
            ),
            b.hotels.map { listOf(it.fromDate, it.toDate, it.hotelName, amt(it.dailyRate), amt(it.totalPaid)) },
        )

        // ---------------- Item 10 ----------------
        w.paragraph(
            "10. Particulars of journey(s) for which higher class of accommodation than the one to which " +
                "the Govt. servant is entitled was used :-", 8.5f, bold = true, after = 3f,
        )
        if (b.higherClass.isEmpty()) w.paragraph("Nil", 8.5f, indent = 20f, after = 4f) else w.table(
            listOf(
                PdfWriter.Col("Date", 55f, CENTER, num = "1"),
                PdfWriter.Col("From", 70f, group = "Name of places", num = "2"),
                PdfWriter.Col("To", 70f, group = "Name of places", num = "3"),
                PdfWriter.Col("Mode of conveyance used", 70f, num = "4"),
                PdfWriter.Col("Class to which entitled", 80f, num = "5"),
                PdfWriter.Col("Class by which travelled", 80f, num = "6"),
                PdfWriter.Col("Fare of the entitled class\nRs.", 98f, RIGHT, num = "7"),
            ),
            b.higherClass.map {
                listOf(it.date, it.fromPlace, it.toPlace, it.mode, it.classEntitled, it.classTravelled, amt(it.entitledFare))
            },
        )
        w.field(
            "If the journey(s) by higher class of accommodation had been performed with the approval of the " +
                "competent authority, No. and date of the sanction may be quoted.",
            b.higherClassSanction, 300f, 10f,
        )

        // ---------------- Item 11 ----------------
        w.paragraph("11. Details of journey(s) performed by road between places connected by rail :-",
            8.5f, bold = true, after = 3f)
        if (b.roadJourneys.isEmpty()) w.paragraph("Nil", 8.5f, indent = 20f, after = 4f) else w.table(
            listOf(
                PdfWriter.Col("Date", 80f, CENTER, num = "1"),
                PdfWriter.Col("From", 160f, group = "Name of places", num = "2"),
                PdfWriter.Col("To", 160f, group = "Name of places", num = "3"),
                PdfWriter.Col("Fare paid\nRs.", 123f, RIGHT, num = "4"),
            ),
            b.roadJourneys.map { listOf(it.date, it.fromPlace, it.toPlace, amt(it.farePaid)) },
        )

        // ---------------- Item 12 + certificate ----------------
        w.field("12. Amount of T.A. advance, if any, drawn", rs(b.advanceAmount), 240f)
        w.gap(2f)
        w.paragraph("Certified that the information given above is true to the best of my knowledge and belief.",
            8.5f, after = 2f)
        w.signatureRow("Date: ${b.claimDate}", "Signature of the Govt. Servant", space = 28f)
        w.gap(6f)

        // ---------------- PART B ----------------
        w.ensure(240f) // keep Part B together where possible
        w.rule(thick = true)
        w.paragraph("PART—B (To be filled in the Bill Section)", 9.5f, bold = true, align = CENTER, after = 5f)
        // Part B is left blank – it is filled in by the Bill Section.
        w.paragraph(
            "The net entitlement on account of travelling allowance works out to Rs. .................... " +
                "as detailed below :-", 8.5f, after = 5f,
        )
        w.amountLine("(a) Railways/Air/Bus/Steamer fare", null)
        w.amountLine("(b) Road mileage for .......... Kms. @ Rs. .......... per Km.", null)
        w.amountLine("(c) Daily allowance", null)
        listOf("(i)", "(ii)", "(iii)").forEach { w.amountLine("$it .......... days @ Rs. .......... per day", null, indent = 18f) }
        w.amountLine("(d) Actual expenses", null)
        w.amountLine("Gross amount", null, bold = true)
        w.amountLine("(e) Less amount of T.A. advance, if any, drawn vide Voucher No. .......... date ..........", null)
        w.amountLine("Net amount", null, bold = true)
        w.gap(3f)
        w.field("The Expenditures debitable to", "", 150f)

        w.signatureRow("Initials of Bill Clerk", "Signature of D.D.O.", space = 30f)
        w.signatureRow("Countersigned", "", space = 14f)
        w.signatureRow("", "Signature of Controlling Officer", space = 24f)

        // ---------------- CALCULATION OF CLAIM (separate page) ----------------
        w.newPage()
        w.paragraph("CALCULATION OF CLAIM", 13f, bold = true, align = CENTER, after = 2f)
        w.paragraph("T.A. Bill for Tour — ${b.name}, ${b.designation}, HQ: ${b.headquarters}" +
            (if (b.subBillNo.isNotBlank()) " — Sub-Bill No. ${b.subBillNo}" else ""), 8.5f, align = CENTER, after = 8f)
        val calcRows = mutableListOf<List<String>>()
        calcRows += listOf("1. Fare by rail / air / bus / other conveyance (actuals)", Money.format(t.fare))
        calcRows += listOf("2. Road mileage: ${Money.qty(t.km)} Km × Rs. ${Money.format(t.mileageRate)} per Km" +
            (if (t.kmCapped) " (limited to ${b.mileageCapKm.trim()} Km per day)" else ""), Money.format(t.mileage))
        if (t.autoDa) {
            val parts = t.autoDaLines.joinToString(" + ") {
                "${it.days} day${if (it.days > 1) "s" else ""} @ Rs. ${Money.format(it.perDay)} (${it.pct}%)"
            }
            calcRows += listOf("3. Daily allowance @ Rs. ${Money.format(t.daRate)} per day" +
                (if (parts.isNotEmpty()) ": $parts" else ""), Money.format(t.daTotal))
        } else {
            val parts = b.daSlabs.mapIndexedNotNull { i, s2 ->
                if (s2.days.isBlank() && s2.rate.isBlank()) null
                else "${s2.days.trim()} days @ Rs. ${amt(s2.rate)} = ${Money.format(t.daLines.getOrElse(i) { java.math.BigDecimal.ZERO })}"
            }.joinToString("; ")
            calcRows += listOf("3. Daily allowance" + (if (parts.isNotEmpty()) ": $parts" else ""), Money.format(t.daTotal))
        }
        calcRows += listOf("4. Actual expenses", Money.format(t.actualExpenses))
        calcRows += listOf("Gross total", Money.format(t.gross))
        calcRows += listOf("Less: T.A. advance drawn" +
            (if (b.advanceVoucherNo.isNotBlank()) " (Voucher No. ${b.advanceVoucherNo}" +
                (if (b.advanceVoucherDate.isNotBlank()) " dt. ${b.advanceVoucherDate}" else "") + ")" else ""),
            Money.format(t.advance))
        calcRows += listOf("Net amount claimed", Money.format(t.net))
        w.table(
            listOf(PdfWriter.Col("Particulars", 75f), PdfWriter.Col("Amount (Rs.)", 25f, RIGHT)),
            calcRows, size = 9f,
        )
        w.paragraph("(${com.postaltools.tr25.logic.Words.rupees(t.net)})", 9f, after = 6f)
        w.signatureRow("Date: ${b.claimDate}", "Signature of the Govt. Servant", space = 34f)

        w.finishPage()
        return w.pageNo
    }
}
