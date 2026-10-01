package com.pennywiseai.tracker.presentation.budgetgroups

import java.time.format.TextStyle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.repository.BudgetGroupSpending
import com.pennywiseai.tracker.data.repository.BudgetOverallSummary
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.toColorOr
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.icons.iconax.History as IconaxHistory
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.graphics.SolidColor
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
import kotlinx.coroutines.delay
import com.pennywiseai.tracker.data.database.entity.BudgetGroupType
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetGroupsScreen(
    viewModel: BudgetGroupsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToGroupEdit: (Long) -> Unit = {},
    onNavigateToHistory: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onNavigateToDetail: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onNavigateToCategory: (category: String, yearMonth: String, currency: String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

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
                title = stringResource(R.string.budgets_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.budgets_back)
                    )
                },
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            if (uiState.hasGroups) {
                // The label is the action ("New Budget"), so the icon is decorative.
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToGroupEdit(-1L) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.budget_edit_title_new)) },
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (!uiState.hasGroups) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                MonthSelector(
                    year = uiState.selectedYear,
                    month = uiState.selectedMonth,
                    isCurrentMonth = uiState.selectedYear == java.time.LocalDate.now().year &&
                        uiState.selectedMonth == java.time.LocalDate.now().monthValue,
                    onPrevious = { viewModel.selectPreviousMonth() },
                    onNext = { viewModel.selectNextMonth() }
                )
                EmptyBudgetState(
                    modifier = Modifier.fillMaxSize(),
                    onSmartDefaults = { viewModel.runSmartDefaults() },
                    onCreateNew = { onNavigateToGroupEdit(-1L) }
                )
            }
        } else {
            BudgetGroupsContent(
                modifier = Modifier.hazeSource(hazeState).background(MaterialTheme.colorScheme.background),
                topPadding = paddingValues.calculateTopPadding(),
                uiState = uiState,
                onPreviousMonth = { viewModel.selectPreviousMonth() },
                onNextMonth = { viewModel.selectNextMonth() },
                onGroupClick = { groupId -> onNavigateToGroupEdit(groupId) },
                onDeleteGroup = { groupId -> viewModel.deleteGroup(groupId) },
                onMoveGroupUp = { groupId -> viewModel.moveGroupUp(groupId) },
                onMoveGroupDown = { groupId -> viewModel.moveGroupDown(groupId) },
                onNavigateToHistory = { groupId ->
                    onNavigateToHistory(groupId, uiState.selectedYear, uiState.selectedMonth)
                },
                onNavigateToDetail = { groupId ->
                    onNavigateToDetail(groupId, uiState.selectedYear, uiState.selectedMonth)
                },
                onCategoryClick = { category ->
                    val yearMonth = "%04d-%02d".format(uiState.selectedYear, uiState.selectedMonth)
                    onNavigateToCategory(category, yearMonth, uiState.currency)
                }
            )
        }
    }
}

@Composable
private fun EmptyBudgetState(
    modifier: Modifier = Modifier,
    onSmartDefaults: () -> Unit,
    onCreateNew: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PennyWiseCardV2(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            contentPadding = Dimensions.Padding.empty
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.emptyStateContainer),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.budgets_setup_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(R.string.budgets_setup_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onSmartDefaults,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small))
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.budgets_setup_smart_defaults))
                }

                OutlinedButton(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.budgets_setup_custom))
                }
            }
        }
    }
}

