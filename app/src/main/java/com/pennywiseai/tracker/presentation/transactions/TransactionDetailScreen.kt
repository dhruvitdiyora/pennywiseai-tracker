package com.pennywiseai.tracker.presentation.transactions

import com.pennywiseai.tracker.presentation.goals.AddToGoalSheet
import com.pennywiseai.tracker.presentation.goals.TowardGoalsSection
import com.pennywiseai.tracker.presentation.goals.TransactionGoalsViewModel
import com.pennywiseai.tracker.presentation.split.SplitBillSheet
import com.pennywiseai.tracker.presentation.split.SplitBillViewModel
import com.pennywiseai.tracker.presentation.split.SplitSection
import com.pennywiseai.tracker.presentation.split.canSplit
import androidx.compose.runtime.saveable.rememberSaveable

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.presentation.add.AddSaveBar
import com.pennywiseai.tracker.presentation.people.PeopleTonalActionButton
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.SplitItem
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Copy
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Folder2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal

internal fun buildReceiptShareIntent(receiptUri: Uri, subject: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, receiptUri)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToLoanDetail: (Long) -> Unit = {},
    onDuplicateTransaction: (Long) -> Unit = {},
    onNavigateToGoal: (Long) -> Unit = {},
    onCreateGoal: () -> Unit = {},
    viewModel: TransactionDetailViewModel = hiltViewModel(),
    goalsViewModel: TransactionGoalsViewModel = hiltViewModel(),
    splitViewModel: SplitBillViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val editableTransaction by viewModel.editableTransaction.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val applyToAllFromMerchant by viewModel.applyToAllFromMerchant.collectAsStateWithLifecycle()
    val updateExistingTransactions by viewModel.updateExistingTransactions.collectAsStateWithLifecycle()
    val existingTransactionCount by viewModel.existingTransactionCount.collectAsStateWithLifecycle()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    val deleteSuccess by viewModel.deleteSuccess.collectAsStateWithLifecycle()
    val accountPrimaryCurrency by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val convertedAmount by viewModel.convertedAmount.collectAsStateWithLifecycle()

    // Split state
    val splits by viewModel.splits.collectAsStateWithLifecycle()
    val showSplitEditor by viewModel.showSplitEditor.collectAsStateWithLifecycle()
    val hasSplits by viewModel.hasSplits.collectAsStateWithLifecycle()

    // Loan state
    val loan by viewModel.loan.collectAsStateWithLifecycle()
    val showMarkAsLoanSheet by viewModel.showMarkAsLoanSheet.collectAsStateWithLifecycle()
    var showUnmarkLoanConfirm by remember { mutableStateOf(false) }
    val recentPersonNames by viewModel.recentPersonNames.collectAsStateWithLifecycle()

    // Account profile state
    val accountProfileId by viewModel.accountProfileId.collectAsStateWithLifecycle()

    // Receipt state
    val receiptUri by viewModel.receiptUri.collectAsStateWithLifecycle()
    val showFullScreenReceipt by viewModel.showFullScreenReceipt.collectAsStateWithLifecycle()

    // Group state
    val currentGroup by viewModel.currentGroup.collectAsStateWithLifecycle()
    val availableGroups by viewModel.availableGroups.collectAsStateWithLifecycle()
    val showGroupSheet by viewModel.showGroupSheet.collectAsStateWithLifecycle()

    // Goals (#914)
    val linkedGoals by goalsViewModel.linked.collectAsStateWithLifecycle()
    val goals by goalsViewModel.goals.collectAsStateWithLifecycle()
    val addToGoal by goalsViewModel.sheet.collectAsStateWithLifecycle()
    var showAddToGoal by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(transaction) { goalsViewModel.setTransaction(transaction) }
    // Split a bill (#346): reload after a save so lent_shares_total is fresh.
    LaunchedEffect(transaction) { splitViewModel.setTransaction(transaction) }
    LaunchedEffect(Unit) { splitViewModel.changed.collect { viewModel.loadTransaction(transactionId) } }
    fun closeAddToGoal() { showAddToGoal = false; goalsViewModel.closeSheet() }
    LaunchedEffect(addToGoal.done) { if (addToGoal.done) closeAddToGoal() }
    // Loads what's left of the transaction whenever the sheet is open without it:
    // on open, and after process recreation restores an open sheet (its typed
    // fields are rememberSaveable and survive; the ViewModel's state doesn't).
    LaunchedEffect(showAddToGoal, transaction?.id) {
        if (showAddToGoal && transaction != null && goalsViewModel.sheet.value.left == null) goalsViewModel.openSheet()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show success snackbar
    val updatedMessage = stringResource(R.string.txn_detail_updated)
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar(updatedMessage)
                viewModel.clearSaveSuccess()
            }
        }
    }

    // Show error snackbar
    val errorText = errorMessage?.asString()
    LaunchedEffect(errorText) {
        errorText?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    // Reload on every resume so external changes — e.g. the linked loan being
    // deleted from the Loan screen — are reflected when returning here (#444).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadTransaction(transactionId)
    }

    // Handle delete success
    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
            onNavigateBack()
        }
    }

    val context = LocalContext.current

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        snackbarHost = {
            // In edit mode the sticky Save button sits at the bottom edge; lift
            // the snackbar above it so an error never hides the button.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(
                    bottom = if (isEditMode) Dimensions.Component.bottomBarHeight else Spacing.none
                )
            )
        },
        // Transaction actions (delete / group / duplicate / report) live in the
        // top-bar overflow menu, not floating buttons — a stack of FABs overlapped
        // the receipt content (#451 follow-up).
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(
                    if (isEditMode) R.string.txn_detail_title_edit else R.string.txn_detail_title
                ),
                hasBackButton = true,
                navigationContent = {
                    if (isEditMode) {
                        TonalNavigationButton(
                            onClick = { viewModel.cancelEdit() },
                            contentDescription = stringResource(R.string.txn_detail_action_cancel),
                            icon = Icons.Default.Close
                        )
                    } else {
                        TonalNavigationButton(
                            onClick = onNavigateBack,
                            contentDescription = stringResource(R.string.txn_detail_back)
                        )
                    }
                },
                actionContent = {
                    if (!isEditMode && transaction != null) {
                        PeopleTonalActionButton(
                            onClick = { viewModel.enterEditMode() },
                            icon = Iconax.Edit2,
                            contentDescription = stringResource(R.string.txn_detail_action_edit),
                            endPadding = Spacing.sm
                        )
                        TransactionOverflowMenu(
                            onAddToGroup = { viewModel.showGroupSheet() },
                            onAddToGoal = { showAddToGoal = true },
                            // Split a bill (#346): only an expense that isn't a loan.
                            splitLabel = transaction?.takeIf(::canSplit)?.let {
                                if (it.lentSharesTotal != null) R.string.split_menu_edit else R.string.split_menu_split
                            },
                            onSplit = { splitViewModel.openSheet() },
                            onDuplicate = { transaction?.let { onDuplicateTransaction(it.id) } },
                            onReportIssue = {
                                val reportUrl = viewModel.getReportUrl()
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(reportUrl))
                                    )
                                }
                            },
                            onDelete = { viewModel.showDeleteDialog() }
                        )
                    }
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            val displayTransaction = if (isEditMode) editableTransaction else transaction
            displayTransaction?.let { txn ->
                TransactionDetailContent(
                    transaction = txn,
                    isEditMode = isEditMode,
                    applyToAllFromMerchant = applyToAllFromMerchant,
                    updateExistingTransactions = updateExistingTransactions,
                    existingTransactionCount = existingTransactionCount,
                    viewModel = viewModel,
                    accountPrimaryCurrency = accountPrimaryCurrency,
                    convertedAmount = convertedAmount,
                    splits = splits,
                    showSplitEditor = showSplitEditor,
                    hasSplits = hasSplits,
                    loan = loan,
                    onNavigateToLoanDetail = onNavigateToLoanDetail,
                    onUnmarkLoanClick = { showUnmarkLoanConfirm = true },
                    accountProfileId = accountProfileId,
                    hazeState = hazeState,
                    paddingValues = paddingValues,
                    towardGoals = {
                        TowardGoalsSection(linkedGoals, onOpenGoal = onNavigateToGoal, onRemove = goalsViewModel::remove)
                    },
                    splitWith = {
                        transaction?.let { SplitSection(splitViewModel, it, onOpenLoan = onNavigateToLoanDetail) }
                    }
                )
            }

            // Sticky Save, as on the Add screen.
            if (isEditMode) {
                AddSaveBar(
                    enabled = !isSaving,
                    isLoading = isSaving,
                    onClick = { viewModel.saveChanges() }
                )
            }
        }
    }

    transaction?.let { SplitBillSheet(splitViewModel, it) }

    if (showAddToGoal) {
        transaction?.let { txn ->
            AddToGoalSheet(
                transaction = txn,
                goals = goals,
                state = addToGoal,
                onDismiss = ::closeAddToGoal,
                onSave = goalsViewModel::add,
                onCreateGoal = { closeAddToGoal(); onCreateGoal() },
                onEdited = goalsViewModel::clearError,
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            shape = MaterialTheme.shapes.extraLarge,
            iconContentColor = MaterialTheme.colorScheme.error,
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.txn_detail_delete_title)) },
            text = {
                Text(stringResource(R.string.txn_detail_delete_message))
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteTransaction() },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimensions.Icon.small),
                            strokeWidth = Spacing.xxs,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            stringResource(R.string.txn_detail_action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text(stringResource(R.string.txn_detail_action_cancel))
                }
            }
        )
    }

    // Group Bottom Sheet
    if (showGroupSheet) {
        TxnDetailGroupSheet(
            currentGroup = currentGroup,
            availableGroups = availableGroups,
            onDismiss = { viewModel.hideGroupSheet() },
            onAddToGroup = { groupId -> viewModel.addToGroup(groupId) },
            onRemoveFromGroup = { viewModel.removeFromGroup() },
            onCreateGroup = { name, note -> viewModel.createGroupAndAdd(name, note) }
        )
    }

    // Mark as Loan Bottom Sheet
    if (showMarkAsLoanSheet) {
        val txType = transaction?.transactionType
        val inferredDirection = if (txType == TransactionType.INCOME) LoanDirection.BORROWED else LoanDirection.LENT
        TxnDetailMarkAsLoanSheet(
            transactionAmount = transaction?.amount ?: BigDecimal.ZERO,
            transactionCurrency = transaction?.currency ?: "INR",
            direction = inferredDirection,
            recentPersonNames = recentPersonNames,
            onDismiss = { viewModel.hideMarkAsLoanSheet() },
            onConfirm = { personName, note, loanAmount ->
                viewModel.createLoanFromTransaction(personName, inferredDirection, note, loanAmount)
            }
        )
    }

    // Unmark-as-loan confirmation (#444)
    if (showUnmarkLoanConfirm) {
        AlertDialog(
            onDismissRequest = { showUnmarkLoanConfirm = false },
            shape = MaterialTheme.shapes.extraLarge,
            icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
            title = { Text(stringResource(R.string.txn_detail_unmark_loan_title)) },
            text = {
                Text(stringResource(R.string.txn_detail_unmark_loan_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showUnmarkLoanConfirm = false
                    viewModel.unlinkLoan()
                }) { Text(stringResource(R.string.txn_detail_unmark_loan_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showUnmarkLoanConfirm = false }) {
                    Text(stringResource(R.string.txn_detail_action_cancel))
                }
            }
        )
    }

    // Full-screen Receipt Dialog
    if (showFullScreenReceipt && receiptUri != null) {
        Dialog(
            onDismissRequest = { viewModel.hideFullScreenReceipt() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = receiptUri,
                    contentDescription = stringResource(R.string.txn_detail_receipt_full_screen),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.md),
                    contentScale = ContentScale.Fit
                )
                FilledIconButton(
                    onClick = { viewModel.hideFullScreenReceipt() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.md)
                        .statusBarsPadding(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.txn_detail_close))
                }
            }
        }
    }
}

