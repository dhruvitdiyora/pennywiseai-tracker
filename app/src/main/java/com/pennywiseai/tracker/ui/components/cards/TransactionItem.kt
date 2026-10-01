package com.pennywiseai.tracker.ui.components.cards

import android.content.res.Resources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import android.view.HapticFeedbackConstants
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.contacts.LocalMerchantDisplay
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.LocalNavAnimatedVisibilityScope
import com.pennywiseai.tracker.ui.LocalSharedTransitionScope
import com.pennywiseai.tracker.ui.sharedElementIcon
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.icons.iconax.Card
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.formatAmount
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * A transaction row: brand logo, merchant, one metadata line, and a trailing
 * trend icon beside the coloured amount.
 *
 * The visible metadata line is plain text with `•` separators
 * ("27 Feb • 4:36 PM • Recurring"). The accessible description keeps its own
 * `·`-joined sentence ("date · category · Recurring · Business · Excluded ·
 * Bal …") so the contract asserted by `TransactionItemScreenshotTest` does not
 * depend on how the line is drawn.
 */
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    convertedAmount: BigDecimal? = null,
    displayCurrency: String? = null,
    showDate: Boolean = true,
    showTypeLabel: Boolean = true,
    listItemPosition: ListItemPosition = ListItemPosition.Single,
    profileAccountKeys: Map<Long, Set<String>> = emptyMap(),
    onClick: () -> Unit = {},
    /** Optional long-press handler — used for bulk-edit selection entry. */
    onLongClick: (() -> Unit)? = null,
    /** Overrides the row's container colour (e.g. for selected state). */
    containerColor: androidx.compose.ui.graphics.Color? = null,
    /**
     * Bulk-selection mode: the brand logo is swapped for a selection
     * indicator reflecting [isSelected]. Tapping still goes through [onClick].
     */
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val amountColor = remember(transaction.transactionType, isDark) {
        when (transaction.transactionType) {
            TransactionType.INCOME -> if (!isDark) income_light else income_dark
            TransactionType.EXPENSE -> if (!isDark) expense_light else expense_dark
            TransactionType.CREDIT -> if (!isDark) credit_light else credit_dark
            TransactionType.TRANSFER -> if (!isDark) transfer_light else transfer_dark
            TransactionType.INVESTMENT -> if (!isDark) investment_light else investment_dark
        }
    }

    val dateTimeFormatter = remember(showDate) {
        DateTimeFormatter.ofPattern(if (showDate) "d MMM · h:mm a" else "h:mm a")
    }
    val dateTimeText = remember(transaction.dateTime, dateTimeFormatter) {
        transaction.dateTime.format(dateTimeFormatter)
    }

    val isEffectivelyBusiness = remember(transaction, profileAccountKeys) {
        val effectiveProfileId = transaction.profileId ?: run {
            if (transaction.bankName != null && transaction.accountNumber != null) {
                val key = "${transaction.bankName}_${transaction.accountNumber}"
                profileAccountKeys.entries.firstOrNull { (_, keys) -> keys.contains(key) }?.key
            } else null
        }
        effectiveProfileId == ProfileEntity.BUSINESS_ID
    }

    val description = transaction.description?.takeIf { it.isNotBlank() }
    val resources = LocalContext.current.resources
    val transferTitle = transferTitleOverride(transaction, resources)
    val hasCategory = transaction.category.isNotBlank() &&
        !transaction.category.equals("Uncategorized", ignoreCase = true)
    val creditLabel = stringResource(R.string.txn_item_tag_credit)
    val transferLabel = stringResource(R.string.txn_item_tag_transfer)
    val investmentLabel = stringResource(R.string.txn_item_tag_investment)
    val recurringLabel = stringResource(R.string.txn_item_tag_recurring)
    val businessLabel = stringResource(R.string.txn_item_tag_business)
    // Mark rows the user excluded from analytics so it's visible in the
    // list which ones are skipped by spending stats (#451).
    val excludedLabel = stringResource(R.string.txn_item_tag_excluded)
    val typeLabel = if (showTypeLabel) {
        when (transaction.transactionType) {
            TransactionType.CREDIT -> creditLabel
            TransactionType.TRANSFER -> transferLabel.takeIf { transferTitle == null }
            TransactionType.INVESTMENT -> investmentLabel
            TransactionType.INCOME, TransactionType.EXPENSE -> null
        }
    } else {
        null
    }
    val balanceAfterText = transaction.balanceAfter?.let { balance ->
        stringResource(
            R.string.txn_item_balance_after,
            CurrencyFormatter.formatCurrency(balance, transaction.currency),
        )
    }

    // The same metadata, in the same order, feeds both renderings below.
    fun metadataParts(dateTimePart: String): List<String> = buildList {
        add(dateTimePart)
        if (hasCategory) add(transaction.category)
        typeLabel?.let(::add)
        if (transaction.isRecurring) add(recurringLabel)
        if (isEffectivelyBusiness) add(businessLabel)
        if (transaction.excludedFromAnalytics) add(excludedLabel)
        balanceAfterText?.let(::add)
        description?.let(::add)
    }

    // Accessibility sentence (also the contract the screenshot test asserts).
    val subtitle = metadataParts(dateTimeText).joinToString(" · ")
    // What is drawn: "27 Feb • 4:36 PM • Recurring".
    val visibleSubtitle = metadataParts(dateTimeText.replace(" · ", " • "))
        .joinToString(" • ")

    val amountPrefix = remember(transaction.transactionType) {
        when (transaction.transactionType) {
            TransactionType.INCOME -> "+"
            TransactionType.EXPENSE, TransactionType.CREDIT, TransactionType.INVESTMENT -> "-"
            TransactionType.TRANSFER -> ""
        }
    }

    val formattedAmount = if (convertedAmount != null && displayCurrency != null) {
        CurrencyFormatter.formatCurrency(convertedAmount, displayCurrency)
    } else {
        transaction.formatAmount()
    }

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val merchantDisplay = LocalMerchantDisplay.current

    // For a paired self-transfer row, the event ("Transfer → 9999" /
    // "Transfer from 1234") is more informative than the merchant name (often
    // the user's own contact name), and stops the two legs from looking like
    // duplicate rows in the list. Falls back to merchant otherwise.
    ListItemCardV2(
        title = transferTitle ?: merchantDisplay(transaction.merchantName) ?: transaction.merchantName,
        subtitle = subtitle,
        subtitleContent = {
            // Plain, wrapping text rather than clipped tags: a long category or
            // note flows onto a second line instead of being cut off.
            Text(
                text = visibleSubtitle,
                style = PennyWiseText.metadata,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        amount = "$amountPrefix$formattedAmount",
        amountColor = amountColor,
        shape = listItemPosition.toShape(),
        contentPadding = Dimensions.Padding.cardCompact,
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onClick()
        },
        onLongClick = onLongClick,
        containerColor = containerColor,
        modifier = modifier,
        leadingContent = {
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.Icon.list)
                        .semantics { selected = isSelected },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSelected) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Outlined.RadioButtonUnchecked
                        },
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.medium),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                }
            } else {
                val iconModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        sharedElementIcon(
                            key = "brand_icon_${transaction.id}",
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else {
                    Modifier
                }
                BrandIcon(
                    merchantName = transaction.merchantName,
                    modifier = iconModifier,
                    size = Dimensions.Icon.list,
                    showBackground = true,
                    category = transaction.category
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Decorative: the sign, colour and type label already carry the
                // direction, so the glyph adds no announcement of its own.
                Icon(
                    imageVector = transactionTypeIcon(transaction.transactionType),
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small),
                    tint = amountColor,
                )
                if (convertedAmount != null && displayCurrency != null) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
                    ) {
                        Text(
                            text = "$amountPrefix${CurrencyFormatter.formatCurrency(convertedAmount, displayCurrency)}",
                            style = PennyWiseText.amountRow,
                            color = amountColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "(${transaction.formatAmount()})",
                            style = PennyWiseText.amountSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "$amountPrefix$formattedAmount",
                        style = PennyWiseText.amountRow,
                        color = amountColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    )
}

/** The glyph shown beside a row's amount, by transaction type. */
private fun transactionTypeIcon(type: TransactionType): ImageVector = when (type) {
    TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
    TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
    TransactionType.CREDIT -> Iconax.Card
    TransactionType.TRANSFER -> Icons.Filled.SwapHoriz
    TransactionType.INVESTMENT -> Icons.AutoMirrored.Filled.ShowChart
}

/**
 * For TRANSFER rows that have `fromAccount` and `toAccount` populated,
 * synthesise a title that describes the event (which leg + the other
 * account's last-4) rather than the merchant. Returns null for any other
 * row, in which case the default merchant-as-title rendering wins.
 */
private fun transferTitleOverride(transaction: TransactionEntity, resources: Resources): String? {
    if (transaction.transactionType != TransactionType.TRANSFER) return null
    val mine = transaction.accountNumber
    val from = transaction.fromAccount
    val to = transaction.toAccount
    return when {
        from != null && to != null && mine == from -> resources.getString(R.string.txn_item_transfer_to, to.takeLast(4))
        from != null && to != null && mine == to -> resources.getString(R.string.txn_item_transfer_from, from.takeLast(4))
        to != null && mine != to -> resources.getString(R.string.txn_item_transfer_to, to.takeLast(4))
        from != null && mine != from -> resources.getString(R.string.txn_item_transfer_from, from.takeLast(4))
        else -> null
    }
}
