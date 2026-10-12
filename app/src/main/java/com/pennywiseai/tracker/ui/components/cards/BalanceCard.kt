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
import com.pennywiseai.tracker.ui.theme.isAppInDarkTheme
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.utils.sumByCurrency
import java.math.BigDecimal

/**
 * Home hero: Cashiro's Net worth card in PennyWise's glass.
 *
 * Collapsed: net worth headline, change since the end of last cycle, currency
 * chip, show/hide eye, net-worth sparkline and a one-line "Spent this month".
 * Expanded: net-worth trend (high/low labels, "Last N days"), the This month /
 * This year / Balance trio, then PennyWise's spending breakdown and accounts so
 * nothing the spending-first hero showed is lost.
 *
 * Every figure is in [currency]; the ViewModel never adds currencies together
 * (see `NetWorthCalculator`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BalanceCard(
    userName: String = "User",
    netWorth: BigDecimal,
    netWorthAssets: BigDecimal,
    hasNetWorth: Boolean = true,
    isNetWorthApproximate: Boolean = false,
    netWorthChangePercent: Int?,
    monthlyChange: BigDecimal,
    monthlyChangePercent: Int,
    currency: String,
    currentMonthIncome: BigDecimal,
    currentMonthExpenses: BigDecimal,
    currentMonthLent: BigDecimal = BigDecimal.ZERO,
    currentMonthTotal: BigDecimal,
    currentYearTotal: BigDecimal = BigDecimal.ZERO,
    balanceHistory: List<BigDecimal>,
    isBalanceHistoryApproximate: Boolean = false,
    spendingHistory: List<BigDecimal> = emptyList(),
    lastMonthSpendingHistory: List<BigDecimal> = emptyList(),
    lastMonthSpending: BigDecimal = BigDecimal.ZERO,
    availableCurrencies: List<String>,
    isUnifiedMode: Boolean = false,
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
    val isDark = isAppInDarkTheme()

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(Dimensions.Animation.medium),
        label = "chevronRotation"
    )

    val netWorthText = when {
        isBalanceHidden -> MASK
        else -> CurrencyFormatter.formatCurrency(netWorth, currency) + if (isNetWorthApproximate) "*" else ""
    }
    val showCurrencyChip = availableCurrencies.size > 1 && !isUnifiedMode
    val toggleVisibility = {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onToggleBalanceVisibility()
    }

    // The summary card is a hero card like the account card, so it takes the
    // same rounder (extraLarge) corners rather than the standard-card radius.
    val cardShape = MaterialTheme.shapes.extraLarge

    Box(modifier = modifier.fillMaxWidth()) {
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
                // ── Hero row (both states) ── Net worth headline with the
                // net-worth sparkline beside it while collapsed.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Text(
                                text = stringResource(R.string.net_worth_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                            BalanceVisibilityToggle(
                                isBalanceHidden = isBalanceHidden,
                                onToggle = toggleVisibility,
                                buttonSize = Dimensions.Component.iconButton
                            )
                        }
                        AnimatedCurrencyText(
                            text = netWorthText,
                            style = if (isExpanded) PennyWiseText.heroAmount else PennyWiseText.amountLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        NetWorthMetaRow(
                            currency = currency,
                            showCurrencyChip = showCurrencyChip,
                            onCurrencyClick = onCurrencyClick,
                            hasNetWorth = hasNetWorth,
                            changePercent = netWorthChangePercent,
                            isBalanceHidden = isBalanceHidden,
                            isDark = isDark,
                        )
                    }

                    // Decorative: the expanded card carries the annotated chart.
                    if (!isExpanded && balanceHistory.size >= 2) {
                        MiniSparkline(
                            data = balanceHistory,
                            lineColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .width(MINI_SPARKLINE_WIDTH)
                                .height(MINI_SPARKLINE_HEIGHT)
                        )
                    }
                }

                if (isNetWorthApproximate) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.net_worth_approximate_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!isExpanded) {
                    // ── Collapsed: keep this month's spending one glance away.
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = stringResource(
                            R.string.net_worth_spent_this_month,
                            if (isBalanceHidden) MASK else CurrencyFormatter.formatCurrency(currentMonthExpenses, currency)
                        ),
                        style = PennyWiseText.amountSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.xs)
                    )
                } else {
                    // ── Expanded View ──
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(Spacing.md))
                        BalanceTrendSection(
                            balanceHistory = balanceHistory,
                            currency = currency,
                            isBalanceHidden = isBalanceHidden,
                            isApproximate = isBalanceHistoryApproximate,
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Cashiro's This month / This year / Balance trio.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrioItem(
                                label = stringResource(R.string.net_worth_this_month),
                                value = if (isBalanceHidden) MASK else CurrencyFormatter.formatCurrency(currentMonthTotal, currency),
                                modifier = Modifier.weight(1f)
                            )
                            TrioDivider()
                            TrioItem(
                                label = stringResource(R.string.net_worth_this_year),
                                value = if (isBalanceHidden) MASK else CurrencyFormatter.formatCurrency(currentYearTotal, currency),
                                modifier = Modifier.weight(1f)
                            )
                            TrioDivider()
                            TrioItem(
                                label = stringResource(R.string.net_worth_balance),
                                value = if (isBalanceHidden) MASK else CurrencyFormatter.formatCurrency(netWorthAssets, currency),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(Spacing.md))

                        SpendingSection(
                            currency = currency,
                            currentMonthIncome = currentMonthIncome,
                            currentMonthExpenses = currentMonthExpenses,
                            currentMonthLent = currentMonthLent,
                            currentMonthTotal = currentMonthTotal,
                            monthlyChange = monthlyChange,
                            monthlyChangePercent = monthlyChangePercent,
                            spendingHistory = spendingHistory,
                            lastMonthSpendingHistory = lastMonthSpendingHistory,
                            lastMonthSpending = lastMonthSpending,
                            isBalanceHidden = isBalanceHidden,
                            isDark = isDark,
                        )

                        // Accounts section (only if accounts exist)
                        if (accountBalances.isNotEmpty() || creditCards.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            AccountsSection(
                                accountBalances = accountBalances,
                                creditCards = creditCards,
                                fallbackCurrency = currency,
                                isBalanceHidden = isBalanceHidden,
                                onAccountClick = onAccountClick,
                            )
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

/**
 * "SGD ⌄  ▲ +4% this month" — currency chip plus the net-worth change. Growth
 * is good here, so up is the income colour and down the expense colour; the
 * signed text carries the meaning for anyone who can't rely on colour.
 */