@Composable
private fun BudgetGroupsContent(
    modifier: Modifier = Modifier,
    topPadding: Dp = Spacing.none,
    uiState: BudgetGroupsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGroupClick: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onMoveGroupUp: (Long) -> Unit,
    onMoveGroupDown: (Long) -> Unit,
    onNavigateToHistory: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val summary = uiState.summary ?: return
    val isCurrentMonth = YearMonth.of(uiState.selectedYear, uiState.selectedMonth) == YearMonth.now()
    val groupCount = summary.groups.size
    var deleteGroupId by remember { mutableStateOf<Long?>(null) }
    var deleteGroupName by remember { mutableStateOf("") }

    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { Spacing.xl.roundToPx() }

    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            delay(350)
            hasAnimated = true
        }
    }

    val lazyListState = rememberLazyListState()
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize().overScrollVertical(),
        contentPadding = PaddingValues(
            start = Dimensions.Padding.content,
            end = Dimensions.Padding.content,
            top = Dimensions.Padding.content + topPadding,
            bottom = Dimensions.Component.bottomBarHeight + Spacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
    ) {
        // Month Selector
        item {
            val visible = remember { mutableStateOf(hasAnimated) }
            LaunchedEffect(Unit) {
                if (!hasAnimated) { delay(0); visible.value = true }
            }
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { slideOffsetPx },
                    animationSpec = tween(300)
                )
            ) {
                MonthSelector(
                    year = uiState.selectedYear,
                    month = uiState.selectedMonth,
                    isCurrentMonth = isCurrentMonth,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth
                )
            }
        }

        // Budget Cards
        itemsIndexed(
            items = summary.groups,
            key = { _, group -> group.group.budget.id }
        ) { index, groupSpending ->
            val visible = remember { mutableStateOf(hasAnimated) }
            LaunchedEffect(Unit) {
                if (!hasAnimated) { delay((index + 1) * 50L); visible.value = true }
            }
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { slideOffsetPx },
                    animationSpec = tween(300)
                )
            ) {
                BudgetOverviewCard(
                    groupSpending = groupSpending,
                    currency = uiState.currency,
                    isFirst = index == 0,
                    isLast = index == groupCount - 1,
                    onEdit = { onGroupClick(groupSpending.group.budget.id) },
                    onDelete = {
                        deleteGroupId = groupSpending.group.budget.id
                        deleteGroupName = groupSpending.group.budget.name
                    },
                    onMoveUp = { onMoveGroupUp(groupSpending.group.budget.id) },
                    onMoveDown = { onMoveGroupDown(groupSpending.group.budget.id) },
                    onViewHistory = { onNavigateToHistory(groupSpending.group.budget.id) },
                    onViewDetails = { onNavigateToDetail(groupSpending.group.budget.id) },
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    // Delete confirmation dialog
    if (deleteGroupId != null) {
        AlertDialog(
            onDismissRequest = { deleteGroupId = null },
            title = { Text(stringResource(R.string.budgets_delete_title)) },
            text = { Text(stringResource(R.string.budgets_delete_message, deleteGroupName)) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteGroupId?.let { onDeleteGroup(it) }
                        deleteGroupId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.budgets_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteGroupId = null }) {
                    Text(stringResource(R.string.budgets_action_cancel))
                }
            }
        )
    }
}

@Composable
private fun MonthSelector(
    year: Int,
    month: Int,
    isCurrentMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val yearMonth = YearMonth.of(year, month)
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = onPrevious,
            modifier = Modifier.size(Dimensions.Component.minTouchTarget)
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = stringResource(R.string.budgets_previous_month),
                modifier = Modifier.size(Dimensions.Icon.medium)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = yearMonth.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        FilledTonalIconButton(
            onClick = onNext,
            enabled = !isCurrentMonth,
            modifier = Modifier.size(Dimensions.Component.minTouchTarget)
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.budgets_next_month),
                modifier = Modifier.size(Dimensions.Icon.medium)
            )
        }
    }
}

