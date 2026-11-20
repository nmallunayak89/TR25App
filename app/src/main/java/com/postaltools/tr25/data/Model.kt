package com.postaltools.tr25.data

/**
 * Data model for one TR-25A / GAR-14A "Travelling Allowance Bill for Tour".
 *
 * Field names follow the printed form item by item. Numbers are kept as the text the
 * user typed, so a half-typed value never gets lost; the calculator parses and
 * validates them.
 */

enum class BillStatus { DRAFT, FINAL }

/** Item 5: one journey line. Columns numbered as on the form (1)–(9). */
data class JourneyRow(
    val depDateTime: String = "",   // (1) Departure – Date and Time   "dd-MM-yyyy HH:mm"
    val fromPlace: String = "",     // (2) Departure – From
    val arrDateTime: String = "",   // (3) Arrival – Date and Time
    val toPlace: String = "",       // (4) Arrival – To
    val modeClass: String = "",     // (5) Mode of travel and class of accommodation
    val fare: String = "",          // (6) Fare paid Rs
    val distanceKm: String = "",    // (7) Distance in Km for mileage
    val haltDays: String = "",      // (8) Duration of halt – Days
    val haltHours: String = "",     // (8) Duration of halt – Hours
    val purpose: String = "",       // (9) Purpose of journey
    val remarks: String = "",       // Remarks (printed with the purpose)
    val workType: String = "",      // Tour diary: inspection / case / visit …
    val office: String = "",        // Tour diary: office or establishment
    val caseRef: String = "",       // Tour diary: case No. / reference
)

/** Item 9: hotel / establishment stay where higher DA is claimed. */
data class HotelRow(
    val fromDate: String = "",      // (1) Period of stay – From
    val toDate: String = "",        // (1) Period of stay – To
    val hotelName: String = "",     // (2) Name of Hotel
    val dailyRate: String = "",     // (3) Daily rate of lodging charged Rs
    val totalPaid: String = "",     // (4) Total amount paid Rs
)

/** Item 10: journeys in a higher class than entitled. */
data class HigherClassRow(
    val date: String = "",          // (1) Date
    val fromPlace: String = "",     // (2) From
    val toPlace: String = "",       // (3) To
    val mode: String = "",          // (4) Mode of conveyance used
    val classEntitled: String = "", // (5) Class to which entitled
    val classTravelled: String = "",// (6) Class by which travelled
    val entitledFare: String = "",  // (7) Fare of the entitled class Rs
)

/** Item 11: road journeys between places connected by rail. */
data class RoadRow(
    val date: String = "",          // (1) Date
    val fromPlace: String = "",     // (2) From
    val toPlace: String = "",       // (3) To
    val farePaid: String = "",      // (4) Fare paid Rs
)

/** Part B (c): one daily allowance line – days @ Rs rate per day. */
data class DaSlab(
    val days: String = "",
    val rate: String = "",
)

data class TaBill(
    val id: Long = 0,
    val status: BillStatus = BillStatus.DRAFT,

    // Header (not printed except Sub-Bill No.) – used to find bills later.
    val officeName: String = "",
    val subBillNo: String = "",

    // ---------- PART A (To be filled up by Government Servant) ----------
    val name: String = "",               // 1
    val designation: String = "",        // 2
    val pay: String = "",                // 3
    val headquarters: String = "",       // 4
    val journeys: List<JourneyRow> = listOf(JourneyRow()), // 5

    // 6 (i) Air
    val airVoucherByOfficer: String = "",    // (a) Yes / No
    val airTicketArrangedBy: String = "",    // (b)
    // 6 (ii) Rail
    val railTrainType: String = "",          // (a) Mail / Express / Ordinary
    val railReturnAvailable: String = "",    // (b) Yes / No
    val railReturnPurchased: String = "",    // (c) Yes / No
    val railNotPurchasedReason: String = "", // (c) reasons
    // 6 (iii) Road
    val roadConveyance: String = "",

    // 7 Dates of absence from place of halt
    val absenceRhCl: String = "",            // (a) R.H. and C.L.
    val absenceNotInCamp: String = "",       // (b) Not in camp on Sundays and Holidays

    // 8 Free board and/or lodging provided
    val freeBoardOnly: String = "",          // (a)
    val freeLodgingOnly: String = "",        // (b)
    val freeBoardAndLodging: String = "",    // (c)

    val hotels: List<HotelRow> = emptyList(),              // 9
    val higherClass: List<HigherClassRow> = emptyList(),   // 10
    val higherClassSanction: String = "",                  // 10 – sanction No. and date
    val roadJourneys: List<RoadRow> = emptyList(),         // 11
    val advanceAmount: String = "",                        // 12 – TA advance drawn
    val claimDate: String = "",                            // Date under certificate "dd-MM-yyyy"

    // ---------- PART B (To be filled in the Bill Section) ----------
    val fareOverride: String = "",       // (a) blank = total of fares in item 5
    val mileageKmOverride: String = "",  // (b) blank = total distance in item 5
    val mileageRate: String = "",        // (b) Rs per Km
    val daSlabs: List<DaSlab> = listOf(DaSlab(), DaSlab(), DaSlab()), // (c)(i)(ii)(iii) – used when autoDa is off
    /** true = DA worked out from journey times (HQ → … → HQ, 30/70/100% per calendar day). */
    val autoDa: Boolean = false,
    val daRate: String = "",             // DA rate per day (100%) for auto DA
    val mileageCapKm: String = "",       // road mileage limit per day (blank = no limit)
    val actualExpenses: String = "",     // (d)
    val advanceVoucherNo: String = "",   // (e)
    val advanceVoucherDate: String = "", // (e)
    val debitableTo: String = "",        // "The Expenditures debitable to"

    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
