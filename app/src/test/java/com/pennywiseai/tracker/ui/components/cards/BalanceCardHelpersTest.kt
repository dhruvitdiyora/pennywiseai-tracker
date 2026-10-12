package com.pennywiseai.tracker.ui.components.cards

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceCardHelpersTest {

    @Test
    fun `abbreviateName follows Cashiro's rules`() {
        assertEquals("JD", abbreviateName("jane doe"))
        assertEquals("JD", abbreviateName("Jane Doe Smith"))
        assertEquals("DEMO", abbreviateName("Demo"))
        assertEquals("STVN", abbreviateName("Steven"))
    }

    @Test
    fun `abbreviateName falls back to the first letters when a long name has no consonants`() {
        assertEquals("AEIO", abbreviateName("aeiouae"))
    }

    @Test
    fun `short trend passes through unchanged`() {
        val values = listOf(BigDecimal("1"), BigDecimal("2"), BigDecimal("3"))

        assertEquals(listOf(0 to BigDecimal("1"), 1 to BigDecimal("2"), 2 to BigDecimal("3")), downsampleTrend(values))
    }

    @Test
    fun `long trend is averaged into runs and keeps its order`() {
        val values = (1..180).map { BigDecimal(it) }

        val points = downsampleTrend(values, maxPoints = 50)

        assertTrue(points.size in 50..90)
        assertEquals(points.map { it.first }, points.map { it.first }.sorted())
        // Runs of three consecutive days: 1, 2, 3 average to 2 at the middle day.
        assertEquals(1, points.first().first)
        assertEquals(0, BigDecimal("2").compareTo(points.first().second))
    }

    @Test
    fun `cash flow card is skipped only when every channel is zero`() {
        assertFalse(cashFlowHasChannels(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal("0.00")))
        assertTrue(cashFlowHasChannels(BigDecimal.ZERO, BigDecimal("12"), BigDecimal.ZERO))
    }
}
