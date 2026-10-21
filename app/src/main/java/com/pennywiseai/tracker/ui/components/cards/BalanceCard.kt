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
import androidx.compose.foundation.layout.wrapContentHeight
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
import com.pennywiseai.tracker.ui.components.legibleOn
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.MathContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home hero: Cashiro's Net worth card (`BalanceCard.kt`) in PennyWise's glass.
 *
 * Collapsed (Cashiro's layout): one bold headline with "▲ +4% this month"
 * under it and the net-worth sparkline at the right. The headline is the net
 * worth; while balances are hidden it is Cashiro's abbreviated name instead of
 * a row of dots, so the hidden card reads exactly like Cashiro's.
 * Expanded: the abbreviated name with "Name • Net worth" (plus the show/hide
 * eye) on the left, the net worth and currency chip on the right, Cashiro's
 * balance-trend chart, the This month / This year / Balance trio, then
 * PennyWise's spending breakdown and accounts so nothing is lost.
 *
 * Every figure is in [currency]; the ViewModel never adds currencies together
 * (see `NetWorthCalculator`).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
    balanceHistoryEndDate: LocalDate? = null,
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
        label = "chevronRotation"
    )

    val displayName = userName.ifBlank { stringResource(R.string.greeting_default_user_name) }
    val nameAbbreviation = remember(displayName) { abbreviateName(displayName) }
    val netWorthText = when {
        isBalanceHidden -> MASK
        else -> CurrencyFormatter.formatCurrency(netWorth, currency) + if (isNetWorthApproximate) "*" else ""
    }
    val showCurrencyChip = availableCurrencies.size > 1 && !isUnifiedMode
    val toggleVisibility = {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onToggleBalanceVisibility()
    }
    val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()
    val cardShape = RoundedCornerShape(Dimensions.Glass.heroRadius)

    // Wraps the card's height even under a min-height constraint, so the
    // overlaid chevron always sits on the card's own bottom edge.
    Box(modifier = modifier.fillMaxWidth().wrapContentHeight(Alignment.Top)) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec()),
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                isExpanded = !isExpanded
            },
            shape = cardShape,
            blurEffects = blurEffects,
            hazeState = hazeState,
            contentPadding = Spacing.md
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Collapsed: headline + change, sparkline at the right.
                AnimatedVisibility(
                    visible = !isExpanded,
                    enter = fadeIn() + expandVertically(spatialSpec),
                    exit = fadeOut() + shrinkVertically(spatialSpec)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AnimatedCurrencyText(
                                text = if (isBalanceHidden) nameAbbreviation else netWorthText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = Spacing.xs)
                            )
                            NetWorthChangeRow(
                                currency = currency,
                                hasNetWorth = hasNetWorth,
                                changePercent = netWorthChangePercent,
                                isBalanceHidden = isBalanceHidden,
                                isDark = isDark,
                            )
                        }

                        if (balanceHistory.size >= 2) {
                            MiniSparkline(
                                data = balanceHistory,
                                lineColor = trendLineColor(),
                                modifier = Modifier
                                    .width(MINI_SPARKLINE_WIDTH)
                                    .height(MINI_SPARKLINE_HEIGHT)
                            )
                        }
                    }
                }

                // ── Expanded ──
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(spatialSpec),
                    exit = fadeOut() + shrinkVertically(spatialSpec)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = nameAbbreviation,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Text(
                                        text = stringResource(R.string.net_worth_owner_label, displayName),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = SUBTITLE_ALPHA),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    BalanceVisibilityToggle(
                                        isBalanceHidden = isBalanceHidden,
                                        onToggle = toggleVisibility,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column(horizontalAlignment = Alignment.End) {
                                AnimatedCurrencyText(
                                    text = netWorthText,
                                    style = MaterialTheme.typography.headlineSmall.copy(lineHeight = HEADLINE_AMOUNT_LINE_HEIGHT),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (showCurrencyChip) {
                                    CurrencyChip(
                                        currency = currency,
                                        onClick = onCurrencyClick,
                                        modifier = Modifier.padding(horizontal = Spacing.smd)
                                    )
                                }
                            }
                        }

                        if (!hasNetWorth) {
                            Text(
                                text = stringResource(R.string.net_worth_no_accounts, currency),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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

                        Spacer(modifier = Modifier.height(Spacing.sm))
                        NetWorthTrendChart(
                            values = balanceHistory,
                            endDate = balanceHistoryEndDate ?: LocalDate.now(),
                            currency = currency,
                            isBalanceHidden = isBalanceHidden,
                            isApproximate = isBalanceHistoryApproximate,
                            pointRingColor = if (blurEffects) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Cashiro's This month / This year / Balance trio. As there, the
                        // period labels are set in caps and "Balance" is not.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrioItem(
                                label = stringResource(R.string.net_worth_this_month).uppercase(),
                                value = if (isBalanceHidden) MASK else CurrencyFormatter.formatCurrency(currentMonthTotal, currency),
                                modifier = Modifier.weight(1f)
                            )
                            TrioDivider()
                            TrioItem(
                                label = stringResource(R.string.net_worth_this_year).uppercase(),
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

                        // PennyWise extras Cashiro doesn't have: this month's
                        // spending breakdown and the accounts behind the figure.
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

                        // Room for the chevron that overlays the card's bottom edge.
                        Spacer(modifier = Modifier.height(EXPANDED_BOTTOM_SPACE))
                    }
                }
            }
        }

        // Cashiro's wide, shallow chevron, overlaid on the card's bottom edge.
        Icon(
            imageVector = Iconax.LongArrow,
            contentDescription = if (isExpanded) stringResource(R.string.card_collapse) else stringResource(R.string.card_expand),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = CHEVRON_ALPHA),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.xs)
                .height(CHEVRON_HEIGHT)
                .width(Dimensions.Icon.list)
                .rotate(chevronRotation)
        )
    }
}

