package com.pennywiseai.tracker.presentation.budgetgroups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.BudgetGroupType
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.repository.BudgetGroupSpending
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.DashedLine
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.BudgetCardCaption
import com.pennywiseai.tracker.ui.components.cards.BudgetCardTitle
import com.pennywiseai.tracker.ui.components.cards.BudgetHeroFigures
import com.pennywiseai.tracker.ui.components.cards.BudgetProgressTrack
import com.pennywiseai.tracker.ui.components.cards.budgetBarColor
import com.pennywiseai.tracker.ui.components.cards.budgetColorWash
import com.pennywiseai.tracker.ui.components.cards.budgetRenewalText
import com.pennywiseai.tracker.ui.components.cards.budgetStatusColor
import androidx.compose.foundation.layout.heightIn
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.TransactionItem
import com.pennywiseai.tracker.ui.components.toColorOr
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.pennywiseai.tracker.ui.icons.iconax.History as IconaxHistory

/**
 * One budget group for the selected month: a large collapsing title, the
 * Cashiro-style hero card, the window dates, the category donut and the
 * transactions that fed the total.
 *
 * [onNavigateToHistory], when supplied, adds a history button to the hero
 * card; it receives the group id and the (year, month) this screen shows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    groupId: Long,
    year: Int,
    month: Int,
    viewModel: BudgetGroupsViewModel,
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onNavigateToHistory: ((groupId: Long, year: Int, month: Int) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    LaunchedEffect(year, month) {
        viewModel.selectYearMonth(year, month)
    }

    val isRequestedPeriod = uiState.selectedYear == year && uiState.selectedMonth == month
    val groupSpending = uiState.summary?.groups?.firstOrNull {
        it.group.budget.id == groupId
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = groupSpending?.group?.budget?.name
                    ?: stringResource(R.string.budget_detail_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.budget_navigate_back),
                    )
                },
                actionContent = {
                    if (groupSpending != null) {
                        BudgetTonalActionButton(
                            onClick = { onEdit(groupId) },
                            icon = Iconax.Edit2,
                            contentDescription = stringResource(R.string.budget_detail_edit),
                        )
                    }
                },
                hazeState = hazeState,
            )
        },
    ) { paddingValues ->
        val topPadding = paddingValues.calculateTopPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                uiState.isLoading || !isRequestedPeriod -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                groupSpending == null -> PennyWiseEmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    headline = stringResource(R.string.budget_detail_missing_title),
                    description = stringResource(R.string.budget_detail_missing_description),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = topPadding),
                )

                else -> BudgetDetailContent(
                    groupSpending = groupSpending,
                    currency = uiState.currency,
                    unifiedMode = uiState.isUnifiedMode,
                    onTransactionClick = onTransactionClick,
                    topPadding = topPadding,
                    onViewHistory = onNavigateToHistory?.let { navigate ->
                        { navigate(groupId, year, month) }
                    },
                )
            }
        }
    }
}

@Composable
internal fun BudgetDetailContent(
    groupSpending: BudgetGroupSpending,
    currency: String,
    unifiedMode: Boolean,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    /** Space the large top bar occupies above the list; the list scrolls under it. */
    topPadding: Dp = 0.dp,
    /** Shows a history button in the hero card when non-null. */
    onViewHistory: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val transactions = groupSpending.matchingTransactions

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .overScrollVertical(),
        contentPadding = PaddingValues(
            start = Dimensions.Padding.content,
            end = Dimensions.Padding.content,
            top = Dimensions.Padding.content + topPadding,
            bottom = Dimensions.Component.bottomBarHeight + Spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
        flingBehavior = rememberOverscrollFlingBehavior { listState },
    ) {
        item {
            BudgetDetailSummaryCard(
                groupSpending = groupSpending,
                currency = currency,
                onViewHistory = onViewHistory,
            )
        }

        item {
            BudgetWindowDivider(groupSpending = groupSpending)
        }

        if (groupSpending.categorySpending.isNotEmpty()) {
            item {
                SectionHeaderV2(
                    title = stringResource(R.string.budget_detail_categories),
                    subtitle = stringResource(
                        R.string.budget_detail_categories_subtitle,
                        groupSpending.categorySpending.size,
                    ),
                )
            }
            item {
                BudgetCategoryBreakdownCard(
                    categories = groupSpending.categorySpending,
                    currency = currency,
                )
            }
        }

        item {
            SectionHeaderV2(
                title = stringResource(R.string.budget_detail_transactions),
                subtitle = if (transactions.isEmpty()) {
                    stringResource(R.string.budget_detail_transactions_empty_subtitle)
                } else {
                    stringResource(R.string.budget_detail_transactions_count, transactions.size)
                },
            )
        }

        if (transactions.isEmpty()) {
            item {
                GlassCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = Dimensions.Padding.empty,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.Icon.avatarLarge),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.budget_detail_no_transactions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.budget_detail_transaction_disclosure),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            itemsIndexed(
                items = transactions,
                key = { _, tx -> tx.transaction.id },
            ) { index, txWithSplits ->
                val transaction = txWithSplits.transaction
                TransactionItem(
                    transaction = transaction,
                    convertedAmount = if (unifiedMode) {
                        groupSpending.matchingTransactionDisplayAmounts[transaction.id]
                    } else {
                        null
                    },
                    displayCurrency = currency.takeIf { unifiedMode },
                    listItemPosition = ListItemPosition.from(index, transactions.size),
                    onClick = { onTransactionClick(transaction.id) },
                )
            }
        }
    }
}