@Composable
internal fun BudgetOverviewCard(
    groupSpending: BudgetGroupSpending,
    currency: String,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onViewHistory: () -> Unit,
    onViewDetails: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val budget = groupSpending.group.budget
    var expanded by remember { mutableStateOf(false) }
    val hasBreakdown = groupSpending.categorySpending.isNotEmpty() ||
        (groupSpending.dailyCumulativeSpending.size >= 2 && groupSpending.dailyBudgetPace.isNotEmpty())
    val expansionState = stringResource(
        if (expanded) R.string.budget_hide_breakdown else R.string.budget_show_breakdown
    )

    val pctUsed = groupSpending.percentageUsed
    val isOverBudget = groupSpending.remaining < BigDecimal.ZERO

    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgressState by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 800),
        label = "progressAnimation"
    )

    LaunchedEffect(pctUsed) {
        animatedProgress = (pctUsed / 100f).coerceIn(0f, 1f)
    }

    val budgetColor = budget.color.toColorOr(MaterialTheme.colorScheme.primary)
    val statusColor: Color = when {
        pctUsed >= 90f -> MaterialTheme.colorScheme.error
        pctUsed >= 70f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    // The bar wears the budget's own color while it is healthy; the percentage
    // and hero text stay semantic so a red budget at 36% does not read as danger.
    val barColor = if (pctUsed >= 70f) statusColor else budgetColor

    PennyWiseCardV2(
        onClick = if (hasBreakdown) ({ expanded = !expanded }) else null,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (hasBreakdown) stateDescription = expansionState
            },
        shape = MaterialTheme.shapes.extraLarge,
        // A faint rim in the budget's own colour, over the standard card surface.
        border = BorderStroke(
            width = Dimensions.Component.dividerThickness,
            color = budgetColor.copy(alpha = BUDGET_CARD_RIM_ALPHA)
        ),
        // The tint is painted by the column below so it covers the whole card.
        contentPadding = Dimensions.Padding.none
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .budgetTint(budgetColor)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .padding(horizontal = Spacing.md + Spacing.xs, vertical = Spacing.smd)
        ) {
            // Header: marker + UPPERCASE name, then history and the overflow menu.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Api,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small),
                    tint = barColor
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = budget.name.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                BudgetTonalIconButton(
                    onClick = onViewHistory,
                    icon = Iconax.IconaxHistory,
                    contentDescription = stringResource(R.string.budgets_view_history)
                )
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    BudgetTonalIconButton(
                        onClick = { showMenu = true },
                        icon = Icons.Default.MoreVert,
                        contentDescription = stringResource(
                            R.string.budget_more_actions,
                            budget.name
                        )
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.budget_edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.budgets_view_history)) },
                            onClick = {
                                showMenu = false
                                onViewHistory()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.History, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.budgets_move_up)) },
                            onClick = {
                                showMenu = false
                                onMoveUp()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                            },
                            enabled = !isFirst
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.budgets_move_down)) },
                            onClick = {
                                showMenu = false
                                onMoveDown()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            },
                            enabled = !isLast
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.budget_delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            if (groupSpending.totalBudget > BigDecimal.ZERO) {
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Hero row. On a spending limit this is what is left to spend per
                // day ("per day" means nothing for a target or for expected
                // bills, and nothing is left once the limit is crossed or the
                // window has ended), so those show the amount left of the whole
                // budget - or how far over it is - instead.
                val remainingAbs = groupSpending.remaining.abs()
                val showDaily = budget.groupType == BudgetGroupType.LIMIT &&
                    !isOverBudget &&
                    groupSpending.dailyAllowance > BigDecimal.ZERO
                val heroLabel = stringResource(
                    when {
                        showDaily -> R.string.budgets_card_daily_left
                        isOverBudget -> R.string.budgets_card_over_label
                        else -> R.string.budgets_card_remaining_label
                    }
                )
                val heroAmount = when {
                    showDaily -> groupSpending.dailyAllowance
                    isOverBudget -> remainingAbs
                    else -> groupSpending.remaining.coerceAtLeast(BigDecimal.ZERO)
                }
                val pairLabel = stringResource(
                    when (budget.groupType) {
                        BudgetGroupType.LIMIT -> R.string.budgets_card_spent_limit
                        BudgetGroupType.EXPECTED -> R.string.budgets_card_spent_expected
                        BudgetGroupType.TARGET -> R.string.budgets_card_actual_target
                    }
                )
                // FlowRow: when there is no room (narrow card, large font) the
                // right-hand figures wrap below instead of squeezing the hero.
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    itemVerticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        BudgetCardLabel(text = heroLabel)
                        Text(
                            text = CurrencyFormatter.formatCurrency(heroAmount, currency),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isOverBudget) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        BudgetCardLabel(text = pairLabel)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = CurrencyFormatter.formatCurrency(groupSpending.totalActual, currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = " / ",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCurrency(groupSpending.totalBudget, currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Progress bar
                val barShape = RoundedCornerShape(50)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.Component.progressBarHeight)
                        .clip(barShape)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = Dimensions.Alpha.divider)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedProgressState)
                            .fillMaxHeight()
                            .clip(barShape)
                            .background(barColor)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Footer: how much of the budget is used, then the per-cadence
                // renewal countdown - always framed as "Resets in X days" so the
                // user knows when the budget's window ends. The displayed
                // window is the budget's own current window (Jun 29..Jul 5 even
                // when the page is the July view) so this number is consistent
                // across month views.
                val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                val locale = LocalConfiguration.current.locales[0]
                val subtitleText = when {
                    groupSpending.daysRemaining == 0 && groupSpending.daysElapsed >= groupSpending.windowDays -> stringResource(R.string.budgets_finished)
                    isOverBudget -> stringResource(R.string.budgets_over_by, CurrencyFormatter.formatCurrency(remainingAbs, currency))
                    groupSpending.periodType == BudgetPeriodType.WEEKLY -> {
                        val renewalIn = (groupSpending.daysRemaining - 1).coerceAtLeast(0)
                        val weekdayName = java.time.DayOfWeek.of((budget.weekStartDay ?: 1).coerceIn(1, 7))
                            .getDisplayName(TextStyle.FULL, locale)
                        if (renewalIn == 0) {
                            stringResource(R.string.budgets_resets_today_weekly, weekdayName)
                        } else {
                            pluralStringResource(R.plurals.budgets_resets_in_weekly, renewalIn, renewalIn, weekdayName)
                        }
                    }
                    groupSpending.periodType == BudgetPeriodType.MONTHLY -> {
                        val startDay = budget.monthStartDay
                            ?: groupSpending.windowStart.dayOfMonth
                        val renewalIn = (groupSpending.daysRemaining - 1).coerceAtLeast(0)
                        if (renewalIn == 0) {
                            stringResource(R.string.budgets_resets_today_monthly, startDay)
                        } else {
                            pluralStringResource(R.plurals.budgets_resets_in_monthly, renewalIn, renewalIn, startDay)
                        }
                    }
                    groupSpending.periodType == BudgetPeriodType.CUSTOM -> {
                        val range = stringResource(
                            R.string.budgets_date_range,
                            groupSpending.windowStart.format(dateFormatter),
                            groupSpending.windowEnd.format(dateFormatter)
                        )
                        if (groupSpending.daysRemaining >= 1) {
                            // >1 counts the days after today; ==1 reads as "1 day" (unchanged behaviour).
                            val left = (groupSpending.daysRemaining - 1).coerceAtLeast(1)
                            pluralStringResource(R.plurals.budgets_runs_days_remaining, left, range, left)
                        } else {
                            stringResource(R.string.budgets_runs_finished, range)
                        }
                    }
                    else -> pluralStringResource(
                        R.plurals.budgets_days_remaining,
                        groupSpending.daysRemaining,
                        groupSpending.daysRemaining
                    )
                }
                val percentText = stringResource(R.string.budgets_percent, pctUsed.toInt())
                val footerText = buildAnnotatedString {
                    withStyle(SpanStyle(color = statusColor, fontWeight = FontWeight.SemiBold)) {
                        append(percentText)
                    }
                    append(" · ")
                    append(subtitleText)
                }
                Text(
                    text = footerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (groupSpending.isTrackingAllExpenses) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                BudgetCardLabel(text = stringResource(R.string.budgets_card_spent_label))
                Text(
                    text = CurrencyFormatter.formatCurrency(groupSpending.totalActual, currency),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.budgets_tracking_all),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions: the breakdown affordance (the whole card toggles it) on
            // the left, "View details" on the right.
            Spacer(modifier = Modifier.height(Spacing.xs))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (hasBreakdown) Arrangement.SpaceBetween else Arrangement.End,
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                if (hasBreakdown) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = expansionState,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.Icon.inline),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier.defaultMinSize(minHeight = Dimensions.Component.minTouchTarget),
                ) {
                    Text(stringResource(R.string.budget_view_details))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.inline),
                    )
                }
            }

            // Expandable category list + pace chart
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    // Per-budget spending pace chart
                    if (groupSpending.dailyCumulativeSpending.size >= 2 && groupSpending.dailyBudgetPace.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        SpendingPaceChart(
                            cumulativeSpending = groupSpending.dailyCumulativeSpending,
                            budgetPace = groupSpending.dailyBudgetPace,
                            currency = currency
                        )
                    }

                    if (groupSpending.categorySpending.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    groupSpending.categorySpending.forEach { catSpending ->
                        val catPctUsed = catSpending.percentageUsed
                        val catStatusColor: Color = when {
                            catPctUsed >= 90f -> MaterialTheme.colorScheme.error
                            catPctUsed >= 70f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Spacing.sm))
                                .clickable { onCategoryClick(catSpending.categoryName) }
                                .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            // Category icon in colored circle
                            Box(
                                modifier = Modifier
                                    .size(Dimensions.Icon.list)
                                    .clip(CircleShape)
                                    .background(CategoryMapping.colorFor(catSpending.categoryName).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryIcon(
                                    category = catSpending.categoryName,
                                    size = Dimensions.Icon.inline,
                                    tint = CategoryMapping.colorFor(catSpending.categoryName)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = catSpending.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (catSpending.budgetAmount > BigDecimal.ZERO) {
                                        Text(
                                            text = stringResource(R.string.budgets_percent, catPctUsed.toInt()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = catStatusColor
                                        )
                                    }
                                }

                                if (catSpending.budgetAmount > BigDecimal.ZERO) {
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    val catBarShape = RoundedCornerShape(50)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(Dimensions.Component.progressBarHeight)
                                            .clip(catBarShape)
                                            .background(catStatusColor.copy(alpha = 0.15f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = (catPctUsed / 100f).coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .clip(catBarShape)
                                                .background(catStatusColor)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    Text(
                                        text = stringResource(
                                            R.string.budgets_amount_of,
                                            CurrencyFormatter.formatCurrency(catSpending.actualAmount, currency),
                                            CurrencyFormatter.formatCurrency(catSpending.budgetAmount, currency)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(catSpending.actualAmount, currency),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

/** How strongly the budget's colour rims the card. */
private const val BUDGET_CARD_RIM_ALPHA = 0.12f

/**
 * Washes a few soft blobs of the budget's colour over the card, like Cashiro's
 * gradient mesh but static: it draws nothing that animates, so it costs a list
 * of cards no frames and renders identically in screenshots.
 */
private fun Modifier.budgetTint(color: Color): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.26f), Color.Transparent),
            center = Offset(size.width * 0.12f, size.height * 0.10f),
            radius = size.width * 0.70f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.95f, size.height * 0.95f),
            radius = size.width * 0.60f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(size.width * 0.75f, size.height * 0.15f),
            radius = size.width * 0.50f
        )
    )
}

/** The small, upper-case, lightly tracked caption above a figure. */
@Composable
private fun BudgetCardLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * A small round tonal button for the card header (history, overflow menu).
 * The drawn disc is [Dimensions.Icon.large]; the touch target is the full
 * [Dimensions.Component.minTouchTarget].
 */
@Composable
private fun BudgetTonalIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(Dimensions.Component.minTouchTarget)
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.Icon.large)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = Dimensions.Alpha.divider),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(Dimensions.Icon.small),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpendingPaceChart(
    cumulativeSpending: List<Double>,
    budgetPace: List<Double>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val themeColors = MaterialTheme.colorScheme
    val isOverPace = cumulativeSpending.lastOrNull()?.let { actual ->
        budgetPace.lastOrNull()?.let { pace -> actual > pace }
    } ?: false
    val spendingColor = if (isOverPace) themeColors.error else themeColors.primary
    val actualLabel = stringResource(R.string.budgets_chart_actual)
    val budgetPaceLabel = stringResource(R.string.budgets_chart_pace)

    Column(modifier = modifier.fillMaxWidth()) {
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.Component.chartCompactHeight),
                data = listOf(
                    Line(
                        label = actualLabel,
                        values = cumulativeSpending,
                        color = SolidColor(spendingColor),
                        firstGradientFillColor = spendingColor.copy(alpha = 0.2f),
                        secondGradientFillColor = Color.Transparent,
                        strokeAnimationSpec = tween(1200),
                        gradientAnimationDelay = 600,
                        drawStyle = DrawStyle.Stroke(width = Dimensions.Component.chartStroke),
                        curvedEdges = true,
                        dotProperties = DotProperties(
                            enabled = false
                        )
                    ),
                    Line(
                        label = budgetPaceLabel,
                        values = budgetPace,
                        color = SolidColor(themeColors.onSurfaceVariant.copy(alpha = 0.4f)),
                        drawStyle = DrawStyle.Stroke(width = Dimensions.Component.chartReferenceStroke),
                        strokeAnimationSpec = tween(1200),
                        curvedEdges = false,
                        dotProperties = DotProperties(enabled = false)
                    )
                ),
                dividerProperties = DividerProperties(enabled = false),
                indicatorProperties = HorizontalIndicatorProperties(
                    enabled = true,
                    textStyle = PennyWiseText.chartLabel.copy(
                        color = themeColors.onSurfaceVariant
                    ),
                    contentBuilder = { value ->
                        CurrencyFormatter.formatAbbreviated(value, currency)
                    }
                ),
                labelHelperProperties = LabelHelperProperties(enabled = false),
                labelProperties = LabelProperties(enabled = false),
                gridProperties = GridProperties(
                    enabled = true,
                    xAxisProperties = GridProperties.AxisProperties(
                        enabled = false
                    ),
                    yAxisProperties = GridProperties.AxisProperties(
                        enabled = true,
                        style = StrokeStyle.Dashed(),
                        color = SolidColor(themeColors.onSurface.copy(alpha = 0.08f))
                    )
                ),
                animationMode = AnimationMode.Together(delayBuilder = { it * 100L }),
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.Component.legendDot)
                        .clip(CircleShape)
                        .background(spendingColor)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = actualLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Box(
                    modifier = Modifier
                        .size(Dimensions.Component.legendDot)
                        .clip(CircleShape)
                        .background(themeColors.onSurfaceVariant.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = budgetPaceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.onSurfaceVariant
                )
            }
        }
}
