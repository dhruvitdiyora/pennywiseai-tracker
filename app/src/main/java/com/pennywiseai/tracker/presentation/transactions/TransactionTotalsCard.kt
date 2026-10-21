package com.pennywiseai.tracker.presentation.transactions

import com.pennywiseai.tracker.R
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
import androidx.compose.ui.text.style.TextOverflow
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
    // Optional fourth tile (#634): credit-card spend of the shown set. The
    // regular list totals leave it off — credit lives on the Cash-Flow card —
    // but selection totals show it, since the ask was income/expense/credit
    // for exactly the rows the user picked.
    credit: BigDecimal? = null,
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
                // Totals Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Income Column
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(
                                color = totalsTileColor(),
                                shape = RoundedCornerShape(
                                    topEnd = Spacing.xs,
                                    topStart = Spacing.lg,
                                    bottomEnd = Spacing.xs,
                                    bottomStart = Spacing.lg
                                )
                            )
                            .padding(Spacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        TotalColumn(
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = stringResource(R.string.txn_totals_income),
                                    modifier = Modifier.size(Dimensions.Icon.inline),
                                    tint = if (!isAppInDarkTheme()) income_light else income_dark
                                )
                            },
                            label = stringResource(R.string.txn_totals_income),
                            amount = CurrencyFormatter.formatCurrency(income, currency),
                            color = if (!isAppInDarkTheme()) income_light else income_dark,
                            estimated = isEstimated,
                            modifier = Modifier.alpha(incomeAlpha)
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.xxs))

                    // Expenses Column
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(
                                color = totalsTileColor(),
                                shape = RoundedCornerShape(
                                    topEnd = Spacing.xs,
                                    topStart = Spacing.xs,
                                    bottomEnd = Spacing.xs,
                                    bottomStart = Spacing.xs
                                )
                            )
                            .padding(Spacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        TotalColumn(
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = stringResource(R.string.txn_totals_expenses),
                                    modifier = Modifier.size(Dimensions.Icon.inline),
                                    tint = if (!isAppInDarkTheme()) expense_light else expense_dark
                                )
                            },
                            // Four tiles leave no room for the plural — it wraps.
                            label = if (credit != null) stringResource(R.string.txn_totals_expense) else stringResource(R.string.txn_totals_expenses),
                            amount = CurrencyFormatter.formatCurrency(expenses, currency),
                            color = if (!isAppInDarkTheme()) expense_light else expense_dark,
                            estimated = isEstimated,
                            modifier = Modifier.alpha(expenseAlpha)
                        )
                    }

                    if (credit != null) {
                        Spacer(modifier = Modifier.width(Spacing.xxs))

                        // Credit Column (selection totals only)
                        val creditColor = if (!isAppInDarkTheme()) credit_light else credit_dark
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .background(
                                    color = totalsTileColor(),
                                    shape = RoundedCornerShape(Spacing.xs)
                                )
                                .padding(Spacing.sm),
                            contentAlignment = Alignment.Center
                        ) {
                            TotalColumn(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.CreditCard,
                                        contentDescription = stringResource(R.string.txn_totals_credit),
                                        modifier = Modifier.size(Dimensions.Icon.inline),
                                        tint = creditColor
                                    )
                                },
                                label = stringResource(R.string.txn_totals_credit),
                                amount = CurrencyFormatter.formatCurrency(credit, currency),
                                color = creditColor,
                                modifier = Modifier.alpha(expenseAlpha)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(Spacing.xxs))

                    // Net Balance Column
                    val netColor = when {
                        netBalance > BigDecimal.ZERO -> if (!isAppInDarkTheme()) income_light else income_dark
                        netBalance < BigDecimal.ZERO -> if (!isAppInDarkTheme()) expense_light else expense_dark
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val netPrefix = when {
                        netBalance > BigDecimal.ZERO -> "+"
                        else -> ""
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(
                                color = totalsTileColor(),
                                shape = RoundedCornerShape(
                                    topEnd = Spacing.lg,
                                    topStart = Spacing.xs,
                                    bottomEnd = Spacing.lg,
                                    bottomStart = Spacing.xs
                                )
                            )
                            .padding(Spacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        TotalColumn(
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.SettingsEthernet,
                                    contentDescription = stringResource(R.string.txn_totals_net),
                                    modifier = Modifier.size(Dimensions.Icon.inline),
                                    tint = netColor
                                )
                            },
                            label = stringResource(R.string.txn_totals_net),
                            amount = "$netPrefix${CurrencyFormatter.formatCurrency(netBalance, currency)}",
                            color = netColor,
                            estimated = isEstimated,
                            modifier = Modifier.alpha(netAlpha)
                        )
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Text(
            text = amount,
            style = PennyWiseText.amountRow,
            color = color,
            textAlign = TextAlign.Center,
            // Wrap rather than marquee-clip: a long figure must show in full.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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
