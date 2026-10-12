package com.pennywiseai.tracker.presentation.loans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import com.pennywiseai.tracker.presentation.people.PersonAvatar
import com.pennywiseai.tracker.presentation.people.initialsOf
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.cards.ListItemCardV2
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.components.parseProfileColor
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
 * colour (the same pairing Cashiro uses for its person rows).
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

/** The accent a person's avatar falls back to: their first open balance's direction, neutral once settled. */
@Composable
internal fun LoanPerson.accentColor(): Color =
    net.values.firstOrNull()?.let { loanBalanceColor(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant

/**
 * A person's avatar on the lend & borrow screens. A linked contact gets
 * Cashiro's solid circle in the contact's own colour; a name without a contact
 * keeps the direction-tinted initials, so the colour still means something.
 */
@Composable
internal fun LoanPersonAvatar(
    name: String,
    contact: LoanContactStyle?,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = Dimensions.Icon.avatar,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    if (contact != null) {
        PersonAvatar(
            initials = initialsOf(name),
            color = parseProfileColor(contact.color, MaterialTheme.colorScheme.primary),
            modifier = modifier,
            size = size,
            textStyle = textStyle,
        )
    } else {
        PersonAvatar(
            initials = initialsOf(name),
            color = accent,
            modifier = modifier,
            size = size,
            textStyle = textStyle,
            tinted = true,
        )
    }
}

/** One face in a [LoanAvatarStack]. */
internal data class LoanAvatarFace(
    val name: String,
    val contact: LoanContactStyle?,
    val accent: Color,
)

/**
 * Cashiro's overlapping avatar stack (its `PersonIconsStack`): up to [maxFaces]
 * small avatars, each ringed in the surface colour, then a "+n" chip.
 */
@Composable
internal fun LoanAvatarStack(
    faces: List<LoanAvatarFace>,
    modifier: Modifier = Modifier,
    faceSize: Dp = Dimensions.Icon.medium,
    maxFaces: Int = 4,
) {
    if (faces.isEmpty()) return
    val shown = faces.take(maxFaces)
    val extra = faces.size - shown.size
    val ring = Spacing.xxs
    val outer = faceSize + ring * 2
    val step = faceSize * AVATAR_OVERLAP
    val circles = shown.size + if (extra > 0) 1 else 0
    val surface = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.width(step * (circles - 1) + outer)) {
        shown.forEachIndexed { index, face ->
            Box(
                modifier = Modifier
                    .offset(x = step * index)
                    .zIndex(index.toFloat())
                    .size(outer)
                    .shadow(Spacing.xxs, CircleShape)
                    .background(surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                LoanPersonAvatar(
                    name = face.name,
                    contact = face.contact,
                    accent = face.accent,
                    size = faceSize,
                    textStyle = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .offset(x = step * shown.size)
                    .zIndex(shown.size.toFloat())
                    .size(outer)
                    .background(surface, CircleShape)
                    .padding(ring)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.loans_avatar_more, extra),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                )
            }
        }
    }
}

private const val AVATAR_OVERLAP = 0.55f

/**
 * One loan as a Cashiro list row: direction-tinted initials, the person, a
 * direction tag, and the amount still open (or the original amount once
 * settled). While open, a thin repayment track runs under the tag. [shape] and
 * [containerColor] let a caller stack rows as a grouped list or nest them
 * inside another surface.
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
    val directionLabel = stringResource(
        if (loan.direction == LoanDirection.LENT) R.string.loans_direction_lent else R.string.loans_direction_borrowed,
    )
    val settledLabel = stringResource(R.string.loans_status_settled)
    val shownAmount = CurrencyFormatter.formatCurrency(
        // Settled loans show what the loan was; zero remaining says nothing.
        if (settled) loan.originalAmount else loan.remainingAmount,
        loan.currency,
    )

    ListItemCardV2(
        title = loan.personName,
        subtitle = if (settled) "$directionLabel, $settledLabel" else directionLabel,
        amount = shownAmount,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        onClick = onClick,
        leadingContent = {
            PersonAvatar(
                initials = initialsOf(loan.personName),
                color = directionColor,
                tinted = true,
            )
        },
        subtitleContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SubtitleTag(text = directionLabel, color = directionColor)
                    if (settled) {
                        SubtitleTag(text = settledLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!settled) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.xs)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.income,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        drawStopIndicator = {},
                    )
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = shownAmount,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (settled) MaterialTheme.colorScheme.onSurfaceVariant else directionColor,
                    maxLines = 1,
                )
                if (!settled) {
                    Text(
                        text = stringResource(
                            R.string.loans_of_original,
                            CurrencyFormatter.formatCurrency(loan.originalAmount, loan.currency),
                        ),
                        style = PennyWiseText.metadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        },
    )
}

/**
 * One person on the Lend & Borrow overview, drawn as Cashiro's person row:
 * avatar, name, a tag line, and where they stand on the trailing edge ("Owes
 * you ₹500" / "You owe $10", one line per currency — never added together).
 * Tapping the row opens the person's loans in place, with a Settle up action
 * while anything is open. The whole block is one surface so [position] shapes
 * it as part of a connected list.
 */
@Composable
internal fun LoanPersonBlock(
    person: LoanPerson,
    contact: LoanContactStyle?,
    position: ListItemPosition,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenLoan: (Long) -> Unit,
    onSettleUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(Dimensions.Animation.medium),
        label = "loanPersonChevron",
    )
    val expandedState = stringResource(
        if (expanded) R.string.loans_person_expanded else R.string.loans_person_collapsed,
    )
    val openCount = person.activeLoans.size

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surfaceContainerLow,
        shape = position.toShape(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .semantics { stateDescription = expandedState }
                    .defaultMinSize(minHeight = Dimensions.Component.listItemMinHeightTwoLine)
                    .padding(horizontal = Spacing.md, vertical = Dimensions.Padding.listRowVertical),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LoanPersonAvatar(
                    name = person.name,
                    contact = contact,
                    accent = person.accentColor(),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        contact?.category?.let { category ->
                            SubtitleTag(
                                text = category,
                                color = parseProfileColor(contact.color, scheme.primary),
                            )
                        }
                        SubtitleTag(
                            text = if (person.hasActive) {
                                pluralStringResource(R.plurals.loans_person_open_count, openCount, openCount)
                            } else {
                                stringResource(R.string.loans_status_settled)
                            },
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    if (person.net.isEmpty()) {
                        // Empty net with open loans = lent and borrowed cancel out.
                        Text(
                            text = stringResource(
                                if (person.hasActive) R.string.loans_person_even else R.string.loans_person_all_settled,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
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
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = loanBalanceColor(money),
                                maxLines = 1,
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .size(Dimensions.Icon.inline)
                        .rotate(chevronRotation),
                    tint = scheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(start = Spacing.smd, end = Spacing.smd, bottom = Spacing.smd),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
                ) {
                    person.loans.forEachIndexed { index, loan ->
                        LoanListItem(
                            loan = loan,
                            onClick = { onOpenLoan(loan.id) },
                            shape = ListItemPosition.from(index, person.loans.size).toShape(),
                            containerColor = scheme.surfaceContainerHigh,
                        )
                    }
                    if (person.hasActive) {
                        // Cashiro's Settle up: a filled, softly squared button.
                        Button(
                            onClick = onSettleUp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.sm)
                                .heightIn(min = Dimensions.Component.minTouchTarget),
                            shape = RoundedCornerShape(Dimensions.CornerRadius.card),
                        ) {
                            Text(
                                text = stringResource(R.string.loans_settle_up),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
