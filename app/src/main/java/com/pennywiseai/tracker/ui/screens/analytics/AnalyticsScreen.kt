package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.ui.res.pluralStringResource
import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import com.pennywiseai.tracker.ui.icons.iconax.AiCommentary
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.data.contacts.LocalMerchantDisplay
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.chipLabel
import com.pennywiseai.tracker.presentation.common.label
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.ui.components.*
import com.pennywiseai.tracker.ui.components.cards.ListItemCardV2
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.filterIcon
import com.pennywiseai.tracker.ui.components.shortLabel
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Menu
import com.pennywiseai.tracker.ui.icons.iconax.Status
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.DateRangeUtils
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal

private enum class CategoryViewType { CHART, LIST }

/** The Transactions slice an Analytics row drills into: its group plus the view's filters. */
data class TransactionsDrillDown(
    val category: String? = null,
    val merchant: String? = null,
    val account: String? = null,
    val tag: String? = null,
    /** A [TransactionTypeFilter] name; null (all types) for Spending, which Transactions can't show. */
    val type: String? = null,
    val period: String? = null,
    val currency: String? = null,
    val startDateEpochDay: Long? = null,
    val endDateEpochDay: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: (TransactionsDrillDown) -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactionTypeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    val defaultTypeFilter by viewModel.defaultTypeFilter.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()
    val budgetCycleStartDay by viewModel.budgetCycleStartDay.collectAsStateWithLifecycle()
    val isUnifiedMode by viewModel.isUnifiedMode.collectAsStateWithLifecycle()
    val chartType by viewModel.selectedChartType.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val selectedProfileId by viewModel.selectedProfileId.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val accountFilter by viewModel.accountFilter.collectAsStateWithLifecycle()
    val accountOptions by viewModel.accountOptions.collectAsStateWithLifecycle()
    val isProEntitled by viewModel.isProEntitled.collectAsStateWithLifecycle()
    val groupBy by viewModel.groupBy.collectAsStateWithLifecycle()
    var showUpgradeSheet by remember { mutableStateOf(false) }
    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }
    var categoryViewType by rememberSaveable { mutableStateOf(CategoryViewType.CHART) }
    var tagViewType by rememberSaveable { mutableStateOf(CategoryViewType.CHART) }
    var showChartTypeSelector by remember { mutableStateOf(false) }
    // The profile / type / currency / category / account chips fold away behind
    // "More Filters"; the period chips above it are always visible.
    var showMoreFilters by rememberSaveable { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showCurrencyMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showGroupByMenu by remember { mutableStateOf(false) }

    // Remember scroll position across navigation
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // Cache expensive operations
    val customRangeLabel = remember(customDateRange) {
        DateRangeUtils.formatDateRange(customDateRange)
    }
    // Carry the custom range through drill-down navigation so the Transactions
    // screen (and its CSV export) shows exactly the slice being viewed here.
    val navStartEpochDay = if (selectedPeriod == TimePeriod.CUSTOM) customDateRange?.first?.toEpochDay() else null
    val navEndEpochDay = if (selectedPeriod == TimePeriod.CUSTOM) customDateRange?.second?.toEpochDay() else null
    // Drill into Transactions for one group, keeping this view's period, currency,
    // type and category filter so the list matches the figure that was tapped.
    val drillDown: (TransactionsDrillDown) -> Unit = { group ->
        onNavigateToTransactions(
            group.copy(
                category = group.category ?: categoryFilter,
                type = transactionTypeFilter.takeIf { it.inTransactionsList }?.name,
                period = selectedPeriod.name,
                currency = selectedCurrency,
                startDateEpochDay = navStartEpochDay,
                endDateEpochDay = navEndEpochDay
            )
        )
    }
    val primaryVisibleCurrency = availableCurrencies.firstOrNull() ?: selectedCurrency
    val hasCurrencyFilter = !isUnifiedMode &&
            availableCurrencies.size > 1 &&
            selectedCurrency.isNotBlank() &&
            !selectedCurrency.equals(primaryVisibleCurrency, ignoreCase = true)
    val hasActiveAnalyticsFilter = selectedPeriod != TimePeriod.THIS_MONTH ||
            customDateRange != null ||
            transactionTypeFilter != defaultTypeFilter ||
            categoryFilter != null ||
            accountFilter != null ||
            hasCurrencyFilter
    // What the "More Filters" row counts: everything except the period, which
    // has its own always-visible chips.
    val moreFilterCount = listOf(
        selectedProfileId != null,
        transactionTypeFilter != defaultTypeFilter,
        hasCurrencyFilter,
        categoryFilter != null,
        accountFilter != null,
    ).count { it }

    // Scroll behaviors for collapsible TopAppBar
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.analytics_title),
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            AnalyticsOptionsFab(
                onSearch = {
                    onNavigateToTransactions(TransactionsDrillDown())
                },
                onAskAi = onNavigateToChat,
                modifier = Modifier.padding(bottom = Dimensions.Component.bottomBarHeight),
            )
        }
    ) { paddingValues ->
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .hazeSource(hazeState)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            // No side padding on the list itself: the period chips bleed to the
            // screen edge, so every other item carries its own gutter.
            top = paddingValues.calculateTopPadding() + Spacing.md,
            // The floating nav bar sits above the system navigation bar, so the
            // last section must clear both, plus a margin, to scroll into view.
            // Extra room for the Search / Ask AI action so it never covers the last row.
            bottom = navBarInset + Dimensions.Component.bottomBarHeight + Dimensions.Component.fabScrollClearance
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        flingBehavior = rememberOverscrollFlingBehavior { listState }
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AnalyticsPeriodChips(
                    selectedPeriod = selectedPeriod,
                    customDateRangeSelected = customDateRange != null,
                    customRangeLabel = customRangeLabel,
                    budgetCycleStartDay = budgetCycleStartDay,
                    onPeriodSelected = { period ->
                        if (period == TimePeriod.CUSTOM) {
                            // Keep the current period until the user confirms dates.
                            showDateRangePicker = true
                        } else {
                            viewModel.selectPeriod(period)
                            // Drop a leftover custom range so it no longer counts
                            // as an active filter once a preset is chosen again.
                            if (customDateRange != null) viewModel.clearCustomDateRange()
                        }
                    }
                )
                AnalyticsMoreFilters(
                    expanded = showMoreFilters,
                    activeCount = moreFilterCount,
                    onToggle = { showMoreFilters = !showMoreFilters }
                ) {
                    AnalyticsFilterBar(
                        transactionTypeFilter = transactionTypeFilter,
                        defaultTypeFilter = defaultTypeFilter,
                        selectedCurrency = selectedCurrency,
                        availableCurrencies = availableCurrencies,
                        isUnifiedMode = isUnifiedMode,
                        categoryFilter = categoryFilter,
                        availableCategories = uiState.availableCategories,
                        profiles = profiles,
                        selectedProfileId = selectedProfileId,
                        accountOptions = accountOptions,
                        accountFilter = accountFilter,
                        showTypeMenu = showTypeMenu,
                        showCurrencyMenu = showCurrencyMenu,
                        showCategoryMenu = showCategoryMenu,
                        showProfileMenu = showProfileMenu,
                        showAccountMenu = showAccountMenu,
                        hasActiveFilter = hasActiveAnalyticsFilter,
                        onTypeClick = { showTypeMenu = true },
                        onTypeDismiss = { showTypeMenu = false },
                        onTypeSelected = { typeFilter ->
                            viewModel.setTransactionTypeFilter(typeFilter)
                            showTypeMenu = false
                        },
                        onCurrencyClick = { showCurrencyMenu = true },
                        onCurrencyDismiss = { showCurrencyMenu = false },
                        onCurrencySelected = { currency ->
                            viewModel.selectCurrency(currency)
                            showCurrencyMenu = false
                        },
                        onCategoryClick = { showCategoryMenu = true },
                        onCategoryDismiss = { showCategoryMenu = false },
                        onCategorySelected = { category ->
                            if (category == null) {
                                viewModel.clearCategoryFilter()
                            } else {
                                viewModel.setCategoryFilter(category)
                            }
                            showCategoryMenu = false
                        },
                        onProfileClick = { showProfileMenu = true },
                        onProfileDismiss = { showProfileMenu = false },
                        onProfileSelected = { profileId ->
                            viewModel.selectProfile(profileId)
                            showProfileMenu = false
                        },
                        onAccountClick = { showAccountMenu = true },
                        onAccountDismiss = { showAccountMenu = false },
                        onAccountSelected = { accountKey ->
                            viewModel.setAccountFilter(accountKey)
                            showAccountMenu = false
                        },
                        onResetFilters = {
                            viewModel.selectPeriod(TimePeriod.THIS_MONTH)
                            if (customDateRange != null) viewModel.clearCustomDateRange()
                            viewModel.resetTransactionTypeFilter()
                            viewModel.clearCategoryFilter()
                            viewModel.setAccountFilter(null)
                            if (!isUnifiedMode && primaryVisibleCurrency.isNotBlank()) {
                                viewModel.selectCurrency(primaryVisibleCurrency)
                            }
                            showTypeMenu = false
                            showCurrencyMenu = false
                            showCategoryMenu = false
                            showAccountMenu = false
                        }
                    )
                }
            }
        }

        // Analytics Summary Card
        if (uiState.totalSpending > BigDecimal.ZERO || uiState.transactionCount > 0) {
            item {
                AnalyticsSummaryCard(
                    totalAmount = uiState.totalSpending,
                    transactionCount = uiState.transactionCount,
                    averageAmount = uiState.averageAmount,
                    topCategory = uiState.topCategory,
                    topCategoryPercentage = uiState.topCategoryPercentage,
                    currency = uiState.currency,
                    isLoading = uiState.isLoading,
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                )
            }
        }

        // Chart Section with Type Selector
        if (uiState.spendingTrend.size >= 2) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    AnalyticsSectionHeader(
                        title = stringResource(R.string.analytics_trends),
                        action = {
                            AnalyticsChartTypePill(
                                chartType = chartType,
                                onClick = { showChartTypeSelector = !showChartTypeSelector }
                            )
                        }
                    )

                    // Expandable chart type selector card
                    AnimatedVisibility(visible = showChartTypeSelector) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.sm),
                            shape = MaterialTheme.shapes.large,
                            contentPadding = Dimensions.Padding.none
                        ) {
                            ChartType.entries.forEach { type ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            viewModel.setChartType(type)
                                            showChartTypeSelector = false
                                        }
                                        .padding(horizontal = Spacing.md, vertical = Dimensions.Padding.listRowVertical),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = type.icon(),
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.inline),
                                            tint = if (chartType == type)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = type.fullLabel(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (chartType == type)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (chartType == type) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(Dimensions.Icon.medium)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnalyticsChartModeCard(
                        chartType = chartType,
                        selectedCurrency = selectedCurrency,
                        data = uiState.spendingTrend,
                    )
                }
            }
        }

        // Group by (#422): a view choice rather than a filter, so it sits above the
        // breakdown it switches (not behind More Filters) and Clear leaves it alone.
        if (uiState.transactionCount > 0) {
            item(key = "group-by") {
                AnalyticsGroupByRow(
                    groupBy = groupBy,
                    isProEntitled = isProEntitled,
                    expanded = showGroupByMenu,
                    onClick = { showGroupByMenu = true },
                    onDismiss = { showGroupByMenu = false },
                    onSelected = { option ->
                        showGroupByMenu = false
                        // A Pro grouping without Pro opens the upgrade sheet; the view stays put.
                        if (option.allowedFor(isProEntitled) == option) {
                            viewModel.setGroupBy(option)
                        } else {
                            showUpgradeSheet = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                )
            }
        }

        // Category Breakdown Section with Pie/List toggle
        if (groupBy == AnalyticsGroupBy.CATEGORY && uiState.categoryBreakdown.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    AnalyticsSectionHeader(
                        title = stringResource(R.string.analytics_top_categories),
                        action = {
                            IconButton(onClick = {
                                categoryViewType = if (categoryViewType == CategoryViewType.CHART) {
                                    CategoryViewType.LIST
                                } else {
                                    CategoryViewType.CHART
                                }
                            }) {
                                Icon(
                                    imageVector = if (categoryViewType == CategoryViewType.CHART)
                                        Iconax.Menu
                                    else Iconax.Status,
                                    contentDescription = stringResource(R.string.analytics_toggle_view),
                                    modifier = Modifier.size(Dimensions.Icon.medium),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    // Animated content swap
                    AnimatedContent(
                        targetState = categoryViewType,
                        transitionSpec = {
                            if (targetState == CategoryViewType.CHART) {
                                (slideInHorizontally { -it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { it } + fadeOut()) using
                                    SizeTransform(clip = false)
                            } else {
                                (slideInHorizontally { it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it } + fadeOut()) using
                                    SizeTransform(clip = false)
                            }
                        },
                        label = "category_view_transition"
                    ) { viewType ->
                        when (viewType) {
                            // Carded like the trend chart and Cashiro's donut, so the
                            // donut no longer floats on the bare background.
                            CategoryViewType.CHART -> AnalyticsDonutCard {
                                CategoryPieChart(
                                    categories = uiState.categoryBreakdown,
                                    currency = selectedCurrency,
                                    onCategoryClick = { category ->
                                        drillDown(TransactionsDrillDown(category = category.name))
                                    }
                                )
                            }
                            CategoryViewType.LIST -> CategoryBreakdownCard(
                                categories = uiState.categoryBreakdown,
                                currency = selectedCurrency,
                                onCategoryClick = { category ->
                                    drillDown(TransactionsDrillDown(category = category.name))
                                }
                            )
                        }
                    }

                    // Explain why the bars can sum above the headline total: refunds are
                    // netted in full off the total, but a category bar can't go negative (#704).
                    if (uiState.refundNettedFromTotal > BigDecimal.ZERO) {
                        Text(
                            text = stringResource(
                                R.string.analytics_refund_note,
                                CurrencyFormatter.formatCurrency(uiState.refundNettedFromTotal, selectedCurrency)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.sm)
                        )
                    }
                }
            }
        }

        // Tag Breakdown Section with Pie/List toggle
        if (groupBy == AnalyticsGroupBy.TAG && uiState.tagBreakdown.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    AnalyticsSectionHeader(
                        title = stringResource(R.string.analytics_top_tags),
                        action = {
                            IconButton(onClick = {
                                tagViewType = if (tagViewType == CategoryViewType.CHART) {
                                    CategoryViewType.LIST
                                } else {
                                    CategoryViewType.CHART
                                }
                            }) {
                                Icon(
                                    imageVector = if (tagViewType == CategoryViewType.CHART)
                                        Iconax.Menu
                                    else Iconax.Status,
                                    contentDescription = stringResource(R.string.analytics_toggle_view),
                                    modifier = Modifier.size(Dimensions.Icon.medium),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    AnimatedContent(
                        targetState = tagViewType,
                        transitionSpec = {
                            if (targetState == CategoryViewType.CHART) {
                                (slideInHorizontally { -it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { it } + fadeOut()) using
                                    SizeTransform(clip = false)
                            } else {
                                (slideInHorizontally { it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it } + fadeOut()) using
                                    SizeTransform(clip = false)
                            }
                        },
                        label = "tag_view_transition"
                    ) { viewType ->
                        when (viewType) {
                            CategoryViewType.CHART -> AnalyticsDonutCard {
                                TagPieChart(
                                    tags = uiState.tagBreakdown,
                                    currency = selectedCurrency,
                                    onTagClick = { tag ->
                                        if (!tag.isUntagged) drillDown(TransactionsDrillDown(tag = tag.name))
                                    }
                                )
                            }
                            CategoryViewType.LIST -> TagBreakdownCard(
                                tags = uiState.tagBreakdown,
                                currency = selectedCurrency,
                                onTagClick = { tag ->
                                    drillDown(TransactionsDrillDown(tag = tag.name))
                                }
                            )
                        }
                    }

                    if (uiState.tagsOverlap) {
                        Text(
                            text = stringResource(R.string.analytics_tag_overlap_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.sm)
                        )
                    }
                }
            }
        }

        // Day-of-week breakdown: there's no Transactions filter for a weekday, so rows don't drill in.
        if (groupBy == AnalyticsGroupBy.DAY_OF_WEEK && uiState.dayOfWeekBreakdown.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    AnalyticsSectionHeader(title = stringResource(R.string.analytics_by_day_of_week))
                    val barColor = MaterialTheme.colorScheme.primary
                    TagBreakdownCard(
                        tags = uiState.dayOfWeekBreakdown,
                        currency = selectedCurrency,
                        title = null,
                        visibleItemCount = uiState.dayOfWeekBreakdown.size,
                        barColor = { barColor },
                        onTagClick = null
                    )
                }
            }
        }

        // Top Merchants Section
        if (groupBy == AnalyticsGroupBy.MERCHANT && uiState.topMerchants.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(
                    title = stringResource(R.string.analytics_top_merchants),
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                )
            }

            // All Merchants with expandable list
            item {
                AnalyticsGlassList(
                    items = uiState.topMerchants,
                    visibleItemCount = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.Padding.content)
                ) { merchant, position ->
                    MerchantListItem(
                        merchant = merchant,
                        currency = selectedCurrency,
                        position = position,
                        amountColor = analyticsAmountColor(transactionTypeFilter),
                        onClick = {
                            drillDown(TransactionsDrillDown(merchant = merchant.name))
                        }
                    )
                }
            }
        }


        // By Account Section
        if (groupBy == AnalyticsGroupBy.ACCOUNT && uiState.accountBreakdown.isNotEmpty()) {
            item {
                AnalyticsSectionHeader(
                    title = stringResource(R.string.analytics_by_account),
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                )
            }

            item {
                AnalyticsGlassList(
                    items = uiState.accountBreakdown,
                    visibleItemCount = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.Padding.content)
                ) { account, position ->
                    AccountBreakdownListItem(
                        account = account,
                        currency = selectedCurrency,
                        position = position,
                        amountColor = analyticsAmountColor(transactionTypeFilter),
                        onClick = { drillDown(TransactionsDrillDown(account = account.key)) },
                    )
                }
            }
        }

        // Empty state
        if (uiState.topMerchants.isEmpty() && uiState.categoryBreakdown.isEmpty() && !uiState.isLoading) {
            item {
                Box(modifier = Modifier.padding(horizontal = Dimensions.Padding.content)) {
                    EmptyAnalyticsState(onScanSmsClick = onNavigateToHome)
                }
            }
        }
    }
    }

    if (showDateRangePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startDate, endDate ->
                viewModel.setCustomDateRange(startDate, endDate)
                showDateRangePicker = false
            },
            initialStartDate = customDateRange?.first,
            initialEndDate = customDateRange?.second
        )
    }

    if (showUpgradeSheet) {
        com.pennywiseai.tracker.presentation.paywall.UpgradeSheet(
            onDismiss = { showUpgradeSheet = false }
        )
    }
}

