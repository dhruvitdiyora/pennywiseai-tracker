package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.ui.res.pluralStringResource
import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import android.view.HapticFeedbackConstants
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.LongArrow
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import dev.chrisbanes.haze.HazeState
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.income_dark
import com.pennywiseai.tracker.ui.theme.income_light
import com.pennywiseai.tracker.ui.theme.expense_dark
import com.pennywiseai.tracker.ui.theme.expense_light
import com.pennywiseai.tracker.ui.components.AnimatedCurrencyText
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.formatBalance
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BalanceCard(
    userName: String = "User",
    totalBalance: BigDecimal,
    monthlyChange: BigDecimal,
    monthlyChangePercent: Int,
    currency: String,
    currentMonthIncome: BigDecimal,
    currentMonthExpenses: BigDecimal,
    currentMonthLent: BigDecimal = BigDecimal.ZERO,
    currentMonthTotal: BigDecimal,
    balanceHistory: List<BigDecimal>,
    isBalanceHistoryApproximate: Boolean = false,
    spendingHistory: List<BigDecimal> = emptyList(),
    lastMonthSpendingHistory: List<BigDecimal> = emptyList(),
    lastMonthSpending: BigDecimal = BigDecimal.ZERO,
    availableCurrencies: List<String>,
    isUnifiedMode: Boolean = false,
    isApproximate: Boolean = false,
    onCurrencyClick: () -> Unit,
    onShowBreakdown: () -> Unit,
    isBalanceHidden: Boolean = false,
    onToggleBalanceVisibility: () -> Unit = {},
    accountBalances: List<AccountBalanceEntity> = emptyList(),
    creditCards: List<AccountBalanceEntity> = emptyList(),
    totalAvailableCredit: BigDecimal = BigDecimal.ZERO,
    onAccountClick: (String, String) -> Unit = { _, _ -> },
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier,
    blurEffects: Boolean = false,
    hazeState: HazeState = remember { HazeState() },
) {
    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val view = LocalView.current

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(Dimensions.Animation.medium),
        label = "chevronRotation"
    )

    val isDark = isSystemInDarkTheme()
    val isPositive = monthlyChange >= BigDecimal.ZERO
    // Inverted for spending context: more spending (positive) = red, less spending (negative) = green
    val changeColor = if (isPositive) {
        if (isDark) expense_dark else expense_light
    } else {
        if (isDark) income_dark else income_light
    }

    val absPercent = kotlin.math.abs(monthlyChangePercent)
    val changeText = if (isPositive) stringResource(R.string.balance_card_change_more, absPercent) else stringResource(R.string.balance_card_change_less, absPercent)

    // The summary card is a hero card like the account card, so it takes the
    // same rounder (extraLarge) corners rather than the standard-card radius.
    val cardShape = MaterialTheme.shapes.extraLarge

    Box(modifier = modifier.fillMaxWidth()) {
        // Cashiro's frosted-glass treatment (shared GlassCard recipe); the
        // content below stays PennyWise's spending-first hero.
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = tween(Dimensions.Animation.medium)
                ),
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                isExpanded = !isExpanded
            },
            shape = cardShape,
            blurEffects = blurEffects,
            hazeState = hazeState
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isExpanded) {
                    // ── Collapsed View ── Spending is the hero, with a mini
                    // sparkline beside it (Cashiro's summary-card layout).
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.xs)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                SpendingAmountHeader(
                                    amountText = if (isBalanceHidden) "••••••" else CurrencyFormatter.formatCurrency(currentMonthExpenses, currency),
                                    amountStyle = PennyWiseText.amountLarge,
                                    isBalanceHidden = isBalanceHidden,
                                    onToggleBalanceVisibility = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onToggleBalanceVisibility()
                                    },
                                    // The sparkline takes the right-hand third, so the
                                    // eye moves up beside the label to leave the amount
                                    // the full column width.
                                    eyeBesideLabel = true
                                )

                                Spacer(modifier = Modifier.height(Spacing.xs))

                                SpendingMetaRow(
                                    currency = currency,
                                    showCurrencyChip = availableCurrencies.size > 1 && !isUnifiedMode,
                                    onCurrencyClick = onCurrencyClick,
                                    changeText = if (isBalanceHidden) "••••" else changeText,
                                    changeColor = changeColor,
                                    isIncrease = isPositive
                                )
                            }

                            // Decorative: the figures it plots are all in the card.
                            if (spendingHistory.size >= 2) {
                                MiniSparkline(
                                    data = spendingHistory,
                                    lineColor = if (isDark) expense_dark else expense_light,
                                    modifier = Modifier
                                        .width(MINI_SPARKLINE_WIDTH)
                                        .height(MINI_SPARKLINE_HEIGHT)
                                )
                            }
                        }

                        // Balance line (only if accounts exist)
                        if (accountBalances.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBalanceHidden) {
                                        stringResource(R.string.balance_card_balance, "••••••")
                                    } else {
                                        stringResource(R.string.balance_card_balance, "${CurrencyFormatter.formatCurrency(totalBalance, currency)}${if (isApproximate) "*" else ""}")
                                    },
                                    style = PennyWiseText.amountSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (accountBalances.size > 1) {
                                    Text(
                                        text = pluralStringResource(R.plurals.balance_card_account_count, accountBalances.size, accountBalances.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ── Expanded View ──
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SpendingAmountHeader(
                            amountText = if (isBalanceHidden) "••••••" else CurrencyFormatter.formatCurrency(currentMonthExpenses, currency),
                            amountStyle = PennyWiseText.heroAmount,
                            isBalanceHidden = isBalanceHidden,
                            onToggleBalanceVisibility = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onToggleBalanceVisibility()
                            },
                            supportingText = stringResource(R.string.balance_card_last_month_amount, if (isBalanceHidden) "••••" else CurrencyFormatter.formatCurrency(lastMonthSpending, currency))
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        SpendingMetaRow(
                            currency = currency,
                            showCurrencyChip = availableCurrencies.size > 1 && !isUnifiedMode,
                            onCurrencyClick = onCurrencyClick,
                            changeText = if (isBalanceHidden) "••••" else changeText,
                            changeColor = changeColor,
                            isIncrease = isPositive
                        )
                        // Spending sparkline
                        if (spendingHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            val expenseColor = if (isDark) expense_dark else expense_light
                            BalanceSparkline(
                                data = spendingHistory,
                                lineColor = expenseColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(SPARKLINE_HEIGHT),
                                currency = currency,
                                isBalanceHidden = isBalanceHidden,
                                comparisonData = lastMonthSpendingHistory.ifEmpty { null },
                                comparisonLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            // Legend
                            if (lastMonthSpendingHistory.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(Spacing.sm)
                                            .background(expenseColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text(
                                        text = stringResource(R.string.balance_card_this_month),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.md))
                                    Box(
                                        modifier = Modifier
                                            .width(Spacing.smd)
                                            .height(Spacing.xxs)
                                            .background(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text(
                                        text = stringResource(R.string.balance_card_last_month),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))

                        // "This month" section label
                        Text(
                            text = stringResource(R.string.balance_card_this_month),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))

                        // Summary row: Income | Expenses | Lent | Saved — with colored
                        // accent bars. FlowRow so long amounts (e.g. a 3-char "MZN"
                        // prefix + big numbers across all four items) wrap onto a second
                        // line instead of crushing the last column into vertical,
                        // one-character-per-line text.
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            val incomeColor = if (isDark) income_dark else income_light
                            val expenseColor = if (isDark) expense_dark else expense_light
                            val netColor = if (currentMonthTotal >= BigDecimal.ZERO) {
                                if (isDark) income_dark else income_light
                            } else {
                                if (isDark) expense_dark else expense_light
                            }

                            SummaryItem(
                                label = stringResource(R.string.home_breakdown_income),
                                value = if (isBalanceHidden) "••••" else CurrencyFormatter.formatCurrency(currentMonthIncome, currency),
                                accentColor = incomeColor
                            )
                            SummaryItem(
                                label = stringResource(R.string.home_breakdown_expenses),
                                value = if (isBalanceHidden) "••••" else CurrencyFormatter.formatCurrency(currentMonthExpenses, currency),
                                accentColor = expenseColor
                            )
                            if (currentMonthLent > BigDecimal.ZERO) {
                                SummaryItem(
                                    label = stringResource(R.string.loans_direction_lent),
                                    value = if (isBalanceHidden) "••••" else CurrencyFormatter.formatCurrency(currentMonthLent, currency),
                                    accentColor = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            SummaryItem(
                                label = stringResource(R.string.home_summary_saved),
                                value = if (isBalanceHidden) "••••" else CurrencyFormatter.formatCurrency(currentMonthTotal, currency),
                                accentColor = netColor
                            )
                        }

                        if (accountBalances.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            BalanceTrendSection(
                                balanceHistory = balanceHistory,
                                currency = currency,
                                isBalanceHidden = isBalanceHidden,
                                isApproximate = isBalanceHistoryApproximate,
                            )
                        }

                        // Accounts section (only if accounts exist)
                        if (accountBalances.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))

                            Text(
                                text = stringResource(R.string.balance_card_accounts),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))

                            accountBalances.forEach { account ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAccountClick(account.bankName, account.accountLast4) }
                                        .padding(vertical = Spacing.xs),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        // Mobile-money wallets have no account number — show
                                        // just the service name (no "•• 1234" suffix).
                                        text = if (account.accountLast4 == AccountBalanceEntity.WALLET_ACCOUNT_MARKER) {
                                            account.bankName
                                        } else {
                                            "${account.bankName} •• ${account.accountLast4}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        // Use the account's own (pre-converted) currency
                                        // so an un-convertible balance shows honestly as
                                        // "$400" rather than a raw amount mislabelled with
                                        // the display currency ("MZN400").
                                        text = if (isBalanceHidden) "••••••" else account.formatBalance(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Credit Cards sub-section
                            if (creditCards.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text(
                                    text = stringResource(R.string.balance_card_credit_cards),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))

                                creditCards.forEach { card ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onAccountClick(card.bankName, card.accountLast4) }
                                            .padding(vertical = Spacing.xs),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${card.bankName} •• ${card.accountLast4}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            // Show the amount spent (outstanding) on the card, not
                                            // the remaining credit limit — for a credit card
                                            // `balance` is the outstanding debt. Format in the
                                            // card's own currency (it may be unconverted). (#684)
                                            text = if (isBalanceHidden) "••••••" else CurrencyFormatter.formatCurrency(card.balance, card.currency),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Total row
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.balance_card_total),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBalanceHidden) {
                                        "••••••"
                                    } else {
                                        "${CurrencyFormatter.formatCurrency(totalBalance, currency)}${if (isApproximate) "*" else ""}"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isApproximate) {
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    text = stringResource(R.string.balance_card_approximate_note),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                }

                // Expand / collapse affordance — Cashiro's wide, shallow chevron.
                // Kept in the card's own column (not overlaid) so it stays part
                // of the card's accessibility node.
                Icon(
                    imageVector = Iconax.LongArrow,
                    contentDescription = if (isExpanded) stringResource(R.string.card_collapse) else stringResource(R.string.card_expand),
                    // Decorative, so it recedes: a strong role at a reduced alpha.
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = Dimensions.Alpha.disabled),
                    modifier = Modifier
                        .width(Dimensions.Icon.list)
                        .height(CHEVRON_HEIGHT)
                        .rotate(chevronRotation)
                )
            }
        }
    }
}

