package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.domain.model.getAccountType
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Padlock
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

/** A balance, outstanding or limit field accepts digits and one decimal point. */
private val AmountInputPattern = Regex("^\\d*\\.?\\d*$")

/**
 * Edits an account's name, currency and balance (and a credit card's limit) in
 * a bottom sheet with a live preview of the account card above the fields. The
 * account number is shown but locked: it is the account's identity.
 *
 * The inputs keep the text-entry rules this dialog always had: digits and one
 * decimal point, parsed to exact `BigDecimal`s on save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditAccountSheet(
    account: AccountBalanceEntity,
    onDismiss: () -> Unit,
    onConfirm: (bankName: String, balance: BigDecimal, creditLimit: BigDecimal?, currency: String) -> Unit
) {
    var bankNameText by remember { mutableStateOf(account.bankName) }
    var balanceText by remember { mutableStateOf(account.balance.toString()) }
    var creditLimitText by remember { mutableStateOf(account.creditLimit?.toString() ?: "") }
    // Pre-fill with the *resolved* currency (what the account actually displays), not
    // the raw stored value — an SMS-tracked non-INR account stores the INR default but
    // shows the parser currency. Seeding from the raw value would let an unrelated edit
    // silently lock the account to INR.
    var currencyText by remember {
        mutableStateOf(
            CurrencyFormatter.resolveAccountCurrency(
                sourceType = account.sourceType,
                storedCurrency = account.currency,
                bankName = account.bankName
            )
        )
    }
    var showCurrencySheet by remember { mutableStateOf(false) }

    val isValid = bankNameText.isNotBlank() &&
        balanceText.isNotBlank() &&
        balanceText.toDoubleOrNull() != null &&
        (if (account.isCreditCard) creditLimitText.isNotBlank() && creditLimitText.toDoubleOrNull() != null else true)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    val symbol = CurrencyFormatter.getCurrencySymbol(currencyText)
    val outstanding = balanceText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val limit = creditLimitText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    // Bank name, account number, currency, balance and (credit cards) limit read
    // as one connected block of fields.
    val fieldCount = if (account.isCreditCard) 5 else 4

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The fields are tonal (surfaceContainerLow), so the sheet sits one step
        // lighter to let them read as raised fields.
        containerColor = glassSheetContainerColor(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Spacing.lg,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.manage_accounts_edit_account_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(
                        if (account.isCreditCard) {
                            R.string.manage_accounts_type_credit_card
                        } else {
                            R.string.manage_accounts_type_bank_account
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Live preview: the card updates as the name, currency and amounts change.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AccountPreviewCard(
                    bankName = bankNameText,
                    accountLast4 = account.accountLast4,
                    balance = outstanding,
                    currencyCode = currencyText,
                    accountType = account.getAccountType(),
                    creditLimit = if (account.isCreditCard) limit else null,
                )
                Text(
                    text = stringResource(R.string.account_editor_preview_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)) {
                // Bank Name (Editable)
                TonalTextField(
                    value = bankNameText,
                    onValueChange = { bankNameText = it },
                    label = stringResource(R.string.manage_accounts_bank_name_label),
                    position = ListItemPosition.from(0, fieldCount),
                )

                // Account Number (Read-only)
                AccountPickerField(
                    label = stringResource(R.string.manage_accounts_account_number_label),
                    value = AccountBalanceEntity.accountLabel("", account.accountLast4).trim(),
                    position = ListItemPosition.from(1, fieldCount),
                    leadingIcon = Iconax.Padlock,
                    leadingIconDescription = stringResource(R.string.manage_accounts_read_only),
                )

                // Currency (editable — lets an existing account switch currency)
                AccountPickerField(
                    label = stringResource(R.string.add_account_currency_label),
                    value = currencyDisplay(currencyText),
                    position = ListItemPosition.from(2, fieldCount),
                    trailingIcon = Icons.Default.KeyboardArrowDown,
                    onClick = { showCurrencySheet = true },
                )

                if (account.isCreditCard) {
                    // Outstanding Balance (Credit Card)
                    TonalTextField(
                        value = balanceText,
                        onValueChange = { text ->
                            if (text.matches(AmountInputPattern)) balanceText = text
                        },
                        label = stringResource(R.string.manage_accounts_outstanding_balance),
                        placeholder = stringResource(R.string.manage_accounts_amount_placeholder),
                        supportingText = stringResource(R.string.manage_accounts_outstanding_balance_hint),
                        prefix = { Text(symbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        position = ListItemPosition.from(3, fieldCount),
                    )

                    // Credit Limit
                    TonalTextField(
                        value = creditLimitText,
                        onValueChange = { text ->
                            if (text.matches(AmountInputPattern)) creditLimitText = text
                        },
                        label = stringResource(R.string.manage_accounts_credit_limit),
                        placeholder = stringResource(R.string.manage_accounts_credit_limit_placeholder),
                        supportingText = stringResource(R.string.manage_accounts_credit_limit_hint),
                        prefix = { Text(symbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        position = ListItemPosition.from(4, fieldCount),
                    )
                } else {
                    // Account Balance (Regular Account)
                    TonalTextField(
                        value = balanceText,
                        onValueChange = { text ->
                            if (text.matches(AmountInputPattern)) balanceText = text
                        },
                        label = stringResource(R.string.manage_accounts_account_balance_label),
                        placeholder = stringResource(R.string.manage_accounts_amount_placeholder),
                        prefix = { Text(symbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        position = ListItemPosition.from(3, fieldCount),
                    )
                }
            }

            // Show available credit preview
            if (account.isCreditCard && limit > BigDecimal.ZERO) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = scheme.secondaryContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.smd),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.manage_accounts_available_credit_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSecondaryContainer,
                        )
                        Text(
                            text = CurrencyFormatter.formatCurrency(limit - outstanding, currencyText),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = scheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.fab),
                ) {
                    Text(stringResource(R.string.accounts_action_cancel))
                }
                Button(
                    onClick = {
                        val balance = balanceText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val creditLimit = if (account.isCreditCard) {
                            creditLimitText.toBigDecimalOrNull()
                        } else null
                        onConfirm(bankNameText, balance, creditLimit, currencyText)
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.fab),
                ) {
                    Text(stringResource(R.string.accounts_action_save))
                }
            }
        }
    }

    if (showCurrencySheet) {
        AccountCurrencySheet(
            selected = currencyText,
            onSelect = { code ->
                currencyText = code
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false },
        )
    }
}

/**
 * The account card as it will look once saved, drawn from the values typed so
 * far — the editor's live preview. It never reads or writes the database, and
 * its single amount is formatted in the account's own [currencyCode].
 */
