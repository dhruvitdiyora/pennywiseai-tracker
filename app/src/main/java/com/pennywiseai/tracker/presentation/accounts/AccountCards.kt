package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.CardEntity
import com.pennywiseai.tracker.data.database.entity.CardType
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.ui.components.TiledIconBackground
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.icons.iconax.Bag
import com.pennywiseai.tracker.ui.icons.iconax.Balance
import com.pennywiseai.tracker.ui.icons.iconax.CalendarEdit
import com.pennywiseai.tracker.ui.icons.iconax.Card
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Eye
import com.pennywiseai.tracker.ui.icons.iconax.EyeSlash
import com.pennywiseai.tracker.ui.icons.iconax.History
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.NotificationBing
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.success
import com.pennywiseai.tracker.ui.theme.warning
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * The account identity card shared by bank accounts, credit cards and the live
 * preview in the editor: a tinted top zone (label, headline amount, optional
 * action menu) over the bank's tiled logo, then a strip with the account's name,
 * masked number and avatar, then an optional [footer] zone for details and the
 * primary action.
 *
 * [isAlert] swaps the top zone for the error container (a low-balance warning);
 * [isHidden] recesses the whole card so ignored accounts read as set aside.
 * [amount] arrives already formatted in the account's own currency, so nothing
 * here ever adds amounts across currencies.
 */
@Composable
internal fun AccountCardShell(
    merchantName: String,
    label: String,
    amount: String,
    title: String,
    avatar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isHidden: Boolean = false,
    isAlert: Boolean = false,
    amountColor: Color? = null,
    menu: (@Composable () -> Unit)? = null,
    titleBadges: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val topColor = when {
        isAlert -> scheme.errorContainer
        isHidden -> scheme.surfaceContainerLow
        else -> scheme.surfaceContainer
    }
    val onTopColor = if (isAlert) scheme.onErrorContainer else scheme.onSurface
    val labelColor = if (isAlert) scheme.onErrorContainer else scheme.onSurfaceVariant
    // Strip: solid, like the card fill.
    val stripColor = (if (isHidden) scheme.surfaceContainerLowest else scheme.surfaceContainerLow)
        .copy(alpha = Dimensions.Glass.fillAlphaSolid)
    val hasMenu = menu != null

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tint = topColor,
        solidFillAlpha = if (isAlert) GLASS_TINTED_ALPHA else Dimensions.Glass.fillAlphaSolid,
        // Full-bleed layers: the watermark and the strip reach the card edge.
        contentPadding = Dimensions.Padding.none,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            TiledIconBackground(
                merchantName = merchantName,
                modifier = Modifier.matchParentSize(),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Spacing.md,
                            end = if (hasMenu) Spacing.sm else Spacing.md,
                            top = if (hasMenu) Spacing.sm else Spacing.md,
                        )
                        .then(
                            if (hasMenu) {
                                Modifier.defaultMinSize(minHeight = Dimensions.Component.minTouchTarget)
                            } else {
                                Modifier
                            }
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isAlert) FontWeight.Medium else null,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    menu?.invoke()
                }

                FittedAmountText(
                    text = amount,
                    style = PennyWiseText.heroAmount,
                    color = amountColor ?: onTopColor,
                    modifier = Modifier.padding(horizontal = Spacing.md),
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Identity strip: fades the watermark out under the text.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, stripColor, stripColor)
                            )
                        )
                        .padding(Spacing.md),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    // Ellipsize a long name instead of pushing the badges
                                    // (and the masked number below) off-screen.
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                titleBadges?.invoke(this)
                            }
                            // The masked number sits on its own line below the name:
                            // inlining it next to a long bank name squeezed it into a
                            // per-character vertical stack. (#465)
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        avatar()
                    }
                }

                if (footer != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(stripColor)
                            .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.smd),
                        content = footer,
                    )
                }
            }
        }
    }
}