@Composable
private fun BalanceTrendSection(
    balanceHistory: List<BigDecimal>,
    currency: String,
    isBalanceHidden: Boolean,
    isApproximate: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = stringResource(R.string.balance_trend_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.balance_trend_period),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isApproximate) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.balance_trend_approximate),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
    if (balanceHistory.size >= 2) {
        BalanceSparkline(
            data = balanceHistory,
            lineColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(SPARKLINE_HEIGHT),
            currency = currency,
            isBalanceHidden = isBalanceHidden,
        )
    } else {
        Text(
            text = stringResource(R.string.balance_trend_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (isApproximate) {
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.balance_trend_approximate_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SpendingAmountHeader(
    amountText: String,
    amountStyle: TextStyle,
    isBalanceHidden: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    supportingText: String? = null,
    /** Put the show/hide eye next to the label instead of the amount. */
    eyeBesideLabel: Boolean = false
) {
    if (eyeBesideLabel) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            SpentThisMonthLabel()
            BalanceVisibilityToggle(
                isBalanceHidden = isBalanceHidden,
                onToggle = onToggleBalanceVisibility,
                buttonSize = Dimensions.Component.iconButton
            )
        }
        AnimatedCurrencyText(
            text = amountText,
            style = amountStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    } else {
        SpentThisMonthLabel()
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            AnimatedCurrencyText(
                text = amountText,
                style = amountStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            BalanceVisibilityToggle(
                isBalanceHidden = isBalanceHidden,
                onToggle = onToggleBalanceVisibility,
                buttonSize = Dimensions.Component.minTouchTarget
            )
        }
    }
    supportingText?.let { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SpentThisMonthLabel() {
    Text(
        text = stringResource(R.string.balance_card_spent_this_month),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun BalanceVisibilityToggle(
    isBalanceHidden: Boolean,
    onToggle: () -> Unit,
    buttonSize: Dp
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(buttonSize)
    ) {
        Icon(
            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (isBalanceHidden) stringResource(R.string.balance_show) else stringResource(R.string.balance_hide),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimensions.Icon.medium)
        )
    }
}

/**
 * "▲ 12% more vs last month" — a coloured direction triangle beside plain
 * text, as in Cashiro's summary card. [isIncrease] is about *spending*, so the
 * triangle takes [changeColor] (red for more, green for less); the wording
 * carries the meaning for anyone who can't rely on colour.
 */
@Composable
private fun SpendingMetaRow(
    currency: String,
    showCurrencyChip: Boolean,
    onCurrencyClick: () -> Unit,
    changeText: String,
    changeColor: Color,
    isIncrease: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (showCurrencyChip) {
            CurrencyChip(
                currency = currency,
                onClick = onCurrencyClick
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isIncrease) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = changeColor,
                modifier = Modifier.size(Dimensions.Icon.inline)
            )
            Text(
                text = changeText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The small trend line beside the collapsed hero figure. No axes, labels or
 * touch handling — the expanded card carries the full, annotated chart.
 */
@Composable
private fun MiniSparkline(
    data: List<BigDecimal>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return
    val strokeWidth = Dimensions.Component.chartStroke

    Canvas(modifier = modifier) {
        val max = data.maxOf { it }.toFloat()
        val min = data.minOf { it }.toFloat()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val width = size.width
        val height = size.height
        // Keep the round-capped stroke fully inside the canvas at the extremes.
        val inset = strokeWidth.toPx() / 2f
        val usableHeight = height - inset * 2f

        val linePath = Path()
        data.forEachIndexed { index, value ->
            val x = index.toFloat() / (data.size - 1) * width
            val y = inset + usableHeight - ((value.toFloat() - min) / range * usableHeight)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0f))
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun CurrencyChip(
    currency: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimensions.CornerRadius.medium),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
        border = BorderStroke(
            Dimensions.Component.hairline,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.sm,
                vertical = Spacing.xs
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = currency,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.balance_card_change_currency),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(Dimensions.Icon.small)
            )
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    accentColor: Color
) {
    Row {
        // Colored left-border accent bar
        Box(
            modifier = Modifier
                .width(Spacing.xs)
                .height(Dimensions.Component.iconButton)
                .background(
                    color = accentColor,
                    shape = RoundedCornerShape(Dimensions.CornerRadius.small)
                )
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                // Keep the amount on one line — never break a "MZN52,245.26" into
                // one-character-per-line text when the column gets narrow.
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Height of the inline spending sparkline inside the expanded balance card. */
private val SPARKLINE_HEIGHT = Spacing.xxl + Spacing.xl

/** Footprint of the mini trend line beside the collapsed hero figure. */
private val MINI_SPARKLINE_WIDTH = Spacing.xxxl + Spacing.xl
private val MINI_SPARKLINE_HEIGHT = Dimensions.Component.iconButton

/** Height of the wide expand / collapse chevron (its width is [Dimensions.Icon.list]). */
private val CHEVRON_HEIGHT = Dimensions.Icon.large - Spacing.xs