/** The tonal "more" button in the top bar, with the actions that don't deserve their own button. */
@Composable
private fun TransactionOverflowMenu(
    onAddToGroup: () -> Unit,
    onAddToGoal: () -> Unit,
    @androidx.annotation.StringRes splitLabel: Int?,
    onSplit: () -> Unit,
    onDuplicate: () -> Unit,
    onReportIssue: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PeopleTonalActionButton(
            onClick = { expanded = true },
            icon = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.txn_detail_more_actions)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.txn_detail_menu_add_to_group)) },
                leadingIcon = { Icon(Iconax.Folder2, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddToGroup()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.goal_add_to_goal)) },
                leadingIcon = { Icon(Icons.Outlined.Savings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddToGoal()
                }
            )
            if (splitLabel != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(splitLabel)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onSplit()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.txn_detail_menu_duplicate)) },
                leadingIcon = { Icon(Iconax.Copy, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDuplicate()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.txn_detail_menu_report_issue)) },
                leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                onClick = {
                    expanded = false
                    onReportIssue()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.txn_detail_action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: TransactionEntity,
    isEditMode: Boolean,
    applyToAllFromMerchant: Boolean,
    updateExistingTransactions: Boolean,
    existingTransactionCount: Int,
    viewModel: TransactionDetailViewModel,
    accountPrimaryCurrency: String,
    convertedAmount: BigDecimal?,
    splits: List<SplitItem>,
    showSplitEditor: Boolean,
    hasSplits: Boolean,
    loan: LoanEntity?,
    onNavigateToLoanDetail: (Long) -> Unit,
    onUnmarkLoanClick: () -> Unit,
    accountProfileId: Long?,
    hazeState: HazeState,
    paddingValues: PaddingValues,
    towardGoals: @Composable () -> Unit = {},
    splitWith: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Content scrolls beneath the large top bar (hence the haze source), so the
    // bar's height is added as top padding inside the scroll area.
    Column(
        modifier = modifier
            .fillMaxSize()
            .hazeSource(hazeState)
            .background(MaterialTheme.colorScheme.background)
            .overScrollVertical()
            .imePadding()
            .verticalScroll(
                state = scrollState,
                flingBehavior = rememberOverscrollFlingBehavior { scrollState }
            )
            .padding(horizontal = Dimensions.Padding.content)
            .padding(
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + Spacing.Layout.scrollBottomPadding
            )
    ) {
        if (isEditMode) {
            TxnDetailEditForm(
                transaction = transaction,
                applyToAllFromMerchant = applyToAllFromMerchant,
                updateExistingTransactions = updateExistingTransactions,
                existingTransactionCount = existingTransactionCount,
                accountProfileId = accountProfileId,
                viewModel = viewModel,
                splits = splits,
                showSplitEditor = showSplitEditor
            )
        } else {
            TxnDetailReadOnlyBody(
                transaction = transaction,
                primaryCurrency = accountPrimaryCurrency,
                convertedAmount = convertedAmount,
                viewModel = viewModel,
                splits = splits,
                hasSplits = hasSplits,
                loan = loan,
                onNavigateToLoanDetail = onNavigateToLoanDetail,
                onUnmarkLoanClick = onUnmarkLoanClick,
                accountProfileId = accountProfileId,
                towardGoals = towardGoals,
                splitWith = splitWith
            )
        }
    }
}

