package com.postaltools.tr25.data

import org.json.JSONArray
import org.json.JSONObject

/** Converts a bill to/from JSON using Android's built-in org.json (no extra library). */
object BillJson {

    fun toJson(b: TaBill): JSONObject = JSONObject().apply {
        put("v", 1)
        put("status", b.status.name)
        put("officeName", b.officeName); put("subBillNo", b.subBillNo)
        put("name", b.name); put("designation", b.designation); put("pay", b.pay); put("headquarters", b.headquarters)
        put("journeys", JSONArray().apply {
            b.journeys.forEach { j ->
                put(JSONObject().apply {
                    put("depDateTime", j.depDateTime); put("fromPlace", j.fromPlace)
                    put("arrDateTime", j.arrDateTime); put("toPlace", j.toPlace)
                    put("modeClass", j.modeClass); put("fare", j.fare); put("distanceKm", j.distanceKm)
                    put("haltDays", j.haltDays); put("haltHours", j.haltHours); put("purpose", j.purpose)
                    put("remarks", j.remarks)
                })
            }
        })
        put("airVoucherByOfficer", b.airVoucherByOfficer); put("airTicketArrangedBy", b.airTicketArrangedBy)
        put("railTrainType", b.railTrainType); put("railReturnAvailable", b.railReturnAvailable)
        put("railReturnPurchased", b.railReturnPurchased); put("railNotPurchasedReason", b.railNotPurchasedReason)
        put("roadConveyance", b.roadConveyance)
        put("absenceRhCl", b.absenceRhCl); put("absenceNotInCamp", b.absenceNotInCamp)
        put("freeBoardOnly", b.freeBoardOnly); put("freeLodgingOnly", b.freeLodgingOnly)
        put("freeBoardAndLodging", b.freeBoardAndLodging)
        put("hotels", JSONArray().apply {
            b.hotels.forEach { h ->
                put(JSONObject().apply {
                    put("fromDate", h.fromDate); put("toDate", h.toDate); put("hotelName", h.hotelName)
                    put("dailyRate", h.dailyRate); put("totalPaid", h.totalPaid)
                })
            }
        })
        put("higherClass", JSONArray().apply {
            b.higherClass.forEach { r ->
                put(JSONObject().apply {
                    put("date", r.date); put("fromPlace", r.fromPlace); put("toPlace", r.toPlace)
                    put("mode", r.mode); put("classEntitled", r.classEntitled)
                    put("classTravelled", r.classTravelled); put("entitledFare", r.entitledFare)
                })
            }
        })
        put("higherClassSanction", b.higherClassSanction)
        put("roadJourneys", JSONArray().apply {
            b.roadJourneys.forEach { r ->
                put(JSONObject().apply {
                    put("date", r.date); put("fromPlace", r.fromPlace); put("toPlace", r.toPlace)
                    put("farePaid", r.farePaid)
                })
            }
        })
        put("advanceAmount", b.advanceAmount); put("claimDate", b.claimDate)
        put("fareOverride", b.fareOverride); put("mileageKmOverride", b.mileageKmOverride)
        put("mileageRate", b.mileageRate)
        put("daSlabs", JSONArray().apply {
            b.daSlabs.forEach { s -> put(JSONObject().apply { put("days", s.days); put("rate", s.rate) }) }
        })
        put("autoDa", b.autoDa); put("daRate", b.daRate); put("mileageCapKm", b.mileageCapKm)
        put("actualExpenses", b.actualExpenses)
        put("advanceVoucherNo", b.advanceVoucherNo); put("advanceVoucherDate", b.advanceVoucherDate)
        put("debitableTo", b.debitableTo)
        put("createdAt", b.createdAt); put("updatedAt", b.updatedAt)
    }

    fun fromJson(o: JSONObject, id: Long = 0): TaBill {
        fun s(k: String) = o.optString(k, "")
        fun arr(k: String): List<JSONObject> {
            val a = o.optJSONArray(k) ?: return emptyList()
            return (0 until a.length()).mapNotNull { a.optJSONObject(it) }
        }
        fun JSONObject.s(k: String) = optString(k, "")

        val journeys = arr("journeys").map {
            JourneyRow(it.s("depDateTime"), it.s("fromPlace"), it.s("arrDateTime"), it.s("toPlace"),
                it.s("modeClass"), it.s("fare"), it.s("distanceKm"), it.s("haltDays"), it.s("haltHours"), it.s("purpose"),
                it.s("remarks"))
        }
        val slabs = arr("daSlabs").map { DaSlab(it.s("days"), it.s("rate")) }
        return TaBill(
            id = id,
            status = runCatching { BillStatus.valueOf(s("status")) }.getOrDefault(BillStatus.DRAFT),
            officeName = s("officeName"), subBillNo = s("subBillNo"),
            name = s("name"), designation = s("designation"), pay = s("pay"), headquarters = s("headquarters"),
            journeys = journeys.ifEmpty { listOf(JourneyRow()) },
            airVoucherByOfficer = s("airVoucherByOfficer"), airTicketArrangedBy = s("airTicketArrangedBy"),
            railTrainType = s("railTrainType"), railReturnAvailable = s("railReturnAvailable"),
            railReturnPurchased = s("railReturnPurchased"), railNotPurchasedReason = s("railNotPurchasedReason"),
            roadConveyance = s("roadConveyance"),
            absenceRhCl = s("absenceRhCl"), absenceNotInCamp = s("absenceNotInCamp"),
            freeBoardOnly = s("freeBoardOnly"), freeLodgingOnly = s("freeLodgingOnly"),
            freeBoardAndLodging = s("freeBoardAndLodging"),
            hotels = arr("hotels").map { HotelRow(it.s("fromDate"), it.s("toDate"), it.s("hotelName"), it.s("dailyRate"), it.s("totalPaid")) },
            higherClass = arr("higherClass").map {
                HigherClassRow(it.s("date"), it.s("fromPlace"), it.s("toPlace"), it.s("mode"),
                    it.s("classEntitled"), it.s("classTravelled"), it.s("entitledFare"))
            },
            higherClassSanction = s("higherClassSanction"),
            roadJourneys = arr("roadJourneys").map { RoadRow(it.s("date"), it.s("fromPlace"), it.s("toPlace"), it.s("farePaid")) },
            advanceAmount = s("advanceAmount"), claimDate = s("claimDate"),
            fareOverride = s("fareOverride"), mileageKmOverride = s("mileageKmOverride"), mileageRate = s("mileageRate"),
            daSlabs = (slabs + List(3) { DaSlab() }).take(maxOf(3, slabs.size)),
            autoDa = o.optBoolean("autoDa", false), daRate = s("daRate"), mileageCapKm = s("mileageCapKm"),
            actualExpenses = s("actualExpenses"),
            advanceVoucherNo = s("advanceVoucherNo"), advanceVoucherDate = s("advanceVoucherDate"),
            debitableTo = s("debitableTo"),
            createdAt = o.optLong("createdAt", 0), updatedAt = o.optLong("updatedAt", 0),
        )
    }
}
