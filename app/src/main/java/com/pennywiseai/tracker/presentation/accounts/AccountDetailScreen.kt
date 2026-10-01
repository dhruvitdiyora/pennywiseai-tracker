package com.pennywiseai.tracker.presentation.accounts

import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.presentation.transactions.TransactionTotalsCard
import com.pennywiseai.tracker.ui.components.*
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.TransactionItem
import com.pennywiseai.tracker.ui.components.skeleton.TransactionItemSkeleton
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    // The user's own name for the account wins; the masked number lives on the
    // balance card, so the title does not need to repeat it.
    val accountAlias = uiState.currentBalance?.alias?.trim()?.takeIf { it.isNotEmpty() }
    val screenTitle = accountAlias ?: uiState.bankName

    val gutter = Dimensions.Padding.content
    // Sections sit `Spacing.md` apart while transaction rows keep the tiny
    // grouped-list gap, so the list's own spacing is the small one and every
    // non-row item adds the difference below itself.
    val sectionGap = Spacing.md - Spacing.Layout.groupedListGap

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = screenTitle,
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.accounts_back),
                    )
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        val lazyListState = rememberLazyListState()
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical(),
            // Horizontal inset is applied per item so the period chips can
            // scroll all the way to the screen edge.
            contentPadding = PaddingValues(
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + Spacing.Layout.scrollBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            // Balance card: tiled bank logo, balance, bank name + masked number
            item {
                AccountBalanceCard(
                    balance = uiState.currentBalance?.balance ?: BigDecimal.ZERO,
                    creditLimit = uiState.currentBalance?.creditLimit,
                    bankName = uiState.bankName,
                    alias = accountAlias,
                    accountLast4 = uiState.accountLast4,
                    primaryCurrency = uiState.primaryCurrency,
                    billedOutstanding = uiState.billedOutstanding,
                    unbilledOutstanding = uiState.unbilledOutstanding,
                    modifier = Modifier
                        .padding(horizontal = gutter)
                        .padding(bottom = sectionGap)
                )
            }

            // Date Range Filter
            item {
                DateRangeFilter(
                    selectedRange = selectedDateRange,
                    onRangeSelected = viewModel::selectDateRange,
                    modifier = Modifier.padding(bottom = sectionGap)
                )
            }

            // Balance Chart (Expandable) - Updates based on selected timeframe
            if (uiState.balanceChartData.isNotEmpty()) {
                item {
                    ExpandableBalanceChart(
                        primaryCurrency = uiState.primaryCurrency,
                        balanceHistory = uiState.balanceChartData,
                        selectedTimeframe = stringResource(selectedDateRange.labelRes),
                        modifier = Modifier
                            .padding(horizontal = gutter)
                            .padding(bottom = sectionGap)
                    )
                }
            }

            // Summary: one currency (the account's, or the display currency in
            // unified mode); figures are "est." when they were converted.
            item {
                TransactionTotalsCard(
                    income = uiState.totalIncome,
                    expenses = uiState.totalExpenses,
                    netBalance = uiState.netBalance,
                    currency = uiState.primaryCurrency,
                    isEstimated = uiState.hasMultipleCurrencies,
                    isLoading = uiState.isLoading,
                    modifier = Modifier
                        .padding(horizontal = gutter)
                        .padding(bottom = sectionGap)
                )
            }

            // Transactions Header
            item {
                SectionHeaderV2(
                    title = stringResource(R.string.account_detail_transactions_header, uiState.transactions.size),
                    modifier = Modifier
                        .padding(horizontal = gutter + Spacing.sm)
                        .padding(bottom = Spacing.sm)
                )
            }

            // Transaction List
            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                item {
                    PennyWiseEmptyState(
                        icon = Icons.Outlined.Receipt,
                        headline = stringResource(R.string.account_detail_empty_title),
                        description = stringResource(R.string.account_detail_empty_description),
                        modifier = Modifier.padding(horizontal = gutter)
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.transactions,
                    key = { _, transaction -> transaction.id }
                ) { index, transaction ->
                    TransactionItem(
                        transaction = transaction,
                        listItemPosition = ListItemPosition.from(index, uiState.transactions.size),
                        onClick = { onTransactionClick(transaction.id) },
                        modifier = Modifier.padding(horizontal = gutter)
                    )
                }
            }

            // Loading State
            if (uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = gutter),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        repeat(5) {
                            TransactionItemSkeleton()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableBalanceChart(
    primaryCurrency: String,
    balanceHistory: List<BalancePoint>,
    selectedTimeframe: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    PennyWiseCardV2(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = { isExpanded = !isExpanded },
        contentPadding = Dimensions.Padding.content
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimensions.Icon.inline)
                    )
                    Text(
                        text = stringResource(R.string.account_detail_balance_trend),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = selectedTimeframe,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Line the subtitle up with the title, past the leading icon.
                    modifier = Modifier.padding(start = Dimensions.Icon.inline + Spacing.sm)
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.accounts_collapse) else stringResource(R.string.accounts_expand),
                modifier = Modifier
                    .size(Dimensions.Icon.medium)
                    .rotate(if (isExpanded) 180f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(Spacing.md))
                BalanceChart(
                    primaryCurrency = primaryCurrency,
                    balanceHistory = balanceHistory,
                    height = 180
                )
            }
        }
    }
}

