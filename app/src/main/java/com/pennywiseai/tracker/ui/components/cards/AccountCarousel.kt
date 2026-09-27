package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.TiledIconBackground
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.formatBalance
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

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
    val containerColor = when {
        isLowBalance -> MaterialTheme.colorScheme.errorContainer.copy(
            alpha = if (blurEffects) 0.5f else 0.7f
        )
        blurEffects -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
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

    val cardShape = MaterialTheme.shapes.extraLarge
    val accountAlias = account.alias?.trim()?.takeIf(String::isNotEmpty)
    val accountNumber = AccountBalanceEntity.accountLabel(account.bankName, account.accountLast4)
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

    PennyWiseCardV2(
        modifier = modifier
            .height(Dimensions.Component.accountCardHeight)
            .then(
                if (blurEffects && hazeState != null) Modifier
                    .clip(cardShape)
                    .hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = HazeDefaults.tint(containerColor),
                                blurRadius = 20.dp,
                                noiseFactor = -1f,
                            )
                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                        }
                    )
                else Modifier
            ),
        onClick = onClick,
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
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
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                containerColor.copy(alpha = 0.72f),
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
                // The top edge is intentionally quiet: the brand mark and type
                // establish identity while the account name remains in the lower
                // content area where it can be read alongside the balance.
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

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = accountTypeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(
                                horizontal = Spacing.sm,
                                vertical = Spacing.xs
                            )
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = accountAlias ?: account.bankName,
                        style = MaterialTheme.typography.titleMedium,
                        color = primaryContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val supportingText = when {
                        accountAlias != null -> accountNumber
                        account.accountLast4 != AccountBalanceEntity.WALLET_ACCOUNT_MARKER -> {
                            "••${account.accountLast4}"
                        }
                        else -> null
                    }
                    supportingText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // The label is deliberately non-clickable: the entire card is
                    // the navigation target, so there is no competing nested action.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isLowBalance -> stringResource(
                                        R.string.account_card_low_balance
                                    )
                                    isCreditCard -> stringResource(
                                        R.string.account_card_outstanding
                                    )
                                    else -> stringResource(R.string.account_card_balance)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isLowBalance) FontWeight.Medium else null,
                                color = secondaryContentColor
                            )
                            Text(
                                // The amount remains hidden by default. In unified
                                // mode the entity already holds the display currency,
                                // so account.formatBalance() preserves its semantics.
                                text = if (isAmountHidden) "••••••" else account.formatBalance(),
                                style = PennyWiseText.amountLarge,
                                color = primaryContentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

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
                                modifier = Modifier.size(Dimensions.Icon.small),
                                tint = secondaryContentColor
                            )
                        }

                        Text(
                            text = stringResource(R.string.account_carousel_view_details),
                            style = MaterialTheme.typography.labelLarge,
                            color = primaryContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
