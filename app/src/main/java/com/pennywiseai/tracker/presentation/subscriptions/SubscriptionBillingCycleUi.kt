package com.pennywiseai.tracker.presentation.subscriptions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.domain.model.SubscriptionBillingCycle
import com.pennywiseai.tracker.domain.model.SubscriptionCycleUnit

internal val SubscriptionCycleUnit.labelResource: Int
    get() = when (this) {
        SubscriptionCycleUnit.DAY -> R.string.subscription_cycle_unit_day
        SubscriptionCycleUnit.WEEK -> R.string.subscription_cycle_unit_week
        SubscriptionCycleUnit.MONTH -> R.string.subscription_cycle_unit_month
        SubscriptionCycleUnit.YEAR -> R.string.subscription_cycle_unit_year
    }

@Composable
internal fun subscriptionBillingCycleLabel(rawValue: String?): String {
    val interval = SubscriptionBillingCycle.parse(rawValue)
    if (!interval.isCustom) {
        return when (interval.fixedLabel) {
            "Daily" -> stringResource(R.string.subscription_cycle_daily)
            "Weekly" -> stringResource(R.string.subscription_cycle_weekly)
            "Quarterly" -> stringResource(R.string.subscription_cycle_quarterly)
            "Semi-Annual" -> stringResource(R.string.subscription_cycle_semi_annual)
            "Annual" -> stringResource(R.string.subscription_cycle_annual)
            else -> stringResource(R.string.subscription_cycle_monthly)
        }
    }
    val count = interval.count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return when (interval.unit) {
        SubscriptionCycleUnit.DAY -> pluralStringResource(R.plurals.subscription_cycle_every_day, count, count)
        SubscriptionCycleUnit.WEEK -> pluralStringResource(R.plurals.subscription_cycle_every_week, count, count)
        SubscriptionCycleUnit.MONTH -> pluralStringResource(R.plurals.subscription_cycle_every_month, count, count)
        SubscriptionCycleUnit.YEAR -> pluralStringResource(R.plurals.subscription_cycle_every_year, count, count)
    }
}
