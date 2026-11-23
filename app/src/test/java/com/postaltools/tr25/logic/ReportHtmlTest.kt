package com.postaltools.tr25.logic

import com.postaltools.tr25.data.Annex
import com.postaltools.tr25.data.JourneyRow
import com.postaltools.tr25.util.Period
import com.postaltools.tr25.util.ReportHeader
import com.postaltools.tr25.util.ReportHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReportHtmlTest {
    private val h = ReportHeader("GAURIBIDANUR SUB DIVISION GAURIBIDANUR-561 208", "Gauribidanur-561 208",
        "The SSPOs\nKolar Dn, Kolar-563 101", "Mallikarjuna", "Inspector Posts", "Gauribidanur Sub Division, GAURIBIDANUR -561 208")
    private val js = listOf(
        JourneyRow("03-07-2026 08:30", "Gauribidanur", "03-07-2026 09:10", "Beechaganahalli", "Bike", "", "18",
            purpose = "Carried out annual inspection", workType = "Surprise visit", office = "Beechaganahalli BO"),
        JourneyRow("03-07-2026 13:00", "Beechaganahalli", "03-07-2026 13:40", "Gauribidanur", "Bike", "", "18",
            purpose = "Carried out annual inspection"),
        JourneyRow("17-07-2026 09:00", "Gauribidanur", "17-07-2026 09:30", "Vidhurashwatha", "Bike", "", "12",
            purpose = "Annual inspection", workType = "Inspection of Branch Office", office = "Vidhurashwatha BO"),
    )

    @Test fun periods() {
        val p1 = Period(YearMonth.of(2026, 7), 1); val p2 = Period(YearMonth.of(2026, 7), 2)
        assertEquals(15, p1.days.size); assertEquals(16, p2.days.size)
        assertEquals("FOR THE FIRST FORTNIGHT OF JULY-2026", p1.title)
        assertEquals("FOR THE MONTH OF JULY-2026", Period(YearMonth.of(2026, 7), 0).title)
    }

    @Test fun diaryFirstFortnight() {
        val p = Period(YearMonth.of(2026, 7), 1)
        val html = ReportHtml.diary(p, h, js, mapOf("2026-07-01" to "At HQ attended pending works"), Annex(), word = false)
        assertTrue(html.contains("DIARY OF INSPECTOR POSTS, GAURIBIDANUR SUB DIVISION GAURIBIDANUR-561 208 FOR THE FIRST FORTNIGHT OF JULY-2026"))
        assertTrue(html.contains("At HQ attended pending works"))
        assertTrue(html.contains("Sunday Off"))                          // 05-07-2026
        assertTrue(html.contains("Surprise visit of Beechaganahalli BO"))
        assertTrue(html.contains("ANNEXURE TO THE FIRST FORTNIGHT DIARY OF JULY-2026"))
        assertTrue(html.contains("No FRAUD CASES WERE DETECTED IN THE FIRST FORTNIGHT OF JULY-2026"))
        assertTrue(html.contains("Book Antiqua"))
        assertTrue(!html.contains("Vidhurashwatha"))                      // second fortnight only
    }

    @Test fun counts() {
        val p2 = ReportHtml.journeysIn(js, Period(YearMonth.of(2026, 7), 2))
        assertEquals(1, ReportHtml.countInspections(p2))
        assertEquals(1, ReportHtml.countVisits(ReportHtml.journeysIn(js, Period(YearMonth.of(2026, 7), 1))))
        assertEquals("Sunday Off", ReportHtml.autoDetails(LocalDate.of(2026, 7, 12), js))
        assertEquals("At HQ", ReportHtml.autoDetails(LocalDate.of(2026, 7, 13), js))
    }

    @Test fun tourMomentMergesPurpose() {
        val html = ReportHtml.tourMoment(Period(YearMonth.of(2026, 7), 1), h, js, Annex(refNo = "IP GBD/Diary/FNR/JULY/2026"), word = false)
        assertTrue(html.contains("JOURNEY PARTICULARS OF INSPECTOR POSTS"))
        assertTrue(html.contains("rowspan=\"2\" style=\"vertical-align:middle"))   // two rows, same purpose
        assertTrue(html.contains("size:A4 landscape"))
        assertTrue(html.contains("No. IP GBD/Diary/FNR/JULY/2026"))
    }
}
