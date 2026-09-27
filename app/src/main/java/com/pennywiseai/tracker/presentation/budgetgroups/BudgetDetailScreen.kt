package com.pennywiseai.tracker.presentation.budgetgroups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.BudgetGroupType
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.repository.BudgetCategorySpending
import com.pennywiseai.tracker.data.repository.BudgetGroupSpending
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.TransactionItem
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

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
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    LaunchedEffect(year, month) {
        viewModel.selectYearMonth(year, month)
    }

    val isRequestedPeriod = uiState.selectedYear == year && uiState.selectedMonth == month
    val groupSpending = uiState.summary?.groups?.firstOrNull {
        it.group.budget.id == groupId
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehavior,
                scrollBehaviorLarge = scrollBehavior,
                title = groupSpending?.group?.budget?.name
                    ?: stringResource(R.string.budget_detail_title),
                hasBackButton = true,
                navigationContent = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(Dimensions.Component.minTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.budget_navigate_back),
                        )
                    }
                },
                actionContent = {
                    if (groupSpending != null) {
                        IconButton(
                            onClick = { onEdit(groupId) },
                            modifier = Modifier.size(Dimensions.Component.minTouchTarget),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.budget_detail_edit),
                            )
                        }
                    }
                },
                hazeState = hazeState,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading || !isRequestedPeriod -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                groupSpending == null -> PennyWiseEmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    headline = stringResource(R.string.budget_detail_missing_title),
                    description = stringResource(R.string.budget_detail_missing_description),
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> BudgetDetailContent(
                    groupSpending = groupSpending,
                    currency = uiState.currency,
                    unifiedMode = uiState.isUnifiedMode,
                    onTransactionClick = onTransactionClick,
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
            top = Dimensions.Padding.content,
            bottom = Dimensions.Component.bottomBarHeight + Spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
        flingBehavior = rememberOverscrollFlingBehavior { listState },
    ) {
        item {
            BudgetDetailSummaryCard(groupSpending = groupSpending, currency = currency)
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
            itemsIndexed(
                items = groupSpending.categorySpending,
                key = { _, category -> category.categoryName },
            ) { _, category ->
                BudgetDetailCategoryRow(category = category, currency = currency)
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
                PennyWiseCardV2 {
                    Text(
                        text = stringResource(R.string.budget_detail_no_transactions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

@Composable
private fun BudgetDetailSummaryCard(
    groupSpending: BudgetGroupSpending,
    currency: String,
) {
    val budget = groupSpending.group.budget
    val statusTextColor = when {
        groupSpending.percentageUsed >= 90f -> MaterialTheme.colorScheme.error
        groupSpending.percentageUsed >= 70f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val progressColor = if (groupSpending.percentageUsed >= 90f) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
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
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }
    val window = stringResource(
        R.string.budget_detail_window,
        groupSpending.windowStart.format(dateFormatter),
        groupSpending.windowEnd.format(dateFormatter),
    )

    PennyWiseCardV2 {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.smd),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$groupType · $period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (groupSpending.totalBudget > BigDecimal.ZERO) {
                    Text(
                        text = stringResource(
                            R.string.budget_percent_used,
                            groupSpending.percentageUsed.toInt(),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = statusTextColor,
                    )
                }
            }

            Text(
                text = CurrencyFormatter.formatCurrency(groupSpending.totalActual, currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = statusTextColor,
            )
            Text(
                text = if (groupSpending.totalBudget > BigDecimal.ZERO) {
                    stringResource(
                        R.string.budget_detail_spent_of,
                        CurrencyFormatter.formatCurrency(groupSpending.totalBudget, currency),
                    )
                } else {
                    stringResource(R.string.budget_tracking_all_expenses)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (groupSpending.totalBudget > BigDecimal.ZERO) {
                LinearProgressIndicator(
                    progress = { (groupSpending.percentageUsed / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.Component.progressBarHeight)
                        .clip(RoundedCornerShape(Dimensions.CornerRadius.full)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = Dimensions.Alpha.divider),
                    drawStopIndicator = {},
                )
                Text(
                    text = if (groupSpending.remaining < BigDecimal.ZERO) {
                        stringResource(
                            R.string.budget_amount_over,
                            CurrencyFormatter.formatCurrency(groupSpending.remaining.abs(), currency),
                        )
                    } else {
                        stringResource(
                            R.string.budget_amount_remaining,
                            CurrencyFormatter.formatCurrency(groupSpending.remaining, currency),
                        )
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusTextColor,
                )
            }

            Text(
                text = window,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (groupSpending.displayedIsLive) {
                    stringResource(R.string.budget_detail_live)
                } else {
                    stringResource(R.string.budget_detail_frozen)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BudgetDetailCategoryRow(
    category: BudgetCategorySpending,
    currency: String,
) {
    val statusTextColor = when {
        category.percentageUsed >= 90f -> MaterialTheme.colorScheme.error
        category.percentageUsed >= 70f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val progressColor = if (category.percentageUsed >= 90f) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val categoryColor = CategoryMapping.categories[category.categoryName]?.color
        ?: MaterialTheme.colorScheme.primary

    PennyWiseCardV2(contentPadding = Dimensions.Padding.cardCompact) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.Icon.list)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = Dimensions.Alpha.tonalIconContainer)),
                contentAlignment = Alignment.Center,
            ) {
                CategoryIcon(
                    category = category.categoryName,
                    size = Dimensions.Icon.inline,
                    tint = categoryColor,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = category.categoryName,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = CurrencyFormatter.formatCurrency(category.actualAmount, currency),
                        style = MaterialTheme.typography.labelLarge,
                        color = statusTextColor,
                    )
                }
                if (category.budgetAmount > BigDecimal.ZERO) {
                    LinearProgressIndicator(
                        progress = { (category.percentageUsed / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimensions.Component.progressBarHeight)
                            .clip(RoundedCornerShape(Dimensions.CornerRadius.full)),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = Dimensions.Alpha.divider),
                        drawStopIndicator = {},
                    )
                    Text(
                        text = stringResource(
                            R.string.budget_category_spent_of,
                            CurrencyFormatter.formatCurrency(category.actualAmount, currency),
                            CurrencyFormatter.formatCurrency(category.budgetAmount, currency),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
