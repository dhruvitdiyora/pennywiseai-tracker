package com.pennywiseai.tracker.presentation.subscriptions

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionDueStatusTest {
    private val today = LocalDate.of(2026, 9, 3)

    @Test
    fun `null date has no status date`() {
        val status = subscriptionDueStatus(null, today, isPaidThisCycle = false)

        assertEquals(SubscriptionDueStatusKind.NO_DATE, status.kind)
        assertNull(status.date)
        assertNull(status.daysUntilDue)
    }

    @Test
    fun `date before today is overdue when unpaid`() {
        val status = subscriptionDueStatus(today.minusDays(1), today, isPaidThisCycle = false)

        assertEquals(SubscriptionDueStatusKind.OVERDUE, status.kind)
        assertEquals(-1L, status.daysUntilDue)
    }

    @Test
    fun `stale date is paid rather than overdue when cycle is paid`() {
        val staleDate = today.minusDays(5)
        val status = subscriptionDueStatus(staleDate, today, isPaidThisCycle = true)

        assertEquals(SubscriptionDueStatusKind.PAID, status.kind)
        assertEquals(staleDate, status.date)
        assertEquals(-5L, status.daysUntilDue)
    }

    @Test
    fun `today and tomorrow have dedicated statuses`() {
        assertEquals(
            SubscriptionDueStatusKind.DUE_TODAY,
            subscriptionDueStatus(today, today, isPaidThisCycle = false).kind,
        )
        assertEquals(
            SubscriptionDueStatusKind.DUE_TOMORROW,
            subscriptionDueStatus(today.plusDays(1), today, isPaidThisCycle = false).kind,
        )
    }

    @Test
    fun `two through seven days are due in days`() {
        val status = subscriptionDueStatus(today.plusDays(4), today, isPaidThisCycle = false)

        assertEquals(SubscriptionDueStatusKind.DUE_IN_DAYS, status.kind)
        assertEquals(4L, status.daysUntilDue)
    }

    @Test
    fun `dates after seven days are later`() {
        val laterDate = today.plusDays(8)
        val status = subscriptionDueStatus(laterDate, today, isPaidThisCycle = false)

        assertEquals(SubscriptionDueStatusKind.LATER, status.kind)
        assertEquals(laterDate, status.date)
        assertEquals(8L, status.daysUntilDue)
    }
}
