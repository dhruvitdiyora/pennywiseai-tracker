package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

data class AccountDraft(
    val bankName: String,
    val accountLast4: String,
    val balance: BigDecimal,
    val creditLimit: BigDecimal?,
    val accountType: AccountType,
    val currency: String
)

/** Shared add/edit account sheet. Editing keeps the detected account type immutable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountSheet(
    account: AccountBalanceEntity?,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (AccountDraft) -> Unit
) {
    var bankName by remember(account) { mutableStateOf(account?.bankName.orEmpty()) }
    var last4 by remember(account) { mutableStateOf(account?.accountLast4.orEmpty()) }
    var balance by remember(account) { mutableStateOf(account?.balance?.toPlainString().orEmpty()) }
    var limit by remember(account) { mutableStateOf(account?.creditLimit?.toPlainString().orEmpty()) }
    var accountType by remember(account) {
        mutableStateOf(account?.accountType?.let { runCatching { AccountType.valueOf(it) }.getOrNull() } ?: AccountType.SAVINGS)
    }
    var currency by remember(account, defaultCurrency) { mutableStateOf(account?.currency ?: defaultCurrency) }
    var typeMenu by remember { mutableStateOf(false) }
    var currencyMenu by remember { mutableStateOf(false) }
    val isCredit = accountType == AccountType.CREDIT
    val valid = bankName.isNotBlank() && (accountType == AccountType.CASH || last4.isNotBlank()) &&
        balance.toBigDecimalOrNull() != null && (!isCredit || limit.isBlank() || limit.toBigDecimalOrNull() != null)
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(if (account == null) "Add account" else "Edit account", style = MaterialTheme.typography.titleLarge)
            PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BrandIcon(merchantName = bankName.ifBlank { "Account" }, size = Dimensions.Icon.list)
                    Column {
                        Text(bankName.ifBlank { "Account name" }, style = MaterialTheme.typography.titleSmall)
                        Text(
                            CurrencyFormatter.formatCurrency(balance.toBigDecimalOrNull() ?: BigDecimal.ZERO, currency),
                            style = PennyWiseText.amountLarge
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { if (account == null) typeMenu = it }) {
                TextField(
                    value = accountType.name.lowercase().replaceFirstChar { it.titlecase() }, onValueChange = {}, readOnly = true,
                    enabled = account == null, label = { Text("Account type") },
                    leadingIcon = { Icon(accountType.icon(), contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), colors = fieldColors
                )
                ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    AccountType.entries.forEach { type -> DropdownMenuItem(
                        text = { Text(type.name.lowercase().replaceFirstChar { it.titlecase() }) },
                        leadingIcon = { Icon(type.icon(), contentDescription = null) },
                        onClick = { accountType = type; typeMenu = false }
                    ) }
                }
            }
            ExposedDropdownMenuBox(expanded = currencyMenu, onExpandedChange = { currencyMenu = it }) {
                TextField(
                    value = "$currency ${CurrencyFormatter.getCurrencySymbol(currency)}", onValueChange = {}, readOnly = true,
                    label = { Text("Currency") }, modifier = Modifier.fillMaxWidth().menuAnchor(), colors = fieldColors
                )
                ExposedDropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                    CurrencyFormatter.getSupportedCurrencies().sorted().forEach { code -> DropdownMenuItem(
                        text = { Text("$code ${CurrencyFormatter.getCurrencySymbol(code)}") },
                        onClick = { currency = code; currencyMenu = false }
                    ) }
                }
            }
            TextField(bankName, { bankName = it }, label = { Text("Account name") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            TextField(last4, { if (accountType == AccountType.CASH || it.length <= 4) last4 = it }, label = { Text(if (accountType == AccountType.CASH) "Identifier (optional)" else "Last 4 digits") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            TextField(balance, { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) balance = it }, label = { Text(if (isCredit) "Outstanding balance" else "Current balance") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            if (isCredit) TextField(limit, { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) limit = it }, label = { Text("Credit limit (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            Button(onClick = {
                onSave(AccountDraft(bankName, last4.ifBlank { "CASH" }, balance.toBigDecimal(), limit.toBigDecimalOrNull(), accountType, currency))
            }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}

private fun AccountType.icon() = when (this) {
    AccountType.SAVINGS, AccountType.CURRENT -> Icons.Default.AccountBalance
    AccountType.CREDIT -> Icons.Default.CreditCard
    AccountType.CASH -> Icons.Default.Money
}