@Composable
private fun NetWorthMetaRow(
    currency: String,
    showCurrencyChip: Boolean,
    onCurrencyClick: () -> Unit,
    hasNetWorth: Boolean,
    changePercent: Int?,
    isBalanceHidden: Boolean,
    isDark: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (showCurrencyChip) {
            CurrencyChip(currency = currency, onClick = onCurrencyClick)
        }
        when {
            !hasNetWorth -> Text(
                text = stringResource(R.string.net_worth_no_accounts, currency),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            changePercent != null -> {
                val isUp = changePercent >= 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isUp) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isUp) {
                            if (isDark) income_dark else income_light
                        } else {
                            if (isDark) expense_dark else expense_light
                        },
                        modifier = Modifier.size(Dimensions.Icon.inline)
                    )
                    Text(
                        text = if (isBalanceHidden) {
                            "••••"
                        } else {
                            stringResource(
                                R.string.net_worth_change_this_month,
                                if (isUp) "+" else "-",
                                kotlin.math.abs(changePercent)
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrioItem(label: String, value: String, modifier: Modifier = Modifier) {
    val valueStyle = MaterialTheme.typography.titleSmall
    Column(
        modifier = modifier.padding(horizontal = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Shrinks rather than wraps, so a long amount never breaks mid-number.
        BasicText(
            text = value,
            style = valueStyle.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = MaterialTheme.typography.labelSmall.fontSize,
                maxFontSize = valueStyle.fontSize
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrioDivider() {
    VerticalDivider(
        modifier = Modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )
}

/** PennyWise's spending-first content, kept intact inside the expanded hero. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpendingSection(
    currency: String,
    currentMonthIncome: BigDecimal,
    currentMonthExpenses: BigDecimal,
    currentMonthLent: BigDecimal,
    currentMonthTotal: BigDecimal,
    monthlyChange: BigDecimal,
    monthlyChangePercent: Int,
    spendingHistory: List<BigDecimal>,
    lastMonthSpendingHistory: List<BigDecimal>,
    lastMonthSpending: BigDecimal,
    isBalanceHidden: Boolean,
    isDark: Boolean,
) {
    val isMoreSpending = monthlyChange >= BigDecimal.ZERO
    // Inverted for spending context: more spending = red, less spending = green
    val changeColor = if (isMoreSpending) {
        if (isDark) expense_dark else expense_light
    } else {
        if (isDark) income_dark else income_light
    }
    val absPercent = kotlin.math.abs(monthlyChangePercent)
    val changeText = if (isMoreSpending) {
        stringResource(R.string.balance_card_change_more, absPercent)
    } else {
        stringResource(R.string.balance_card_change_less, absPercent)
    }
    val expenseColor = if (isDark) expense_dark else expense_light

    Text(
        text = stringResource(R.string.balance_card_spent_this_month),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    AnimatedCurrencyText(
        text = if (isBalanceHidden) MASK else CurrencyFormatter.formatCurrency(currentMonthExpenses, currency),
        style = PennyWiseText.amountLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = stringResource(
            R.string.balance_card_last_month_amount,
            if (isBalanceHidden) "••••" else CurrencyFormatter.formatCurrency(lastMonthSpending, currency)
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    SpendingMetaRow(
        changeText = if (isBalanceHidden) "••••" else changeText,
        changeColor = changeColor,
        isIncrease = isMoreSpending
    )

    if (spendingHistory.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Spacing.md))
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

    Spacer(modifier = Modifier.height(Spacing.md))

    // Summary row: Income | Expenses | Lent | Saved — with colored accent bars.
    // FlowRow so long amounts wrap onto a second line instead of crushing the
    // last column into one-character-per-line text.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        val incomeColor = if (isDark) income_dark else income_light
        val netColor = if (currentMonthTotal >= BigDecimal.ZERO) incomeColor else expenseColor

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
}

@Composable
private fun AccountsSection(
    accountBalances: List<AccountBalanceEntity>,
    creditCards: List<AccountBalanceEntity>,
    fallbackCurrency: String,
    isBalanceHidden: Boolean,
    onAccountClick: (String, String) -> Unit,
) {
    Text(
        text = stringResource(R.string.balance_card_accounts),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(Spacing.sm))

    accountBalances.forEach { account ->
        AccountRow(
            label = if (account.accountLast4 == AccountBalanceEntity.WALLET_ACCOUNT_MARKER) {
                // Mobile-money wallets have no account number — show just the service name.
                account.bankName
            } else {
                "${account.bankName} •• ${account.accountLast4}"
            },
            // The account's own (pre-converted) currency, so an unconvertible
            // balance shows honestly as "$400" rather than "MZN400".
            value = if (isBalanceHidden) MASK else account.formatBalance(),
            onClick = { onAccountClick(account.bankName, account.accountLast4) }
        )
    }

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
            AccountRow(
                label = "${card.bankName} •• ${card.accountLast4}",
                // Outstanding (what's owed), in the card's own currency (#684).
                value = if (isBalanceHidden) MASK else CurrencyFormatter.formatCurrency(card.balance, card.currency),
                onClick = { onAccountClick(card.bankName, card.accountLast4) }
            )
        }
    }

    if (accountBalances.isNotEmpty()) {
        // Total of the accounts, per currency: a mixed list reads "₹1,250 · $600"
        // instead of one meaningless cross-currency sum.
        Spacer(modifier = Modifier.height(Spacing.xs))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
                    MASK
                } else {
                    CurrencyFormatter.formatByCurrency(
                        accountBalances.sumByCurrency({ it.currency }, { it.balance }),
                        fallbackCurrency = fallbackCurrency
                    )
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AccountRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Cashiro's balance-trend block: the high value top-left with the title
 * opposite, the chart, the low value beneath, and a "Last N days" caption.
 */
@Composable
private fun BalanceTrendSection(
    balanceHistory: List<BigDecimal>,
    currency: String,
    isBalanceHidden: Boolean,
    isApproximate: Boolean,
) {
    val hasChart = balanceHistory.size >= 2
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (hasChart) {
                stringResource(
                    R.string.net_worth_trend_high,
                    if (isBalanceHidden) "••••" else CurrencyFormatter.formatCurrency(balanceHistory.max(), currency)
                )
            } else {
                ""
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
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
            Text(
                text = stringResource(R.string.balance_trend_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
    if (hasChart) {
        BalanceSparkline(
            data = balanceHistory,
            lineColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(TREND_CHART_HEIGHT),
            currency = currency,
            isBalanceHidden = isBalanceHidden,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = stringResource(
                R.string.net_worth_trend_low,
                if (isBalanceHidden) "••••" else CurrencyFormatter.formatCurrency(balanceHistory.min(), currency)
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val days = balanceHistory.size - 1
        Text(
            text = pluralStringResource(R.plurals.net_worth_trend_last_days, days, days),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
 * text. [isIncrease] is about *spending*, so the triangle takes [changeColor]
 * (red for more, green for less); the wording carries the meaning for anyone
 * who can't rely on colour.
 */
@Composable
private fun SpendingMetaRow(
    changeText: String,
    changeColor: Color,
    isIncrease: Boolean
) {
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

/** Masked stand-in for any amount while balances are hidden. */
private const val MASK = "••••••"

/** Height of the net-worth trend chart in the expanded hero. */
private val TREND_CHART_HEIGHT = Spacing.xxxl + Spacing.xxl

/** Height of the inline spending sparkline inside the expanded balance card. */
private val SPARKLINE_HEIGHT = Spacing.xxl + Spacing.xl

/** Footprint of the mini trend line beside the collapsed hero figure. */
private val MINI_SPARKLINE_WIDTH = Spacing.xxxl + Spacing.xl
private val MINI_SPARKLINE_HEIGHT = Dimensions.Component.iconButton

/** Height of the wide expand / collapse chevron (its width is [Dimensions.Icon.list]). */
private val CHEVRON_HEIGHT = Dimensions.Icon.large - Spacing.xs
