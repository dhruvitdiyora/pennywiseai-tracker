package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.CardEntity
import com.pennywiseai.tracker.data.database.entity.CardType
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.icons.iconax.CalendarEdit
import com.pennywiseai.tracker.ui.icons.iconax.Card
import com.pennywiseai.tracker.ui.icons.iconax.Danger
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.NotificationBing
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

/*
 * The small confirmation and entry dialogs of Manage Accounts. They share one
 * look: a rounded dialog, a tonal icon on top, tonal fields for input and
 * plain text buttons, matching the Lend & Borrow dialogs.
 */

/** Earliest and latest day of the month a statement date can fall on. */
private const val STATEMENT_DAY_MIN = 1
private const val STATEMENT_DAY_MAX = 28

@Composable
internal fun StatementDayPickerDialog(
    currentDay: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(currentDay ?: STATEMENT_DAY_MIN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Iconax.CalendarEdit, contentDescription = null) },
        title = { Text(stringResource(R.string.manage_accounts_statement_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.manage_accounts_statement_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    FilledTonalIconButton(
                        onClick = { if (selectedDay > STATEMENT_DAY_MIN) selectedDay-- }
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = stringResource(R.string.manage_accounts_decrease)
                        )
                    }
                    Text(
                        text = "$selectedDay",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    FilledTonalIconButton(
                        onClick = { if (selectedDay < STATEMENT_DAY_MAX) selectedDay++ }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.manage_accounts_increase)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDay) }) {
                Text(stringResource(R.string.accounts_action_save))
            }
        },
        dismissButton = {
            if (currentDay != null) {
                TextButton(onClick = { onConfirm(null) }) {
                    Text(stringResource(R.string.manage_accounts_action_clear))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.accounts_action_cancel))
                }
            }
        }
    )
}

@Composable
internal fun LowBalanceThresholdDialog(
    currentThreshold: BigDecimal?,
    accountLabel: String,
    currency: String,
    currentBalance: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal?) -> Unit
) {
    var text by remember { mutableStateOf(currentThreshold?.toPlainString().orEmpty()) }
    val parsed = text.trim().toBigDecimalOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Iconax.NotificationBing, contentDescription = null) },
        title = { Text(stringResource(R.string.manage_accounts_low_balance_alert)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = accountLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.manage_accounts_low_balance_alert_description,
                        CurrencyFormatter.formatCurrency(currentBalance, currency)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TonalTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = stringResource(R.string.manage_accounts_threshold_label, currency),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(parsed) },
                enabled = parsed != null && parsed >= BigDecimal.ZERO
            ) { Text(stringResource(R.string.accounts_action_save)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (currentThreshold != null) {
                    TextButton(onClick = { onConfirm(null) }) {
                        Text(stringResource(R.string.manage_accounts_action_clear))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.accounts_action_cancel))
                }
            }
        }
    )
}

@Composable
internal fun AccountAliasDialog(
    currentAlias: String?,
    accountLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var aliasText by remember { mutableStateOf(currentAlias.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Iconax.Edit2, contentDescription = null) },
        title = { Text(stringResource(R.string.manage_accounts_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = accountLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TonalTextField(
                    value = aliasText,
                    onValueChange = { aliasText = it },
                    label = stringResource(R.string.manage_accounts_alias_label),
                    placeholder = stringResource(R.string.manage_accounts_alias_placeholder)
                )
                Text(
                    text = stringResource(R.string.manage_accounts_alias_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(aliasText.trim().ifBlank { null }) }) {
                Text(stringResource(R.string.accounts_action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accounts_action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCardDialog(
    card: CardEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        bankName: String,
        cardType: CardType,
        nickname: String?
    ) -> Unit
) {
    var bankName by remember { mutableStateOf(card.bankName) }
    var cardType by remember { mutableStateOf(card.cardType) }
    var nickname by remember { mutableStateOf(card.nickname.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Iconax.Card, contentDescription = null) },
        title = { Text(stringResource(R.string.manage_accounts_edit_card_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.manage_accounts_masked_number, card.cardLast4),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TonalTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = stringResource(R.string.manage_accounts_bank_label)
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = stringResource(R.string.manage_accounts_card_type_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            CardType.DEBIT to stringResource(R.string.manage_accounts_card_type_debit),
                            CardType.CREDIT to stringResource(R.string.manage_accounts_card_type_credit)
                        )
                        options.forEachIndexed { index, (type, label) ->
                            SegmentedButton(
                                selected = cardType == type,
                                onClick = { cardType = type },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size)
                            ) { Text(label) }
                        }
                    }
                }
                TonalTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = stringResource(R.string.manage_accounts_nickname_label)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(bankName, cardType, nickname.ifBlank { null })
                },
                enabled = bankName.isNotBlank()
            ) { Text(stringResource(R.string.accounts_action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accounts_action_cancel))
            }
        }
    )
}

@Composable
internal fun LinkCardDialog(
    card: CardEntity,
    accounts: List<AccountBalanceEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Iconax.Card, contentDescription = null) },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.manage_accounts_link_card_title))
                Text(
                    text = stringResource(
                        R.string.manage_accounts_card_identity,
                        card.bankName,
                        card.cardLast4
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (accounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.manage_accounts_link_no_accounts, card.bankName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.manage_accounts_link_select_account),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    accounts.forEach { account ->
                        val isSelected = selectedAccount == account.accountLast4
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = isSelected,
                                        role = Role.RadioButton,
                                        onClick = { selectedAccount = account.accountLast4 }
                                    )
                                    .padding(Spacing.smd),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AccountAvatar(account = account, size = Dimensions.Icon.list)
                                Column(modifier = Modifier.weight(1f)) {
                                    if (account.accountLast4 != AccountBalanceEntity.WALLET_ACCOUNT_MARKER) {
                                        Text(
                                            text = stringResource(
                                                R.string.manage_accounts_masked_number,
                                                account.accountLast4
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(account.balance, account.currency),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) scheme.onPrimaryContainer else scheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = scheme.onPrimaryContainer,
                                        modifier = Modifier.size(Dimensions.Icon.medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedAccount?.let(onConfirm) },
                enabled = selectedAccount != null
            ) {
                Text(stringResource(R.string.manage_accounts_action_link))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accounts_action_cancel))
            }
        }
    )
}

@Composable
internal fun DeleteAccountConfirmDialog(
    bankName: String,
    accountLast4: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        iconContentColor = scheme.error,
        icon = { Icon(Iconax.Danger, contentDescription = null) },
        title = {
            Text(stringResource(R.string.manage_accounts_delete_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.manage_accounts_delete_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = scheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.smd),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandIcon(
                            merchantName = bankName,
                            size = Dimensions.Icon.list,
                            showBackground = true
                        )
                        Column {
                            Text(
                                text = bankName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.manage_accounts_account_ending_in, accountLast4),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.manage_accounts_delete_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = scheme.error
                )
            ) {
                Text(stringResource(R.string.accounts_action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accounts_action_cancel))
            }
        }
    )
}
