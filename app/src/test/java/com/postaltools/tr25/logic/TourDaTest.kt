package com.postaltools.tr25.logic

import com.postaltools.tr25.data.JourneyRow
import com.postaltools.tr25.data.TaBill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TourDaTest {
    private fun bd(s: String) = BigDecimal(s)
    private fun j(dep: String, from: String, arr: String, to: String, mode: String, fare: String = "", km: String = "") =
        JourneyRow(depDateTime = dep, fromPlace = from, arrDateTime = arr, toPlace = to, modeClass = mode, fare = fare, distanceKm = km)
    private fun bill(vararg js: JourneyRow) = TaBill(
        name = "Mallikarjuna", designation = "Inspector of Posts", pay = "44900", headquarters = "Gauribidanur",
        journeys = js.toList(), autoDa = true, daRate = "500", mileageRate = "15", mileageCapKm = "200",
    )

    /** User's example: left HQ 08:30, Chikkaballapur 09:10, left 16:30, reached HQ 17:30 → 9 h → 70%. */
    @Test fun sameDayTour() {
        val t = Calculator.compute(bill(
            j("18-11-2026 08:30", "Gauribidanur", "18-11-2026 09:10", "Chikkaballapur", "Own motor cycle (RMA)", km = "40"),
            j("18-11-2026 16:30", "Chikkaballapur", "18-11-2026 17:30", "Gauribidanur HO", "Own motor cycle (RMA)", km = "40"),
        ))
        assertEquals(1, t.tours.size)
        assertTrue(t.tours[0].returnedToHq)
        assertEquals(9 * 60L, t.tours[0].minutes)
        assertEquals(70, t.daDays.single().pct)
        assertEquals(bd("350.00"), t.daTotal)
        assertEquals(bd("80"), t.km)
        assertEquals(bd("1200.00"), t.mileage)
        assertEquals(bd("0.00"), t.fare)
        assertEquals(bd("1550.00"), t.gross)
    }

    /** HQ → Kolar → Bangarpet → Kolar → HQ over two days by bus: DA per calendar day, bus distance not mileage. */
    @Test fun connectingJourneysOverTwoDays() {
        val t = Calculator.compute(bill(
            j("10-11-2026 08:00", "Gauribidanur", "10-11-2026 10:30", "Kolar", "Bus - Ordinary", "120", "90"),
            j("10-11-2026 14:00", "Kolar", "10-11-2026 15:00", "Bangarpet", "Auto rickshaw", "40"),
            j("11-11-2026 16:00", "Bangarpet", "11-11-2026 17:00", "Kolar", "Bus - Ordinary", "40"),
            j("11-11-2026 17:30", "Kolar", "11-11-2026 19:30", "Gauribidanur", "Bus - Ordinary", "120", "90"),
        ))
        assertEquals(1, t.tours.size)
        assertEquals(listOf(100, 100), t.daDays.map { it.pct })   // 16 h and 19.5 h
        assertEquals(bd("1000.00"), t.daTotal)
        assertEquals(bd("320.00"), t.fare)
        assertEquals(bd("0.00"), t.mileage)
        assertEquals(bd("1320.00"), t.gross)
    }

    /** Two short tours on different days, and the 200 km per day limit. */
    @Test fun shortToursAndMileageCap() {
        val t = Calculator.compute(bill(
            j("12-11-2026 09:00", "Gauribidanur", "12-11-2026 11:00", "Gudibande", "Own motor cycle (RMA)", km = "120"),
            j("12-11-2026 13:00", "Gudibande", "12-11-2026 14:30", "Gauribidanur", "Own motor cycle (RMA)", km = "120"),
            j("13-11-2026 10:00", "Gauribidanur", "13-11-2026 10:30", "Hosur BO", "Own motor cycle (RMA)", km = "20"),
            j("13-11-2026 12:00", "Hosur BO", "13-11-2026 12:30", "Gauribidanur", "Own motor cycle (RMA)", km = "20"),
        ))
        assertEquals(2, t.tours.size)
        assertEquals(listOf(30, 30), t.daDays.map { it.pct })   // 5.5 h and 2.5 h
        assertEquals(bd("300.00"), t.daTotal)
        assertEquals(bd("240"), t.km)                           // 200 (capped from 240) + 40
        assertTrue(t.kmCapped)
        assertEquals(bd("3600.00"), t.mileage)
    }

    /** Return to HQ not entered: warned, and DA only up to the last arrival. */
    @Test fun missingReturnIsWarned() {
        val t = Calculator.compute(bill(
            j("14-11-2026 07:00", "Gauribidanur", "14-11-2026 08:30", "Chikkaballapur", "Bus - Ordinary", "60"),
        ))
        assertEquals(1, t.tours.size)
        assertTrue(!t.tours[0].returnedToHq)
        assertTrue(t.warnings.any { it.contains("return journey") })
    }

    @Test fun words() {
        assertEquals("Rupees One Thousand Five Hundred Fifty Only", Words.rupees(bd("1550")))
        assertEquals("Rupees Two Lakh Three Thousand and Fifty Paise Only", Words.rupees(bd("203000.50")))
    }
}
