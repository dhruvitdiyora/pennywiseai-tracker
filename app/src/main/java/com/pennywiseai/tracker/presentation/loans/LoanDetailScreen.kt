package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.background
import com.pennywiseai.tracker.ui.icons.iconax.ReceiptItem
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.presentation.people.PeopleExtendedFab
import com.pennywiseai.tracker.presentation.people.PeopleTonalActionButton
import com.pennywiseai.tracker.presentation.people.LiftedHeaderAvatarSize
import com.pennywiseai.tracker.presentation.people.PersonAvatar
import com.pennywiseai.tracker.presentation.people.initialsOf
import com.pennywiseai.tracker.domain.loan.LoanMath
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemCardV2
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTransactionDetail: (Long) -> Unit = {},
    viewModel: LoanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    // The record-payment button carries its label only while the list is at the top.
    val fabExpanded by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(uiState.repaymentFailed) {
        if (uiState.repaymentFailed) {
            viewModel.repaymentFailureShown()
            snackbarHostState.showSnackbar(context.getString(R.string.loan_detail_record_payment_failed))
        }
    }

    val loan = uiState.loan

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = loan?.personName ?: stringResource(R.string.loan_detail_title_fallback),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.accounts_back)
                    )
                },
                actionContent = {
                    if (loan != null) {
                        LoanOverflowMenu(
                            loan = loan,
                            onSetExpectedReturn = { viewModel.showEditAmountDialog() },
                            onSettle = { viewModel.showSettleDialog() },
                            onReopen = { viewModel.reopenLoan() },
                            onDelete = { viewModel.showDeleteDialog() },
                        )
                    }
                },
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            if (loan?.status == LoanStatus.ACTIVE) {
                PeopleExtendedFab(
                    label = stringResource(R.string.loan_detail_record_payment),
                    onClick = { viewModel.showRecordPayment() },
                    expanded = fabExpanded,
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading || loan == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

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
            // History rows sit 2dp apart as one connected list; the hero and the
            // section header carry their own spacing.
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            item(key = "hero") {
                LoanHeroCard(
                    loan = loan,
                    onSettle = { viewModel.showSettleDialog() },
                    onReopen = { viewModel.reopenLoan() },
                    modifier = Modifier.padding(bottom = Dimensions.Padding.content),
                )
            }

            // Transaction history
            item(key = "history-header") {
                SectionHeaderV2(
                    title = stringResource(R.string.loan_detail_history),
                    modifier = Modifier.padding(bottom = Spacing.Layout.headerToContent),
                )
            }

            if (uiState.linkedTransactions.isEmpty()) {
                item(key = "history-empty") { LoanHistoryEmpty() }
            } else {
                itemsIndexed(uiState.linkedTransactions, key = { _, txn -> txn.id }) { index, txn ->
                    // Principal flows the loan's way (LENT→money out, BORROWED→money in).
                    val isOriginal = LoanMath.isPrincipal(loan.direction, txn.transactionType)
                    LoanTransactionItem(
                        transaction = txn,
                        isOriginal = isOriginal,
                        position = ListItemPosition.from(index, uiState.linkedTransactions.size),
                        onClick = { onNavigateToTransactionDetail(txn.id) }
                    )
                }
            }
        }
    }

    if (uiState.showSettleDialog) {
        SettleLoanDialog(
            onConfirm = { viewModel.settleLoan() },
            onDismiss = { viewModel.hideSettleDialog() },
        )
    }

    if (uiState.showEditAmountDialog && loan != null) {
        EditExpectedReturnDialog(
            loan = loan,
            onSave = { viewModel.updateLoanAmount(it) },
            onDismiss = { viewModel.hideEditAmountDialog() },
        )
    }

    if (uiState.showDeleteDialog) {
        DeleteLoanDialog(
            onConfirm = { viewModel.deleteLoan() },
            onDismiss = { viewModel.hideDeleteDialog() },
        )
    }

    // Record Payment bottom sheet
    if (uiState.showRecordPaymentSheet && loan != null) {
        RecordPaymentBottomSheet(
            personName = loan.personName,
            remainingAmount = loan.remainingAmount,
            currency = loan.currency,
            recentUnlinkedTransactions = uiState.recentUnlinkedTransactions,
            onDismiss = { viewModel.hideRecordPayment() },
            onLinkTransaction = { viewModel.linkTransactionAsRepayment(it) },
            onManualPayment = { viewModel.recordManualRepayment(it) }
        )
    }
}

/** The tonal "more" button in the top bar with the loan's actions. */
@Composable
private fun LoanOverflowMenu(
    loan: LoanEntity,
    onSetExpectedReturn: () -> Unit,
    onSettle: () -> Unit,
    onReopen: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        PeopleTonalActionButton(
            onClick = { showMenu = true },
            icon = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.loan_detail_more),
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = MaterialTheme.shapes.large,
        ) {
            if (loan.status == LoanStatus.ACTIVE) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.loan_detail_menu_set_expected_return)) },
                    onClick = { showMenu = false; onSetExpectedReturn() },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.loan_detail_settle)) },
                    onClick = { showMenu = false; onSettle() },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.loan_detail_menu_reopen)) },
                    onClick = { showMenu = false; onReopen() },
                    leadingIcon = { Icon(Icons.Default.Refresh, null) }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.accounts_action_delete), color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

/**
 * The loan at a glance, drawn as Cashiro's person header — straight on the
 * page, no card: the lifted avatar, the direction tag, how much is still open
 * and of what, the repayment track, then the action row (Settle beside a
 * status chip, or Reopen once settled) and the note.
 */
