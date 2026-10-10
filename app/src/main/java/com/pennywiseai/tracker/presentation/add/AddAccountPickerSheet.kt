package com.pennywiseai.tracker.presentation.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.domain.model.getAccountType
import com.pennywiseai.tracker.presentation.accounts.AccountType
import com.pennywiseai.tracker.presentation.transactions.TxnGlassSheet
import com.pennywiseai.tracker.presentation.transactions.bankBrandColor
import com.pennywiseai.tracker.presentation.transactions.txnGlass
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.TiledIconBackground
import com.pennywiseai.tracker.ui.icons.BrandIcons
import com.pennywiseai.tracker.ui.icons.iconax.Card as CardIconax
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Wallet3
import com.pennywiseai.tracker.ui.icons.iconax.WalletMoney
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.formatBalance

/**
 * Cashiro-style account picker for the Add screens (single account, both legs
 * of a transfer, a subscription's funding account), on glass.
 *
 * Each account is a bank-brand card: logo, faint tiled watermark of the logo,
 * the account's name and type, and its balance (in the account's own currency).
 * Accounts stay grouped by type, and "manual entry" (no account) stays first
 * when the caller allows it. The selected card takes a primary rim and a check
 * badge; others have no border.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddAccountPickerSheet(
    accounts: List<AccountBalanceEntity>,
    selectedAccount: AccountBalanceEntity?,
    allowManualEntry: Boolean,
    onAccountSelected: (AccountBalanceEntity?) -> Unit,
    onDismissRequest: () -> Unit,
    title: String = stringResource(R.string.account_selection_title),
) {
    val groupedAccounts = remember(accounts) { accounts.groupBy { it.getAccountType() } }

    TxnGlassSheet(onDismissRequest = onDismissRequest) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                bottom = Dimensions.Padding.dialog,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.sm)
                        .semantics { heading() },
                )
            }

            if (allowManualEntry) {
                item(key = "manual") {
                    ManualEntryCard(
                        selected = selectedAccount == null,
                        onClick = { onAccountSelected(null) },
                    )
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.account_selection_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.md),
                    )
                }
            } else {
                groupedAccounts.forEach { (accountType, accountList) ->
                    item(key = "account-type-${accountType.name}") {
                        Text(
                            text = accountType.localizedName(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(top = Spacing.sm)
                                .semantics { heading() },
                        )
                    }
                    items(
                        items = accountList,
                        key = { account -> "${account.id}|${account.bankName}|${account.accountLast4}" },
                    ) { account ->
                        AccountBrandCard(
                            account = account,
                            accountType = accountType,
                            selected = account.isSameAccountAs(selectedAccount),
                            onClick = { onAccountSelected(account) },
                        )
                    }
                }
            }
        }
    }
}

/** The "no account" option, as a glass pill-card matching the account cards. */
@Composable
private fun ManualEntryCard(selected: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge
    // Selected fill is primaryContainer, so text must use its on-role.
    val titleColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val supportingColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .txnGlass(
                shape = shape,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                rimColor = if (selected) MaterialTheme.colorScheme.primary else null,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = Dimensions.Component.listItemMinHeightTwoLine)
            .padding(Dimensions.Padding.card),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
    ) {
        TypeGlyph(icon = Icons.Default.Block)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.account_selection_manual),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
            Text(
                text = stringResource(R.string.account_selection_manual_description),
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor,
            )
        }
        if (selected) SelectedBadge()
    }
}

/**
 * One account as a Cashiro bank-brand card: logo, watermark, name, type and
 * ••last4, and the balance in the account's own currency on the right.
 */
@Composable
private fun AccountBrandCard(
    account: AccountBalanceEntity,
    accountType: AccountType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val hasLogo = remember(account.bankName) { BrandIcons.getIconResource(account.bankName) != null }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .txnGlass(
                shape = shape,
                rimColor = if (selected) MaterialTheme.colorScheme.primary else null,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        TiledIconBackground(merchantName = account.bankName, modifier = Modifier.matchParentSize())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Component.listItemMinHeightTwoLine)
                .padding(Dimensions.Padding.card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
        ) {
            if (hasLogo) {
                BrandIcon(
                    merchantName = account.bankName,
                    size = Dimensions.Icon.avatarLarge,
                    showBackground = true,
                )
            } else {
                TypeGlyph(icon = accountType.glyph())
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = account.subtitle(accountType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = account.formatBalance(),
                    style = PennyWiseText.amountRow,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (selected) SelectedBadge(modifier = Modifier.padding(top = Spacing.xs))
            }
        }
    }
}

@Composable
private fun TypeGlyph(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(Dimensions.Icon.avatarLarge)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = Dimensions.Alpha.tonalIconContainer),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimensions.Icon.medium),
        )
    }
}

@Composable
private fun SelectedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Dimensions.Icon.medium)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(R.string.account_selection_selected),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .padding(Spacing.xs)
                .fillMaxSize(),
        )
    }
}

/** Balance rows get a new id on refresh; bank + tail is the stable identity. */
private fun AccountBalanceEntity.isSameAccountAs(other: AccountBalanceEntity?): Boolean {
    if (other == null) return false
    return bankName.equals(other.bankName, ignoreCase = true) && accountLast4 == other.accountLast4
}

@Composable
private fun AccountBalanceEntity.subtitle(accountType: AccountType): String {
    val tail = accountLast4
        .takeUnless { it == AccountBalanceEntity.WALLET_ACCOUNT_MARKER }
        ?.let { "••$it" }
    // Without an alias the label already ends in the tail ("BANK ••1234").
    return listOfNotNull(accountType.localizedName(), tail.takeIf { !alias.isNullOrBlank() })
        .joinToString(" · ")
}

@Composable
private fun AccountType.localizedName(): String = when (this) {
    AccountType.SAVINGS -> stringResource(R.string.account_type_savings)
    AccountType.CURRENT -> stringResource(R.string.account_type_current)
    AccountType.CREDIT -> stringResource(R.string.account_type_credit)
    AccountType.CASH -> stringResource(R.string.account_type_cash)
}

private fun AccountType.glyph(): ImageVector = when (this) {
    AccountType.CASH -> Iconax.WalletMoney
    AccountType.CREDIT -> Iconax.CardIconax
    AccountType.SAVINGS, AccountType.CURRENT -> Iconax.Wallet3
}