/** The small "Business" tag beside an account's name. */
@Composable
private fun BusinessBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = stringResource(R.string.manage_accounts_business_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        )
    }
}

/** The eye-slash glyph that marks an ignored (hidden) account. */
@Composable
private fun IgnoredMarker() {
    Icon(
        imageVector = Iconax.EyeSlash,
        contentDescription = stringResource(R.string.manage_accounts_hidden_badge),
        modifier = Modifier.size(Dimensions.Icon.small),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun AccountItem(
    account: AccountBalanceEntity,
    linkedCards: List<CardEntity> = emptyList(),
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit,
    onUnlinkCard: (cardId: Long) -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onEditAccount: () -> Unit = {},
    onSetProfile: (Long) -> Unit = {},
    onSetAlias: (String?) -> Unit = {},
    onSetLowBalanceThreshold: (BigDecimal?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isManualAccount = account.sourceType == "MANUAL"
    var showAliasDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    // Low-balance alert: only for non-credit accounts with a threshold set, when the
    // current balance has fallen at or below it. (Credit cards invert this — their
    // "low" concept is available limit, not balance — so they're excluded.)
    val isLowBalance = !account.isCreditCard &&
        account.lowBalanceThreshold != null &&
        account.balance <= account.lowBalanceThreshold
    val alias = account.alias?.takeIf { it.isNotBlank() }
    val isBusiness = account.profileId == ProfileEntity.BUSINESS_ID
    // Resolve so the list matches Account Detail — SMS-tracked non-INR accounts
    // show their parser currency, not stored INR.
    val currencyCode = CurrencyFormatter.resolveAccountCurrency(
        sourceType = account.sourceType,
        storedCurrency = account.currency,
        bankName = account.bankName,
    )

    AccountCardShell(
        merchantName = account.bankName,
        label = stringResource(
            if (isLowBalance) R.string.manage_accounts_low_balance else R.string.manage_accounts_balance
        ),
        amount = CurrencyFormatter.formatCurrency(account.balance, currencyCode),
        title = alias ?: account.bankName,
        subtitle = if (alias != null) {
            AccountBalanceEntity.accountLabel(account.bankName, account.accountLast4)
        } else {
            AccountBalanceEntity.accountLabel("", account.accountLast4).trim()
        },
        avatar = { AccountAvatar(account = account) },
        modifier = modifier,
        isHidden = isHidden,
        isAlert = isLowBalance,
        menu = {
            AccountMoreMenu { closeMenu ->
                AccountMenuItem(
                    text = stringResource(R.string.manage_accounts_menu_history),
                    icon = Iconax.History,
                    onClick = {
                        closeMenu()
                        onViewHistory()
                    },
                )
                AccountMenuItem(
                    text = stringResource(
                        if (isHidden) R.string.manage_accounts_menu_show else R.string.manage_accounts_menu_hide
                    ),
                    icon = if (isHidden) Iconax.Eye else Iconax.EyeSlash,
                    onClick = {
                        closeMenu()
                        onToggleVisibility()
                    },
                )
                AccountMenuItem(
                    text = stringResource(
                        if (isBusiness) {
                            R.string.manage_accounts_menu_mark_personal
                        } else {
                            R.string.manage_accounts_menu_mark_business
                        }
                    ),
                    icon = if (isBusiness) Icons.Default.Person else Icons.Default.Business,
                    onClick = {
                        closeMenu()
                        onSetProfile(
                            if (isBusiness) ProfileEntity.PERSONAL_ID else ProfileEntity.BUSINESS_ID
                        )
                    },
                )
                AccountMenuItem(
                    text = stringResource(
                        if (alias == null) {
                            R.string.manage_accounts_menu_set_alias
                        } else {
                            R.string.manage_accounts_menu_rename
                        }
                    ),
                    icon = Iconax.Edit2,
                    onClick = {
                        closeMenu()
                        showAliasDialog = true
                    },
                )
                // Only non-credit accounts — credit cards' "low" concept is
                // available limit, not balance, so the alert (and this entry)
                // don't apply (matches the isLowBalance exclusion above).
                if (!account.isCreditCard) {
                    AccountMenuItem(
                        text = stringResource(
                            if (account.lowBalanceThreshold == null) {
                                R.string.manage_accounts_low_balance_alert
                            } else {
                                R.string.manage_accounts_menu_edit_low_balance_alert
                            }
                        ),
                        icon = Iconax.NotificationBing,
                        onClick = {
                            closeMenu()
                            showThresholdDialog = true
                        },
                    )
                }
                AccountMenuItem(
                    text = stringResource(R.string.accounts_action_delete),
                    icon = Iconax.Bag,
                    destructive = true,
                    onClick = {
                        closeMenu()
                        onDeleteAccount()
                    },
                )
            }
        },
        titleBadges = {
            if (isBusiness) BusinessBadge()
            if (isHidden) IgnoredMarker()
        },
        footer = {
            if (linkedCards.isNotEmpty()) {
                LinkedCardsSection(linkedCards = linkedCards, onUnlinkCard = onUnlinkCard)
            }
            FilledTonalButton(onClick = if (isManualAccount) onEditAccount else onUpdateBalance) {
                Icon(
                    imageVector = if (isManualAccount) Iconax.Edit2 else Iconax.Balance,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    stringResource(
                        if (isManualAccount) {
                            R.string.manage_accounts_action_edit
                        } else {
                            R.string.manage_accounts_update_balance
                        }
                    )
                )
            }
        },
    )

    if (showAliasDialog) {
        AccountAliasDialog(
            currentAlias = account.alias,
            accountLabel = AccountBalanceEntity.accountLabel(account.bankName, account.accountLast4),
            onDismiss = { showAliasDialog = false },
            onConfirm = { newAlias ->
                onSetAlias(newAlias)
                showAliasDialog = false
            },
        )
    }

    if (showThresholdDialog) {
        LowBalanceThresholdDialog(
            currentThreshold = account.lowBalanceThreshold,
            accountLabel = AccountBalanceEntity.accountLabel(account.bankName, account.accountLast4),
            currency = currencyCode,
            currentBalance = account.balance,
            onDismiss = { showThresholdDialog = false },
            onConfirm = { threshold ->
                onSetLowBalanceThreshold(threshold)
                showThresholdDialog = false
            },
        )
    }
}

/** The debit cards tied to a bank account, as a connected block of rows. */
@Composable
private fun LinkedCardsSection(
    linkedCards: List<CardEntity>,
    onUnlinkCard: (cardId: Long) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(R.string.manage_accounts_linked_cards),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
        )
        GroupedList {
            linkedCards.forEachIndexed { index, card ->
                GroupedRow(
                    position = ListItemPosition.from(index, linkedCards.size),
                    containerColor = scheme.surfaceContainer,
                    contentPadding = PaddingValues(
                        start = Spacing.md,
                        end = Spacing.xs,
                        top = Spacing.xs,
                        bottom = Spacing.xs,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                ) {
                    Icon(
                        imageVector = Iconax.Card,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.inline),
                        tint = scheme.onSurfaceVariant,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(
                                text = stringResource(R.string.manage_accounts_masked_number, card.cardLast4),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface,
                            )
                            if (!card.isActive) {
                                Text(
                                    text = stringResource(R.string.manage_accounts_card_inactive),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.error,
                                )
                            }
                        }
                        // The last time this card moved the balance, when known.
                        card.lastBalanceDate?.let { updatedAt ->
                            Text(
                                text = stringResource(
                                    R.string.manage_accounts_card_updated,
                                    updatedAt.format(DateTimeFormatter.ofPattern("MMM dd")),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { onUnlinkCard(card.id) }) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = stringResource(R.string.manage_accounts_unlink_card),
                            modifier = Modifier.size(Dimensions.Icon.inline),
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CreditCardItem(
    card: AccountBalanceEntity,
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit,
    onDeleteAccount: () -> Unit,
    onEditAccount: () -> Unit = {},
    onSetStatementDay: (Int?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showStatementDayDialog by remember { mutableStateOf(false) }
    val isManualAccount = card.sourceType == "MANUAL"
    val creditLimit = card.creditLimit
    val available = (creditLimit ?: BigDecimal.ZERO) - card.balance
    val utilization = if (creditLimit != null && creditLimit > BigDecimal.ZERO) {
        ((card.balance.toDouble() / creditLimit.toDouble()) * 100).toInt()
    } else {
        0
    }
    val scheme = MaterialTheme.colorScheme
    val utilizationColor = when {
        utilization > 70 -> scheme.error
        utilization > 30 -> scheme.warning
        else -> scheme.success
    }

    AccountCardShell(
        merchantName = card.bankName,
        label = stringResource(R.string.manage_accounts_outstanding),
        amount = CurrencyFormatter.formatCurrency(card.balance, card.currency),
        title = card.bankName,
        // Masked card number on its own line below the name (#465).
        subtitle = stringResource(R.string.manage_accounts_masked_number, card.accountLast4),
        avatar = { AccountAvatar(account = card) },
        modifier = modifier,
        isHidden = isHidden,
        amountColor = if (card.balance > BigDecimal.ZERO) scheme.error else null,
        menu = {
            AccountMoreMenu { closeMenu ->
                AccountMenuItem(
                    text = stringResource(R.string.manage_accounts_menu_history),
                    icon = Iconax.History,
                    onClick = {
                        closeMenu()
                        onViewHistory()
                    },
                )
                AccountMenuItem(
                    text = stringResource(
                        if (isHidden) R.string.manage_accounts_menu_show else R.string.manage_accounts_menu_hide
                    ),
                    icon = if (isHidden) Iconax.Eye else Iconax.EyeSlash,
                    onClick = {
                        closeMenu()
                        onToggleVisibility()
                    },
                )
                AccountMenuItem(
                    text = if (card.statementDay != null) {
                        stringResource(R.string.manage_accounts_menu_statement_date, card.statementDay)
                    } else {
                        stringResource(R.string.manage_accounts_menu_set_statement_date)
                    },
                    icon = Iconax.CalendarEdit,
                    onClick = {
                        closeMenu()
                        showStatementDayDialog = true
                    },
                )
                AccountMenuItem(
                    text = stringResource(R.string.accounts_action_delete),
                    icon = Iconax.Bag,
                    destructive = true,
                    onClick = {
                        closeMenu()
                        onDeleteAccount()
                    },
                )
            }
        },
        titleBadges = {
            if (isHidden) IgnoredMarker()
        },
        footer = {
            if (creditLimit != null) {
                // How much of the limit is in use, at a glance.
                LinearProgressIndicator(
                    progress = { (utilization / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.Component.progressBarHeight)
                        .clip(CircleShape),
                    color = utilizationColor,
                    trackColor = scheme.surfaceContainerHighest,
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    // Available Credit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.manage_accounts_available),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = Spacing.sm),
                        )
                        Text(
                            text = CurrencyFormatter.formatCurrency(available, card.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = scheme.success,
                        )
                    }
                    // Credit Limit with Utilization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = stringResource(R.string.manage_accounts_credit_limit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                        ) {
                            Text(
                                text = CurrencyFormatter.formatCurrency(creditLimit, card.currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = scheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.manage_accounts_utilization_used, utilization),
                                style = MaterialTheme.typography.bodySmall,
                                color = utilizationColor,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.manage_accounts_credit_limit_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            FilledTonalButton(onClick = if (isManualAccount) onEditAccount else onUpdateBalance) {
                Icon(
                    imageVector = if (isManualAccount) Iconax.Edit2 else Iconax.Balance,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    stringResource(
                        if (isManualAccount) {
                            R.string.manage_accounts_action_edit
                        } else {
                            R.string.manage_accounts_action_update
                        }
                    )
                )
            }
        },
    )

    if (showStatementDayDialog) {
        StatementDayPickerDialog(
            currentDay = card.statementDay,
            onDismiss = { showStatementDayDialog = false },
            onConfirm = { day ->
                onSetStatementDay(day)
                showStatementDayDialog = false
            },
        )
    }
}

/**
 * A debit card seen in SMS that is not yet tied to an account: the card's
 * identity, last known balance and the message that revealed it, with Link as
 * the primary action and Edit / Delete in the overflow menu.
 */
@Composable
internal fun OrphanedCardItem(
    card: CardEntity,
    accounts: List<AccountBalanceEntity>,
    onLinkToAccount: (String) -> Unit,
    onDeleteCard: (Long) -> Unit,
    onUpdateCard: (
        bankName: String,
        cardType: CardType,
        nickname: String?
    ) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    var showLinkDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var expandedSource by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = { expandedSource = !expandedSource },
        contentPadding = Dimensions.Padding.card,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.smd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(
                    icon = Iconax.Card,
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    Text(
                        text = stringResource(
                            R.string.manage_accounts_card_identity,
                            card.bankName,
                            card.cardLast4,
                        ),
                        style = PennyWiseText.rowTitle,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            if (card.cardType == CardType.CREDIT) {
                                R.string.manage_accounts_unlinked_credit_card
                            } else {
                                R.string.manage_accounts_unlinked_debit_card
                            }
                        ),
                        style = PennyWiseText.rowSubtitle,
                        color = scheme.onSurfaceVariant,
                    )
                    // Show last known balance if available
                    card.lastBalance?.let { lastBalance ->
                        Text(
                            text = stringResource(
                                R.string.manage_accounts_last_balance,
                                CurrencyFormatter.formatCurrency(lastBalance, card.currency),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.primary,
                        )
                    }
                }
                AccountMoreMenu { closeMenu ->
                    AccountMenuItem(
                        text = stringResource(R.string.manage_accounts_action_edit),
                        icon = Iconax.Edit2,
                        onClick = {
                            closeMenu()
                            showEditDialog = true
                        },
                    )
                    AccountMenuItem(
                        text = stringResource(R.string.accounts_action_delete),
                        icon = Iconax.Bag,
                        destructive = true,
                        onClick = {
                            closeMenu()
                            onDeleteCard(card.id)
                        },
                    )
                }
            }

            // Show source SMS that triggered card detection
            card.lastBalanceSource?.let { source ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = scheme.surfaceContainer,
                ) {
                    Text(
                        text = if (expandedSource) {
                            stringResource(R.string.manage_accounts_card_sms, source)
                        } else {
                            stringResource(R.string.manage_accounts_card_sms_truncated, source.take(SMS_PREVIEW_CHARS))
                        },
                        modifier = Modifier.padding(Spacing.smd),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = scheme.onSurfaceVariant,
                        maxLines = if (expandedSource) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            FilledTonalButton(onClick = { showLinkDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(stringResource(R.string.manage_accounts_action_link))
            }
        }
    }

    if (showLinkDialog) {
        LinkCardDialog(
            card = card,
            accounts = accounts.filter { it.bankName == card.bankName },
            onDismiss = { showLinkDialog = false },
            onConfirm = { accountLast4 ->
                onLinkToAccount(accountLast4)
                showLinkDialog = false
            },
        )
    }

    if (showEditDialog) {
        EditCardDialog(
            card = card,
            onDismiss = { showEditDialog = false },
            onConfirm = { bankName, cardType, nickname ->
                onUpdateCard(bankName, cardType, nickname)
                showEditDialog = false
            },
        )
    }
}

/** How much of the source SMS the collapsed orphaned-card row previews. */
private const val SMS_PREVIEW_CHARS = 80
