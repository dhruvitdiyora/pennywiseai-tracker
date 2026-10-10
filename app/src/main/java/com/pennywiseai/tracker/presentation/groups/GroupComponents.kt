package com.pennywiseai.tracker.presentation.groups

import com.pennywiseai.tracker.presentation.accounts.glassSheetContainerColor
import com.pennywiseai.tracker.presentation.accounts.glassRowColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.presentation.people.PersonAvatar
import com.pennywiseai.tracker.presentation.people.PersonHeaderAvatarSize
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TINTED_CONTAINER_ALPHA
import com.pennywiseai.tracker.ui.components.cards.ListItemCardV2
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.icons.iconax.Calendar
import com.pennywiseai.tracker.ui.icons.iconax.Folder2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.ReceiptItem
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.ui.theme.investment
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.Money
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

/*
 * Pieces shared by the Transaction Groups list and the group detail screen, so
 * the two read as one family with the rest of the Cashiro-style app: a summary
 * card with tinted figure tiles, tonal grouped transaction rows, and a rounded
 * bottom sheet for naming a group.
 */

// ── Summary card ──────────────────────────────────────────────────────────

private class GroupStat(
    val label: String,
    val value: String,
    val tint: Color,
    val valueColor: Color,
    val icon: ImageVector,
)

/**
 * The header card: [title] (and an optional [subtitle]) with a round folder
 * glyph, then tinted tiles for the figures that exist — an optional transaction
 * count, then what was spent, invested and received.
 *
 * The three money figures are per-currency maps (a group can mix currencies),
 * rendered with `formatByCurrency`, so a rupee and a dollar amount read
 * "₹1,250 · $600" and are never added together. Tiles wrap two to a row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GroupSummaryCard(
    title: String,
    subtitle: String?,
    expenseByCurrency: Map<String, Money>,
    investedByCurrency: Map<String, Money>,
    incomeByCurrency: Map<String, Money>,
    modifier: Modifier = Modifier,
    transactionCount: Int? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val countLabel = stringResource(R.string.group_detail_transactions)
    val expenseLabel = stringResource(R.string.group_detail_expenses)
    val investedLabel = stringResource(R.string.group_detail_invested)
    val incomeLabel = stringResource(R.string.group_detail_income)
    val expenseColor = scheme.expense
    val incomeColor = scheme.income
    val investedColor = scheme.investment

    val stats = buildList {
        if (transactionCount != null) {
            add(
                GroupStat(
                    label = countLabel,
                    value = NumberFormat.getInstance().format(transactionCount),
                    tint = scheme.primary,
                    valueColor = scheme.onSurface,
                    icon = Iconax.ReceiptItem,
                )
            )
        }
        if (expenseByCurrency.values.any { it.isPositive }) {
            val value = CurrencyFormatter.formatByCurrency(expenseByCurrency)
            add(GroupStat(expenseLabel, value, expenseColor, expenseColor, Icons.AutoMirrored.Filled.TrendingDown))
        }
        if (investedByCurrency.values.any { it.isPositive }) {
            val value = CurrencyFormatter.formatByCurrency(investedByCurrency)
            add(GroupStat(investedLabel, value, investedColor, investedColor, Icons.AutoMirrored.Filled.ShowChart))
        }
        if (incomeByCurrency.values.any { it.isPositive }) {
            val value = CurrencyFormatter.formatByCurrency(incomeByCurrency)
            add(GroupStat(incomeLabel, value, incomeColor, incomeColor, Icons.AutoMirrored.Filled.TrendingUp))
        }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = Dimensions.Padding.card,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = PennyWiseText.metadata,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(Dimensions.Icon.list)
                    .background(
                        color = scheme.tertiaryContainer.copy(alpha = Dimensions.Alpha.medium),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Iconax.Folder2,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.medium),
                    tint = scheme.onTertiaryContainer,
                )
            }
        }

        if (stats.isNotEmpty()) {
            // Up to four figures, each possibly listing several currencies — they
            // wrap two to a row instead of squeezing on a narrow screen.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                verticalArrangement = Arrangement.spacedBy(Spacing.smd),
                maxItemsInEachRow = 2,
            ) {
                stats.forEach { stat ->
                    GroupStatTile(stat = stat, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GroupStatTile(
    stat: GroupStat,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = stat.tint.copy(alpha = TINTED_CONTAINER_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.smd),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.Icon.inline)
                        .clip(CircleShape)
                        .background(stat.tint.copy(alpha = TINTED_CONTAINER_ALPHA)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = stat.icon,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.tiny),
                        tint = stat.valueColor,
                    )
                }
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stat.value,
                // A long figure steps down a size and wraps rather than being cut
                // off: a truncated amount of money is worse than a smaller one.
                style = if (stat.value.length > LONG_FIGURE_LENGTH) {
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                } else {
                    PennyWiseText.amountMedium
                },
                color = stat.valueColor,
                maxLines = 3,
            )
        }
    }
}

private const val LONG_FIGURE_LENGTH = 11

// ── Transaction row ───────────────────────────────────────────────────────

/**
 * One transaction inside a group (or in the picker that adds to one). The
 * user-written description leads when there is one, falling back to the (often
 * cryptic) merchant name; the date sits on a small chip, with the merchant
 * beside it when it is not already the title (#383).
 *
 * [trailing] is the row's own action (remove from the group, or add to it),
 * placed after the amount.
 */
