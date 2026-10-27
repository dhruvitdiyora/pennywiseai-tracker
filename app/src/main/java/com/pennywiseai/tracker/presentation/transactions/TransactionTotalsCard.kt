package com.pennywiseai.tracker.presentation.transactions

import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.pennywiseai.tracker.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun TransactionTotalsCard(
    income: BigDecimal,
    expenses: BigDecimal,
    netBalance: BigDecimal,
    currency: String,
    availableCurrencies: List<String> = emptyList(),
    onCurrencySelected: (String) -> Unit = {},
    isUnifiedMode: Boolean = false,
    isLoading: Boolean = false,
    // Optional fourth tile: a total the Income/Expenses/Net tiles leave out —
    // credit-card spend, or the transfers/investments a type filter shows
    // (#634, #893). See [extraTotalFor].
    extra: ExtraTotal? = null,
    // Optional heading, e.g. "3 selected".
    title: String? = null,
    // The figures were converted across currencies, so they are approximate:
    // each is shown as "est. …". Only for callers that already convert.
    isEstimated: Boolean = false,
    modifier: Modifier = Modifier
) {
    val incomeAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.5f else 1f,
        animationSpec = tween(300),
        label = "income_alpha"
    )

    val expenseAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.5f else 1f,
        animationSpec = tween(300),
        label = "expense_alpha"
    )

    val netAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.5f else 1f,
        animationSpec = tween(300),
        label = "net_alpha"
    )

    // The bottom inset only exists to host the optional [CurrencyPickerPill],
    // which floats at BottomEnd of the parent Box. When no pill is rendered
    // (single-currency case) keep the card flush — otherwise a phantom blank
    // band sits under the totals.
    val hasCurrencyPill = availableCurrencies.size > 1 && !isUnifiedMode
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // A glass pill (GlassCard.kt recipe): the tiles' outer corners
        // (Spacing.lg) plus the card padding (Spacing.sm) give the concentric
        // outer radius.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasCurrencyPill) Modifier.padding(bottom = Spacing.lg) else Modifier)
                .txnGlass(RoundedCornerShape(Spacing.xl))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sm)
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs)
                    )
                }
                val dark = isAppInDarkTheme()
                val netColor = when {
                    netBalance > BigDecimal.ZERO -> if (!dark) income_light else income_dark
                    netBalance < BigDecimal.ZERO -> if (!dark) expense_light else expense_dark
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val tiles = buildList {
                    add(TotalTileData(Icons.AutoMirrored.Filled.TrendingUp, stringResource(R.string.txn_totals_income),
                        CurrencyFormatter.formatCurrency(income, currency), if (!dark) income_light else income_dark, incomeAlpha, isEstimated))
                    add(TotalTileData(Icons.AutoMirrored.Filled.TrendingDown, stringResource(R.string.txn_totals_expenses),
                        CurrencyFormatter.formatCurrency(expenses, currency), if (!dark) expense_light else expense_dark, expenseAlpha, isEstimated))
                    if (extra != null) {
                        val (extraIcon, extraLabel, extraColor) = when (extra.kind) {
                            ExtraTotalKind.CREDIT -> Triple(Icons.Filled.CreditCard, stringResource(R.string.txn_totals_credit), if (dark) credit_dark else credit_light)
                            ExtraTotalKind.TRANSFER -> Triple(Icons.Filled.SwapHoriz, stringResource(R.string.txn_totals_transfer), if (dark) transfer_dark else transfer_light)
                            ExtraTotalKind.INVESTMENT -> Triple(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.txn_totals_invest), if (dark) investment_dark else investment_light)
                        }
                        add(TotalTileData(extraIcon, extraLabel, CurrencyFormatter.formatCurrency(extra.amount, currency), extraColor, expenseAlpha, isEstimated))
                    }
                    val netPrefix = if (netBalance > BigDecimal.ZERO) "+" else ""
                    add(TotalTileData(Icons.Filled.SettingsEthernet, stringResource(R.string.txn_totals_net),
                        CurrencyFormatter.signed(netPrefix, CurrencyFormatter.formatCurrency(netBalance, currency)), netColor, netAlpha, isEstimated))
                }
                // Four tiles in one row squeezed the amounts; two rows of two keep
                // every figure whole (#920).
                val rows = if (tiles.size > 3) tiles.chunked(2) else listOf(tiles)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    rows.forEachIndexed { r, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
                        ) {
                            row.forEachIndexed { c, tile ->
                                TotalTile(
                                    tile = tile,
                                    shape = tileShape(r == 0, r == rows.lastIndex, c == 0, c == row.lastIndex),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }

        if (hasCurrencyPill) {
            CurrencyPickerPill(
                selectedCurrency = currency,
                availableCurrencies = availableCurrencies,
                onCurrencySelected = onCurrencySelected,
                modifier = Modifier.padding(end = Spacing.sm)
            )
        }
    }
}

