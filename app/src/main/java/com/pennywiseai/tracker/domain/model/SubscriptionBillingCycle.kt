package com.pennywiseai.tracker.domain.model

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

/** The units supported by a user-defined subscription interval. */
enum class SubscriptionCycleUnit(
    val wireValue: String,
) {
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
    ;

    companion object {
        fun fromWireValue(value: String): SubscriptionCycleUnit? =
            entries.firstOrNull { it.wireValue == value }
    }
}

data class SubscriptionCycleInterval(
    val count: Long,
    val unit: SubscriptionCycleUnit,
    val isCustom: Boolean,
    val fixedLabel: String? = null,
) {
}

/**
 * Codec and calendar math for subscription billing cycles.
 *
 * Fixed cycles retain their existing human-readable values for compatibility.
 * Custom cycles use an exact, versioned wire format so malformed or future
 * values cannot be accidentally interpreted as a valid interval:
 * `PENNYWISE_CYCLE_V1|<positive-count>|<day|week|month|year>`.
 */
object SubscriptionBillingCycle {
    private const val PREFIX = "PENNYWISE_CYCLE_V1"
    private val monthlyMathContext = MathContext.DECIMAL64
    private val monthsPerYear = BigDecimal("12")
    private val weeksPerYear = BigDecimal("52")
    private val daysPerYear = BigDecimal("365.2425")

    fun encodeCustom(count: Long, unit: SubscriptionCycleUnit): String {
        require(count > 0) { "Subscription cycle count must be positive" }
        return "$PREFIX|$count|${unit.wireValue}"
    }

    /** Returns Monthly for null, legacy unknown, or malformed stored values. */
    fun parse(rawValue: String?): SubscriptionCycleInterval {
        val raw = rawValue?.trim().orEmpty()
        val fixed = when (raw.uppercase()) {
            "DAILY" -> SubscriptionCycleInterval(1, SubscriptionCycleUnit.DAY, false, "Daily")
            "WEEKLY" -> SubscriptionCycleInterval(1, SubscriptionCycleUnit.WEEK, false, "Weekly")
            "MONTHLY" -> SubscriptionCycleInterval(1, SubscriptionCycleUnit.MONTH, false, "Monthly")
            "QUARTERLY" -> SubscriptionCycleInterval(3, SubscriptionCycleUnit.MONTH, false, "Quarterly")
            "SEMI-ANNUAL", "SEMI ANNUAL", "SEMIANNUAL" ->
                SubscriptionCycleInterval(6, SubscriptionCycleUnit.MONTH, false, "Semi-Annual")
            "ANNUAL", "YEARLY" -> SubscriptionCycleInterval(1, SubscriptionCycleUnit.YEAR, false, "Annual")
            else -> null
        }
        if (fixed != null) return fixed

        val parts = raw.split('|')
        if (parts.size == 3 && parts[0] == PREFIX) {
            val count = parts[1].toLongOrNull()
            val unit = SubscriptionCycleUnit.fromWireValue(parts[2])
            if (count != null && count > 0 && unit != null) {
                return SubscriptionCycleInterval(count, unit, true)
            }
        }
        return SubscriptionCycleInterval(1, SubscriptionCycleUnit.MONTH, false, "Monthly")
    }

    fun advance(date: LocalDate, rawValue: String?, reverse: Boolean = false): LocalDate {
        val interval = parse(rawValue)
        val signedCount = if (reverse) -interval.count else interval.count
        return when (interval.unit) {
            SubscriptionCycleUnit.DAY -> date.plusDays(signedCount)
            SubscriptionCycleUnit.WEEK -> date.plusWeeks(signedCount)
            SubscriptionCycleUnit.MONTH -> date.plusMonths(signedCount)
            SubscriptionCycleUnit.YEAR -> date.plusYears(signedCount)
        }
    }

    /**
     * Converts one charge into an average monthly amount using calendar-year
     * frequencies: 365.2425 days, 52 weeks, 12 months, and 1 year.
     */
    fun monthlyEquivalent(amount: BigDecimal, rawValue: String?): BigDecimal {
        val interval = parse(rawValue)
        val count = BigDecimal(interval.count)
        val cyclesPerMonth = when (interval.unit) {
            SubscriptionCycleUnit.DAY -> daysPerYear.divide(monthsPerYear, monthlyMathContext).divide(count, monthlyMathContext)
            SubscriptionCycleUnit.WEEK -> weeksPerYear.divide(monthsPerYear, monthlyMathContext).divide(count, monthlyMathContext)
            SubscriptionCycleUnit.MONTH -> BigDecimal.ONE.divide(count, monthlyMathContext)
            SubscriptionCycleUnit.YEAR -> BigDecimal.ONE.divide(monthsPerYear.multiply(count), monthlyMathContext)
        }
        return amount.multiply(cyclesPerMonth, monthlyMathContext)
    }
}
