package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.sumByCurrency
import java.math.BigDecimal

/** What is still open in one currency: how much you lent and how much you borrowed. */
internal data class CurrencyBalance(
    val currency: String,
    val lent: BigDecimal,
    val borrowed: BigDecimal,
)

/**
 * Open loan balances, one entry per currency (sorted by code). Currencies are
 * only ever totalled within themselves, so a rupee loan and a dollar loan stay
 * two separate figures.
 */
internal fun loanBalancesByCurrency(activeLoans: List<LoanEntity>): List<CurrencyBalance> {
    val lent = activeLoans
        .filter { it.direction == LoanDirection.LENT }
        .sumByCurrency({ it.currency }) { it.remainingAmount }
    val borrowed = activeLoans
        .filter { it.direction == LoanDirection.BORROWED }
        .sumByCurrency({ it.currency }) { it.remainingAmount }
    return (lent.keys + borrowed.keys).sorted().map { code ->
        CurrencyBalance(
            currency = code,
            lent = lent[code]?.amount ?: BigDecimal.ZERO,
            borrowed = borrowed[code]?.amount ?: BigDecimal.ZERO,
        )
    }
}

/** How strongly a tile's semantic colour washes its background. */
private const val TILE_TINT_ALPHA = 0.2f

/**
 * The Lend & Borrow summary: the net balance with a status pill, then what you
 * are owed and what you owe. [totalLent] / [totalBorrowed] are already in one
 * [currency] (the view model converts or filters), so the net is a safe
 * difference. When open loans span several currencies, [balances] adds a
 * per-currency breakdown so nothing hides behind the single figure.
 */
@Composable
internal fun LoanSummaryCard(
    totalLent: BigDecimal,
    totalBorrowed: BigDecimal,
    currency: String,
    balances: List<CurrencyBalance>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val net = totalLent.subtract(totalBorrowed)
    val statusColor = when {
        net.signum() > 0 -> scheme.income
        net.signum() < 0 -> scheme.expense
        else -> scheme.onSurfaceVariant
    }
    val statusText = stringResource(
        when {
            net.signum() > 0 -> R.string.loans_hero_status_get
            net.signum() < 0 -> R.string.loans_hero_status_owe
            else -> R.string.people_settled_up
        },
    )
    val showBreakdown = balances.size > 1 ||
        balances.any { !it.currency.equals(currency, ignoreCase = true) }

    PennyWiseCardV2(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = Dimensions.Padding.card,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.loans_hero_net_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
            StatusPill(text = statusText, color = statusColor)
        }

        Text(
            text = CurrencyFormatter.formatCurrency(net.abs(), currency),
            modifier = Modifier.padding(vertical = Spacing.sm),
            style = PennyWiseText.heroAmount,
            color = if (net.signum() == 0) scheme.onSurfaceVariant else scheme.onSurface,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
        ) {
            SummaryTile(
                label = stringResource(R.string.loans_owed_to_you),
                amount = CurrencyFormatter.formatCurrency(totalLent, currency),
                color = scheme.income,
                icon = Icons.Default.ArrowUpward,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SummaryTile(
                label = stringResource(R.string.loans_you_owe),
                amount = CurrencyFormatter.formatCurrency(totalBorrowed, currency),
                color = scheme.expense,
                icon = Icons.Default.ArrowDownward,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        if (showBreakdown) {
            Text(
                text = stringResource(R.string.loans_hero_by_currency),
                modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                balances.forEach { balance -> CurrencyBalanceRow(balance) }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = Dimensions.Alpha.tonalIconContainer),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.smd, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .size(Spacing.sm)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    amount: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = TILE_TINT_ALPHA),
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
                        .background(color.copy(alpha = TILE_TINT_ALPHA)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.tiny),
                        tint = color,
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = amount,
                // A long figure steps down a size and wraps rather than being cut off:
                // a truncated amount of money is worse than a smaller one.
                style = if (amount.length > LONG_AMOUNT_LENGTH) {
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                } else {
                    PennyWiseText.amountMedium
                },
                color = color,
                maxLines = 2,
            )
        }
    }
}

private const val LONG_AMOUNT_LENGTH = 11

@Composable
private fun CurrencyBalanceRow(balance: CurrencyBalance) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = balance.currency,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            if (balance.lent.signum() != 0) {
                Text(
                    text = stringResource(
                        R.string.home_loans_owed_to_you,
                        CurrencyFormatter.formatCurrency(balance.lent, balance.currency),
                    ),
                    style = PennyWiseText.amountSmall,
                    color = MaterialTheme.colorScheme.income,
                )
            }
            if (balance.borrowed.signum() != 0) {
                Text(
                    text = stringResource(
                        R.string.home_loans_you_owe,
                        CurrencyFormatter.formatCurrency(balance.borrowed, balance.currency),
                    ),
                    style = PennyWiseText.amountSmall,
                    color = MaterialTheme.colorScheme.expense,
                )
            }
        }
    }
}