@Composable
private fun TotalColumn(
    icon: @Composable (() -> Unit)?,
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
    estimated: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            icon?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                // One line, shrinking to fit: four tiles at a large font size
                // otherwise break "Income" mid-word.
                autoSize = TextAutoSize.StepBased(
                    minFontSize = PennyWiseText.autoSizeFloor,
                    maxFontSize = MaterialTheme.typography.labelMedium.fontSize
                ),
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Text(
            text = amount,
            style = PennyWiseText.amountRow,
            color = color,
            textAlign = TextAlign.Center,
            // One line, shrinking to fit: a long figure must show in full, and
            // wrapping split "+₹1,44,051.96" mid-number.
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = PennyWiseText.autoSizeFloor,
                maxFontSize = PennyWiseText.amountRow.fontSize
            ),
        )
        if (estimated) {
            // Under the figure, not beside the label: in a three-column row the
            // badge would squeeze "Expenses" down to "Expen…".
            Text(
                text = stringResource(R.string.account_detail_estimated_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f),
                        CircleShape,
                    )
                    .padding(horizontal = Spacing.xs, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun CurrencyPickerPill(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .txnGlass(CircleShape, tint = MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = selectedCurrency,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = stringResource(R.string.txn_totals_select_currency),
                    modifier = Modifier.size(Dimensions.Icon.small),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large
        ) {
            availableCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    },
                    leadingIcon = {
                        if (currency == selectedCurrency) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
    }
}

/** A total's tile: a quiet tonal wash over the glass pill. */
@Composable
private fun totalsTileColor(): androidx.compose.ui.graphics.Color =
    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = TOTALS_TILE_ALPHA)

private const val TOTALS_TILE_ALPHA = 0.55f

enum class ExtraTotalKind { CREDIT, TRANSFER, INVESTMENT }

data class ExtraTotal(val kind: ExtraTotalKind, val amount: BigDecimal)

/**
 * The fourth tile for the totals card. Income/Expenses/Net never include
 * credit-card spend, transfers or investments (Net is cash flow), so a list of
 * only those read 0 / 0 / 0 (#893). Transfer and Investment filters show
 * their own total; the Credit filter and a selection (#634) always show the
 * credit tile, even at zero; otherwise it shows whenever the shown rows have
 * card spend.
 */
internal fun extraTotalFor(totals: FilteredTotals, typeFilter: TransactionTypeFilter, selectionMode: Boolean): ExtraTotal? = when {
    !selectionMode && typeFilter == TransactionTypeFilter.TRANSFER -> ExtraTotal(ExtraTotalKind.TRANSFER, totals.transfer)
    !selectionMode && typeFilter == TransactionTypeFilter.INVESTMENT -> ExtraTotal(ExtraTotalKind.INVESTMENT, totals.investment)
    selectionMode || typeFilter == TransactionTypeFilter.CREDIT || totals.credit.signum() != 0 -> ExtraTotal(ExtraTotalKind.CREDIT, totals.credit)
    else -> null
}

private data class TotalTileData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val amount: String,
    val color: Color,
    val alpha: Float,
    val estimated: Boolean,
)

/** Outer corners of the grid follow the glass pill (large), inner ones are small. */
private fun tileShape(top: Boolean, bottom: Boolean, start: Boolean, end: Boolean) = RoundedCornerShape(
    topStart = if (top && start) Spacing.lg else Spacing.xs,
    topEnd = if (top && end) Spacing.lg else Spacing.xs,
    bottomStart = if (bottom && start) Spacing.lg else Spacing.xs,
    bottomEnd = if (bottom && end) Spacing.lg else Spacing.xs
)

@Composable
private fun TotalTile(tile: TotalTileData, shape: RoundedCornerShape, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color = totalsTileColor(), shape = shape)
            .padding(Spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        TotalColumn(
            icon = {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.label,
                    modifier = Modifier.size(Dimensions.Icon.inline),
                    tint = tile.color
                )
            },
            label = tile.label,
            amount = tile.amount,
            color = tile.color,
            estimated = tile.estimated,
            modifier = Modifier.alpha(tile.alpha)
        )
    }
}
