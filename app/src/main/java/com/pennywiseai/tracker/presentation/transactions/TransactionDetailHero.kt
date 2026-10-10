package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.contacts.LocalMerchantDisplay
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.LocalNavAnimatedVisibilityScope
import com.pennywiseai.tracker.ui.LocalSharedTransitionScope
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.DashedLine
import com.pennywiseai.tracker.ui.components.EmojiGlyph
import com.pennywiseai.tracker.ui.components.TINTED_CONTAINER_ALPHA
import com.pennywiseai.tracker.ui.components.legibleOn
import com.pennywiseai.tracker.ui.icons.IconProvider
import com.pennywiseai.tracker.ui.icons.IconResource
import com.pennywiseai.tracker.ui.icons.iconax.Card
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.sharedElementIcon
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.credit
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.ui.theme.investment
import com.pennywiseai.tracker.ui.theme.transfer
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.formatAmount
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/*
 * The read-only receipt of the Transaction Detail screen, Cashiro-style: one
 * paper-receipt card with scalloped top and bottom edges. The merchant sits in
 * a pill on a dashed rule at the top, the details read as dotted-leader rows
 * (label ........ [chip]), and a torn edge — side notches plus a dashed rule —
 * separates them from the amount block at the bottom.
 */

/** The semantic colour of a transaction type (income green, expense red, ...). */
@Composable
internal fun txnDetailTypeColor(type: TransactionType): Color {
    val scheme = MaterialTheme.colorScheme
    return when (type) {
        TransactionType.INCOME -> scheme.income
        TransactionType.EXPENSE -> scheme.expense
        TransactionType.CREDIT -> scheme.credit
        TransactionType.TRANSFER -> scheme.transfer
        TransactionType.INVESTMENT -> scheme.investment
    }
}

/** The glyph that names a transaction type beside its label. */
internal fun txnDetailTypeIcon(type: TransactionType): ImageVector = when (type) {
    TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
    TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
    TransactionType.CREDIT -> Iconax.Card
    TransactionType.TRANSFER -> Icons.Default.SwapHoriz
    TransactionType.INVESTMENT -> Icons.AutoMirrored.Filled.ShowChart
}

/** The sign shown before the amount. Credit, transfer and investment stay unsigned. */
private fun txnDetailSign(type: TransactionType): String = when (type) {
    TransactionType.INCOME -> "+"
    TransactionType.EXPENSE -> "-"
    TransactionType.CREDIT,
    TransactionType.TRANSFER,
    TransactionType.INVESTMENT -> ""
}

/** The merchant avatar inside the top pill. */
private val PillAvatarSize: Dp = Dimensions.Icon.large

/** The category glyph fills this share of the avatar circle. */
private const val HERO_GLYPH_FRACTION = 0.5f

/** The merchant pill may take at most this share of the top rule. */
private const val MERCHANT_PILL_MAX_FRACTION = 0.8f

/** A chip may take at most this share of a leader row, leaving room for label + dots. */
private const val RECEIPT_CHIP_MAX_FRACTION = 0.62f

/** Radius of the half-circle bumps along the top and bottom edges. */
private val ScallopRadius: Dp = Spacing.xs

/** Radius of the notches punched into the sides at the tear line. */
private val NotchRadius: Dp = Spacing.smd

/**
 * @param merchantTitle The name to show: the user's alias, else the
 *  contact-resolved name, else the raw merchant. Resolved by the caller so the
 *  receipt and the edit form agree on it.
 * @param details The leader rows and collapsible sections between the merchant
 *  pill and the tear line.
 */