/**
 * The account's identity card: the bank's logo tiled faintly behind the
 * balance, with the bank (or the user's alias) and a masked number plus the
 * logo along the bottom edge. Credit cards swap the headline for available
 * credit and keep their outstanding / billed / unbilled breakdown.
 *
 * The headline is formatted in [primaryCurrency], the account's own currency
 * (or the display currency in unified mode), so no amounts are ever summed
 * across currencies here.
 */
@Composable
private fun AccountBalanceCard(
    balance: BigDecimal,
    creditLimit: BigDecimal?,
    bankName: String,
    alias: String?,
    accountLast4: String,
    primaryCurrency: String,
    modifier: Modifier = Modifier,
    billedOutstanding: BigDecimal? = null,
    unbilledOutstanding: BigDecimal? = null
) {
    val isCreditCard = creditLimit != null
    val headlineLabel = if (isCreditCard) {
        stringResource(R.string.account_detail_available_credit)
    } else {
        stringResource(R.string.account_detail_current_balance)
    }
    val headlineAmount = creditLimit ?: balance

    val cardColor = MaterialTheme.colorScheme.surfaceContainer
    val stripColor = MaterialTheme.colorScheme.surfaceContainerLow

    // Wallets have no number to mask. With an alias on top, the bank name moves
    // to the supporting line so the card still says which bank it is.
    val maskedNumber = accountLast4
        .takeIf { it.isNotBlank() && it != AccountBalanceEntity.WALLET_ACCOUNT_MARKER }
        ?.let { "•••• •••• •••• $it" }
    val supportingLine = listOfNotNull(bankName.takeIf { alias != null }, maskedNumber)
        .joinToString(" · ")
        .ifBlank { null }

    PennyWiseCardV2(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = cardColor,
        // Full-bleed layers: the watermark and the bottom strip reach the edge.
        contentPadding = Dimensions.Padding.none
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            TiledIconBackground(
                merchantName = bankName,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = headlineLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md)
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(headlineAmount, primaryCurrency),
                    style = PennyWiseText.heroAmount,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )

                // Credit cards: what is owed, and how much of it is billed.
                if (isCreditCard && balance > BigDecimal.ZERO) {
                    Column(
                        modifier = Modifier.padding(horizontal = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.account_detail_outstanding,
                                CurrencyFormatter.formatCurrency(balance, primaryCurrency)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (billedOutstanding != null && unbilledOutstanding != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
                            ) {
                                Column {
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(billedOutstanding, primaryCurrency),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = stringResource(R.string.account_detail_billed),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column {
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(unbilledOutstanding, primaryCurrency),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = stringResource(R.string.account_detail_unbilled),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Identity strip: fades the watermark out under the text.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, stripColor, stripColor)
                            )
                        )
                        .padding(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alias ?: bankName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            supportingLine?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        BrandIcon(
                            merchantName = bankName,
                            size = Dimensions.Icon.avatarLarge,
                            showBackground = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateRangeFilter(
    selectedRange: DateRange,
    onRangeSelected: (DateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(horizontal = Dimensions.Padding.content)
    ) {
        items(DateRange.entries.toList(), key = { it.name }) { range ->
            PeriodFilterChip(
                selected = selectedRange == range,
                label = stringResource(range.labelRes),
                onClick = { onRangeSelected(range) }
            )
        }
    }
}