/**
 * Cashiro draws its trend lines in `primary`; some PennyWise accents are too
 * dark (or too pale) for that on the card, so nudge it until it reads.
 */
@Composable
private fun trendLineColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme.primary, scheme.surfaceContainerLow, scheme.onSurface) {
        scheme.primary.legibleOn(background = scheme.surfaceContainerLow, towards = scheme.onSurface)
    }
}

/**
 * Cashiro's short headline for the hero: initials for a multi-word name, the
 * first four consonants of a long single word, or the whole short word.
 */
internal fun abbreviateName(name: String): String {
    val trimmed = name.trim()
    val words = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        words.size > 1 -> words.take(2).joinToString("") { it.first().toString() }.uppercase()
        trimmed.length > 4 -> trimmed.filter { it.lowercaseChar() !in "aeiou" }.take(4).uppercase()
            .ifEmpty { trimmed.take(4).uppercase() }
        else -> trimmed.uppercase()
    }
}

/**
 * "▲ +4% this month" under the collapsed headline. Growth is good here, so up
 * is the income colour and down the expense colour; the signed text carries
 * the meaning for anyone who can't rely on colour.
 */
@Composable
private fun NetWorthChangeRow(
    currency: String,
    hasNetWorth: Boolean,
    changePercent: Int?,
    isBalanceHidden: Boolean,
    isDark: Boolean,
) {
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = CHANGE_TEXT_ALPHA)
    when {
        !hasNetWorth -> Text(
            text = stringResource(R.string.net_worth_no_accounts, currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
        changePercent != null -> {
            val isUp = changePercent >= 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isUp) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
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
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun TrioItem(label: String, value: String, modifier: Modifier = Modifier) {
    val valueStyle = MaterialTheme.typography.titleMedium
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
            style = MaterialTheme.typography.labelSmall.copy(fontSize = TRIO_LABEL_SIZE),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrioDivider() {
    VerticalDivider(
        modifier = Modifier.height(TRIO_DIVIDER_HEIGHT),
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
 * Cashiro's balance-trend chart: the high value top-left with "Balance trend"
 * opposite, a dashed grid with the line, its gradient fill and point dots
 * (revealed left to right), the first and last dates beneath, then the low
 * value and a "Last N days" caption.
 *
 * Unlike Cashiro, the high / low labels are the series' real extremes, not the
 * chart's padded axis bounds, so they never show a figure the user never had.
 */
@Composable
private fun NetWorthTrendChart(
    values: List<BigDecimal>,
    endDate: LocalDate,
    currency: String,
    isBalanceHidden: Boolean,
    isApproximate: Boolean,
    pointRingColor: Color,
) {
    val hasChart = values.size >= 2
    val lineColor = trendLineColor()
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val high = values.maxOrNull()
    val low = values.minOrNull()
    fun money(value: BigDecimal?): String = when {
        value == null -> ""
        isBalanceHidden -> "••••"
        else -> CurrencyFormatter.formatCurrency(value, currency)
    }
    val highDescription = stringResource(R.string.net_worth_trend_high, money(high))
    val lowDescription = stringResource(R.string.net_worth_trend_low, money(low))

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (hasChart) money(high) else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = highDescription }
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
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
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

        if (!hasChart) {
            Text(
                text = stringResource(R.string.balance_trend_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val points = remember(values) { downsampleTrend(values) }
            val reveal = remember(values) { Animatable(0f) }
            LaunchedEffect(values) {
                reveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = TREND_REVEAL_MILLIS, easing = TrendRevealEasing)
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TREND_CANVAS_HEIGHT)
                    .padding(horizontal = Spacing.xs)
            ) {
                val w = size.width
                val h = size.height
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                for (i in 0..TREND_GRID_ROWS) {
                    val y = h * i / TREND_GRID_ROWS
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
                }
                for (i in 0..TREND_GRID_COLUMNS) {
                    val x = w * i / TREND_GRID_COLUMNS
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx(), pathEffect = dash)
                }

                val max = points.maxOf { it.second }.toFloat()
                val min = points.minOf { it.second }.toFloat()
                val range = (max - min).takeIf { it > 0f } ?: 1f
                // 10% headroom above and below, as in Cashiro.
                val paddedMin = min - range * 0.1f
                val paddedRange = range * 1.2f
                val lastIndex = values.lastIndex.coerceAtLeast(1)
                val offsets = points.map { (index, value) ->
                    Offset(
                        x = w * index / lastIndex,
                        y = h * (1f - (value.toFloat() - paddedMin) / paddedRange)
                    )
                }
                val line = Path().apply {
                    offsets.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
                }
                clipRect(right = w * reveal.value) {
                    val fill = Path().apply {
                        addPath(line)
                        lineTo(offsets.last().x, h)
                        lineTo(offsets.first().x, h)
                        close()
                    }
                    drawPath(
                        path = fill,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.05f)),
                            startY = 0f,
                            endY = h
                        )
                    )
                    drawPath(
                        path = line,
                        color = lineColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                offsets.forEachIndexed { i, o ->
                    val progress = (reveal.value * offsets.size - i).coerceIn(0f, 1f)
                    if (progress > 0f) {
                        val radius = TREND_POINT_RADIUS.toPx() * progress
                        drawCircle(color = lineColor, radius = radius, center = o)
                        drawCircle(color = pointRingColor, radius = radius, center = o, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }

            val dateFormat = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = endDate.minusDays(values.lastIndex.toLong()).format(dateFormat),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = endDate.format(dateFormat),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = money(low),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = lowDescription }
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            val days = values.size - 1
            Text(
                text = pluralStringResource(R.plurals.net_worth_trend_last_days, days, days),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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
}

/**
 * Cashiro's chart smoothing: at most [maxPoints] points, each the average of a
 * run of consecutive days, placed at the run's middle day. Returns
 * (day index into [values], value) pairs; a short series passes through as is.
 * All values share one currency, so averaging them is meaningful.
 */
internal fun downsampleTrend(
    values: List<BigDecimal>,
    maxPoints: Int = TREND_MAX_POINTS,
): List<Pair<Int, BigDecimal>> {
    if (values.size <= maxPoints) return values.mapIndexed { i, v -> i to v }
    val interval = maxOf(1, (values.size - 1) / maxPoints)
    return values.indices
        .groupBy { it / interval }
        .values
        .map { run ->
            val average = run.fold(BigDecimal.ZERO) { acc, i -> acc + values[i] }
                .divide(BigDecimal(run.size), MathContext.DECIMAL64)
            run[run.size / 2] to average
        }
}

/** Cashiro's chart has no show/hide control; PennyWise keeps a small one beside "Net worth". */
@Composable
private fun BalanceVisibilityToggle(
    isBalanceHidden: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(Dimensions.Component.iconButton - Spacing.sm)
    ) {
        Icon(
            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (isBalanceHidden) stringResource(R.string.balance_show) else stringResource(R.string.balance_hide),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SUBTITLE_ALPHA),
            modifier = Modifier.size(Dimensions.Icon.small)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
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

/** Cashiro's 180dp chart block minus its label rows: the plotted area. */
private val TREND_CANVAS_HEIGHT = 120.dp
private const val TREND_GRID_ROWS = 4
private const val TREND_GRID_COLUMNS = 5
private const val TREND_MAX_POINTS = 50
private const val TREND_REVEAL_MILLIS = 1500
private val TrendRevealEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)
private val TREND_POINT_RADIUS = 4.dp

/** Height of the inline spending sparkline inside the expanded balance card. */
private val SPARKLINE_HEIGHT = Spacing.xxl + Spacing.xl

/** Cashiro's 100 x 40dp sparkline beside the collapsed headline. */
private val MINI_SPARKLINE_WIDTH = 100.dp
private val MINI_SPARKLINE_HEIGHT = Dimensions.Component.iconButton

/** Cashiro's 28 x 40dp expand / collapse chevron. */
private val CHEVRON_HEIGHT = 28.dp
private const val CHEVRON_ALPHA = 0.3f

/** Space under the expanded content so the overlaid chevron never covers it. */
private val EXPANDED_BOTTOM_SPACE = 18.dp + Spacing.md

/** Cashiro's dimmed "this month" change text and "Name • Net worth" line. */
private const val CHANGE_TEXT_ALPHA = 0.6f
private const val SUBTITLE_ALPHA = 0.55f

/** Line height Cashiro gives the expanded amount so it sits level with the name. */
private val HEADLINE_AMOUNT_LINE_HEIGHT = 24.sp

private val TRIO_LABEL_SIZE = 10.sp
private val TRIO_DIVIDER_HEIGHT = 30.dp