@androidx.annotation.StringRes
internal fun transactionTypeLabel(type: TransactionType): Int = when (type) {
    TransactionType.INCOME -> R.string.txn_type_income
    TransactionType.EXPENSE -> R.string.txn_type_expense
    TransactionType.CREDIT -> R.string.txn_type_credit
    TransactionType.TRANSFER -> R.string.txn_type_transfer
    TransactionType.INVESTMENT -> R.string.txn_type_investment
}

/**
 * The account an edit field's [accountNumber] (and [bankName]) refers to (#876).
 * Same bank and number first; failing that, the one account with that number.
 * The number must match whole — a short or empty last-4 used to suffix-match
 * every account, so picking HDFC ••1155 could show some other account.
 */
internal fun List<TransactionDetailViewModel.AccountInfo>.matchFor(
    accountNumber: String?,
    bankName: String?
): TransactionDetailViewModel.AccountInfo? {
    // A blank number names an account only together with its bank: a Cash
    // account added without a number is ("Cash", "") (#913).
    if (accountNumber.isNullOrBlank()) {
        return if (accountNumber != null && bankName != null) find { it.accountLast4 == accountNumber && it.bankName == bankName } else null
    }
    val sameNumber = filter {
        it.accountLast4 == accountNumber ||
            (it.accountLast4.length >= 4 && accountNumber.endsWith(it.accountLast4))
    }
    return bankName?.let { bank -> sameNumber.find { it.bankName == bank } } ?: sameNumber.singleOrNull()
}
