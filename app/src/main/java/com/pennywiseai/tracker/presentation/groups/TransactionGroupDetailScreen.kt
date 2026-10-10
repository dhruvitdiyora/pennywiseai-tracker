package com.pennywiseai.tracker.presentation.groups

import com.pennywiseai.tracker.presentation.accounts.glassSheetContainerColor
import com.pennywiseai.tracker.presentation.accounts.glassSheetRim
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.presentation.people.PeopleExtendedFab
import com.pennywiseai.tracker.presentation.people.PeopleSearchField
import com.pennywiseai.tracker.presentation.people.PeopleTonalActionButton
import com.pennywiseai.tracker.presentation.transactions.ExportTransactionsDialog
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionGroupDetailScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTransactionDetail: (Long) -> Unit = {},
    viewModel: TransactionGroupDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    // The add button carries its label only while the list is at the top.
    val fabExpanded by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    val group = uiState.group

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = group?.name ?: stringResource(R.string.group_detail_title_fallback),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.group_back)
                    )
                },
                actionContent = {
                    if (group != null) {
                        var showMenu by remember { mutableStateOf(false) }
                        // The menu is anchored to the button; the screen gutter sits on
                        // the box so the menu lines up with the button itself.
                        Box(modifier = Modifier.padding(end = Dimensions.Padding.content)) {
                            PeopleTonalActionButton(
                                onClick = { showMenu = true },
                                icon = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.group_detail_more),
                                endPadding = Dimensions.Padding.none
                            )
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.group_detail_edit)) },
                                    onClick = { showMenu = false; viewModel.showEditDialog() },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                                )
                                if (uiState.linkedTransactions.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.group_detail_export_csv)) },
                                        onClick = { showMenu = false; showExportDialog = true },
                                        leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.group_delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = { showMenu = false; viewModel.showDeleteDialog() },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            PeopleExtendedFab(
                label = stringResource(R.string.group_detail_add_transaction),
                onClick = { viewModel.showAddSheet() },
                expanded = fabExpanded
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading || group == null) {
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
                bottom = paddingValues.calculateBottomPadding() +
                    Dimensions.Component.fabScrollClearance
            ),
            // Transaction rows sit 2dp apart as one connected list; the summary and
            // heading carry their own spacing.
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            // Summary card: the group's name and note, then its figures, each
            // possibly listing several currencies.
            item(key = "summary") {
                GroupSummaryCard(
                    title = group.name,
                    subtitle = group.note,
                    expenseByCurrency = uiState.expenseByCurrency,
                    investedByCurrency = uiState.investedByCurrency,
                    incomeByCurrency = uiState.incomeByCurrency,
                    transactionCount = uiState.linkedTransactions.size,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }

            if (uiState.linkedTransactions.isNotEmpty()) {
                item(key = "transactions-header") {
                    SectionHeaderV2(
                        title = stringResource(R.string.group_detail_transactions),
                        modifier = Modifier.padding(bottom = Spacing.Layout.headerToContent)
                    )
                }
                itemsIndexed(
                    uiState.linkedTransactions,
                    key = { _, txn -> txn.id }
                ) { index, txn ->
                    GroupTransactionRow(
                        transaction = txn,
                        position = ListItemPosition.from(index, uiState.linkedTransactions.size),
                        onClick = { onNavigateToTransactionDetail(txn.id) }
                    ) {
                        IconButton(onClick = { viewModel.removeTransaction(txn.id) }) {
                            Icon(
                                Icons.Default.RemoveCircleOutline,
                                contentDescription = stringResource(R.string.group_detail_remove_from_group),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimensions.Icon.inline)
                            )
                        }
                    }
                }
            } else {
                item(key = "empty") {
                    PennyWiseEmptyState(
                        icon = Icons.Default.AddCircleOutline,
                        headline = stringResource(R.string.group_detail_empty_title),
                        description = stringResource(R.string.group_detail_empty_hint)
                    )
                }
            }
        }
    }

    // Add transaction sheet
    if (uiState.showAddSheet) {
        AddTransactionToGroupSheet(
            searchQuery = uiState.addSearchQuery,
            onSearchQueryChange = { viewModel.updateAddSearchQuery(it) },
            ungroupedTransactions = uiState.ungroupedTransactions,
            onAdd = { viewModel.addTransaction(it) },
            onDismiss = { viewModel.hideAddSheet() }
        )
    }

    // Edit sheet
    if (uiState.showEditDialog && group != null) {
        GroupEditorSheet(
            title = stringResource(R.string.group_detail_edit_title),
            confirmLabel = stringResource(R.string.group_save),
            initialName = group.name,
            initialNote = group.note,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { name, note -> viewModel.updateGroupName(name, note) }
        )
    }

    // Export dialog — reuses the transactions CSV exporter (same Pro/free
    // row-limit behavior) on this group's linked transactions.
    if (showExportDialog) {
        ExportTransactionsDialog(
            transactions = uiState.linkedTransactions,
            onDismiss = { showExportDialog = false }
        )
    }

    // Delete dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text(stringResource(R.string.group_detail_delete_title)) },
            text = { Text(stringResource(R.string.group_detail_delete_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteGroup() }) {
                    Text(stringResource(R.string.group_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text(stringResource(R.string.group_cancel))
                }
            }
        )
    }
}

/**
 * Bottom sheet listing ungrouped transactions to add to the group: a rounded
 * search pill over a connected list of the same tonal rows the group itself
 * uses, so what you pick looks like what you get.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionToGroupSheet(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    ungroupedTransactions: List<TransactionEntity>,
    onAdd: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.glassSheetRim(),
        // The rows are tonal (surfaceContainerLow), so the sheet sits one step
        // lighter to let them read as raised.
        containerColor = glassSheetContainerColor(),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = Dimensions.Padding.content)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = stringResource(R.string.group_detail_picker_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            PeopleSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = stringResource(R.string.group_detail_picker_search),
                clearDescription = stringResource(R.string.people_clear_search)
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            if (ungroupedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            stringResource(R.string.group_detail_picker_empty)
                        } else {
                            stringResource(R.string.group_detail_picker_no_results)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
                    contentPadding = PaddingValues(bottom = Spacing.md)
                ) {
                    itemsIndexed(
                        ungroupedTransactions,
                        key = { _, txn -> txn.id }
                    ) { index, txn ->
                        GroupTransactionRow(
                            transaction = txn,
                            position = ListItemPosition.from(index, ungroupedTransactions.size),
                            onClick = { onAdd(txn.id) },
                            showYear = false
                        ) {
                            Icon(
                                Icons.Default.AddCircleOutline,
                                contentDescription = stringResource(R.string.group_detail_picker_add),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimensions.Icon.inline)
                            )
                        }
                    }
                }
            }
        }
    }
}
