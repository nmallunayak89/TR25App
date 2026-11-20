package com.postaltools.tr25.logic

import com.postaltools.tr25.data.DaSlab
import com.postaltools.tr25.data.HotelRow
import com.postaltools.tr25.data.JourneyRow
import com.postaltools.tr25.data.TaBill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculatorTest {

    private fun bd(s: String) = BigDecimal(s)

    private fun sample() = TaBill(
        name = "A. Kumar", designation = "Postal Assistant", pay = "35400", headquarters = "Bengaluru GPO",
        journeys = listOf(
            JourneyRow("05-10-2026 08:00", "Bengaluru", "05-10-2026 14:30", "Mysuru", "Bus", "250", "", "1", "2", "Inspection"),
            JourneyRow("06-10-2026 17:00", "Mysuru", "06-10-2026 21:15", "Bengaluru", "Bus", "250.50", "", "", "", "Return"),
        ),
        daSlabs = listOf(DaSlab("1", "450"), DaSlab("0.5", "450"), DaSlab()),
        actualExpenses = "120",
        advanceAmount = "500",
        advanceVoucherNo = "12",
    )

    @Test fun indianFormatting() {
        assertEquals("0.00", Money.format(bd("0")))
        assertEquals("999.00", Money.format(bd("999")))
        assertEquals("1,000.00", Money.format(bd("1000")))
        assertEquals("12,34,567.50", Money.format(bd("1234567.5")))
        assertEquals("1,00,00,000.00", Money.format(bd("10000000")))
        assertEquals("-5,000.25", Money.format(bd("-5000.25")))
    }

    @Test fun parsing() {
        assertEquals(BigDecimal.ZERO, Money.parse(""))
        assertEquals(bd("1234.5"), Money.parse("1,234.5"))
        assertEquals(bd("12"), Money.parse("12."))
        assertNull(Money.parse("12.345"))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("-5"))
        assertNull(Money.parse("1.2.3"))
        assertNull(Money.parse("2.5", 0))
    }

    @Test fun fullBill() {
        val t = Calculator.compute(sample())
        assertTrue(t.errors.toString(), t.isValid)
        assertEquals(bd("500.50"), t.fare)                 // (a) 250 + 250.50
        assertEquals(bd("0.00"), t.mileage)                // (b) nothing entered
        assertEquals(listOf(bd("450.00"), bd("225.00"), bd("0.00")), t.daLines)
        assertEquals(bd("675.00"), t.daTotal)              // (c)
        assertEquals(bd("1295.50"), t.gross)               // 500.50 + 0 + 675 + 120
        assertEquals(bd("795.50"), t.net)                  // less 500 advance
    }

    @Test fun mileageAndOverrides() {
        val b = sample().copy(
            journeys = listOf(JourneyRow(fromPlace = "A", toPlace = "B", distanceKm = "42.5", fare = "100")),
            mileageRate = "8.75", fareOverride = "90",
        )
        val t = Calculator.compute(b)
        assertEquals(bd("90.00"), t.fare)                  // manual (a) wins
        assertEquals(bd("42.5"), t.km)
        assertEquals(bd("371.88"), t.mileage)              // 42.5 x 8.75 = 371.875 -> 371.88
        val t2 = Calculator.compute(b.copy(mileageKmOverride = "40"))
        assertEquals(bd("350.00"), t2.mileage)
    }

    @Test fun invalidValuesAreReportedAndCountAsZero() {
        val b = sample().copy(
            journeys = listOf(JourneyRow(fare = "12a", haltHours = "30", haltDays = "1.5")),
            daSlabs = listOf(DaSlab("2", "4x0")),
        )
        val t = Calculator.compute(b)
        assertFalse(t.isValid)
        assertTrue("journey.0.fare" in t.errors)
        assertTrue("journey.0.haltHours" in t.errors)
        assertTrue("journey.0.haltDays" in t.errors)
        assertTrue("da.0.rate" in t.errors)
        assertEquals(bd("0.00"), t.fare)
    }

    @Test fun negativeNetGivesWarning() {
        val t = Calculator.compute(sample().copy(advanceAmount = "5000"))
        assertEquals(bd("-3704.50"), t.net)
        assertTrue(t.warnings.any { it.contains("recoverable") })
    }

    @Test fun hotelTotal() {
        val t = Calculator.compute(sample().copy(hotels = listOf(HotelRow(totalPaid = "1200"), HotelRow(totalPaid = "800.25"))))
        assertEquals(bd("2000.25"), t.hotelTotal)
    }

    @Test fun requiredFieldsForFinal() {
        val issues = Validator.validateForFinal(TaBill())
        val keys = issues.map { it.key }
        assertTrue(keys.containsAll(listOf("name", "designation", "pay", "headquarters", "journeys")))
        assertTrue(Validator.validateForFinal(sample()).isEmpty())
    }

    @Test fun journeyDateChecks() {
        val b = sample().copy(journeys = listOf(
            JourneyRow("06-10-2026 10:00", "A", "05-10-2026 10:00", "B"),
            JourneyRow("31-02-2026 10:00", "A", "", "B"),
        ))
        val keys = Validator.validateForFinal(b).map { it.key }
        assertTrue("journey.0.arrDateTime" in keys)
        assertTrue("journey.1.depDateTime" in keys)
    }
}
