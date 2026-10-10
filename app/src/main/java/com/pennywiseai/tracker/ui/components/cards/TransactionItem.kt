package com.pennywiseai.tracker.ui.components.cards

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.pennywiseai.tracker.ui.components.EmojiGlyph
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TINTED_CONTAINER_ALPHA
import com.pennywiseai.tracker.ui.components.legibleOn
import com.pennywiseai.tracker.ui.effects.LocalBlurEffects
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.icons.IconProvider
import com.pennywiseai.tracker.ui.icons.IconResource
import com.pennywiseai.tracker.ui.icons.iconax.Calendar
import com.pennywiseai.tracker.ui.icons.iconax.Card
import com.pennywiseai.tracker.ui.icons.iconax.Clock
import com.pennywiseai.tracker.ui.icons.iconax.DocumentText2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.RefreshCircle
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.MerchantDisplayName
import com.pennywiseai.tracker.utils.formatAmount
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A transaction row: a 48dp leading avatar, the merchant, a line of tinted
 * metadata chips, and a trailing trend icon beside the coloured amount.
 *
 * The chip line reads `[calendar Oct 1] • [Category] [Recurring] ...`: a date
 * chip, a `•`, the category named in its own colour, then any further tags
 * (type, Business, Excluded, balance, note) as neutral-text chips. The chip
 * line never wraps (Cashiro-style one-line rows): recurring is a small icon
 * beside the category, and lower-priority chips that do not fit are dropped
 * whole rather than pushed onto a second line. A merchant with no
 * brand logo gets its category colour at low alpha with the category icon in
 * that colour. The accessible description keeps its own `·`-joined sentence
 * ("date · category · Recurring · Business · Excluded · Bal …") so the contract
 * asserted by `TransactionItemScreenshotTest` does not depend on how the chips
 * are drawn.
 */