@Composable
internal fun GroupTransactionRow(
    transaction: TransactionEntity,
    position: ListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showYear: Boolean = true,
    trailing: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val amountColor = when (transaction.transactionType) {
        TransactionType.EXPENSE, TransactionType.CREDIT -> scheme.expense
        TransactionType.INCOME -> scheme.income
        TransactionType.INVESTMENT -> scheme.investment
        else -> scheme.onSurface
    }
    val sign = when (transaction.transactionType) {
        TransactionType.EXPENSE, TransactionType.CREDIT -> "-"
        TransactionType.INCOME -> "+"
        else -> ""
    }
    val amountText = "$sign${CurrencyFormatter.formatCurrency(transaction.amount, transaction.currency)}"
    val description = transaction.description?.takeIf { it.isNotBlank() }
    val dateText = remember(transaction.dateTime, showYear) {
        transaction.dateTime.format(DateTimeFormatter.ofPattern(if (showYear) "d MMM yyyy" else "d MMM"))
    }
    val accessibleSubtitle = buildString {
        if (description != null) {
            append(transaction.merchantName)
            append(" · ")
        }
        append(dateText)
    }

    ListItemCardV2(
        title = description ?: transaction.merchantName,
        subtitle = accessibleSubtitle,
        amount = amountText,
        amountColor = amountColor,
        shape = position.toShape(),
        containerColor = glassRowColor(),
        onClick = onClick,
        leadingContent = {
            BrandIcon(
                merchantName = transaction.merchantName,
                category = transaction.category,
                size = Dimensions.Icon.avatarLarge,
            )
        },
        subtitleContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                SubtitleTag(
                    text = dateText,
                    color = scheme.primary,
                    icon = Iconax.Calendar,
                )
                if (description != null) {
                    Text(
                        text = transaction.merchantName,
                        style = PennyWiseText.metadata,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = amountText,
                    style = PennyWiseText.amountRow,
                    color = amountColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                trailing()
            }
        },
    )
}

// ── Group editor ──────────────────────────────────────────────────────────

/**
 * Bottom sheet for naming a group, used both to create one and to edit it: a
 * live letter avatar over two connected tonal fields (name, optional note), and
 * a full-width confirm button that stays disabled until there is a name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupEditorSheet(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialNote: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, note: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var note by remember { mutableStateOf(initialNote.orEmpty()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The fields are tonal (surfaceContainerLow), so the sheet sits one step
        // lighter to let them read as raised fields.
        containerColor = glassSheetContainerColor(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Spacing.lg,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            // Live preview: the avatar updates with the name.
            PersonAvatar(
                initials = name.trim().take(1).uppercase().ifEmpty { "?" },
                color = scheme.primary,
                size = PersonHeaderAvatarSize,
                textStyle = MaterialTheme.typography.headlineMedium,
                tinted = true,
            )

            // Name and note read as one connected block of fields.
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)) {
                TonalTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.group_name),
                    position = ListItemPosition.Top,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                TonalTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(R.string.group_note),
                    position = ListItemPosition.Bottom,
                )
            }

            Button(
                onClick = { onConfirm(name, note.ifBlank { null }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimensions.Component.fab),
                enabled = name.isNotBlank(),
            ) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
