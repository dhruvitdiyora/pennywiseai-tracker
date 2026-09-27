package com.pennywiseai.tracker.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionBillingCycleTest {
    @Test
    fun `custom values round trip through strict versioned encoding`() {
        val encoded = SubscriptionBillingCycle.encodeCustom(3, SubscriptionCycleUnit.WEEK)

        assertEquals("PENNYWISE_CYCLE_V1|3|week", encoded)
        assertEquals(
            SubscriptionCycleInterval(3, SubscriptionCycleUnit.WEEK, isCustom = true),
            SubscriptionBillingCycle.parse(encoded),
        )
    }

    @Test
    fun `malformed and non-positive custom values safely fall back to monthly`() {
        listOf(
            "PENNYWISE_CYCLE_V1|0|day",
            "PENNYWISE_CYCLE_V1|-2|week",
            "PENNYWISE_CYCLE_V1|2|fortnight",
            "PENNYWISE_CYCLE_V1|2|week|extra",
            "custom_2_week_forever",
            null,
        ).forEach { raw ->
            assertEquals(
                SubscriptionCycleInterval(1, SubscriptionCycleUnit.MONTH, isCustom = false, fixedLabel = "Monthly"),
                SubscriptionBillingCycle.parse(raw),
            )
        }
    }

    @Test
    fun `fixed and custom intervals advance using calendar units`() {
        val start = LocalDate.of(2026, 1, 31)

        assertEquals(
            LocalDate.of(2026, 2, 28),
            SubscriptionBillingCycle.advance(start, "Monthly"),
        )
        assertEquals(
            LocalDate.of(2026, 2, 14),
            SubscriptionBillingCycle.advance(
                LocalDate.of(2026, 1, 31),
                SubscriptionBillingCycle.encodeCustom(2, SubscriptionCycleUnit.WEEK),
            ),
        )
        assertEquals(
            LocalDate.of(2025, 1, 31),
            SubscriptionBillingCycle.advance(start, "Annual", reverse = true),
        )
    }

    @Test
    fun `monthly equivalent uses cycle frequency`() {
        assertEquals(
            0,
            BigDecimal("520").compareTo(
                SubscriptionBillingCycle.monthlyEquivalent(BigDecimal("120"), "Weekly")
            ),
        )
        val quarterly = SubscriptionBillingCycle.monthlyEquivalent(BigDecimal("100"), "Quarterly")
        assertTrue(quarterly.subtract(BigDecimal("33.33333333333333")).abs() < BigDecimal("0.0000000000001"))

        val customDaily = SubscriptionBillingCycle.monthlyEquivalent(
            BigDecimal("30"),
            SubscriptionBillingCycle.encodeCustom(30, SubscriptionCycleUnit.DAY),
        )
        assertTrue(customDaily.subtract(BigDecimal("30.436875")).abs() < BigDecimal("0.0000000000001"))
    }
}
