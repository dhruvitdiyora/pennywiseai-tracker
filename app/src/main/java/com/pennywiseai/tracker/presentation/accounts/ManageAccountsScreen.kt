package com.pennywiseai.tracker.presentation.accounts

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.CardEntity
import com.pennywiseai.tracker.domain.model.getAccountType
import com.pennywiseai.tracker.presentation.people.PeopleExtendedFab
import com.pennywiseai.tracker.presentation.people.PeopleTonalActionButton
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.SupportDevelopmentDialog
import com.pennywiseai.tracker.ui.components.SupportNudgeCard
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.glassFill
import com.pennywiseai.tracker.ui.components.cards.glassRim
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.EyeSlash
import com.pennywiseai.tracker.ui.icons.iconax.HierarchySquare3
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Wallet3
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToBalanceHistory: (bankName: String, accountLast4: String) -> Unit,
    viewModel: ManageAccountsViewModel = hiltViewModel(),
    onNavigateToAccountDetail: ((bankName: String, accountLast4: String) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedAccountEntity by remember { mutableStateOf<AccountBalanceEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showHiddenAccounts by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountBalanceEntity?>(null) }
    // Account merge (#368) — single screen-level entry point; the sheet handles
    // source + target selection + confirmation in one self-contained flow.
    var showMergeSheet by remember { mutableStateOf(false) }
    val isProEntitled by viewModel.isProEntitled.collectAsState()
    var showUpgradeSheet by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    val pendingProfileReassign by viewModel.pendingProfileReassign.collectAsState()

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    // The add button carries its label only while the list is at the top.
    val fabExpanded by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }

    val openUpdateBalance: (AccountBalanceEntity) -> Unit = { account ->
        selectedAccount = account.bankName to account.accountLast4
        selectedAccountEntity = account
        showUpdateDialog = true
    }
    val openHistory: (AccountBalanceEntity) -> Unit = { account ->
        onNavigateToBalanceHistory(account.bankName, account.accountLast4)
    }
    // Tapping a card opens Account Detail, as in Cashiro.
    val openDetail: ((AccountBalanceEntity) -> Unit)? = onNavigateToAccountDetail?.let { navigate ->
        { account -> navigate(account.bankName, account.accountLast4) }
    }
    val askDelete: (AccountBalanceEntity) -> Unit = { account ->
        accountToDelete = account.bankName to account.accountLast4
        showDeleteConfirmDialog = true
    }
    val openEdit: (AccountBalanceEntity) -> Unit = { account ->
        accountToEdit = account
        showEditDialog = true
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.manage_accounts_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.accounts_back)
                    )
                },
                actionContent = {
                    // Show Merge only when there are at least 2 accounts to choose between.
                    // Pro-only feature — free users see the icon (so the feature is
                    // discoverable) but the tap routes to the paywall instead.
                    if (uiState.accounts.size >= 2) {
                        PeopleTonalActionButton(
                            onClick = {
                                if (isProEntitled) showMergeSheet = true
                                else showUpgradeSheet = true
                            },
                            icon = Iconax.HierarchySquare3,
                            contentDescription = stringResource(R.string.merge_accounts_title),
                        )
                    }
                },
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            if (uiState.accounts.isNotEmpty()) {
                PeopleExtendedFab(
                    label = stringResource(R.string.add_account_title),
                    onClick = onNavigateToAddAccount,
                    expanded = fabExpanded,
                )
            }
        }
    ) { paddingValues ->
        if (uiState.accounts.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                PennyWiseEmptyState(
                    icon = Iconax.Wallet3,
                    headline = stringResource(R.string.manage_accounts_empty_title),
                    description = stringResource(R.string.manage_accounts_empty_description),
                    actionLabel = stringResource(R.string.add_account_title),
                    onAction = onNavigateToAddAccount,
                )
            }
        } else {
            // Separate visible and hidden accounts. Cash accounts and mobile-money
            // wallets sit in their own section, apart from the bank accounts.
            val visibleBankAccounts = uiState.accounts.filter {
                !it.isCreditCard && !it.isCashOrWallet() && !viewModel.isAccountHidden(it.bankName, it.accountLast4)
            }
            val visibleCashAccounts = uiState.accounts.filter {
                !it.isCreditCard && it.isCashOrWallet() && !viewModel.isAccountHidden(it.bankName, it.accountLast4)
            }
            val visibleCreditCards = uiState.accounts.filter {
                it.isCreditCard && !viewModel.isAccountHidden(it.bankName, it.accountLast4)
            }
            val hiddenRegularAccounts = uiState.accounts.filter {
                !it.isCreditCard && viewModel.isAccountHidden(it.bankName, it.accountLast4)
            }
            val hiddenCreditCards = uiState.accounts.filter {
                it.isCreditCard && viewModel.isAccountHidden(it.bankName, it.accountLast4)
            }
            val allRegularAccounts = uiState.accounts.filter { !it.isCreditCard }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .overScrollVertical(),
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + Dimensions.Component.fabScrollClearance
                ),
                // Items carry their own bottom gap, so a section header can sit
                // closer to its cards than to the section above it.
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
            ) {
                // Show success message if available
                uiState.successMessage?.let { message ->
                    item(key = "success") {
                        AccountMessageBanner(
                            text = message.asString(),
                            icon = Icons.Default.CheckCircle,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = Spacing.md)
                        )
                    }
                }

                // F-Droid tip nudge after a merge — persists past the transient
                // success banner; tapping opens the tip jar and dismisses it.
                if (uiState.showSupportNudge) {
                    item(key = "support-nudge") {
                        SupportNudgeCard(
                            onClick = {
                                showSupportDialog = true
                                viewModel.dismissSupportNudge()
                            },
                            modifier = Modifier.padding(bottom = Spacing.md)
                        )
                    }
                }

                // Show error message if available
                uiState.errorMessage?.let { message ->
                    item(key = "error") {
                        AccountMessageBanner(
                            text = message.asString(),
                            icon = Icons.Default.Error,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(bottom = Spacing.md)
                        )
                    }
                }

                // The first section needs no extra gap above its header.
                var sectionsShown = 0

                // Regular Bank Accounts Section (Visible Only)
                if (visibleBankAccounts.isNotEmpty()) {
                    manageSectionHeader("bank-header", R.string.manage_accounts_section_bank, sectionsShown++ == 0)
                    items(visibleBankAccounts, key = { "bank-${it.bankName}-${it.accountLast4}" }) { account ->
                        ManageBankAccountRow(
                            account = account,
                            isHidden = false,
                            linkedCards = uiState.linkedCards[account.accountLast4] ?: emptyList(),
                            viewModel = viewModel,
                            onUpdateBalance = { openUpdateBalance(account) },
                            onViewHistory = { openHistory(account) },
                            onOpenDetail = openDetail,
                            onDelete = { askDelete(account) },
                            onEdit = { openEdit(account) },
                        )
                    }
                }

                // Cash accounts and wallets
                if (visibleCashAccounts.isNotEmpty()) {
                    manageSectionHeader("cash-header", R.string.manage_accounts_section_cash, sectionsShown++ == 0)
                    items(visibleCashAccounts, key = { "cash-${it.bankName}-${it.accountLast4}" }) { account ->
                        ManageBankAccountRow(
                            account = account,
                            isHidden = false,
                            linkedCards = uiState.linkedCards[account.accountLast4] ?: emptyList(),
                            viewModel = viewModel,
                            onUpdateBalance = { openUpdateBalance(account) },
                            onViewHistory = { openHistory(account) },
                            onOpenDetail = openDetail,
                            onDelete = { askDelete(account) },
                            onEdit = { openEdit(account) },
                        )
                    }
                }

                // Orphaned Cards Section
                if (uiState.orphanedCards.isNotEmpty()) {
                    manageSectionHeader("orphan-header", R.string.manage_accounts_section_unlinked_cards, sectionsShown++ == 0)
                    items(uiState.orphanedCards, key = { "orphan-${it.id}" }) { card ->
                        OrphanedCardItem(
                            card = card,
                            accounts = allRegularAccounts,
                            onLinkToAccount = { accountLast4 ->
                                viewModel.linkCardToAccount(card.id, accountLast4)
                            },
                            onDeleteCard = { cardId ->
                                viewModel.deleteCard(cardId)
                            },
                            onUpdateCard = { bankName, cardType, nickname ->
                                viewModel.updateCardDetails(card.id, bankName, cardType, nickname)
                            },
                            modifier = Modifier.padding(bottom = Spacing.md)
                        )
                    }
                }

                // Credit Cards Section (Visible Only)
                if (visibleCreditCards.isNotEmpty()) {
                    manageSectionHeader("credit-header", R.string.manage_accounts_section_credit_cards, sectionsShown++ == 0)
                    items(visibleCreditCards, key = { "credit-${it.bankName}-${it.accountLast4}" }) { card ->
                        ManageCreditCardRow(
                            card = card,
                            isHidden = false,
                            viewModel = viewModel,
                            onUpdateBalance = { openUpdateBalance(card) },
                            onViewHistory = { openHistory(card) },
                            onOpenDetail = openDetail,
                            onDelete = { askDelete(card) },
                            onEdit = { openEdit(card) },
                        )
                    }
                }

                // Hidden Accounts Section (Collapsible)
                if (hiddenRegularAccounts.isNotEmpty() || hiddenCreditCards.isNotEmpty()) {
                    val hiddenToggleTopGap = if (sectionsShown == 0) Spacing.none else Spacing.sm
                    item(key = "hidden-toggle") {
                        HiddenAccountsToggle(
                            count = hiddenRegularAccounts.size + hiddenCreditCards.size,
                            expanded = showHiddenAccounts,
                            onClick = { showHiddenAccounts = !showHiddenAccounts },
                            modifier = Modifier.padding(
                                top = hiddenToggleTopGap,
                                bottom = Spacing.md,
                            )
                        )
                    }

                    if (showHiddenAccounts) {
                        // Hidden Bank Accounts
                        items(hiddenRegularAccounts, key = { "hidden-bank-${it.bankName}-${it.accountLast4}" }) { account ->
                            ManageBankAccountRow(
                                account = account,
                                isHidden = true,
                                linkedCards = uiState.linkedCards[account.accountLast4] ?: emptyList(),
                                viewModel = viewModel,
                                onUpdateBalance = { openUpdateBalance(account) },
                                onViewHistory = { openHistory(account) },
                                onOpenDetail = openDetail,
                                onDelete = { askDelete(account) },
                                onEdit = { openEdit(account) },
                            )
                        }

                        // Hidden Credit Cards
                        items(hiddenCreditCards, key = { "hidden-credit-${it.bankName}-${it.accountLast4}" }) { card ->
                            ManageCreditCardRow(
                                card = card,
                                isHidden = true,
                                viewModel = viewModel,
                                onUpdateBalance = { openUpdateBalance(card) },
                                onViewHistory = { openHistory(card) },
                                onOpenDetail = openDetail,
                                onDelete = { askDelete(card) },
                                onEdit = { openEdit(card) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Update Balance Dialog
    val updateAccount = selectedAccount
    val updateEntity = selectedAccountEntity
    if (showUpdateDialog && updateAccount != null && updateEntity != null) {
        val currencyCode = CurrencyFormatter.resolveAccountCurrency(
            updateEntity.sourceType,
            updateEntity.currency,
            updateEntity.bankName
        )
        val dismissUpdate = {
            showUpdateDialog = false
            selectedAccount = null
            selectedAccountEntity = null
        }
        if (updateEntity.isCreditCard) {
            // Credit Card Update Dialog
            UpdateCreditCardDialog(
                bankName = updateAccount.first,
                accountLast4 = updateAccount.second,
                currentOutstanding = updateEntity.balance,
                currentLimit = updateEntity.creditLimit ?: BigDecimal.ZERO,
                currencyCode = currencyCode,
                onDismiss = dismissUpdate,
                onConfirm = { newBalance, newLimit ->
                    viewModel.updateCreditCard(
                        updateAccount.first,
                        updateAccount.second,
                        newBalance,
                        newLimit
                    )
                    dismissUpdate()
                }
            )
        } else {
            // Regular Account Update Dialog
            UpdateBalanceDialog(
                bankName = updateAccount.first,
                accountLast4 = updateAccount.second,
                currentBalance = updateEntity.balance,
                currencyCode = currencyCode,
                onDismiss = dismissUpdate,
                onConfirm = { newBalance ->
                    viewModel.updateAccountBalance(
                        updateAccount.first,
                        updateAccount.second,
                        newBalance
                    )
                    dismissUpdate()
                }
            )
        }
    }

    // Delete Account Confirmation Dialog
    val deleteTarget = accountToDelete
    if (showDeleteConfirmDialog && deleteTarget != null) {
        DeleteAccountConfirmDialog(
            bankName = deleteTarget.first,
            accountLast4 = deleteTarget.second,
            onDismiss = {
                showDeleteConfirmDialog = false
                accountToDelete = null
            },
            onConfirm = {
                viewModel.deleteAccount(deleteTarget.first, deleteTarget.second)
                showDeleteConfirmDialog = false
                accountToDelete = null
            }
        )
    }

    // Edit Account Sheet
    val editTarget = accountToEdit
    if (showEditDialog && editTarget != null) {
        EditAccountSheet(
            account = editTarget,
            onDismiss = {
                showEditDialog = false
                accountToEdit = null
            },
            onConfirm = { newBankName, newBalance, newCreditLimit, newCurrency ->
                viewModel.editAccount(
                    oldBankName = editTarget.bankName,
                    accountLast4 = editTarget.accountLast4,
                    newBankName = newBankName,
                    newBalance = newBalance,
                    newCreditLimit = newCreditLimit,
                    isCreditCard = editTarget.isCreditCard,
                    newCurrency = newCurrency
                )
                showEditDialog = false
                accountToEdit = null
            }
        )
    }

    // Merge accounts sheet (#368)
    if (showMergeSheet) {
        MergeAccountsSheet(
            accounts = uiState.accounts,
            countTransactionsOn = { bankName, last4 ->
                viewModel.countTransactionsOn(bankName, last4)
            },
            onConfirm = { source, target ->
                viewModel.mergeAccounts(source, target)
                showMergeSheet = false
            },
            onDismiss = { showMergeSheet = false }
        )
    }

    if (showUpgradeSheet) {
        com.pennywiseai.tracker.presentation.paywall.UpgradeSheet(
            onDismiss = { showUpgradeSheet = false },
        )
    }

    if (showSupportDialog) {
        SupportDevelopmentDialog(onDismiss = { showSupportDialog = false })
    }

    // Offer to move existing transactions that carry an explicit, mismatched
    // profile after the account's profile changes (#420).
    pendingProfileReassign?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingProfileReassign() },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.manage_accounts_reassign_title)) },
            text = {
                Text(
                    pluralStringResource(R.plurals.manage_accounts_reassign_message, pending.transactionCount, pending.transactionCount)
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.applyPendingProfileReassign() }) {
                    Text(stringResource(R.string.manage_accounts_reassign_move))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingProfileReassign() }) {
                    Text(stringResource(R.string.manage_accounts_reassign_keep))
                }
            }
        )
    }
}