@Composable
private fun LoanHeroCard(
    loan: LoanEntity,
    onSettle: () -> Unit,
    onReopen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val directionColor = loanDirectionColor(loan.direction)
    val settled = loan.status == LoanStatus.SETTLED
    val progress = loanRepaidFraction(loan)
    val statusColor = if (settled) scheme.onSurfaceVariant else scheme.income
    val actionShape = RoundedCornerShape(Dimensions.CornerRadius.card)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.smd),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        PersonAvatar(
            initials = initialsOf(loan.personName),
            color = directionColor,
            modifier = Modifier.shadow(Spacing.xxs, CircleShape),
            size = LiftedHeaderAvatarSize,
            textStyle = MaterialTheme.typography.headlineMedium,
        )
        SubtitleTag(
            text = stringResource(
                if (loan.direction == LoanDirection.LENT) {
                    R.string.loans_direction_lent
                } else {
                    R.string.loans_direction_borrowed
                },
            ),
            color = directionColor,
            modifier = Modifier.padding(top = Spacing.xs),
        )

        Text(
            text = CurrencyFormatter.formatCurrency(loan.remainingAmount, loan.currency),
            style = PennyWiseText.heroAmount,
            color = if (settled) scheme.onSurfaceVariant else directionColor,
        )
        Text(
            text = if (settled) {
                stringResource(R.string.loans_status_settled)
            } else {
                stringResource(
                    R.string.loan_detail_remaining_of,
                    CurrencyFormatter.formatCurrency(loan.originalAmount, loan.currency),
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface.copy(alpha = Dimensions.Alpha.subtitle),
        )

        if (!settled) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs)
                    .height(Dimensions.Component.progressBarHeight)
                    .clip(CircleShape),
                color = scheme.income,
                trackColor = scheme.surfaceContainerHighest,
                drawStopIndicator = {},
            )
        }

        // Cashiro's action row: the primary action beside a washed status chip.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (settled) {
                FilledTonalButton(
                    onClick = onReopen,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.minTouchTarget),
                    shape = actionShape,
                ) {
                    Text(
                        text = stringResource(R.string.loan_detail_menu_reopen),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Button(
                    onClick = onSettle,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.minTouchTarget),
                    shape = actionShape,
                ) {
                    Text(
                        text = stringResource(R.string.loan_detail_settle),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = actionShape,
                color = statusColor.copy(alpha = STATUS_CHIP_ALPHA),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimensions.Component.minTouchTarget),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (settled) {
                            stringResource(R.string.loans_status_settled)
                        } else {
                            stringResource(R.string.loan_detail_percent_repaid, (progress * 100).toInt())
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // The note captured in "mark as loan" had no home until now
        // (#754) — it is the only place the user says what the loan
        // was for, so it sits with the amount rather than below the
        // transaction history. Cashiro shows a person's notes the same way.
        loan.note?.takeIf { it.isNotBlank() }?.let { note ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs),
                shape = actionShape,
                color = scheme.surfaceContainerLow,
            ) {
                Text(
                    text = note,
                    modifier = Modifier.padding(Spacing.smd),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Cashiro washes its status chip at 10%. */
private const val STATUS_CHIP_ALPHA = 0.1f

/** Cashiro's empty-transactions card, for a loan with no history yet. */
@Composable
private fun LoanHistoryEmpty(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                imageVector = Iconax.ReceiptItem,
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = Spacing.smd)
                    .size(Dimensions.Icon.avatarLarge),
                tint = scheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.loan_detail_history_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.loan_detail_history_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoanTransactionItem(
    transaction: TransactionEntity,
    isOriginal: Boolean,
    position: ListItemPosition,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    // Color reflects the actual money flow: red = money out, green = money in.
    // Every type but INCOME is money out, as in the loan's own math (#876).
    val moneyIn = transaction.transactionType == TransactionType.INCOME
    val color = if (moneyIn) scheme.income else scheme.expense
    val sign = if (moneyIn) "+" else "-"
    val icon = if (moneyIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
    val date = transaction.dateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    val originalLabel = stringResource(R.string.loan_detail_original_chip)
    val note = transaction.description?.takeIf { it.isNotBlank() }

    ListItemCardV2(
        title = transaction.merchantName,
        // One sentence for accessibility; the tag below is its visual form.
        subtitle = listOfNotNull(date, originalLabel.takeIf { isOriginal }, note).joinToString(", "),
        // Show the portion assigned to this loan, not the full transaction
        // amount — matches how the loan total is computed (#681).
        amount = CurrencyFormatter.signed(sign, CurrencyFormatter.formatCurrency(transaction.loanContribution ?: transaction.amount, transaction.currency)),
        amountColor = color,
        leadingContent = {
            IconTile(
                icon = icon,
                containerColor = color.copy(alpha = Dimensions.Alpha.tonalIconContainer),
                contentColor = color,
                size = Dimensions.Icon.list,
                glyphSize = Dimensions.Icon.inline,
            )
        },
        shape = position.toShape(),
        onClick = onClick,
        subtitleContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = date,
                        style = PennyWiseText.metadata,
                        color = scheme.onSurfaceVariant,
                    )
                    if (isOriginal) {
                        SubtitleTag(text = originalLabel, color = scheme.primary)
                    }
                }
                // This transaction's own note, not the loan's (#876).
                note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
    )
}
