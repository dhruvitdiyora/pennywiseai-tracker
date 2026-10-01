package com.pennywiseai.tracker.presentation.transactions

import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.database.entity.TransactionGroupEntity
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.chipLabel
import com.pennywiseai.tracker.presentation.common.label
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.ui.components.*
import com.pennywiseai.tracker.ui.components.skeleton.TransactionItemSkeleton
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import androidx.compose.foundation.shape.CircleShape
import com.pennywiseai.tracker.ui.icons.iconax.CloseCircle
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Search
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.DateRangeUtils
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    initialCategory: String? = null,
    initialMerchant: String? = null,
    initialPeriod: String? = null,
    initialCurrency: String? = null,
    // Custom date range carried by an analytics drill-down (period == CUSTOM)
    initialCustomStartEpochDay: Long? = null,
    initialCustomEndEpochDay: Long? = null,
    focusSearch: Boolean = false,
    // New parameters for budget navigation
    initialStartDateEpochDay: Long? = null,
    initialEndDateEpochDay: Long? = null,
    initialCategories: String? = null,  // Comma-separated category names
    initialTransactionType: String? = null,
    viewModel: TransactionsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    // False for a plain bottom-nav tab tap (no back arrow, like the other tab
    // roots); true when reached as a filtered drill-down so the user can return. (#635)
    showBackButton: Boolean = true,
    // True when the app's bottom nav is overlaid on this screen (tab context), so
    // the list + FAB reserve matching clearance regardless of the back arrow. (#635)
    reserveBottomBarSpace: Boolean = false,
    onTransactionClick: (Long) -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val categoriesFilter by viewModel.categoriesFilter.collectAsState()
    val categoriesFromBudget by viewModel.categoriesFromBudget.collectAsStateWithLifecycle()
    val transactionTypeFilter by viewModel.transactionTypeFilter.collectAsState()
    val deletedTransaction by viewModel.deletedTransaction.collectAsState()
    val categoriesMap by viewModel.categories.collectAsState()
    val filteredTotals by viewModel.filteredTotals.collectAsState()
    val availableCurrencies by viewModel.availableCurrencies.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val customDateRange by viewModel.customDateRange.collectAsState()
    val budgetCycleStartDay by viewModel.budgetCycleStartDay.collectAsStateWithLifecycle()
    val isUnifiedMode by viewModel.isUnifiedMode.collectAsState()
    val convertedAmounts by viewModel.convertedAmounts.collectAsState()
    val selectedProfileId by viewModel.selectedProfileId.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val profileAccountKeys by viewModel.profileAccountKeys.collectAsState()
    val accountFilter by viewModel.accountFilter.collectAsState()
    val accountOptions by viewModel.accountOptions.collectAsState()
    val tagFilter by viewModel.tagFilter.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val amountFilter by viewModel.amountFilter.collectAsState()
    val availableOriginalCurrencies by viewModel.availableOriginalCurrencies.collectAsState()

    // Bulk-edit selection (#369)
    val selectedIds by viewModel.selectedIds.collectAsState()
    val selectionTotals by viewModel.selectionTotals.collectAsState()
    val bulkSnack by viewModel.bulkSnack.collectAsState()
    val selectionMode = selectedIds.isNotEmpty()
    var showBulkCategorySheet by remember { mutableStateOf(false) }
    var showBulkGroupSheet by remember { mutableStateOf(false) }
    val groups by viewModel.groups.collectAsState()

    // Self-transfer suggestions (#385): map of txn-id → partner-id.
    val transferPartnerOf by viewModel.suggestedTransferPartnerOf.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    // Holds the id of the transaction whose category is being quick-edited via
    // swipe. Stored as Long? (not TransactionEntity?) so it survives rotation
    // without making the entity Parcelable; we look up the live row from the
    // current list when rendering the sheet.
    var pendingCategoryEditId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSortMenu by remember { mutableStateOf(false) } // Menu doesn't need saving
    var showFiltersSheet by rememberSaveable { mutableStateOf(false) }
    var showCustomRangePicker by rememberSaveable { mutableStateOf(false) }

    // Focus management for search field
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val view = LocalView.current

    val primaryVisibleCurrency = availableCurrencies.firstOrNull() ?: selectedCurrency
    val hasCurrencyFilter = !isUnifiedMode &&
            availableCurrencies.size > 1 &&
            !selectedCurrency.equals(primaryVisibleCurrency, ignoreCase = true)
    
    // Check if any filter is active (for showing "Clear all" button)
    val hasAnyActiveFilter = searchQuery.isNotEmpty() ||
        selectedPeriod != TimePeriod.THIS_MONTH ||
        categoryFilter != null ||
        categoriesFilter != null ||
        transactionTypeFilter != TransactionTypeFilter.ALL ||
        selectedProfileId != null ||
        accountFilter != null ||
        tagFilter != null ||
        amountFilter.isActive ||
        hasCurrencyFilter ||
        customDateRange != null

    val committedFilterDraft = remember(
        selectedPeriod,
        customDateRange,
        categoryFilter,
        categoriesFilter,
        categoriesFromBudget,
        transactionTypeFilter,
        selectedProfileId,
        accountFilter,
        tagFilter,
        amountFilter,
    ) {
        TransactionFilterDraft(
            period = selectedPeriod,
            customDateRange = customDateRange,
            category = categoryFilter,
            navigationCategories = categoriesFilter,
            categoriesFromBudget = categoriesFromBudget,
            transactionType = transactionTypeFilter,
            profileId = selectedProfileId,
            accountKey = accountFilter,
            tag = tagFilter,
            minimumText = amountFilter.range.minimum?.toPlainString().orEmpty(),
            maximumText = amountFilter.range.maximum?.toPlainString().orEmpty(),
            originalCurrencies = amountFilter.originalCurrencies,
        )
    }

    // Remember scroll position across navigation
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
    val collapseThresholdPx = with(density) { 48.dp.roundToPx() }
    val collapseTransactionHeader by remember(collapseThresholdPx) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > collapseThresholdPx
        }
    }

    // Cache expensive operations
    val customRangeLabel = remember(customDateRange) {
        DateRangeUtils.formatDateRange(customDateRange)
    }
    // True when the period differs from the default; it is the one dimension of
    // the draft that has inline chips rather than living only in the sheet.
    val periodFilterActive = selectedPeriod != TimePeriod.THIS_MONTH || customDateRange != null

    // Apply initial filters only once when screen is first created
    LaunchedEffect(Unit) {
        viewModel.applyInitialFilters(
            initialCategory,
            initialMerchant,
            initialPeriod,
            initialCurrency,
            initialCustomStartEpochDay,
            initialCustomEndEpochDay
        )
    }

    // Track if we've already processed these specific nav params
    var processedNavParams by rememberSaveable { mutableStateOf(false) }

    // Apply navigation filters only ONCE when actually navigating (not when returning from detail)
    LaunchedEffect(initialCategory, initialMerchant, initialPeriod, initialCurrency, initialCustomStartEpochDay, initialCustomEndEpochDay) {
        if (!processedNavParams && (initialCategory != null || initialMerchant != null || initialPeriod != null || initialCurrency != null)) {
            viewModel.applyNavigationFilters(
                initialCategory,
                initialMerchant,
                initialPeriod,
                initialCurrency,
                initialCustomStartEpochDay,
                initialCustomEndEpochDay
            )
            processedNavParams = true
        }
    }

    // Apply budget filters when navigating from budget screen
    LaunchedEffect(initialStartDateEpochDay, initialEndDateEpochDay, initialCategories, initialTransactionType) {
        if (initialStartDateEpochDay != null && initialEndDateEpochDay != null) {
            viewModel.applyBudgetFilters(
                startDateEpochDay = initialStartDateEpochDay,
                endDateEpochDay = initialEndDateEpochDay,
                currency = initialCurrency,
                categories = initialCategories,
                transactionType = initialTransactionType
            )
        }
    }
    
    // Handle delete undo snackbar
    val deletedMessage = stringResource(R.string.txn_list_deleted)
    val undoLabel = stringResource(R.string.txn_list_undo)
    LaunchedEffect(deletedTransaction) {
        deletedTransaction?.let { transaction ->
            // Clear the state immediately to prevent re-triggering
            viewModel.clearDeletedTransaction()
            
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = deletedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    // Pass the transaction directly since state is already cleared
                    viewModel.undoDeleteTransaction(transaction)
                }
            }
        }
    }
    
    // Focus search field if requested
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    
    // Clear snackbar when navigating away
    DisposableEffect(Unit) {
        onDispose {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    // One pinned behaviour for both slots of CustomTitleTopAppBar: it then
    // renders only the compact, centre-aligned bar (round back button, title
    // in the middle) instead of a large collapsing header.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    // When the app bottom nav is overlaid on this screen (#635), it sits over an
    // 80.dp strip the Scaffold inset here doesn't know about, so the list + FAB
    // stack need matching bottom clearance (whether or not a back arrow shows).
    val bottomBarClearance = if (reserveBottomBarSpace) Dimensions.Component.bottomBarHeight else 0.dp

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (selectionMode) {
                // Contextual top bar for bulk-edit (#369): close left, count as title,
                // Change Category + Delete on the right.
                CustomTitleTopAppBar(
                    scrollBehaviorSmall = scrollBehavior,
                    scrollBehaviorLarge = scrollBehavior,
                    title = stringResource(R.string.txn_list_selected_count, selectedIds.size),
                    hasBackButton = true,
                    navigationContent = {
                        TonalNavigationButton(
                            onClick = { viewModel.clearSelection() },
                            contentDescription = stringResource(R.string.txn_list_exit_selection),
                            icon = Icons.Default.Close,
                        )
                    },
                    actionContent = {
                        // Wrap explicitly in a Row so both action IconButtons render
                        // even when the top-bar's actions slot is constrained.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Link exactly two selected rows as a transfer — the
                            // manual counterpart to the auto-detected chip (#614),
                            // covering pairs the detector misses (fees, wide gaps).
                            if (selectedIds.size == 2) {
                                IconButton(onClick = { viewModel.bulkMarkAsTransfer() }) {
                                    Icon(
                                        Icons.Default.SwapHoriz,
                                        contentDescription = stringResource(R.string.txn_list_mark_as_transfer)
                                    )
                                }
                            }
                            IconButton(onClick = { showBulkCategorySheet = true }) {
                                Icon(Icons.Default.Category, contentDescription = stringResource(R.string.txn_list_change_category))
                            }
                            IconButton(onClick = { showBulkGroupSheet = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = stringResource(R.string.txn_list_add_to_group)
                                )
                            }
                            IconButton(onClick = { viewModel.bulkDelete() }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.txn_list_delete_selected),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    hazeState = hazeState
                )
            } else {
                CustomTitleTopAppBar(
                    scrollBehaviorSmall = scrollBehavior,
                    scrollBehaviorLarge = scrollBehavior,
                    title = stringResource(R.string.txn_list_title),
                    hasBackButton = showBackButton,
                    navigationContent = {
                        TonalNavigationButton(
                            onClick = onNavigateBack,
                            contentDescription = stringResource(R.string.txn_list_back),
                        )
                    },
                    hazeState = hazeState
                )
            }
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = bottomBarClearance),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Export FAB (only show if transactions exist): a small tonal
                // button stacked above the primary add action.
                if (uiState.transactions.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = { showExportDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.txn_list_export_csv),
                            modifier = Modifier.size(Dimensions.Icon.medium)
                        )
                    }
                }

                // Add Transaction FAB
                FloatingActionButton(
                    onClick = onAddTransactionClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.txn_list_add_transaction)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .padding(top = paddingValues.calculateTopPadding())
        ) {
        UnifiedTransactionFilterHeader(
            searchQuery = searchQuery,
            selectedPeriod = selectedPeriod,
            customDateRangeSelected = customDateRange != null,
            customRangeLabel = customRangeLabel,
            budgetCycleStartDay = budgetCycleStartDay,
            // The period has its own inline chips, so "More Filters" counts
            // everything else the unified sheet can narrow by.
            moreFilterCount = committedFilterDraft.activeFilterCount -
                if (periodFilterActive) 1 else 0,
            hasAnyActiveFilter = hasAnyActiveFilter,
            showSortMenu = showSortMenu,
            collapsed = collapseTransactionHeader,
            sortOption = sortOption,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSortClick = { showSortMenu = true },
            onSortDismiss = { showSortMenu = false },
            onSortSelected = { option ->
                viewModel.setSortOption(option)
                showSortMenu = false
            },
            onPeriodSelected = { period ->
                if (period == TimePeriod.CUSTOM) {
                    // Keep the current period until the user confirms dates.
                    showCustomRangePicker = true
                } else {
                    viewModel.selectPeriod(period)
                    // Drop a leftover custom range so it no longer counts as an
                    // active filter once a preset period is chosen again.
                    if (customDateRange != null) viewModel.clearCustomDateRange()
                }
            },
            onFiltersClick = { showFiltersSheet = true },
            onResetFilters = viewModel::resetFilters,
            focusRequester = searchFocusRequester,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm)
        )
        
        // Transaction List
        when {
            uiState.isLoading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Spacing.md,
                        bottom = paddingValues.calculateBottomPadding() + bottomBarClearance
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    item {
                        TransactionTotalsCard(
                            income = filteredTotals.income,
                            expenses = filteredTotals.expenses,
                            netBalance = filteredTotals.netBalance,
                            currency = selectedCurrency,
                            availableCurrencies = availableCurrencies,
                            onCurrencySelected = { viewModel.selectCurrency(it) },
                            isUnifiedMode = isUnifiedMode,
                            isLoading = true,
                            modifier = Modifier.padding(bottom = Spacing.sm)
                        )
                    }
                    items(8) {
                        TransactionItemSkeleton()
                    }
                }
            }
            uiState.transactions.isEmpty() -> {
                EmptyTransactionsState(
                    searchQuery = searchQuery,
                    selectedPeriod = selectedPeriod,
                    onAddClick = onAddTransactionClick
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().overScrollVertical(),
                    contentPadding = PaddingValues(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Spacing.md,
                        bottom = paddingValues.calculateBottomPadding() + bottomBarClearance
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
                    flingBehavior = rememberOverscrollFlingBehavior { listState }
                ) {
                    stickyHeader {
                        Surface(
                            // Match the Scaffold's `background` so AMOLED-style
                            // themes (true-black bg + tinted surface) don't paint
                            // a visible slab behind the sticky totals card.
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // In selection mode the sticky card shows the totals of
                            // just the selected rows (#634), with the credit tile the
                            // reporter asked for; otherwise the filtered-list totals.
                            val cardTotals = if (selectionMode) selectionTotals else filteredTotals
                            TransactionTotalsCard(
                                income = cardTotals.income,
                                expenses = cardTotals.expenses,
                                netBalance = cardTotals.netBalance,
                                credit = if (selectionMode) cardTotals.credit else null,
                                title = if (selectionMode) {
                                    stringResource(R.string.txn_list_selected_count, selectedIds.size)
                                } else {
                                    null
                                },
                                currency = selectedCurrency,
                                availableCurrencies = availableCurrencies,
                                onCurrencySelected = { viewModel.selectCurrency(it) },
                                isUnifiedMode = isUnifiedMode,
                                isLoading = uiState.isLoading,
                                modifier = Modifier.padding(bottom = Spacing.sm)
                            )
                        }
                    }

                    // Show info banner when viewing budget transactions
                    if (categoriesFromBudget) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = Spacing.sm)
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.sm),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimensions.Icon.small),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.txn_list_budget_split_notice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Iterate through date groups in order
                    listOf(
                        DateGroup.TODAY,
                        DateGroup.YESTERDAY,
                        DateGroup.THIS_WEEK,
                        DateGroup.EARLIER
                    ).forEach { dateGroup ->
                        uiState.groupedTransactions[dateGroup]?.let { transactions ->
                            // Date group header
                            val headerContent: @Composable LazyItemScope.(Int) -> Unit = { _ ->
                                TransactionDateHeader(title = stringResource(dateGroup.labelRes))
                            }
                            stickyHeader(content = headerContent)
                            
                            // Transactions in this group
                            itemsIndexed(
                                items = transactions,
                                key = { _, transaction -> transaction.id }
                            ) { index, transaction ->
                                val isSelected = transaction.id in selectedIds
                                // Selected highlight via the Card's container colour — the prior
                                // Modifier.background on a wrapping Box was painted *under* the
                                // Card and was completely covered by the Card's own surface, so
                                // selection had no visible effect.
                                val rowContainerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else null

                                // Long-press now lives on TransactionItem's own
                                // gesture surface (Material combinedClickable),
                                // which cooperates with the parent SwipeToDismissBox
                                // — fixes bulk-edit not firing in nested gesture
                                // contexts.
                                val longPressToggle = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    viewModel.toggleSelection(transaction.id)
                                }
                                if (selectionMode) {
                                    com.pennywiseai.tracker.ui.components.cards.TransactionItem(
                                        transaction = transaction,
                                        listItemPosition = ListItemPosition.from(index, transactions.size),
                                        convertedAmount = convertedAmounts[transaction.id],
                                        displayCurrency = if (isUnifiedMode) selectedCurrency else null,
                                        profileAccountKeys = profileAccountKeys,
                                        onClick = { viewModel.toggleSelection(transaction.id) },
                                        onLongClick = longPressToggle,
                                        containerColor = rowContainerColor,
                                        isSelectionMode = true,
                                        isSelected = isSelected
                                    )
                                } else {
                                    Column {
                                        SwipeToEditCategory(
                                            transaction = transaction,
                                            onRequestEdit = { pendingCategoryEditId = it.id }
                                        ) {
                                            com.pennywiseai.tracker.ui.components.cards.TransactionItem(
                                                transaction = transaction,
                                                listItemPosition = ListItemPosition.from(index, transactions.size),
                                                convertedAmount = convertedAmounts[transaction.id],
                                                displayCurrency = if (isUnifiedMode) selectedCurrency else null,
                                                profileAccountKeys = profileAccountKeys,
                                                onClick = { onTransactionClick(transaction.id) },
                                                onLongClick = longPressToggle
                                            )
                                        }
                                        // Self-transfer suggestion (#385): show the affordance on
                                        // the EXPENSE row only so each pair surfaces once.
                                        val partnerId = transferPartnerOf[transaction.id]
                                        if (partnerId != null &&
                                            transaction.transactionType == TransactionType.EXPENSE
                                        ) {
                                            AssistChip(
                                                onClick = { viewModel.markPairAsTransfer(transaction.id, partnerId) },
                                                label = { Text(stringResource(R.string.txn_list_mark_as_transfer)) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.SwapHoriz,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(Dimensions.Icon.small)
                                                    )
                                                },
                                                modifier = Modifier.padding(start = Spacing.sm, top = Spacing.xs)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
    
    // Export Dialog
    if (showExportDialog) {
        ExportTransactionsDialog(
            transactions = uiState.transactions,
            onDismiss = { showExportDialog = false }
        )
    }

    // Custom range picked from the inline "Custom Range" chip. Confirming sets
    // both the dates and the CUSTOM period in one ViewModel call.
    if (showCustomRangePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showCustomRangePicker = false },
            onConfirm = { start, end ->
                viewModel.setCustomDateRange(start, end)
                showCustomRangePicker = false
            },
            initialStartDate = customDateRange?.first,
            initialEndDate = customDateRange?.second,
        )
    }

    if (showFiltersSheet) {
        TransactionFiltersSheet(
            initialDraft = committedFilterDraft,
            availableCategories = availableCategories,
            profiles = profiles,
            accountOptions = accountOptions,
            availableTags = availableTags,
            availableOriginalCurrencies = availableOriginalCurrencies,
            unifiedMode = isUnifiedMode,
            displayCurrency = selectedCurrency,
            onApply = { draft ->
                val result = viewModel.applyFilterDraft(draft)
                if (result.isValid) {
                    showFiltersSheet = false
                }
                result
            },
            onDismiss = { showFiltersSheet = false },
        )
    }

    pendingCategoryEditId?.let { id ->
        val transaction = uiState.transactions.firstOrNull { it.id == id }
        if (transaction == null) {
            // Row vanished (e.g. deleted via another path while sheet was open).
            // Drop the pending edit so we don't keep an orphan sheet alive.
            LaunchedEffect(id) { pendingCategoryEditId = null }
        } else {
            QuickCategoryPickerSheet(
                transaction = transaction,
                categories = categoriesMap.values.toList(),
                onCategorySelected = { name ->
                    viewModel.updateCategory(transaction, name)
                    pendingCategoryEditId = null
                },
                onDismiss = { pendingCategoryEditId = null }
            )
        }
    }

    // Bulk category picker (#369). If every selected row already shares a
    // category, mark it; otherwise pass a "(multiple)" sentinel so nothing is
    // pre-checked.
    if (showBulkCategorySheet && selectedIds.isNotEmpty()) {
        val selectedTxns = uiState.transactions.filter { it.id in selectedIds }
        val commonCategory = selectedTxns.map { it.category }.distinct().singleOrNull()
        QuickCategoryPickerSheet(
            currentCategory = commonCategory ?: "(multiple)",
            categories = categoriesMap.values.toList(),
            onCategorySelected = { name ->
                viewModel.bulkUpdateCategory(name)
                showBulkCategorySheet = false
            },
            onDismiss = { showBulkCategorySheet = false }
        )
    }

    // Bulk add-to-group picker (#506): add every selected row to an existing
    // group or a brand-new one in a single action.
    if (showBulkGroupSheet && selectedIds.isNotEmpty()) {
        BulkGroupPickerSheet(
            selectedCount = selectedIds.size,
            groups = groups,
            onGroupSelected = { group ->
                viewModel.bulkAddToGroup(group.id, group.name)
                showBulkGroupSheet = false
            },
            onCreateGroup = { name ->
                viewModel.bulkCreateGroupAndAdd(name)
                showBulkGroupSheet = false
            },
            onDismiss = { showBulkGroupSheet = false }
        )
    }

    // Bulk action snackbar with Undo (#369).
    val bulkSnackText = bulkSnack?.message?.asString()
    LaunchedEffect(bulkSnack) {
        bulkSnack?.let { snack ->
            val result = snackbarHostState.showSnackbar(
                message = bulkSnackText.orEmpty(),
                actionLabel = snack.undo?.let { undoLabel },
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) snack.undo?.invoke()
            viewModel.consumeBulkSnack()
        }
    }

    // Hardware back exits selection mode instead of leaving the screen.
    BackHandler(enabled = selectionMode) {
        viewModel.clearSelection()
    }
}

/**
 * Bulk "Add to group" picker (#506). Lists existing groups (tap to add all
 * selected transactions) and a "Create new group" affordance. Unlike the
 * single-transaction [GroupBottomSheet], there is no "current group" — the
 * selection can span rows in different groups, so adding simply moves them all
 * to the chosen group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkGroupPickerSheet(
    selectedCount: Int,
    groups: List<TransactionGroupEntity>,
    onGroupSelected: (TransactionGroupEntity) -> Unit,
    onCreateGroup: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateField by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.Padding.content)
                .padding(bottom = Dimensions.Padding.content)
        ) {
            Text(
                text = stringResource(R.string.txn_list_bulk_group_title, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = Spacing.md)
            )

            if (groups.isNotEmpty()) {
                // Cap the list height and let it scroll so a long group list
                // doesn't push "Create new group" off the bottom of the sheet.
                Column(
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    groups.forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGroupSelected(group) }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimensions.Icon.medium)
                            )
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            if (showCreateField) {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text(stringResource(R.string.txn_list_group_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newGroupName.isNotBlank()) {
                                onCreateGroup(newGroupName.trim())
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newGroupName.isNotBlank()) {
                                    onCreateGroup(newGroupName.trim())
                                }
                            },
                            enabled = newGroupName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.txn_list_group_create))
                        }
                    }
                )
            } else {
                TextButton(
                    onClick = { showCreateField = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.small)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.txn_list_group_create_new))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToEditCategory(
    transaction: TransactionEntity,
    onRequestEdit: (TransactionEntity) -> Unit,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onRequestEdit(transaction)
            }
            // Never let the swipe complete — we use it purely as a gesture trigger
            // and snap back to the resting state.
            false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart ->
                        MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.Transparent
                },
                label = "swipe_edit_background"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = Dimensions.Padding.content),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(R.string.txn_list_change_category),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        content = { content() }
    )
}

@Composable
private fun TransactionDateHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    // Fade the sticky date header into a transparent base so scrolling content
    // looks like it's passing under the label. Use `background` (the Scaffold
    // surface) instead of `surface`, otherwise an AMOLED-style theme — where
    // `background` is true-black but `surface` is tinted — paints a visible
    // dark slab behind every date header.
    val pageBg = MaterialTheme.colorScheme.background
    // Accent-coloured label inset to line up with the row content below it.
    // Deliberately `primary` rather than SectionHeaderV2's neutral title: in
    // this list the heading is the only accent text, so it reads as the
    // anchor for the group of rows that follows.
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        pageBg,
                        pageBg,
                        pageBg.copy(alpha = 0.92f),
                        pageBg.copy(alpha = 0f)
                    )
                )
            )
            .padding(start = Spacing.md, top = Spacing.md, bottom = Spacing.sm)
            .semantics { heading() }
    )
}

/**
 * The Transactions header: search field with its "…" menu, the scrolling
 * period chips, and the "More Filters" row that opens the unified filter
 * sheet. The chips and the row fold away as the list scrolls; the search
 * field stays put.
 */
@Composable
private fun UnifiedTransactionFilterHeader(
    searchQuery: String,
    selectedPeriod: TimePeriod,
    customDateRangeSelected: Boolean,
    customRangeLabel: String?,
    budgetCycleStartDay: Int,
    moreFilterCount: Int,
    hasAnyActiveFilter: Boolean,
    showSortMenu: Boolean,
    collapsed: Boolean,
    sortOption: SortOption,
    onSearchQueryChange: (String) -> Unit,
    onSortClick: () -> Unit,
    onSortDismiss: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onFiltersClick: () -> Unit,
    onResetFilters: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        TransactionSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            categoryFilter = null,
            focusRequester = focusRequester,
            trailingContent = {
                Box {
                    IconButton(onClick = onSortClick) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = stringResource(R.string.txn_list_more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = onSortDismiss,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = sortOption == option,
                                            onClick = null,
                                            modifier = Modifier.size(Dimensions.Icon.medium),
                                        )
                                        Text(stringResource(option.labelRes))
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimensions.Icon.small),
                                    )
                                },
                                onClick = { onSortSelected(option) },
                            )
                        }
                        if (hasAnyActiveFilter) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.txn_list_clear_filters))
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    onResetFilters()
                                    onSortDismiss()
                                },
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.Padding.content),
        )

        AnimatedVisibility(
            visible = !collapsed,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                    // The row bleeds to the screen edge so chips scroll under
                    // the gutter, but the first one lines up with the content.
                    contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(TimePeriod.entries.toList(), key = { it.name }) { period ->
                        PeriodFilterChip(
                            // CUSTOM only reads as selected once dates exist.
                            selected = if (period == TimePeriod.CUSTOM) {
                                selectedPeriod == period && customDateRangeSelected
                            } else {
                                selectedPeriod == period
                            },
                            // Budget-cycle-aware: a cycle that does not start on
                            // the 1st shows its resolved dates, not "This Month".
                            label = period.chipLabel(
                                budgetCycleStartDay,
                                customRangeLabel,
                                period.label,
                            ),
                            onClick = { onPeriodSelected(period) },
                        )
                    }
                }
                TransactionMoreFiltersRow(
                    activeCount = moreFilterCount,
                    onClick = onFiltersClick,
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                )
            }
        }
    }
}