/** Cash accounts and mobile-money wallets: tracked by hand, with no bank behind them. */
private fun AccountBalanceEntity.isCashOrWallet(): Boolean =
    getAccountType() == AccountType.CASH || accountLast4 == AccountBalanceEntity.WALLET_ACCOUNT_MARKER

/** One section heading; sits closer to its cards than to the section above it. */
private fun LazyListScope.manageSectionHeader(key: String, @StringRes titleRes: Int, isFirst: Boolean) {
    item(key = key) {
        SectionHeaderV2(
            title = stringResource(titleRes),
            modifier = Modifier
                .padding(horizontal = Spacing.sm)
                .padding(bottom = Spacing.Layout.headerToContent),
            topSpacing = if (isFirst) Spacing.none else Spacing.sm,
        )
    }
}

/** A bank, cash or wallet account, wired to the screen's ViewModel actions. */
@Composable
private fun ManageBankAccountRow(
    account: AccountBalanceEntity,
    isHidden: Boolean,
    linkedCards: List<CardEntity>,
    viewModel: ManageAccountsViewModel,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onOpenDetail: ((AccountBalanceEntity) -> Unit)? = null,
) {
    AccountItem(
        account = account,
        linkedCards = linkedCards,
        isHidden = isHidden,
        onToggleVisibility = {
            viewModel.toggleAccountVisibility(account.bankName, account.accountLast4)
        },
        onUpdateBalance = onUpdateBalance,
        onViewHistory = onViewHistory,
        onUnlinkCard = { cardId ->
            viewModel.unlinkCard(cardId)
        },
        onDeleteAccount = onDelete,
        onEditAccount = onEdit,
        onSetProfile = { profileId ->
            viewModel.setAccountProfile(account.bankName, account.accountLast4, profileId)
        },
        onSetAlias = { alias ->
            viewModel.setAccountAlias(account.bankName, account.accountLast4, alias)
        },
        onSetLowBalanceThreshold = { threshold ->
            viewModel.setLowBalanceThreshold(account.bankName, account.accountLast4, threshold)
        },
        modifier = Modifier
            .padding(bottom = Spacing.md)
            .openDetailOnClick(onOpenDetail?.let { open -> { open(account) } }),
    )
}

