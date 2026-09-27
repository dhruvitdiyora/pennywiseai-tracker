package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.domain.model.getAccountType
import com.pennywiseai.tracker.presentation.accounts.AccountType
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter

/**
 * Shared account picker for manual transaction, transfer, and subscription
 * entry. This component only presents choices; callers remain responsible for
 * updating their existing ViewModel state and dismissing the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionSheet(
    accounts: List<AccountBalanceEntity>,
    selectedAccount: AccountBalanceEntity?,
    allowManualEntry: Boolean,
    onAccountSelected: (AccountBalanceEntity?) -> Unit,
    onDismissRequest: () -> Unit,
    title: String = stringResource(R.string.account_selection_title)
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val groupedAccounts = remember(accounts) { accounts.groupBy { it.getAccountType() } }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Dimensions.Padding.dialog,
                end = Dimensions.Padding.dialog,
                bottom = Dimensions.Padding.dialog
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            item {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }

            if (allowManualEntry) {
                item {
                    AccountSelectionRow(
                        title = stringResource(R.string.account_selection_manual),
                        subtitle = stringResource(R.string.account_selection_manual_description),
                        icon = Icons.Default.Block,
                        isSelected = selectedAccount == null,
                        onClick = { onAccountSelected(null) }
                    )
                }
                if (accounts.isNotEmpty()) {
                    item { Spacer(Modifier.height(Spacing.sm)) }
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.account_selection_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.md)
                    )
                }
            } else {
                groupedAccounts.forEach { (accountType, accountList) ->
                    item(key = "account-type-${accountType.name}") {
                        Text(
                            text = accountType.localizedDisplayName(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                top = Spacing.sm,
                                bottom = Spacing.xs
                            )
                        )
                    }
                    items(
                        items = accountList,
                        key = { account ->
                            "${account.id}|${account.bankName}|${account.accountLast4}"
                        }
                    ) { account ->
                        AccountSelectionRow(
                            title = account.displayLabel,
                            subtitle = account.accountSubtitle(accountType),
                            icon = accountType.icon(),
                            isSelected = account.isSameAccountAs(selectedAccount),
                            balance = CurrencyFormatter.formatCurrency(account.balance, account.currency),
                            onClick = { onAccountSelected(account) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSelectionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    balance: String? = null
) {
    val primaryContentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val supportingContentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.medium),
                    tint = primaryContentColor
                )
            },
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = primaryContentColor
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = supportingContentColor
                    )
                    balance?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = supportingContentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            trailingContent = if (isSelected) {
                {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.account_selection_selected),
                        tint = primaryContentColor
                    )
                }
            } else null,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            tonalElevation = Dimensions.Elevation.none
        )
    }
}

private fun AccountBalanceEntity.isSameAccountAs(other: AccountBalanceEntity?): Boolean {
    if (other == null) return false
    // Balance rows receive a new database id as balances are refreshed. The
    // bank/tail pair is the stable account identity used by the entry flows.
    return bankName.equals(other.bankName, ignoreCase = true) &&
        accountLast4 == other.accountLast4
}

@Composable
private fun AccountBalanceEntity.accountSubtitle(accountType: AccountType): String {
    val tail = accountLast4
        .takeUnless { it == AccountBalanceEntity.WALLET_ACCOUNT_MARKER }
        ?.let { "••$it" }
    return listOfNotNull(
        accountType.localizedDisplayName(),
        if (alias?.isNullOrBlank() == false) tail else null
    ).joinToString(" · ")
}

@Composable
private fun AccountType.localizedDisplayName(): String = when (this) {
    AccountType.SAVINGS -> stringResource(R.string.account_type_savings)
    AccountType.CURRENT -> stringResource(R.string.account_type_current)
    AccountType.CREDIT -> stringResource(R.string.account_type_credit)
    AccountType.CASH -> stringResource(R.string.account_type_cash)
}

private fun AccountType.icon() = when (this) {
    AccountType.CASH -> Icons.Default.Money
    AccountType.CREDIT -> Icons.Default.CreditCard
    AccountType.SAVINGS, AccountType.CURRENT -> Icons.Default.AccountBalance
}
