package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.icons.iconax.Danger
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/**
 * Self-contained two-step picker for account merge (#368).
 *
 *  Step 1 — pick the **source** (account whose transactions will be moved).
 *  Step 2 — pick the **target** (account they'll be moved into). The list
 *           filters to accounts compatible with the source: same currency,
 *           same `isCreditCard` flag, not the source itself.
 *  Step 3 — a confirmation dialog showing the actual transaction count.
 *
 * On confirm the sheet fires [onConfirm] and dismisses; the parent
 * ViewModel runs the merge + emits a success message. One-way operation
 * for v1 — no undo, hence the explicit confirmation step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeAccountsSheet(
    accounts: List<AccountBalanceEntity>,
    countTransactionsOn: suspend (bankName: String, accountLast4: String) -> Int,
    onConfirm: (source: AccountBalanceEntity, target: AccountBalanceEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var source by remember { mutableStateOf<AccountBalanceEntity?>(null) }
    var target by remember { mutableStateOf<AccountBalanceEntity?>(null) }
    var sourceTxnCount by remember { mutableStateOf<Int?>(null) }

    // Whenever the source changes, resolve its transaction count so the
    // confirmation step can show "Move N transactions into …".
    LaunchedEffect(source) {
        sourceTxnCount = source?.let { countTransactionsOn(it.bankName, it.accountLast4) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The account rows are tonal, so the sheet sits one step lighter.
        containerColor = glassSheetContainerColor()
    ) {
        // A single LazyColumn drives the whole sheet so that, with many accounts,
        // the source and target lists share one scroll surface — the second list
        // stays reachable instead of being pushed off-screen (#624). Previously
        // two nested LazyColumns competed for height inside a non-scrolling Column.
        // Rows form a connected 2dp-gap block; structural items (header, section
        // labels) carry their own paddings.
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.dialog,
                end = Dimensions.Padding.dialog,
                bottom = Spacing.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.merge_accounts_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.merge_accounts_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Source picker
            item(key = "from-label") { SectionLabel(stringResource(R.string.merge_accounts_move_from)) }
            accountPickerItems(
                idPrefix = "src",
                accounts = accounts,
                selected = source,
                onSelect = { picked ->
                    source = picked
                    // If the previously-chosen target is no longer compatible, clear it.
                    target?.let { t -> if (!compatible(picked, t)) target = null }
                }
            )

            // Target picker — only meaningful once a source is picked.
            source?.let { src ->
                val targets = accounts.filter { compatible(src, it) }
                if (targets.isEmpty()) {
                    item(key = "no-targets") {
                        Text(
                            text = stringResource(R.string.merge_accounts_no_targets),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.md)
                        )
                    }
                } else {
                    item(key = "into-header") {
                        Column(modifier = Modifier.padding(top = Spacing.md)) {
                            SectionLabel(stringResource(R.string.merge_accounts_into))
                            Row(
                                modifier = Modifier.padding(bottom = Spacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimensions.Icon.small),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = AccountBalanceEntity.accountLabel(src.bankName, src.accountLast4),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    accountPickerItems(
                        idPrefix = "tgt",
                        accounts = targets,
                        selected = target,
                        onSelect = { target = it }
                    )
                }
            }
        }
    }

    // Confirmation dialog appears once both ends are chosen. Tap "Merge" once
    // here to actually run the operation — sheet is one-way and not undoable.
    val s = source
    val t = target
    if (s != null && t != null) {
        AlertDialog(
            onDismissRequest = { target = null },
            shape = MaterialTheme.shapes.extraLarge,
            iconContentColor = MaterialTheme.colorScheme.error,
            icon = { Icon(Iconax.Danger, contentDescription = null) },
            title = { Text(stringResource(R.string.merge_accounts_confirm_title)) },
            text = {
                val n = sourceTxnCount
                Text(
                    if (n != null)
                        pluralStringResource(R.plurals.merge_accounts_confirm_message_count, n, n, AccountBalanceEntity.accountLabel(s.bankName, s.accountLast4), AccountBalanceEntity.accountLabel(t.bankName, t.accountLast4))
                    else
                        stringResource(R.string.merge_accounts_confirm_message_all, AccountBalanceEntity.accountLabel(s.bankName, s.accountLast4), AccountBalanceEntity.accountLabel(t.bankName, t.accountLast4))
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(s, t) }) { Text(stringResource(R.string.merge_accounts_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { target = null }) { Text(stringResource(R.string.accounts_action_cancel)) }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = Spacing.sm)
    )
}

/**
 * Emits selectable account rows directly into the parent [LazyColumn] (rather
 * than nesting its own scroll container), so both the source and target lists
 * live on the sheet's single scroll surface. [idPrefix] keeps item keys unique
 * across the two lists.
 */
private fun LazyListScope.accountPickerItems(
    idPrefix: String,
    accounts: List<AccountBalanceEntity>,
    selected: AccountBalanceEntity?,
    onSelect: (AccountBalanceEntity) -> Unit
) {
    itemsIndexed(
        accounts,
        key = { _, account -> "$idPrefix|${account.bankName}|${account.accountLast4}|${account.id}" }
    ) { index, acct ->
        val isSelected = selected?.bankName == acct.bankName &&
            selected.accountLast4 == acct.accountLast4
        AccountPickerRow(
            acct = acct,
            position = ListItemPosition.from(index, accounts.size),
            isSelected = isSelected,
            onClick = { onSelect(acct) }
        )
    }
}

@Composable
private fun AccountPickerRow(
    acct: AccountBalanceEntity,
    position: ListItemPosition,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    AccountChoiceRow(
        selected = isSelected,
        onClick = onClick,
        position = position
    ) { titleColor, supportingColor ->
        AccountAvatar(account = acct, size = Dimensions.Icon.list)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = acct.bankName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val creditTag = stringResource(R.string.merge_accounts_credit_tag)
            Text(
                text = buildString {
                    if (acct.accountLast4 != AccountBalanceEntity.WALLET_ACCOUNT_MARKER) {
                        append("••")
                        append(acct.accountLast4)
                        append(" · ")
                    }
                    append(acct.currency)
                    if (acct.isCreditCard) append(" · $creditTag")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor
            )
        }
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) titleColor else supportingColor
        )
    }
}

/** Two accounts can be merged when they share currency + credit-card flag. */
private fun compatible(a: AccountBalanceEntity, b: AccountBalanceEntity): Boolean {
    val sameAccount = a.bankName.equals(b.bankName, ignoreCase = true) &&
        a.accountLast4 == b.accountLast4
    if (sameAccount) return false
    if (!a.currency.equals(b.currency, ignoreCase = true)) return false
    if (a.isCreditCard != b.isCreditCard) return false
    return true
}