/**
 * Gives every analytics chart mode the same visual boundary without changing
 * its data or interaction model.
 */
@Composable
internal fun AnalyticsChartModeCard(
    chartType: ChartType,
    selectedCurrency: String,
    data: List<BalancePoint>,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = Spacing.xs,
    ) {
        Crossfade(
            targetState = chartType,
            label = "chart_transition",
        ) { type ->
            when (type) {
                ChartType.LINE -> BalanceChart(
                    primaryCurrency = selectedCurrency,
                    balanceHistory = data,
                    height = 280,
                    seriesLabel = stringResource(R.string.analytics_spending_trend),
                )
                ChartType.BAR -> SpendingBarChart(
                    primaryCurrency = selectedCurrency,
                    data = data,
                    height = 220,
                )
                ChartType.HEATMAP -> SpendingHeatmap(data = data)
            }
        }
        // The trend runs to today at most (never into the future), so say where
        // it stops instead of leaving the empty right-hand side unexplained.
        data.lastOrNull()?.let { last ->
            Text(
                text = stringResource(
                    R.string.analytics_trend_through,
                    last.timestamp.format(java.time.format.DateTimeFormatter.ofPattern("d MMM")),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.sm),
            )
        }
    }
}

/** The card surface behind a category / tag donut and its legend. */
@Composable
private fun AnalyticsDonutCard(content: @Composable () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = Spacing.sm,
    ) {
        content()
    }
}