@OptIn(ExperimentalLayoutApi::class)
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

    // Accessibility sentence: the full date (and time) in the app's long-standing format.
    val dateTimeFormatter = remember(showDate) {
        DateTimeFormatter.ofPattern(if (showDate) "d MMM · h:mm a" else "h:mm a")
    }
    val dateTimeText = remember(transaction.dateTime, dateTimeFormatter) {
        transaction.dateTime.format(dateTimeFormatter)
    }
    // Date chip: a short, locale-ordered date ("Oct 1"); only the time when the
    // surrounding list already shows the day (showDate = false).
    val chipDateFormatter = remember(showDate) {
        if (showDate) shortDateFormatter() else DateTimeFormatter.ofPattern("h:mm a")
    }
    val dateChipText = remember(transaction.dateTime, chipDateFormatter) {
        transaction.dateTime.format(chipDateFormatter)
    }
    // Like Cashiro, tint the date chip by day so a run of rows reads in blocks.
    val dateChipTint = remember(transaction.dateTime, isDark) {
        val palette = if (isDark) {
            listOf(income_dark, expense_dark, credit_dark, transfer_dark, investment_dark)
        } else {
            listOf(income_light, expense_light, credit_light, transfer_light, investment_light)
        }
        palette[transaction.dateTime.toLocalDate().hashCode().mod(palette.size)]
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

    // Accessibility sentence (also the contract the screenshot test asserts).
    // It mirrors, in order, the chips drawn below.
    val subtitle = buildList {
        add(dateTimeText)
        if (hasCategory) add(transaction.category)
        typeLabel?.let(::add)
        if (transaction.isRecurring) add(recurringLabel)
        if (isEffectivelyBusiness) add(businessLabel)
        if (transaction.excludedFromAnalytics) add(excludedLabel)
        balanceAfterText?.let(::add)
        description?.let(::add)
    }.joinToString(" · ")

    val colors = MaterialTheme.colorScheme
    // The category colour (the user's own if they set one) names the category on
    // its chip; nudged only when it would not read on the row's own background.
    val rowBackground = containerColor ?: colors.surfaceContainerLow
    val categoryColor = CategoryMapping.colorFor(transaction.category)
    val categoryTextColor = remember(categoryColor, rowBackground, colors.onSurface) {
        categoryColor.legibleOn(rowBackground, towards = colors.onSurface)
    }
    // Neutral chip text is measured against the chip as actually drawn (its
    // tint washed over the row). On a selected row the container is a tonal
    // primary, so the default onSurfaceVariant can sink into a same-hue chip;
    // nudge it toward the container's own content colour until it reads.
    val chipTextTowards = if (containerColor != null) {
        contentColorFor(rowBackground).takeIf { it != Color.Unspecified } ?: colors.onSurface
    } else {
        colors.onSurface
    }
    fun chipText(tint: Color): Color =
        colors.onSurfaceVariant.legibleOn(
            background = tint.copy(alpha = TINTED_CONTAINER_ALPHA).compositeOver(rowBackground),
            towards = chipTextTowards,
            minContrast = CHIP_TEXT_MIN_CONTRAST,
        )
    val dateChipTextColor = remember(dateChipTint, rowBackground, chipTextTowards, colors.onSurfaceVariant) {
        chipText(dateChipTint)
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

    // Some parsers capture SMS boilerplate ("Not you? Call ...") as the
    // merchant; show the bank or a neutral label instead (display only).
    val unknownMerchantLabel = stringResource(R.string.txn_item_unknown_merchant)
    val merchantTitle = if (
        transaction.merchantName.isBlank() ||
        MerchantDisplayName.looksLikeSmsBody(transaction.merchantName)
    ) {
        MerchantDisplayName.titleFor(transaction.merchantName, transaction.bankName, unknownMerchantLabel)
    } else {
        merchantDisplay(transaction.merchantName) ?: transaction.merchantName
    }

    // Cashiro's frosted-glass material (GlassCard.kt) on every row; a selected
    // row keeps its tonal tint as the glass fill.
    val rowShape = listItemPosition.toShape()

    // For a paired self-transfer row, the event ("Transfer → 9999" /
    // "Transfer from 1234") is more informative than the merchant name (often
    // the user's own contact name), and stops the two legs from looking like
    // duplicate rows in the list. Falls back to merchant otherwise.
    ListItemCardV2(
        title = transferTitle ?: merchantTitle,
        subtitle = subtitle,
        subtitleContent = {
            // One line only: with maxLines = 1 FlowRow drops whole chips that do
            // not fit instead of wrapping the row onto a second line. Chips are
            // ordered by priority; the accessible subtitle still lists them all.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                itemVerticalAlignment = Alignment.CenterVertically,
                maxLines = 1,
            ) {
                // Date, category and the recurring glyph travel together so they
                // are never dropped and the dot is never stranded.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    SubtitleTag(
                        text = dateChipText,
                        color = dateChipTint,
                        textColor = dateChipTextColor,
                        icon = if (showDate) Iconax.Calendar else Iconax.Clock,
                    )
                    if (hasCategory) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            // Full-strength muted role: an alpha on onSurfaceVariant
                            // drops the separator below the contrast floor.
                            color = colors.onSurfaceVariant,
                        )
                        SubtitleTag(
                            text = transaction.category,
                            color = categoryColor,
                            textColor = categoryTextColor,
                        )
                    }
                    if (transaction.isRecurring) {
                        RecurringGlyph(tint = colors.primary)
                    }
                }
                // Credit is already marked by the card icon beside the amount, so
                // its chip is left out to keep the row on one line (Cashiro-style);
                // the accessible subtitle still says it.
                typeLabel
                    ?.takeIf { transaction.transactionType != TransactionType.CREDIT }
                    ?.let { SubtitleTag(text = it, color = amountColor, textColor = chipText(amountColor)) }
                if (isEffectivelyBusiness) {
                    SubtitleTag(
                        text = businessLabel,
                        color = colors.tertiary,
                        textColor = chipText(colors.tertiary),
                    )
                }
                if (transaction.excludedFromAnalytics) {
                    SubtitleTag(
                        text = excludedLabel,
                        color = colors.onSurfaceVariant,
                        textColor = chipText(colors.onSurfaceVariant),
                    )
                }
                balanceAfterText?.let {
                    SubtitleTag(text = it, color = colors.secondary, textColor = chipText(colors.secondary))
                }
                description?.let {
                    SubtitleTag(
                        text = it,
                        color = colors.onSurfaceVariant,
                        textColor = chipText(colors.onSurfaceVariant),
                        icon = Iconax.DocumentText2,
                    )
                }
            }
        },
        amount = "$amountPrefix$formattedAmount",
        amountColor = amountColor,
        shape = rowShape,
        contentPadding = Dimensions.Padding.cardCompact,
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onClick()
        },
        onLongClick = onLongClick,
        // The glass surface below paints the row (solid fill); the card
        // itself stays clear so its ripple lands on the glass.
        containerColor = Color.Transparent,
        modifier = modifier.glassSurface(
            shape = rowShape,
            blurEffects = LocalBlurEffects.current,
            tint = rowBackground,
        ),
        leadingContent = {
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.Icon.avatarLarge)
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
                val avatarSize = Dimensions.Icon.avatarLarge
                // IconProvider decides logo vs category icon vs the user's emoji.
                // A real brand logo keeps BrandIcon's brand-coloured tile; every
                // other merchant gets its category colour at low alpha instead of
                // a neutral placeholder.
                when (val icon = IconProvider.getTransactionIcon(transaction.merchantName, transaction.category)) {
                    is IconResource.DrawableResource -> BrandIcon(
                        merchantName = transaction.merchantName,
                        modifier = iconModifier,
                        size = avatarSize,
                        showBackground = true,
                        category = transaction.category
                    )
                    is IconResource.VectorIcon -> CategoryAvatar(
                        tint = icon.tint,
                        rowBackground = rowBackground,
                        size = avatarSize,
                        modifier = iconModifier,
                    ) { legibleTint ->
                        Icon(
                            imageVector = icon.icon,
                            contentDescription = transaction.merchantName,
                            tint = legibleTint,
                            modifier = Modifier.size(avatarSize * CATEGORY_GLYPH_FRACTION),
                        )
                    }
                    is IconResource.Emoji -> CategoryAvatar(
                        tint = icon.tint,
                        rowBackground = rowBackground,
                        size = avatarSize,
                        modifier = iconModifier,
                    ) { _ ->
                        EmojiGlyph(icon.emoji, avatarSize * CATEGORY_GLYPH_FRACTION)
                    }
                }
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

