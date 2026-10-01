package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.data.contacts.LocalMerchantDisplay
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.LocalNavAnimatedVisibilityScope
import com.pennywiseai.tracker.ui.LocalSharedTransitionScope
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.DashedLine
import com.pennywiseai.tracker.ui.components.EmojiGlyph
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TINTED_CONTAINER_ALPHA
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
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

/*
 * The read-only hero of the Transaction Detail screen: who the money went to,
 * what kind of transaction it was, and how much. Cashiro-style: a large tonal
 * card with a big avatar up top, a dashed "tear" line like the paper receipt
 * Cashiro draws, and the amount in its type colour underneath.
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

/** The hero avatar: a step above the 48dp row avatar the transaction list shows. */
private val HeroAvatarSize: Dp = Dimensions.Icon.avatarLarge + Spacing.md

/** The category glyph fills this share of the avatar circle. */
private const val HERO_GLYPH_FRACTION = 0.5f

/**
 * @param merchantTitle The name to show: the user's alias, else the
 *  contact-resolved name, else the raw merchant. Resolved by the caller so the
 *  hero and the edit form agree on it.
 */
@Composable
internal fun TxnDetailHero(
    transaction: TransactionEntity,
    merchantTitle: String,
    primaryCurrency: String,
    convertedAmount: BigDecimal?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val typeColor = txnDetailTypeColor(transaction.transactionType)
    val cardColor = scheme.surfaceContainerLow
    val onSurface = scheme.onSurface
    val typeTextColor = remember(typeColor, cardColor, onSurface) {
        typeColor.legibleOn(background = cardColor, towards = onSurface)
    }

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

    PennyWiseCardV2(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = Spacing.lg
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            TxnDetailHeroAvatar(
                merchantName = transaction.merchantName,
                category = transaction.category,
                size = HeroAvatarSize,
                background = cardColor,
                modifier = avatarModifier
            )

            Text(
                text = merchantTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            SubtitleTag(
                text = stringResource(transactionTypeLabel(transaction.transactionType)),
                color = typeColor,
                textColor = typeTextColor,
                icon = txnDetailTypeIcon(transaction.transactionType)
            )

            // The tear line: identity above, money below.
            DashedLine(
                modifier = Modifier.padding(vertical = Spacing.sm),
                color = scheme.outlineVariant
            )

            Text(
                text = "${txnDetailSign(transaction.transactionType)}${transaction.formatAmount()}",
                style = PennyWiseText.heroAmount,
                color = typeColor,
                textAlign = TextAlign.Center
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
        }
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