/**
 * The profile / type / currency / category / account chips (plus "Clear"),
 * shown when "More Filters" is expanded. The period has its own chip row above.
 */
@Composable
private fun AnalyticsFilterBar(
    transactionTypeFilter: TransactionTypeFilter,
    defaultTypeFilter: TransactionTypeFilter,
    selectedCurrency: String,
    availableCurrencies: List<String>,
    isUnifiedMode: Boolean,
    categoryFilter: String?,
    availableCategories: List<String>,
    profiles: List<ProfileEntity>,
    selectedProfileId: Long?,
    accountOptions: List<com.pennywiseai.tracker.presentation.common.AccountOption>,
    accountFilter: String?,
    showTypeMenu: Boolean,
    showCurrencyMenu: Boolean,
    showCategoryMenu: Boolean,
    showProfileMenu: Boolean,
    showAccountMenu: Boolean,
    hasActiveFilter: Boolean,
    onTypeClick: () -> Unit,
    onTypeDismiss: () -> Unit,
    onTypeSelected: (TransactionTypeFilter) -> Unit,
    onCurrencyClick: () -> Unit,
    onCurrencyDismiss: () -> Unit,
    onCurrencySelected: (String) -> Unit,
    onCategoryClick: () -> Unit,
    onCategoryDismiss: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onProfileClick: () -> Unit,
    onProfileDismiss: () -> Unit,
    onProfileSelected: (Long?) -> Unit,
    onAccountClick: () -> Unit,
    onAccountDismiss: () -> Unit,
    onAccountSelected: (String?) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        // Bleeds to the screen edge like the period chips above it.
        contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (hasActiveFilter) {
            item {
                AssistChip(
                    onClick = onResetFilters,
                    label = { Text(stringResource(R.string.analytics_filter_clear)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.Icon.small)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.error,
                        leadingIconContentColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }

        if (profiles.isNotEmpty()) {
            item {
                val selectedProfileLabel = profiles.find { it.id == selectedProfileId }?.name
                Box {
                    ExpressiveFilterChip(
                        colors = analyticsFilterChipColors(),
                        border = analyticsFilterChipBorder(selected = selectedProfileId != null),
                        selected = selectedProfileId != null,
                        text = selectedProfileLabel ?: stringResource(R.string.analytics_filter_all_profiles),
                        icon = profileFilterIcon(profiles, selectedProfileId),
                        onClick = onProfileClick
                    )

                    ProfileFilterDropdown(
                        expanded = showProfileMenu,
                        profiles = profiles,
                        selectedProfileId = selectedProfileId,
                        onProfileSelected = onProfileSelected,
                        onDismiss = onProfileDismiss
                    )
                }
            }
        }

        item {
            Box {
                ExpressiveFilterChip(
                    colors = analyticsFilterChipColors(),
                    border = analyticsFilterChipBorder(
                        selected = transactionTypeFilter != defaultTypeFilter
                    ),
                    selected = transactionTypeFilter != defaultTypeFilter,
                    text = transactionTypeFilter.shortLabel(),
                    icon = transactionTypeFilter.filterIcon(),
                    onClick = onTypeClick
                )

                DropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = onTypeDismiss,
                    shape = MaterialTheme.shapes.large
                ) {
                    TransactionTypeFilter.values().forEach { typeFilter ->
                        DropdownMenuItem(
                            text = { Text(typeFilter.label) },
                            leadingIcon = {
                                if (transactionTypeFilter == typeFilter) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                } else {
                                    Icon(typeFilter.filterIcon(), contentDescription = null)
                                }
                            },
                            onClick = { onTypeSelected(typeFilter) }
                        )
                    }
                }
            }
        }

        if (availableCurrencies.size > 1 && !isUnifiedMode) {
            item {
                Box {
                    ExpressiveFilterChip(
                        colors = analyticsFilterChipColors(),
                        border = analyticsFilterChipBorder(
                            selected = selectedCurrency != availableCurrencies.firstOrNull()
                        ),
                        selected = selectedCurrency != availableCurrencies.firstOrNull(),
                        text = selectedCurrency.ifBlank { stringResource(R.string.analytics_filter_currency) },
                        icon = Icons.Default.CurrencyExchange,
                        onClick = onCurrencyClick
                    )

                    DropdownMenu(
                        expanded = showCurrencyMenu,
                        onDismissRequest = onCurrencyDismiss,
                        shape = MaterialTheme.shapes.large
                    ) {
                        availableCurrencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                leadingIcon = {
                                    if (selectedCurrency == currency) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                onClick = { onCurrencySelected(currency) }
                            )
                        }
                    }
                }
            }
        }

        if (availableCategories.isNotEmpty() || categoryFilter != null) {
            item {
                Box {
                    ExpressiveFilterChip(
                        colors = analyticsFilterChipColors(),
                        border = analyticsFilterChipBorder(selected = categoryFilter != null),
                        selected = categoryFilter != null,
                        text = categoryFilter ?: stringResource(R.string.analytics_filter_category),
                        icon = Icons.Default.Category,
                        onClick = onCategoryClick
                    )

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = onCategoryDismiss,
                        shape = MaterialTheme.shapes.large
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.analytics_filter_all_categories)) },
                            leadingIcon = {
                                if (categoryFilter == null) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                } else {
                                    Icon(Icons.Default.Category, contentDescription = null)
                                }
                            },
                            onClick = { onCategorySelected(null) }
                        )

                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        category,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    if (categoryFilter == category) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    } else {
                                        CategoryIcon(
                                            category = category,
                                            size = Dimensions.Icon.small
                                        )
                                    }
                                },
                                onClick = { onCategorySelected(category) }
                            )
                        }
                    }
                }
            }
        }

        if (accountOptions.isNotEmpty()) {
            item {
                val selectedAccountLabel =
                    accountOptions.firstOrNull { it.key == accountFilter }?.label
                Box {
                    ExpressiveFilterChip(
                        colors = analyticsFilterChipColors(),
                        border = analyticsFilterChipBorder(selected = accountFilter != null),
                        selected = accountFilter != null,
                        text = selectedAccountLabel ?: stringResource(R.string.analytics_filter_account),
                        icon = Icons.Default.AccountBalanceWallet,
                        onClick = onAccountClick
                    )

                    DropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = onAccountDismiss,
                        shape = MaterialTheme.shapes.large
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.analytics_filter_all_accounts)) },
                            leadingIcon = {
                                if (accountFilter == null) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                } else {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                                }
                            },
                            onClick = { onAccountSelected(null) }
                        )
                        accountOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    if (accountFilter == option.key) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    } else {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                                    }
                                },
                                onClick = { onAccountSelected(option.key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun analyticsFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    labelColor = MaterialTheme.colorScheme.onSurface,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
    selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
)

@Composable
private fun analyticsFilterChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    selected = selected,
    enabled = true,
    borderWidth = Dimensions.Elevation.none,
    selectedBorderWidth = Dimensions.Elevation.none
)