/** A credit card, wired to the screen's ViewModel actions. */
@Composable
private fun ManageCreditCardRow(
    card: AccountBalanceEntity,
    isHidden: Boolean,
    viewModel: ManageAccountsViewModel,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onOpenDetail: ((AccountBalanceEntity) -> Unit)? = null,
) {
    CreditCardItem(
        card = card,
        isHidden = isHidden,
        onToggleVisibility = {
            viewModel.toggleAccountVisibility(card.bankName, card.accountLast4)
        },
        onUpdateBalance = onUpdateBalance,
        onViewHistory = onViewHistory,
        onDeleteAccount = onDelete,
        onEditAccount = onEdit,
        onSetStatementDay = { day ->
            viewModel.setStatementDay(card.bankName, card.accountLast4, day)
        },
        modifier = Modifier
            .padding(bottom = Spacing.md)
            .openDetailOnClick(onOpenDetail?.let { open -> { open(card) } }),
    )
}

/** Makes an account card open Account Detail; a no-op when [onClick] is null. */
@Composable
private fun Modifier.openDetailOnClick(onClick: (() -> Unit)?): Modifier =
    if (onClick == null) {
        this
    } else {
        this
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(
                onClickLabel = stringResource(R.string.manage_accounts_open_detail),
                onClick = onClick,
            )
    }

/** A rounded tonal message strip for a success or error from the last action. */
@Composable
internal fun AccountMessageBanner(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        // Glass: a tinted wash with the rim, like Home's low-balance card.
        color = glassFill(containerColor, blurLive = false, solidFillAlpha = GLASS_TINTED_ALPHA),
        border = glassRim(),
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.Padding.card),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

/** The "Ignored Accounts (n)" row that shows or hides the accounts set aside. */
@Composable
private fun HiddenAccountsToggle(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = glassFill(MaterialTheme.colorScheme.surfaceContainerLow, blurLive = false),
        border = glassRim(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimensions.Component.minTouchTarget)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Iconax.EyeSlash,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.manage_accounts_hidden_header, count),
                    style = PennyWiseText.sectionHeader,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(
                    if (expanded) R.string.accounts_collapse else R.string.accounts_expand
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
