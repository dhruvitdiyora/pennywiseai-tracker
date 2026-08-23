package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.contacts.LocalMerchantDisplay
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.LocalNavAnimatedVisibilityScope
import com.pennywiseai.tracker.ui.LocalSharedTransitionScope
import com.pennywiseai.tracker.ui.sharedElementIcon
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.LocalCategoryIcons
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.parseColor
import com.pennywiseai.tracker.ui.components.generateColorFromString
import com.pennywiseai.tracker.ui.effects.horizontalScrollFade
import com.pennywiseai.tracker.ui.icons.iconsax.Calendar
import com.pennywiseai.tracker.ui.icons.iconsax.Card
import com.pennywiseai.tracker.ui.icons.iconsax.Chart2
import com.pennywiseai.tracker.ui.icons.iconsax.DocumentText2
import com.pennywiseai.tracker.ui.icons.iconsax.Iconsax
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.formatAmount
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * Fixed tint for the "Recurring" chip. Deliberately not theme-generated (see
 * [SubtitleTag] and ui-revamp doc 20 step 5) — recurring is a distinct concept
 * from any category or transaction-type colour, so it gets its own constant
 * rather than borrowing one of those palettes.
 */
private val RecurringTagColor = androidx.compose.ui.graphics.Color(0xFF5B54D6)

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    convertedAmount: BigDecimal? = null,
    displayCurrency: String? = null,
    showDate: Boolean = true,
    listItemPosition: ListItemPosition = ListItemPosition.Single,
    profileAccountKeys: Map<Long, Set<String>> = emptyMap(),
    onClick: () -> Unit = {},
    /** Optional long-press handler — used for bulk-edit selection entry. */
    onLongClick: (() -> Unit)? = null,
    /** Overrides the row's container colour (e.g. for selected state). */
    containerColor: androidx.compose.ui.graphics.Color? = null,
    modifier: Modifier = Modifier,
) {
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
        DateTimeFormatter.ofPattern(if (showDate) "d MMM \u00B7 h:mm a" else "h:mm a")
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

    // User-written description, if present, is surfaced as the LEAD segment of
    // the subtitle (kept short) — not as the title. Promoting it to title made
    // casual notes ("movie night with sarah") read as inconsistent next to
    // brand-name merchants ("Uber", "Netflix") and routinely got truncated. The
    // merchant stays the visual heading; the description is a small contextual
    // tag below. (#383)
    val description = transaction.description?.takeIf { it.isNotBlank() }

    // Transaction type is an attribute *of the amount* - whether a payment left
    // a credit card or moved between the user's own accounts - so it rides next
    // to the amount as a small tinted glyph rather than spending width in the
    // subtitle, which is the row's contended real estate (doc 21).
    //
    // INCOME and EXPENSE get no glyph: they are the common cases and are
    // already carried non-visually by the +/- prefix, so a glyph on every row
    // would be noise. The remaining three are the exceptions worth marking.
    val typeIcon: ImageVector? = when (transaction.transactionType) {
        TransactionType.CREDIT -> Iconsax.Card
        TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
        TransactionType.INVESTMENT -> Iconsax.Chart2
        TransactionType.INCOME, TransactionType.EXPENSE -> null
    }
    // Reinforces the tint the amount already carries, so the signal survives
    // for users who can't separate the two colours.
    val typeTint = when (transaction.transactionType) {
        TransactionType.CREDIT -> MaterialTheme.colorScheme.credit
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.transfer
        TransactionType.INVESTMENT -> MaterialTheme.colorScheme.investment
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // Never null when `typeIcon` is non-null: with the text label gone this is
    // the only carrier of type for a screen reader.
    val typeDescription = when (transaction.transactionType) {
        TransactionType.CREDIT -> stringResource(R.string.cd_transaction_type_credit)
        TransactionType.TRANSFER -> stringResource(R.string.cd_transaction_type_transfer)
        TransactionType.INVESTMENT -> stringResource(R.string.cd_transaction_type_investment)
        else -> null
    }

    val hasCategory = transaction.category.isNotBlank() &&
        !transaction.category.equals("Uncategorized", ignoreCase = true)

    val recurringLabel = stringResource(R.string.recurring)
    val businessLabel = stringResource(R.string.business)
    val excludedLabel = stringResource(R.string.excluded)
    val balanceAfterText = transaction.balanceAfter?.let { balance ->
        stringResource(
            R.string.balance_after_format,
            CurrencyFormatter.formatCurrency(balance, transaction.currency)
        )
    }

    // Screen-reader text: chips are a visual grouping, so TalkBack still gets
    // one plain sentence with everything in it (see ListItemCardV2's
    // `subtitleContent` kdoc). Credit/Transfer/Investment are deliberately
    // absent: they moved to the trailing glyph (doc 21), which carries its own
    // contentDescription outside this box's `clearAndSetSemantics`.
    val subtitle = remember(
        transaction, dateTimeText, isEffectivelyBusiness, description,
        recurringLabel, businessLabel, excludedLabel, balanceAfterText, hasCategory
    ) {
        buildList {
            if (description != null) add(description)
            add(dateTimeText)
            if (hasCategory) add(transaction.category)
            transaction.subcategory?.takeIf { it.isNotBlank() }?.let { add(it) }
            if (transaction.isRecurring) add(recurringLabel)
            if (isEffectivelyBusiness) add(businessLabel)
            // Mark rows the user excluded from analytics so it's visible in the
            // list which ones are skipped by spending stats (#451).
            if (transaction.excludedFromAnalytics) add(excludedLabel)
            balanceAfterText?.let { add(it) }
        }.joinToString(" \u00B7 ")
    }

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
    val transferTitle = transferTitleOverride(transaction)

    ListItemCardV2(
        title = transferTitle ?: merchantDisplay(transaction.merchantName) ?: transaction.merchantName,
        subtitle = subtitle,
        subtitleContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .horizontalScrollFade(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date · time — always present, one fixed colour (doc 20 step 5:
                // deliberately NOT Cashiro's per-date hash, which implies a
                // meaning that isn't there).
                SubtitleTag(
                    text = dateTimeText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = {
                        Icon(
                            imageVector = Iconsax.Calendar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                )
                if (hasCategory) {
                    // The user's chosen colour when the category resolves, and the
                    // name hash only as a fallback (doc 18).
                    //
                    // The hash alone rendered "Income" in RED — sitting next to a
                    // green +₹30. A hash cannot know that income should not look
                    // like a loss; the entity's colour does.
                    val categoryColor = LocalCategoryIcons.current
                        .category(transaction.category)
                        ?.let { parseColor(it.color, MaterialTheme.colorScheme.primary) }
                        ?: generateColorFromString(transaction.category)

                    SubtitleTag(text = transaction.category, color = categoryColor)
                }
                transaction.subcategory?.takeIf { it.isNotBlank() }?.let { sub ->
                    // Sits after its parent so the pair reads as a hierarchy.
                    // Resolution is allowed to miss (doc 12): a renamed or deleted
                    // subcategory still shows its stored name, just in the
                    // fallback colour.
                    val subColor = LocalCategoryIcons.current
                        .subcategory(transaction.category, sub)
                        ?.let { parseColor(it.color, MaterialTheme.colorScheme.primary) }
                        ?: generateColorFromString(sub)

                    SubtitleTag(text = sub, color = subColor)
                }
                if (transaction.isRecurring) {
                    SubtitleTag(text = recurringLabel, color = RecurringTagColor)
                }
                if (isEffectivelyBusiness) {
                    SubtitleTag(text = businessLabel, color = MaterialTheme.colorScheme.tertiary)
                }
                if (transaction.excludedFromAnalytics) {
                    SubtitleTag(text = excludedLabel, color = MaterialTheme.colorScheme.outline)
                }
                balanceAfterText?.let {
                    SubtitleTag(text = it, color = MaterialTheme.colorScheme.secondary)
                }
                if (description != null) {
                    // Glyph-only: the note's content stays available to
                    // TalkBack via `subtitle` above, it just doesn't spend
                    // visual row width as leading text (#383).
                    Icon(
                        imageVector = Iconsax.DocumentText2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        },
        amount = "$amountPrefix$formattedAmount",
        amountColor = amountColor,
        shape = listItemPosition.toShape(),
        contentPadding = Dimensions.Padding.cardCompact,
        // No haptic here: PennyWiseCardV2 fires it for every card tap
        // (doc 24). A second call is a double buzz, which reads worse than
        // none at all.
        onClick = onClick,
        onLongClick = onLongClick,
        containerColor = containerColor,
        modifier = modifier,
        leadingContent = {
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
                category = transaction.category,
                subcategory = transaction.subcategory
            )
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (typeIcon != null) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = typeDescription,
                        tint = typeTint,
                        modifier = Modifier.size(Dimensions.Icon.small)
                    )
                }
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
                        // No parentheses: brackets on a right-aligned numeric
                        // column read as an accounting negative. The size and
                        // colour difference already say "this is the original".
                        // These are two renderings of ONE amount - never summed
                        // (hard constraint 2).
                        Text(
                            text = transaction.formatAmount(),
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

/**
 * For TRANSFER rows that have `fromAccount` and `toAccount` populated,
 * synthesise a title that describes the event (which leg + the other
 * account's last-4) rather than the merchant. Returns null for any other
 * row, in which case the default merchant-as-title rendering wins.
 */
private fun transferTitleOverride(transaction: TransactionEntity): String? {
    if (transaction.transactionType != TransactionType.TRANSFER) return null
    val mine = transaction.accountNumber
    val from = transaction.fromAccount
    val to = transaction.toAccount
    return when {
        from != null && to != null && mine == from -> "Transfer → ${to.takeLast(4)}"
        from != null && to != null && mine == to -> "Transfer from ${from.takeLast(4)}"
        to != null && mine != to -> "Transfer → ${to.takeLast(4)}"
        from != null && mine != from -> "Transfer from ${from.takeLast(4)}"
        else -> null
    }
}
