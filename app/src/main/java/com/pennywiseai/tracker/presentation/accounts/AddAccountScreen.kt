package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.domain.model.displayName
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.icons.iconax.Card
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Information
import com.pennywiseai.tracker.ui.icons.iconax.Wallet3
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageAccountsViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    var showCurrencySheet by remember { mutableStateOf(false) }

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    val scheme = MaterialTheme.colorScheme
    val isCredit = formState.accountType == AccountType.CREDIT
    val isCash = formState.accountType == AccountType.CASH
    // Name, last digits, currency and balance (plus a credit limit for cards)
    // read as one connected block of fields.
    val fieldCount = if (isCredit) 5 else 4
    val symbol = CurrencyFormatter.getCurrencySymbol(formState.currency)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.add_account_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.accounts_back)
                    )
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(scheme.background)
                .imePadding()
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    bottom = Spacing.lg + paddingValues.calculateBottomPadding()
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Info Card
            AccountMessageBanner(
                text = stringResource(R.string.add_account_intro),
                icon = Iconax.Information,
                containerColor = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
            )

            // Error Message
            formState.errorMessage?.let { error ->
                AccountMessageBanner(
                    text = error.asString(),
                    icon = Icons.Default.Error,
                    containerColor = scheme.errorContainer,
                    contentColor = scheme.onErrorContainer,
                )
            }

            // Account Type
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.add_account_type_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    AccountType.entries.forEach { type ->
                        FilterChip(
                            selected = formState.accountType == type,
                            onClick = { viewModel.updateAccountType(type) },
                            label = { Text(type.displayName()) },
                            leadingIcon = {
                                Icon(
                                    imageVector = type.glyph(),
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = scheme.surfaceContainerLow,
                                selectedContainerColor = scheme.secondaryContainer,
                                selectedLabelColor = scheme.onSecondaryContainer,
                                selectedLeadingIconColor = scheme.onSecondaryContainer,
                            ),
                            // The tonal fill is the container; no outline.
                            border = null,
                        )
                    }
                }
            }

            // Live preview: the card the account will get, from what is typed so far.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AccountPreviewCard(
                    bankName = formState.bankName,
                    accountLast4 = if (isCash) "" else formState.accountLast4,
                    balance = formState.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    currencyCode = formState.currency,
                    accountType = formState.accountType,
                    creditLimit = formState.creditLimit.toBigDecimalOrNull(),
                )
                Text(
                    text = stringResource(R.string.account_editor_preview_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            // Account Name + Last 4 + Currency + Balance (+ Credit Limit): one connected group
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
            ) {
                TonalTextField(
                    value = formState.bankName,
                    onValueChange = viewModel::updateBankName,
                    label = stringResource(R.string.add_account_name_label),
                    position = ListItemPosition.from(0, fieldCount),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )

                TonalTextField(
                    value = formState.accountLast4,
                    onValueChange = viewModel::updateAccountLast4,
                    label = if (isCash) {
                        stringResource(R.string.add_account_identifier_label)
                    } else {
                        stringResource(R.string.add_account_last4_label)
                    },
                    position = ListItemPosition.from(1, fieldCount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                // Currency
                AccountPickerField(
                    label = stringResource(R.string.add_account_currency_label),
                    value = currencyDisplay(formState.currency),
                    position = ListItemPosition.from(2, fieldCount),
                    trailingIcon = Icons.Default.KeyboardArrowDown,
                    onClick = { showCurrencySheet = true },
                )

                TonalTextField(
                    value = formState.balance,
                    onValueChange = viewModel::updateBalance,
                    label = stringResource(R.string.add_account_balance_label),
                    placeholder = stringResource(R.string.manage_accounts_amount_placeholder),
                    prefix = { Text(symbol) },
                    position = ListItemPosition.from(3, fieldCount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                // Credit Limit (only for credit cards)
                if (isCredit) {
                    TonalTextField(
                        value = formState.creditLimit,
                        onValueChange = viewModel::updateCreditLimit,
                        label = stringResource(R.string.add_account_credit_limit_label),
                        placeholder = stringResource(R.string.manage_accounts_credit_limit_placeholder),
                        supportingText = stringResource(R.string.add_account_credit_limit_hint),
                        prefix = { Text(symbol) },
                        position = ListItemPosition.from(4, fieldCount),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    viewModel.addAccount()
                    if (formState.errorMessage == null) {
                        onNavigateBack()
                    }
                },
                enabled = formState.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimensions.Component.fab)
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.accounts_action_save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showCurrencySheet) {
        AccountCurrencySheet(
            selected = formState.currency,
            onSelect = { code ->
                viewModel.updateCurrency(code)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false },
        )
    }
}

/** The glyph on an account-type chip. */
private fun AccountType.glyph(): ImageVector = when (this) {
    AccountType.SAVINGS, AccountType.CURRENT -> Icons.Default.AccountBalance
    AccountType.CREDIT -> Iconax.Card
    AccountType.CASH -> Iconax.Wallet3
}
