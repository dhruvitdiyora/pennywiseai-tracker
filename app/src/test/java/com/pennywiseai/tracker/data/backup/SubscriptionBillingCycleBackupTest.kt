package com.pennywiseai.tracker.data.backup

import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionBillingCycleBackupTest {
    @Test
    fun customBillingCycleSurvivesEntityBackupRoundTrip() {
        val rawCycle = "PENNYWISE_CYCLE_V1|45|day"
        val subscription = SubscriptionEntity(
            merchantName = "Example service",
            amount = BigDecimal("12.50"),
            nextPaymentDate = LocalDate.of(2026, 9, 7),
            billingCycle = rawCycle,
            createdAt = LocalDateTime.of(2026, 9, 1, 10, 0),
            updatedAt = LocalDateTime.of(2026, 9, 1, 10, 0),
        )

        val restored = backupJson.decodeFromString<SubscriptionEntity>(
            backupJson.encodeToString(subscription)
        )

        assertEquals(rawCycle, restored.billingCycle)
    }
}
