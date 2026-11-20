package com.postaltools.tr25.logic

import com.postaltools.tr25.data.TaBill
import java.math.BigDecimal

/**
 * Part B arithmetic, exactly as laid out on TR-25A:
 *
 *   (a) Railway/Air/Bus/Steamer fare      = fares in item 5 col (6), unless entered by hand
 *   (b) Road mileage for __ Kms @ Rs __   = Km x rate   (Km = item 5 col (7), unless entered by hand)
 *   (c) Daily allowance (i)(ii)(iii)      = days x rate per day, each line
 *   (d) Actual expenses                   = as entered
 *       Gross amount                      = (a) + (b) + (c) + (d)
 *   (e) Less T.A. advance                 = item 12
 *       Net amount                        = Gross - (e)
 *
 * No allowance rates are built in: every rate is typed by the user.
 * Invalid entries count as zero and are reported in [Totals.errors] so the bill cannot be finalised.
 */
data class Totals(
    val fareAuto: BigDecimal,
    val fare: BigDecimal,
    val kmAuto: BigDecimal,
    val km: BigDecimal,
    val mileageRate: BigDecimal,
    val mileage: BigDecimal,
    val daLines: List<BigDecimal>,
    val daTotal: BigDecimal,
    val actualExpenses: BigDecimal,
    val gross: BigDecimal,
    val advance: BigDecimal,
    val net: BigDecimal,
    val hotelTotal: BigDecimal,
    /** field key -> message. Keys: "journey.<i>.fare", "da.<i>.rate", "pay", ... */
    val errors: Map<String, String>,
    /** Non-blocking notices shown before generating. */
    val warnings: List<String>,
    val tours: List<Tour> = emptyList(),
    val daDays: List<DaDay> = emptyList(),
    /** Auto DA grouped as on Part B (c): days @ rate per day. Empty when DA is entered by hand. */
    val autoDaLines: List<AutoDaLine> = emptyList(),
    val daRate: BigDecimal = BigDecimal.ZERO,
    val kmCapped: Boolean = false,
    val autoDa: Boolean = false,
) {
    val isValid get() = errors.isEmpty()
}

data class AutoDaLine(val pct: Int, val days: Int, val perDay: BigDecimal, val amount: BigDecimal)

object Calculator {

