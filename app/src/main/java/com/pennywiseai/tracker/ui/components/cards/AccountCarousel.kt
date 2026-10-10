package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.TiledIconBackground
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.formatBalance
import dev.chrisbanes.haze.HazeState

@Composable
fun AccountCarousel(
    bankAccounts: List<AccountBalanceEntity>,
    creditCards: List<AccountBalanceEntity>,
    onAccountClick: (bankName: String, accountLast4: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    isUnifiedMode: Boolean = false,
    selectedCurrency: String = "INR",
    blurEffects: Boolean = false,
    hazeState: HazeState? = null
) {
    val allAccounts = bankAccounts + creditCards

    if (allAccounts.isEmpty()) return

    if (allAccounts.size == 1) {
        val account = allAccounts.first()
        AccountCarouselCard(
            account = account,
            isCreditCard = creditCards.contains(account),
            onClick = { onAccountClick(account.bankName, account.accountLast4) },
            modifier = modifier.fillMaxWidth(),
            isUnifiedMode = isUnifiedMode,
            selectedCurrency = selectedCurrency,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    } else {
        val pagerState = rememberPagerState(pageCount = { allAccounts.size })

        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(end = Spacing.xl),
            pageSpacing = Spacing.md
        ) { page ->
            val account = allAccounts[page]
            AccountCarouselCard(
                account = account,
                isCreditCard = creditCards.contains(account),
                onClick = { onAccountClick(account.bankName, account.accountLast4) },
                modifier = Modifier.fillMaxWidth(),
                isUnifiedMode = isUnifiedMode,
                selectedCurrency = selectedCurrency,
                blurEffects = blurEffects,
                hazeState = hazeState
            )
        }
    }
}

/** Share of the card height the surface fade covers, measured up from the bottom. */
private const val ACCOUNT_CARD_FADE_FRACTION = 0.6f

/**
 * Cashiro-style account card: bank logo top-left, the faint tilted logo
 * pattern behind, then "BANK ••1234", the balance, the account type and an
 * outlined "View details" pill. The balance stays hidden until the eye is
 * tapped (privacy default), and a low-balance / outstanding qualifier rides
 * on the type line so no PennyWise state is lost.
 */
@Composable
private fun AccountCarouselCard(
    account: AccountBalanceEntity,
    isCreditCard: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUnifiedMode: Boolean = false,
    selectedCurrency: String = "INR",
    blurEffects: Boolean = false,
    hazeState: HazeState? = null
) {
    var isAmountHidden by remember { mutableStateOf(true) }
    // Low-balance alert: tint the home card red when a non-credit account's balance
    // is at or below its user-set threshold (matches Manage Accounts). #509
    val isLowBalance = !isCreditCard &&
        account.lowBalanceThreshold != null &&
        account.balance <= account.lowBalanceThreshold
    // Glass recipe (GlassCard): a low-balance card keeps its error-container
    // wash; every other card takes the neutral frosted fill.
    val glassTint = if (isLowBalance) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val glassSolidAlpha = if (isLowBalance) Dimensions.Glass.fillAlphaTinted else Dimensions.Glass.fillAlphaSolid
    val containerColor = glassFill(glassTint, blurEffects && hazeState != null, glassSolidAlpha)
    val primaryContentColor = if (isLowBalance) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryContentColor = if (isLowBalance) {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = Dimensions.Alpha.subtitle)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val pillBorderColor = if (isLowBalance) {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = Dimensions.Alpha.medium)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val cardShape = MaterialTheme.shapes.extraLarge
    val accountAlias = account.alias?.trim()?.takeIf(String::isNotEmpty)
    // "BANK ••1234" (or just the name for a wallet, which has no number). An
    // alias the user chose replaces the bank name, as it does everywhere else.
    val captionText = AccountBalanceEntity.accountLabel(
        accountAlias ?: account.bankName,
        account.accountLast4
    ).uppercase()
    val accountTypeLabel = when {
        isCreditCard || account.accountType.equals("CREDIT", ignoreCase = true) -> {
            stringResource(R.string.account_type_credit)
        }
        account.accountType.equals("CURRENT", ignoreCase = true) -> {
            stringResource(R.string.account_type_current)
        }
        account.accountType.equals("CASH", ignoreCase = true) -> {
            stringResource(R.string.account_type_cash)
        }
        else -> stringResource(R.string.account_type_savings)
    }
    val balanceQualifier: String? = when {
        isLowBalance -> stringResource(R.string.account_card_low_balance)
        isCreditCard -> stringResource(R.string.account_card_outstanding)
        else -> null
    }

    GlassCard(
        modifier = modifier.height(Dimensions.Component.accountCardHeight),
        onClick = onClick,
        shape = cardShape,
        blurEffects = blurEffects,
        hazeState = hazeState,
        tint = glassTint,
        solidFillAlpha = glassSolidAlpha,
        // Keep the decorative layers full-bleed; the foreground column owns the
        // standard card inset below so the watermark reaches the rounded edge.
        contentPadding = Dimensions.Padding.none
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            TiledIconBackground(
                merchantName = account.bankName,
                modifier = Modifier.matchParentSize()
            )
            // Fade the pattern out toward the bottom so the balance text sits on
            // a calm surface. The gradient starts from the container colour at
            // zero alpha (not Color.Transparent, which would tint the blend
            // toward black).
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(ACCOUNT_CARD_FADE_FRACTION)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                containerColor.copy(alpha = 0f),
                                containerColor,
                                containerColor
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimensions.Padding.card),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BrandIcon(
                        merchantName = account.bankName,
                        size = Dimensions.Icon.avatarLarge,
                        showBackground = true
                    )

                    // The eye lives apart from the card's own tap target, so
                    // revealing a balance never navigates.
                    IconButton(
                        onClick = { isAmountHidden = !isAmountHidden },
                        modifier = Modifier.size(Dimensions.Component.minTouchTarget)
                    ) {
                        Icon(
                            imageVector = if (isAmountHidden) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (isAmountHidden) {
                                stringResource(R.string.balance_show)
                            } else {
                                stringResource(R.string.balance_hide)
                            },
                            modifier = Modifier.size(Dimensions.Icon.inline),
                            tint = secondaryContentColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = captionText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = secondaryContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            // The amount remains hidden by default. In unified
                            // mode the entity already holds the display currency,
                            // so account.formatBalance() preserves its semantics.
                            text = if (isAmountHidden) "••••••" else account.formatBalance(),
                            style = PennyWiseText.amountLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = accountTypeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryContentColor,
                                maxLines = 1
                            )
                            if (balanceQualifier != null) {
                                Text(
                                    text = "·",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryContentColor
                                )
                                Text(
                                    text = balanceQualifier,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLowBalance) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        secondaryContentColor
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    ViewDetailsPill(
                        onClick = onClick,
                        contentColor = primaryContentColor,
                        borderColor = pillBorderColor
                    )
                }
            }
        }
    }
}

/** The outlined "View details ›" pill at the card's bottom-end corner. */
@Composable
private fun ViewDetailsPill(
    onClick: () -> Unit,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        contentColor = contentColor,
        border = BorderStroke(Dimensions.Component.hairline, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.smd, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = stringResource(R.string.account_carousel_view_details),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.Icon.small),
                tint = contentColor
            )
        }
    }
}