@Composable
private fun CategoryListItem(
    category: CategoryData,
    currency: String
) {
    val categoryInfo = CategoryMapping.categories[category.name]
        ?: CategoryMapping.categories["Others"]!!

    ListItemCardV2(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(Dimensions.Icon.avatar)
                    .clip(CircleShape)
                    .background(CategoryMapping.colorFor(category.name).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    category = category.name,
                    size = Dimensions.Icon.medium,
                    tint = CategoryMapping.colorFor(category.name)
                )
            }
        },
        title = category.name,
        subtitle = pluralStringResource(
            R.plurals.analytics_transaction_count,
            category.transactionCount,
            category.transactionCount
        ),
        amount = CurrencyFormatter.formatCurrency(category.amount, currency),
        trailingContent = {
            Text(
                text = stringResource(R.string.analytics_percent, category.percentage.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun MerchantListItem(
    merchant: MerchantData,
    currency: String,
    position: ListItemPosition,
    amountColor: Color,
    onClick: () -> Unit = {}
) {
    val subtitle = pluralStringResource(
        if (merchant.isSubscription) R.plurals.analytics_transaction_count_subscription
        else R.plurals.analytics_transaction_count,
        merchant.transactionCount,
        merchant.transactionCount
    )

    // Brand icon stays keyed on the raw merchant; only the label uses the alias (#583).
    val merchantDisplay = LocalMerchantDisplay.current

    AnalyticsGlassRow(
        position = position,
        leadingContent = {
            BrandIcon(
                merchantName = merchant.name,
                size = Dimensions.Icon.avatarLarge,
                showBackground = true
            )
        },
        title = merchantDisplay(merchant.name) ?: merchant.name,
        subtitle = subtitle,
        amount = CurrencyFormatter.formatCurrency(merchant.amount, currency),
        amountColor = amountColor,
        onClick = onClick
    )
}

@Composable
private fun AccountBreakdownListItem(
    account: AccountBreakdownData,
    currency: String,
    position: ListItemPosition,
    amountColor: Color,
    onClick: () -> Unit
) {
    AnalyticsGlassRow(
        position = position,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(Dimensions.Icon.avatar)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            }
        },
        title = account.label,
        subtitle = pluralStringResource(
            R.plurals.analytics_transaction_count,
            account.transactionCount,
            account.transactionCount
        ),
        amount = CurrencyFormatter.formatCurrency(account.amount, currency),
        amountColor = amountColor,
        supporting = stringResource(R.string.analytics_percent, account.percentage.toInt()),
        onClick = onClick
    )
}

@Composable
private fun EmptyAnalyticsState(
    onScanSmsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.Padding.content),
        contentAlignment = Alignment.Center
    ) {
        PennyWiseEmptyState(
            icon = Icons.AutoMirrored.Filled.ShowChart,
            headline = stringResource(R.string.analytics_empty_headline),
            description = stringResource(R.string.analytics_empty_description),
            actionLabel = stringResource(R.string.analytics_empty_action),
            onAction = onScanSmsClick
        )
    }
}

