package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.ui.components.cards.GlassCard
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

/** How strongly a tile's colour washes its background (Cashiro's `LoanSummaryItem`). */
private const val TILE_TINT_ALPHA = 0.2f

/** The de-emphasised decimals of the headline figure (Cashiro's `Alpha.faint`). */
private const val DECIMAL_ALPHA = 0.65f

/** Zero reads quieter than money (Cashiro dims a zero headline the same way). */
private const val ZERO_ALPHA = 0.65f

/**
 * The Lend & Borrow summary — Cashiro's `LoanBalanceContent`: a title with the
 * net-status pill, the net figure with its decimals de-emphasised, then Lent /
 * Borrowed tiles (arrow chip, amount, the faces behind it).
 *
 * [totalLent] / [totalBorrowed] are already in one [currency] (the view model
 * converts or filters), so the net is a safe difference. When open loans span
 * several currencies, [balances] adds a per-currency breakdown so nothing hides
 * behind the single figure.
 */
@Composable
internal fun LoanSummaryCard(
    totalLent: BigDecimal,
    totalBorrowed: BigDecimal,
    currency: String,
    balances: List<CurrencyBalance>,
    modifier: Modifier = Modifier,
    lentFaces: List<LoanAvatarFace> = emptyList(),
    borrowedFaces: List<LoanAvatarFace> = emptyList(),
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

    // Cashiro's summary header: a plain surfaceContainerLow card, 28dp corners, no rim.
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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
                LoanStatusPill(text = statusText, color = statusColor)
            }

            NetFigure(amount = net.abs(), currency = currency)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SummaryTile(
                    label = stringResource(R.string.loans_owed_to_you),
                    amount = CurrencyFormatter.formatCurrency(totalLent, currency),
                    color = scheme.primary,
                    icon = Icons.Default.ArrowUpward,
                    faces = lentFaces,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                SummaryTile(
                    label = stringResource(R.string.loans_you_owe),
                    amount = CurrencyFormatter.formatCurrency(totalBorrowed, currency),
                    color = scheme.secondary,
                    icon = Icons.Default.ArrowDownward,
                    faces = borrowedFaces,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }

            if (showBreakdown) {
                Text(
                    text = stringResource(R.string.loans_hero_by_currency),
                    modifier = Modifier.padding(top = Spacing.smd, bottom = Spacing.xs),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    balances.forEach { balance -> CurrencyBalanceRow(balance) }
                }
            }
        }
    }
}

/**
 * The headline figure, Cashiro-style: the whole part large and bold, the
 * decimals a step smaller and faint. Read aloud as one amount.
 */
@Composable
private fun NetFigure(amount: BigDecimal, currency: String) {
    val scheme = MaterialTheme.colorScheme
    val formatted = CurrencyFormatter.formatCurrency(amount, currency)
    val (whole, decimal) = remember(formatted) { splitDecimalPart(formatted) }
    val isZero = amount.signum() == 0
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = formatted },
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = whole,
            // Cashiro's 44sp balance figure; tabular so digits keep their width.
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
            color = if (isZero) scheme.onSurface.copy(alpha = ZERO_ALPHA) else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (decimal.isNotEmpty()) {
            Text(
                text = decimal,
                modifier = Modifier.padding(bottom = Spacing.xs),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurfaceVariant.copy(alpha = DECIMAL_ALPHA),
                maxLines = 1,
            )
        }
    }
}

/**
 * Splits a formatted amount into whole and decimal parts on the last
 * separator, when it sits within the last three characters ("₹1,234.56" →
 * "₹1,234" + ".56"). Anything else (no decimals, a trailing symbol) stays whole.
 */
internal fun splitDecimalPart(formatted: String): Pair<String, String> {
    val index = maxOf(formatted.lastIndexOf('.'), formatted.lastIndexOf(','))
    return if (index > 0 && index >= formatted.length - 3) {
        formatted.substring(0, index) to formatted.substring(index)
    } else {
        formatted to ""
    }
}

/** Cashiro's status pill: a dot and a word on a faint wash of the same colour. */
@Composable
private fun LoanStatusPill(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = Dimensions.Alpha.tonalIconContainer),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .size(STATUS_DOT)
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

private val STATUS_DOT = Spacing.xs + Spacing.xxs

/** Cashiro's `LoanSummaryItem`: arrow chip + label, the amount, then the faces it is owed by / to. */
@Composable
private fun SummaryTile(
    label: String,
    amount: String,
    color: Color,
    icon: ImageVector,
    faces: List<LoanAvatarFace>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimensions.Glass.heroRadius),
        color = color.copy(alpha = TILE_TINT_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.Icon.inline)
                        .clip(RoundedCornerShape(ICON_CHIP_RADIUS))
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
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                    )
                },
                color = color,
                maxLines = 2,
            )
            if (faces.isNotEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                LoanAvatarStack(
                    faces = faces,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

private val ICON_CHIP_RADIUS = Spacing.xs + Spacing.xxs

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