    fun compute(b: TaBill): Totals {
        val errors = linkedMapOf<String, String>()
        val warnings = mutableListOf<String>()

        fun num(key: String, text: String, label: String, decimals: Int = 2): BigDecimal {
            val v = Money.parse(text, decimals)
            if (v == null) {
                errors[key] = if (decimals == 0) "$label must be a whole number"
                else "$label must be a number with at most $decimals decimal places"
                return BigDecimal.ZERO
            }
            return v
        }

        num("pay", b.pay, "Pay")

        var fareAuto = BigDecimal.ZERO
        val kmByDay = linkedMapOf<String, BigDecimal>()
        b.journeys.forEachIndexed { i, j ->
            val n = i + 1
            fareAuto += num("journey.$i.fare", j.fare, "Journey $n fare")
            val dayKey = Dates.parseDateTime(j.depDateTime)?.toLocalDate()?.toString() ?: "row$i"
            kmByDay[dayKey] = (kmByDay[dayKey] ?: BigDecimal.ZERO) + num("journey.$i.distanceKm", j.distanceKm, "Journey $n distance")
            num("journey.$i.haltDays", j.haltDays, "Journey $n halt days", 0)
            val h = num("journey.$i.haltHours", j.haltHours, "Journey $n halt hours", 0)
            if (h > BigDecimal(23)) errors["journey.$i.haltHours"] = "Journey $n halt hours must be 0–23"
        }

        var hotelTotal = BigDecimal.ZERO
        b.hotels.forEachIndexed { i, h ->
            num("hotel.$i.dailyRate", h.dailyRate, "Hotel ${i + 1} daily rate")
            hotelTotal += num("hotel.$i.totalPaid", h.totalPaid, "Hotel ${i + 1} total paid")
        }
        b.higherClass.forEachIndexed { i, r ->
            num("higher.$i.entitledFare", r.entitledFare, "Higher class row ${i + 1} fare")
        }
        b.roadJourneys.forEachIndexed { i, r ->
            num("road.$i.farePaid", r.farePaid, "Road journey ${i + 1} fare")
        }

        // Road mileage limit per day (e.g. 200 km) – applied to the total distance of each day.
        val cap = num("mileageCapKm", b.mileageCapKm, "Mileage limit per day")
        var kmCapped = false
        val kmAuto = kmByDay.values.fold(BigDecimal.ZERO) { acc, d ->
            if (cap.signum() > 0 && d > cap) { kmCapped = true; acc + cap } else acc + d
        }

        // (a)
        val fare = if (Money.isBlank(b.fareOverride)) fareAuto
        else num("fareOverride", b.fareOverride, "Fare (a)")

        // (b)
        val km = if (Money.isBlank(b.mileageKmOverride)) kmAuto
        else num("mileageKmOverride", b.mileageKmOverride, "Mileage Km (b)")
        val rate = num("mileageRate", b.mileageRate, "Mileage rate per Km")
        val mileage = Money.paise(km * rate)
        if (km.signum() > 0 && rate.signum() == 0 && Money.isBlank(b.mileageRate)) {
            warnings += "Distance is entered but no road mileage rate, so mileage (b) is ₹0."
        }

        // (c)
        val tours = Tours.tours(b.journeys, b.headquarters)
        val daDays = Tours.daDays(tours)
        val daRate = num("daRate", b.daRate, "DA rate per day")
        val autoLines = if (!b.autoDa) emptyList() else listOf(100, 70, 30).mapNotNull { p ->
            val n = daDays.count { it.pct == p }
            if (n == 0) null else {
                val per = Money.paise(daRate * BigDecimal(p) / BigDecimal(100))
                AutoDaLine(p, n, per, Money.paise(per * BigDecimal(n)))
            }
        }
        if (b.autoDa) {
            if (daDays.isNotEmpty() && daRate.signum() == 0) warnings += "DA is calculated from journey times but the DA rate per day is blank, so DA is ₹0."
            if (b.headquarters.isNotBlank()) tours.filterNot { it.returnedToHq }.forEach {
                warnings += "Tour ${it.no}: return journey to headquarters (${b.headquarters}) is not entered."
            }
        }
        val daLines = if (b.autoDa) autoLines.map { it.amount } else b.daSlabs.mapIndexed { i, s ->
            val d = num("da.$i.days", s.days, "DA line ${i + 1} days")
            val r = num("da.$i.rate", s.rate, "DA line ${i + 1} rate")
            if (d.signum() > 0 && Money.isBlank(s.rate)) {
                warnings += "DA line ${i + 1} has days but no rate."
            }
            Money.paise(d * r)
        }
        val daTotal = daLines.fold(BigDecimal.ZERO) { a, x -> a + x }

        // (d)
        val actual = num("actualExpenses", b.actualExpenses, "Actual expenses (d)")

        val gross = Money.paise(fare + mileage + daTotal + actual)

        // (e)
        val advance = num("advanceAmount", b.advanceAmount, "T.A. advance")
        val net = Money.paise(gross - advance)
        if (net.signum() < 0) {
            warnings += "Advance (₹${Money.format(advance)}) is more than the gross amount; net is negative (amount recoverable)."
        }
        if (advance.signum() > 0 && b.advanceVoucherNo.isBlank()) {
            warnings += "An advance is entered but the voucher number in Part B (e) is blank."
        }

        return Totals(
            fareAuto = Money.paise(fareAuto), fare = Money.paise(fare),
            kmAuto = kmAuto, km = km, mileageRate = rate, mileage = mileage,
            daLines = daLines, daTotal = Money.paise(daTotal),
            actualExpenses = Money.paise(actual), gross = gross,
            advance = Money.paise(advance), net = net,
            hotelTotal = Money.paise(hotelTotal),
            errors = errors, warnings = warnings,
            tours = tours, daDays = daDays, autoDaLines = autoLines, daRate = Money.paise(daRate),
            kmCapped = kmCapped, autoDa = b.autoDa,
        )
    }
}