/**
 * Cashiro's Analytics action: a round "more" button that opens a small menu
 * with Search (the transaction list) and Ask AI (the Chat tab).
 */
@Composable
private fun AnalyticsOptionsFab(
    onSearch: () -> Unit,
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { expanded = true },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = stringResource(R.string.analytics_fab_options),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.analytics_fab_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSearch()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.analytics_fab_ask_ai)) },
                leadingIcon = { Icon(Iconax.AiCommentary, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAskAi()
                },
            )
        }
    }
}

/** "Group by" chip with its menu (#422), in the analytics filter-chip style. */
@Composable
private fun AnalyticsGroupByRow(
    groupBy: AnalyticsGroupBy,
    isProEntitled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onSelected: (AnalyticsGroupBy) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box {
            ExpressiveFilterChip(
                colors = analyticsFilterChipColors(),
                border = analyticsFilterChipBorder(selected = groupBy != AnalyticsGroupBy.CATEGORY),
                selected = groupBy != AnalyticsGroupBy.CATEGORY,
                text = groupBy.label(),
                icon = groupBy.icon(),
                onClick = onClick
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismiss,
                shape = MaterialTheme.shapes.large
            ) {
                AnalyticsGroupBy.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label()) },
                        leadingIcon = {
                            Icon(
                                if (groupBy == option) Icons.Default.Check else option.icon(),
                                contentDescription = null
                            )
                        },
                        // Subtle Pro hint for free users; picking it opens the upgrade sheet.
                        trailingIcon = if (option.isPro && !isProEntitled) {
                            {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.analytics_group_by_pro),
                                    tint = yellow_dark,
                                    modifier = Modifier.size(Dimensions.Icon.small)
                                )
                            }
                        } else null,
                        onClick = { onSelected(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsGroupBy.label(): String = stringResource(
    when (this) {
        AnalyticsGroupBy.CATEGORY -> R.string.analytics_group_by_category
        AnalyticsGroupBy.MERCHANT -> R.string.analytics_group_by_merchant
        AnalyticsGroupBy.ACCOUNT -> R.string.analytics_group_by_account
        AnalyticsGroupBy.TAG -> R.string.analytics_group_by_tag
        AnalyticsGroupBy.DAY_OF_WEEK -> R.string.analytics_group_by_day_of_week
    }
)

private fun AnalyticsGroupBy.icon() = when (this) {
    AnalyticsGroupBy.CATEGORY -> Icons.Default.Category
    AnalyticsGroupBy.MERCHANT -> Icons.Default.Store
    AnalyticsGroupBy.ACCOUNT -> Icons.Default.AccountBalanceWallet
    AnalyticsGroupBy.TAG -> Icons.Default.Sell
    AnalyticsGroupBy.DAY_OF_WEEK -> Icons.Default.DateRange
}