@Composable
internal fun AccountPreviewCard(
    bankName: String,
    accountLast4: String,
    balance: BigDecimal,
    currencyCode: String,
    accountType: AccountType,
    creditLimit: BigDecimal?,
    modifier: Modifier = Modifier,
) {
    val isCredit = accountType == AccountType.CREDIT
    val isCash = accountType == AccountType.CASH
    val name = bankName.trim().ifEmpty { stringResource(R.string.account_editor_preview_name) }
    val subtitle = when {
        isCash -> stringResource(R.string.account_type_cash)
        accountLast4.isBlank() -> null
        else -> stringResource(R.string.manage_accounts_masked_number, accountLast4)
    }
    val limitToShow = creditLimit?.takeIf { isCredit && it > BigDecimal.ZERO }
    val limitFooter: @Composable ColumnScope.() -> Unit = {
        if (limitToShow != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.manage_accounts_credit_limit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(limitToShow, currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    AccountCardShell(
        merchantName = bankName,
        label = stringResource(
            if (isCredit) R.string.manage_accounts_outstanding else R.string.manage_accounts_balance
        ),
        amount = CurrencyFormatter.formatCurrency(balance, currencyCode),
        title = name,
        subtitle = subtitle,
        avatar = { AccountAvatar(bankName = bankName, isCash = isCash) },
        modifier = modifier,
        footer = if (limitToShow != null) limitFooter else null,
    )
}
