package com.pennywiseai.tracker.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceChartTest {

    @Test
    fun `all zero values have nothing to plot`() {
        assertFalse(hasValuesToPlot(listOf(0.0, 0.0, 0.0)))
    }

    @Test
    fun `any non-zero value can be plotted`() {
        assertTrue(hasValuesToPlot(listOf(0.0, 12.5, 0.0)))
        assertTrue(hasValuesToPlot(listOf(-3.0, -3.0)))
    }
}