/** "More Filters" row with a filter glyph and chevron; opens the unified sheet. */
@Composable
private fun TransactionMoreFiltersRow(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = activeCount > 0
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.inline),
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Text(
            text = if (active) {
                pluralStringResource(R.plurals.filter_row_more_active, activeCount, activeCount)
            } else {
                stringResource(R.string.filter_row_more)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.medium),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    categoryFilter: String? = null,
    focusRequester: FocusRequester? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    // A tall, fully rounded field on the quiet `surfaceContainerLow` tone, so it
    // reads as the primary control of the screen without needing a border.
    Surface(
        modifier = modifier.height(Dimensions.Component.listItemMinHeight),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = Spacing.md, end = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Iconax.Search,
                contentDescription = stringResource(R.string.txn_list_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimensions.Icon.medium)
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = if (categoryFilter != null) stringResource(R.string.txn_list_search_in_category, categoryFilter)
                                else stringResource(R.string.txn_list_search_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Iconax.CloseCircle,
                        contentDescription = stringResource(R.string.txn_list_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailingContent?.invoke()
        }
    }
}


@Composable
private fun EmptyTransactionsState(
    searchQuery: String,
    selectedPeriod: TimePeriod,
    onAddClick: () -> Unit = {}
) {
    val headline = when {
        searchQuery.isNotEmpty() -> stringResource(R.string.txn_list_empty_no_results, searchQuery)
        selectedPeriod != TimePeriod.ALL -> stringResource(R.string.txn_list_empty_period, selectedPeriod.label.lowercase())
        else -> stringResource(R.string.txn_list_empty_title)
    }
    val description = when {
        searchQuery.isNotEmpty() -> stringResource(R.string.txn_list_empty_search_hint)
        selectedPeriod != TimePeriod.ALL -> stringResource(R.string.txn_list_empty_period_hint)
        else -> stringResource(R.string.txn_list_empty_hint)
    }
    val actionLabel = if (searchQuery.isEmpty() && selectedPeriod == TimePeriod.ALL) stringResource(R.string.txn_list_add_transaction) else null
    val onAction = if (actionLabel != null) onAddClick else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.Padding.content),
        contentAlignment = Alignment.Center
    ) {
        PennyWiseEmptyState(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            headline = headline,
            description = description,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }
}