@Composable
internal fun TxnDetailReceipt(
    transaction: TransactionEntity,
    merchantTitle: String,
    primaryCurrency: String,
    convertedAmount: BigDecimal?,
    modifier: Modifier = Modifier,
    details: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val typeColor = txnDetailTypeColor(transaction.transactionType)
    val cardColor = scheme.surfaceContainerLow
    val ruleColor = scheme.outlineVariant
    val density = LocalDensity.current

    val notchRadiusPx = with(density) { NotchRadius.toPx() }
    val scallopRadiusPx = with(density) { ScallopRadius.toPx() }
    // Where the tear line sits; the side notches follow it as content grows.
    var tearOffsetPx by remember { mutableFloatStateOf(-1f) }
    val shape = remember(notchRadiusPx, scallopRadiusPx, tearOffsetPx) {
        TxnReceiptShape(notchRadiusPx, tearOffsetPx, scallopRadiusPx)
    }

    // The receipt is cut from the app's glass material (GlassCard.kt): the
    // fill, sheen and rim follow its scalloped, notched outline.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .txnGlass(shape),
        shape = shape,
        color = Color.Transparent
    ) {
        // No vertical padding on this column: the tear line's position in it
        // must equal its position in the shape.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.lg))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xs)
            ) {
                val pillMax = maxWidth * MERCHANT_PILL_MAX_FRACTION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DashedLine(modifier = Modifier.weight(1f), color = ruleColor)
                    TxnDetailMerchantPill(
                        transaction = transaction,
                        merchantTitle = merchantTitle,
                        background = cardColor,
                        modifier = Modifier.widthIn(max = pillMax)
                    )
                    DashedLine(modifier = Modifier.weight(1f), color = ruleColor)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.card)
                    .padding(top = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.smd)
            ) {
                TxnReceiptDateTime(transaction)
                details()
            }

            Spacer(Modifier.height(Spacing.xl))

            // The tear: notches in the sides, dashes across.
            DashedLine(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NotchRadius + Spacing.xs)
                    .onGloballyPositioned { coordinates ->
                        tearOffsetPx = coordinates.positionInParent().y + coordinates.size.height / 2f
                    },
                color = ruleColor
            )

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = stringResource(R.string.txn_detail_receipt_amount),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = "${txnDetailSign(transaction.transactionType)}${transaction.formatAmount()}",
                style = PennyWiseText.heroAmount,
                color = typeColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimensions.Padding.card)
            )
            if (transaction.currency.isNotEmpty() &&
                !transaction.currency.equals(primaryCurrency, ignoreCase = true) &&
                convertedAmount != null
            ) {
                Text(
                    text = "≈ ${CurrencyFormatter.formatCurrency(convertedAmount, primaryCurrency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

/** The merchant pill at the top of the receipt; its avatar carries the shared-element hand-off from the list. */
@Composable
private fun TxnDetailMerchantPill(
    transaction: TransactionEntity,
    merchantTitle: String,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val avatarModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            sharedElementIcon(
                key = "brand_icon_${transaction.id}",
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = background,
        border = BorderStroke(Dimensions.Component.hairline, scheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(
                start = Spacing.sm,
                end = Spacing.md,
                top = Spacing.sm,
                bottom = Spacing.sm
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            TxnDetailHeroAvatar(
                merchantName = transaction.merchantName,
                category = transaction.category,
                size = PillAvatarSize,
                background = background,
                modifier = avatarModifier
            )
            Text(
                text = merchantTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** "Date / 8 Oct 2026" on the left, the time as hour : minute chips on the right. */
@Composable
private fun TxnReceiptDateTime(transaction: TransactionEntity) {
    val scheme = MaterialTheme.colorScheme
    val dateTime = transaction.dateTime
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = stringResource(R.string.txn_detail_receipt_date),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = dateTime.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            TxnTimeChip(
                text = dateTime.format(DateTimeFormatter.ofPattern("hh")),
                container = scheme.primary.copy(alpha = Dimensions.Alpha.tonalIconContainer),
                content = scheme.primary
            )
            Text(text = ":", fontWeight = FontWeight.Bold, color = scheme.onSurface)
            TxnTimeChip(
                text = dateTime.format(DateTimeFormatter.ofPattern("mm")),
                container = scheme.surfaceContainerHighest,
                content = scheme.onSurface
            )
            Text(
                text = dateTime.format(DateTimeFormatter.ofPattern("a")),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.xs)
            )
        }
    }
}

@Composable
private fun TxnTimeChip(text: String, container: Color, content: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = content,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    )
}

/**
 * A dotted-leader row: an uppercase label, dashes, and the value as a chip on
 * the right. The chip is capped so the label and a stretch of dots always show.
 */
@Composable
internal fun TxnReceiptRow(
    label: String,
    modifier: Modifier = Modifier,
    chip: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
    ) {
        val chipMax = maxWidth * RECEIPT_CHIP_MAX_FRACTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            DashedLine(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Box(modifier = Modifier.widthIn(max = chipMax)) { chip() }
        }
    }
}

/** The value chip of a leader row: optional leading glyph, then the text. */
@Composable
internal fun TxnReceiptChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurface,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = Spacing.smd, vertical = Spacing.xs + Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        leading?.invoke(this)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A small glyph for the leading slot of a [TxnReceiptChip]. */
@Composable
internal fun TxnReceiptChipIcon(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(Dimensions.Icon.small)
    )
}

/**
 * The paper-receipt outline: scalloped top and bottom edges, and a half-circle
 * notch in each side at [tearOffset] (skipped until it has been measured).
 */
private class TxnReceiptShape(
    private val notchRadius: Float,
    private val tearOffset: Float,
    private val scallopRadius: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val scallopCount = (size.width / (scallopRadius * 2)).toInt().coerceAtLeast(1)
        val scallopWidth = size.width / scallopCount
        val bumpHeight = scallopWidth / 2f
        val hasNotch = tearOffset > bumpHeight + notchRadius &&
            tearOffset < size.height - bumpHeight - notchRadius

        val path = Path().apply {
            moveTo(0f, size.height - bumpHeight)
            if (hasNotch) {
                lineTo(0f, tearOffset + notchRadius)
                arcTo(
                    rect = Rect(-notchRadius, tearOffset - notchRadius, notchRadius, tearOffset + notchRadius),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false
                )
            }
            lineTo(0f, bumpHeight)
            for (i in 0 until scallopCount) {
                val x = i * scallopWidth
                arcTo(
                    rect = Rect(x, 0f, x + scallopWidth, scallopWidth),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }
            if (hasNotch) {
                lineTo(size.width, tearOffset - notchRadius)
                arcTo(
                    rect = Rect(
                        size.width - notchRadius, tearOffset - notchRadius,
                        size.width + notchRadius, tearOffset + notchRadius
                    ),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false
                )
            }
            lineTo(size.width, size.height - bumpHeight)
            for (i in 0 until scallopCount) {
                val x = size.width - i * scallopWidth
                arcTo(
                    rect = Rect(x - scallopWidth, size.height - scallopWidth, x, size.height),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * The same avatar rules as the transaction list row, so the shared-element
 * transition lands on the same picture: a brand logo keeps its brand tile, any
 * other merchant gets its category colour at low alpha with the category glyph.
 */
@Composable
private fun TxnDetailHeroAvatar(
    merchantName: String,
    category: String?,
    size: Dp,
    background: Color,
    modifier: Modifier = Modifier,
) {
    when (val icon = IconProvider.getTransactionIcon(merchantName, category)) {
        is IconResource.DrawableResource -> BrandIcon(
            merchantName = merchantName,
            category = category,
            modifier = modifier,
            size = size,
            showBackground = true
        )

        is IconResource.VectorIcon -> TxnDetailTintedAvatar(
            tint = icon.tint,
            size = size,
            background = background,
            modifier = modifier
        ) { glyphColor ->
            Icon(
                imageVector = icon.icon,
                contentDescription = merchantName,
                tint = glyphColor,
                modifier = Modifier.size(size * HERO_GLYPH_FRACTION)
            )
        }

        is IconResource.Emoji -> TxnDetailTintedAvatar(
            tint = icon.tint,
            size = size,
            background = background,
            modifier = modifier
        ) { _ ->
            EmojiGlyph(icon.emoji, size * HERO_GLYPH_FRACTION)
        }
    }
}

@Composable
private fun TxnDetailTintedAvatar(
    tint: Color,
    size: Dp,
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable (glyphColor: Color) -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val glyphColor = remember(tint, background, onSurface) {
        tint.legibleOn(background, towards = onSurface)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = TINTED_CONTAINER_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        content(glyphColor)
    }
}

/**
 * The title shown for a transaction: the user's saved alias (#583), else the
 * contact-resolved name when that toggle is on, else the raw merchant. The raw
 * merchant is still what the edit field renders so users can correct
 * mis-detections.
 */
@Composable
internal fun txnDetailMerchantTitle(
    transaction: TransactionEntity,
    alias: String?,
): String {
    val merchantDisplay = LocalMerchantDisplay.current
    return alias ?: merchantDisplay(transaction.merchantName) ?: transaction.merchantName
}