/**
 * Cashiro's budget hero: the budget's colour washed over a rounded card, a
 * lightly tracked name, the daily-left / spend-over-limit pair, a progress bar
 * and a footer. PennyWise's extras (type, cadence, live/completed window and
 * percent used) ride along as small tinted chips and a percent figure.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetDetailSummaryCard(
    groupSpending: BudgetGroupSpending,
    currency: String,
    onViewHistory: (() -> Unit)?,
) {
    val budget = groupSpending.group.budget
    val scheme = MaterialTheme.colorScheme
    val pct = groupSpending.percentageUsed
    val hasLimit = groupSpending.totalBudget > BigDecimal.ZERO
    val isOver = groupSpending.remaining < BigDecimal.ZERO
    val budgetColor = budget.color.toColorOr(scheme.primary)
    val statusColor = budgetStatusColor(pct)
    val barColor = budgetBarColor(pct, budgetColor)

    val groupType = when (budget.groupType) {
        BudgetGroupType.LIMIT -> stringResource(R.string.budget_detail_type_limit)
        BudgetGroupType.TARGET -> stringResource(R.string.budget_detail_type_target)
        BudgetGroupType.EXPECTED -> stringResource(R.string.budget_detail_type_expected)
    }
    val period = when (groupSpending.periodType) {
        BudgetPeriodType.WEEKLY -> stringResource(R.string.budget_detail_period_weekly)
        BudgetPeriodType.MONTHLY -> stringResource(R.string.budget_detail_period_monthly)
        BudgetPeriodType.CUSTOM -> stringResource(R.string.budget_detail_period_custom)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        rimColor = budgetColor,
        // The wash is painted by the column below so it covers the whole card.
        contentPadding = Dimensions.Padding.none,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .budgetColorWash(budgetColor)
                .padding(horizontal = Spacing.md + Spacing.xs, vertical = Spacing.smd),
        ) {
            // Header: marker + name, then percent used and the history button.
            BudgetCardTitle(
                name = budget.name,
                markerColor = barColor,
                modifier = Modifier.heightIn(min = Dimensions.Component.minTouchTarget),
            ) {
                if (hasLimit) {
                    Text(
                        text = stringResource(R.string.budget_percent_used, pct.toInt()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = Spacing.sm),
                    )
                }
                if (onViewHistory != null) {
                    BudgetHeroIconButton(
                        onClick = onViewHistory,
                        icon = Iconax.IconaxHistory,
                        contentDescription = stringResource(R.string.budgets_view_history),
                    )
                }
            }

            // What kind of budget this is, how often it renews, and whether the
            // window is still accumulating.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                SubtitleTag(text = groupType, color = budgetColor)
                SubtitleTag(text = period, color = budgetColor)
                SubtitleTag(
                    text = if (groupSpending.displayedIsLive) {
                        stringResource(R.string.budget_detail_live)
                    } else {
                        stringResource(R.string.budget_detail_frozen)
                    },
                    color = if (groupSpending.displayedIsLive) scheme.tertiary else scheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.smd))

            if (hasLimit) {
                BudgetHeroFigures(groupSpending = groupSpending, currency = currency)

                Spacer(modifier = Modifier.height(Spacing.md))

                BudgetProgressTrack(progress = pct / 100f, color = barColor)

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Footer: when the hero shows the per-day allowance, what is
                // left of the whole budget (the hero already carries it
                // otherwise, including any overage), then when the window
                // renews while it is still live.
                val showsDaily = budget.groupType == BudgetGroupType.LIMIT &&
                    !isOver &&
                    groupSpending.dailyAllowance > BigDecimal.ZERO
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (showsDaily) Arrangement.SpaceBetween else Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    if (showsDaily) {
                        Text(
                            text = stringResource(
                                R.string.budget_amount_remaining,
                                CurrencyFormatter.formatCurrency(groupSpending.remaining, currency),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                        )
                    }
                    if (groupSpending.displayedIsLive) {
                        Text(
                            text = budgetRenewalText(groupSpending),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // A budget with no limit only tracks spending.
                BudgetCardCaption(text = stringResource(R.string.budgets_card_spent_label))
                Text(
                    text = CurrencyFormatter.formatCurrency(groupSpending.totalActual, currency),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.budget_tracking_all_expenses),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The window's dates centred between two dashed rules, as in Cashiro. */
@Composable
private fun BudgetWindowDivider(groupSpending: BudgetGroupSpending) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }
    val window = stringResource(
        R.string.budget_detail_window,
        groupSpending.windowStart.format(dateFormatter),
        groupSpending.windowEnd.format(dateFormatter),
    )
    val ruleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DashedLine(modifier = Modifier.weight(1f), color = ruleColor)
        Text(
            text = window,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        DashedLine(modifier = Modifier.weight(1f), color = ruleColor)
    }
}
