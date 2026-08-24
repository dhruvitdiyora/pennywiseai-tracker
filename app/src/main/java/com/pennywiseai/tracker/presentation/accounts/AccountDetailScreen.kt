package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.components.*
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.skeleton.TransactionItemSkeleton
import com.pennywiseai.tracker.ui.icons.iconsax.Iconsax
import com.pennywiseai.tracker.ui.icons.iconsax.ArrowLeft02
import com.pennywiseai.tracker.ui.icons.iconsax.Card
import com.pennywiseai.tracker.ui.icons.iconsax.Graph
import com.pennywiseai.tracker.ui.icons.iconsax.ReceiptItem
import com.pennywiseai.tracker.ui.icons.iconsax.Wallet3
import com.pennywiseai.tracker.ui.icons.iconsax.WalletMoney
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.LocalNavAnimatedVisibilityScope
import com.pennywiseai.tracker.ui.LocalSharedTransitionScope
import com.pennywiseai.tracker.ui.sharedElementIcon
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = AccountBalanceEntity.accountLabel(uiState.bankName, uiState.accountLast4),
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Iconsax.ArrowLeft02, contentDescription = "Back")
                    }
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
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                bottom = 0.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            // Current Balance Card
            item {
                CurrentBalanceCard(
                    balance = uiState.currentBalance?.balance ?: BigDecimal.ZERO,
                    creditLimit = uiState.currentBalance?.creditLimit,
                    isCreditCard = uiState.currentBalance?.isCreditCard == true,
                    bankName = uiState.bankName,
                    accountLast4 = uiState.accountLast4,
                    primaryCurrency = uiState.primaryCurrency,
                    billedOutstanding = uiState.billedOutstanding,
                    unbilledOutstanding = uiState.unbilledOutstanding
                )
            }

            // Date Range Filter
            item {
                DateRangeFilter(
                    selectedRange = selectedDateRange,
                    onRangeSelected = viewModel::selectDateRange
                )
            }

            // Balance Chart (Expandable) - Updates based on selected timeframe
            if (uiState.balanceChartData.isNotEmpty()) {
                item {
                    ExpandableBalanceChart(
                        primaryCurrency = uiState.primaryCurrency,
                        balanceHistory = uiState.balanceChartData,
                        selectedTimeframe = selectedDateRange.label
                    )
                }
            }

            // Summary Statistics
            item {
                SummaryStatistics(
                    totalIncome = uiState.totalIncome,
                    totalExpenses = uiState.totalExpenses,
                    netBalance = uiState.netBalance,
                    period = selectedDateRange.label,
                    primaryCurrency = uiState.primaryCurrency,
                    hasMultipleCurrencies = uiState.hasMultipleCurrencies
                )
            }
            
            // Transactions Header
            item {
                SectionHeaderV2(
                    title = "Transactions (${uiState.transactions.size})"
                )
            }
            
            // Transaction List
            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                item {
                    PennyWiseEmptyState(
                        icon = Iconsax.ReceiptItem,
                        headline = "No transactions",
                        description = "Transactions for this account will appear here"
                    )
                }
            } else {
                items(
                    items = uiState.transactions,
                    key = { it.id }
                ) { transaction ->
                    AccountTransactionItem(
                        transaction = transaction,
                        primaryCurrency = uiState.primaryCurrency,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
            
            // Loading State
            if (uiState.isLoading) {
                item {
                    Column(
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
    selectedTimeframe: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    PennyWiseCardV2(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
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
                            imageVector = Iconsax.Graph,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimensions.Icon.medium)
                        )
                        Text(
                            text = "Balance Trend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = selectedTimeframe,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 28.dp)
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
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
}

@Composable
private fun CurrentBalanceCard(
    balance: BigDecimal,
    creditLimit: BigDecimal? = null,
    isCreditCard: Boolean,
    bankName: String,
    accountLast4: String,
    primaryCurrency: String,
    billedOutstanding: BigDecimal? = null,
    unbilledOutstanding: BigDecimal? = null
) {
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The landing point for the carousel card's icon. This screen had no
            // brand icon of its own, so there was nothing for the shared element
            // to become — the transition needs an element that genuinely exists
            // on BOTH screens, not a plausible-looking substitute.
            val sharedTransitionScope = LocalSharedTransitionScope.current
            val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
            val iconModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    sharedElementIcon(
                        key = "account_icon_${bankName}_${accountLast4}",
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            } else {
                Modifier
            }
            BrandIcon(
                merchantName = bankName,
                modifier = iconModifier,
                size = 48.dp,
                showBackground = true
            )
            Spacer(modifier = Modifier.height(Spacing.smd))

            if (isCreditCard) {
                // Credit card layout
                Text(
                    text = if (creditLimit == null) "Outstanding" else "Available Credit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = CurrencyFormatter.formatCurrency(
                        if (creditLimit == null) balance else creditLimit,
                        primaryCurrency
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // A configured limit enables the derived available-credit and
                // billed/unbilled figures. A null limit means not set up, not
                // "not a credit card"; never invent a limit-derived number.
                if (creditLimit != null && balance > BigDecimal.ZERO) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Outstanding: ${CurrencyFormatter.formatCurrency(balance, primaryCurrency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    // Show billed/unbilled split when statement day is configured
                    if (billedOutstanding != null && unbilledOutstanding != null) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = CurrencyFormatter.formatCurrency(billedOutstanding, primaryCurrency),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Billed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = CurrencyFormatter.formatCurrency(unbilledOutstanding, primaryCurrency),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "Unbilled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (creditLimit == null) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Set a credit limit to see available credit and utilisation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Regular account layout
                Text(
                    text = "Current Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = CurrencyFormatter.formatCurrency(balance, primaryCurrency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCreditCard) Iconsax.Card else Iconsax.Wallet3,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = AccountBalanceEntity.accountLabel(bankName, accountLast4),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryStatistics(
    totalIncome: BigDecimal,
    totalExpenses: BigDecimal,
    netBalance: BigDecimal,
    period: String,
    primaryCurrency: String,
    hasMultipleCurrencies: Boolean = false
) {
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Text(
                text = period,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Income",
                    value = formatWithEstimatedDisplay(totalIncome, primaryCurrency, hasMultipleCurrencies),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = if (!isSystemInDarkTheme()) income_light else income_dark
                )
                StatisticItem(
                    label = "Expenses",
                    value = formatWithEstimatedDisplay(totalExpenses, primaryCurrency, hasMultipleCurrencies),
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    color = if (!isSystemInDarkTheme()) expense_light else expense_dark
                )
                StatisticItem(
                    label = "Net",
                    value = formatWithEstimatedDisplay(netBalance, primaryCurrency, hasMultipleCurrencies),
                    icon = Iconsax.WalletMoney,
                    color = if (netBalance >= BigDecimal.ZERO) {
                        if (!isSystemInDarkTheme()) income_light else income_dark
                    } else {
                        if (!isSystemInDarkTheme()) expense_light else expense_dark
                    }
                )
            }
        }
    }
}

/**
 * Formats currency with estimated display for multi-currency accounts
 */
private fun formatWithEstimatedDisplay(
    amount: BigDecimal,
    currency: String,
    hasMultipleCurrencies: Boolean
): String {
    val formattedAmount = CurrencyFormatter.formatCurrency(amount, currency)
    return if (hasMultipleCurrencies) {
        "est. $formattedAmount"
    } else {
        formattedAmount
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.medium),
            tint = color
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilter(
    selectedRange: DateRange,
    onRangeSelected: (DateRange) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(DateRange.values().toList()) { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun AccountTransactionItem(
    transaction: TransactionEntity,
    primaryCurrency: String,
    onClick: () -> Unit
) {
    val amountColor = when (transaction.transactionType) {
        TransactionType.INCOME -> if (!isSystemInDarkTheme()) income_light else income_dark
        TransactionType.EXPENSE -> if (!isSystemInDarkTheme()) expense_light else expense_dark
        TransactionType.CREDIT -> if (!isSystemInDarkTheme()) credit_light else credit_dark
        TransactionType.TRANSFER -> if (!isSystemInDarkTheme()) transfer_light else transfer_dark
        TransactionType.INVESTMENT -> if (!isSystemInDarkTheme()) investment_light else investment_dark
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandIcon(
                    merchantName = transaction.merchantName,
                    category = transaction.category,
                    size = 48.dp,
                    showBackground = true
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.merchantName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transaction.dateTime.format(
                                DateTimeFormatter.ofPattern("MMM d, h:mm a")
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Show balance after if available
                        transaction.balanceAfter?.let { balance ->
                            Text(
                                text = "• Bal: ${CurrencyFormatter.formatCurrency(balance, primaryCurrency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = CurrencyFormatter.formatCurrency(transaction.amount, transaction.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                
                // Transaction type indicator
                when (transaction.transactionType) {
                    TransactionType.CREDIT -> Icon(
                        Iconsax.Card,
                        contentDescription = "Credit",
                        modifier = Modifier.size(Dimensions.Icon.small),
                        tint = amountColor
                    )
                    TransactionType.TRANSFER -> Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Transfer",
                        modifier = Modifier.size(Dimensions.Icon.small),
                        tint = amountColor
                    )
                    TransactionType.INVESTMENT -> Icon(
                        Iconsax.Graph,
                        contentDescription = "Investment",
                        modifier = Modifier.size(Dimensions.Icon.small),
                        tint = amountColor
                    )
                    else -> {}
                }
            }
        }
    }
}