/**
 * Compact recurring marker: a tiny repeat glyph in a tinted circle, used in place
 * of a "Recurring" text chip so the row stays on one line. Decorative — the row's
 * accessible subtitle already says "Recurring".
 */
@Composable
private fun RecurringGlyph(tint: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = TINTED_CONTAINER_ALPHA))
            .padding(Spacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Iconax.RefreshCircle,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.tiny),
            tint = tint,
        )
    }
}

/** WCAG AA for small text: chip labels are labelSmall. */
private const val CHIP_TEXT_MIN_CONTRAST = 4.5f

/** The category glyph fills this share of the avatar circle (24dp in 48dp). */
private const val CATEGORY_GLYPH_FRACTION = 0.5f

/**
 * Leading circle for a merchant with no brand logo: the category colour at low
 * alpha, with the category glyph drawn in that colour ([content] receives it,
 * already nudged to stay legible on [rowBackground]).
 */
@Composable
private fun CategoryAvatar(
    tint: Color,
    rowBackground: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (glyphColor: Color) -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val glyphColor = remember(tint, rowBackground, onSurface) {
        tint.legibleOn(rowBackground, towards = onSurface)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = TINTED_CONTAINER_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        content(glyphColor)
    }
}

/**
 * Short day-and-month formatter in the user's locale order and month names
 * ("Oct 1" in en-US, "1 Oct" in en-IN). Falls back to "MMM d" if the platform
 * has no pattern for the locale.
 */
private fun shortDateFormatter(locale: Locale = Locale.getDefault()): DateTimeFormatter {
    val pattern = runCatching { DateFormat.getBestDateTimePattern(locale, "MMMd") }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: "MMM d"
    return runCatching { DateTimeFormatter.ofPattern(pattern, locale) }
        .getOrElse { DateTimeFormatter.ofPattern("MMM d", locale) }
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
