package com.pennywiseai.tracker.presentation.loans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import com.pennywiseai.tracker.presentation.people.PersonAvatar
import com.pennywiseai.tracker.presentation.people.initialsOf
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The colour a loan direction reads in everywhere on the lend & borrow
 * screens: money owed to you is the income colour, money you owe the expense
 * colour (the same pairing Cashiro uses).
 */
@Composable
internal fun loanDirectionColor(direction: LoanDirection): Color =
    if (direction == LoanDirection.LENT) MaterialTheme.colorScheme.income else MaterialTheme.colorScheme.expense

/** The colour for a per-currency net balance: positive = they owe you. */
@Composable
internal fun loanBalanceColor(money: Money): Color =
    if (money.isOwedToYou) MaterialTheme.colorScheme.income else MaterialTheme.colorScheme.expense

/** How much of [loan] has been paid back, 0..1. */
internal fun loanRepaidFraction(loan: LoanEntity): Float =
    if (loan.originalAmount > BigDecimal.ZERO) {
        (BigDecimal.ONE - loan.remainingAmount.divide(loan.originalAmount, 2, RoundingMode.HALF_UP))
            .toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

/**
 * One loan as a tonal row: direction-tinted initials, the person, a direction
 * tag, the amount still open (or the original amount once settled) and, while
 * open, a repayment track. [shape] and [containerColor] let a caller stack rows
 * as a grouped list or nest them inside another surface.
 */
@Composable
fun LoanListItem(
    loan: LoanEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    val directionColor = loanDirectionColor(loan.direction)
    val settled = loan.status == LoanStatus.SETTLED
    val progress = loanRepaidFraction(loan)

    PennyWiseCardV2(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        containerColor = containerColor,
        // Rows in a stack read as one block; a hairline per row would turn it into a grid.
        border = BorderStroke(0.dp, Color.Transparent),
        onClick = onClick,
        contentPadding = Dimensions.Padding.cardCompact,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimensions.Component.minTouchTarget),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonAvatar(
                initials = initialsOf(loan.personName),
                color = directionColor,
                tinted = true,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = loan.personName,
                    style = PennyWiseText.rowTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SubtitleTag(
                        text = stringResource(
                            if (loan.direction == LoanDirection.LENT) {
                                R.string.loans_direction_lent
                            } else {
                                R.string.loans_direction_borrowed
                            },
                        ),
                        color = directionColor,
                    )
                    if (settled) {
                        SubtitleTag(
                            text = stringResource(R.string.loans_status_settled),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!settled) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimensions.Component.progressBarHeight)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.income,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    // Settled loans show what the loan was; zero remaining says nothing.
                    text = CurrencyFormatter.formatCurrency(
                        if (settled) loan.originalAmount else loan.remainingAmount,
                        loan.currency,
                    ),
                    style = PennyWiseText.amountRow,
                    color = if (settled) MaterialTheme.colorScheme.onSurfaceVariant else directionColor,
                )
                if (!settled) {
                    Text(
                        text = stringResource(
                            R.string.loans_of_original,
                            CurrencyFormatter.formatCurrency(loan.originalAmount, loan.currency),
                        ),
                        style = PennyWiseText.metadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * One person on the Lend & Borrow overview. Tapping the header opens the
 * person's loans in place (each opens its detail) with, while anything is open,
 * a settle-up action. The whole block is one surface so [position] shapes it as
 * part of a connected list.
 */
@Composable
internal fun LoanPersonBlock(
    person: LoanPerson,
    position: ListItemPosition,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenLoan: (Long) -> Unit,
    onSettleUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Tint the avatar by the first open balance's direction; neutral once all settled.
    val accent = person.net.values.firstOrNull()?.let { loanBalanceColor(it) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = position.toShape(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .defaultMinSize(minHeight = Dimensions.Component.listItemMinHeightTwoLine)
                    .padding(
                        horizontal = Spacing.md,
                        vertical = Dimensions.Padding.listRowVertical,
                    ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PersonAvatar(
                    initials = initialsOf(person.name),
                    color = accent,
                    tinted = true,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    Text(
                        text = person.name,
                        style = PennyWiseText.rowTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (person.net.isEmpty()) {
                        // Empty net with open loans = lent and borrowed cancel out.
                        Text(
                            text = stringResource(
                                if (person.hasActive) R.string.loans_person_even else R.string.loans_person_all_settled,
                            ),
                            style = PennyWiseText.rowSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // One line per currency: balances in different currencies are never added.
                        person.net.values.forEach { money ->
                            val formatted = CurrencyFormatter.formatCurrency(money.amount.abs(), money.currency)
                            Text(
                                text = stringResource(
                                    if (money.isOwedToYou) R.string.loans_person_owes_you else R.string.loans_person_you_owe,
                                    formatted,
                                ),
                                style = PennyWiseText.rowSubtitle,
                                fontWeight = FontWeight.SemiBold,
                                color = loanBalanceColor(money),
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(
                        start = Spacing.md,
                        end = Spacing.md,
                        bottom = Spacing.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    person.loans.forEach { loan ->
                        LoanListItem(
                            loan = loan,
                            onClick = { onOpenLoan(loan.id) },
                            shape = MaterialTheme.shapes.medium,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    }
                    if (person.hasActive) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            FilledTonalButton(onClick = onSettleUp) {
                                Text(stringResource(R.string.loans_settle_up))
                            }
                        }
                    }
                }
            }
        }
    }
}
