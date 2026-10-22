package com.pennywiseai.tracker.presentation.home

import androidx.annotation.StringRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.pennywiseai.tracker.data.preferences.HomeSection
import com.pennywiseai.tracker.data.preferences.HomeSectionLayout
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.work.WorkInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import com.pennywiseai.tracker.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import com.pennywiseai.tracker.ui.components.legibleOn
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.data.share.SharePeriod
import com.pennywiseai.tracker.presentation.share.ShareCardSheet
import com.pennywiseai.tracker.ui.components.ShareMonthBanner
import androidx.navigation.NavController
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.people.initialsOf
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.ui.components.cards.HomeGroupCard
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.SmsParsingProgressDialog
import com.pennywiseai.tracker.ui.components.cards.AccountCarousel
import com.pennywiseai.tracker.ui.components.cards.BudgetCarousel
import com.pennywiseai.tracker.ui.components.cards.CashFlowCard
import com.pennywiseai.tracker.ui.components.cards.cashFlowHasChannels
import com.pennywiseai.tracker.ui.components.cards.GroupCard
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.TransactionItem
import com.pennywiseai.tracker.ui.components.skeleton.BalanceCardSkeleton
import com.pennywiseai.tracker.ui.components.skeleton.TransactionItemSkeleton
import com.pennywiseai.tracker.ui.components.spotlightTarget
import com.pennywiseai.tracker.data.preferences.CoverStyle
import com.pennywiseai.tracker.presentation.common.buildProfileAccountKeys
import com.pennywiseai.tracker.ui.components.CoverGradientBanner
import com.pennywiseai.tracker.ui.components.HomeBannerImage
import com.pennywiseai.tracker.ui.components.GreetingCard
import com.pennywiseai.tracker.ui.components.HomeTopBar
import com.pennywiseai.tracker.ui.components.ProfileAvatar
import com.pennywiseai.tracker.ui.components.SubscriptionIconsStack
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Search
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

/** Distance each card travels on the first-composition entrance animation. */
private val ENTRANCE_SLIDE_DISTANCE = Spacing.lg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavController,
    coverStyle: CoverStyle = CoverStyle.AURORA,
    blurEffects: Boolean = false,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPersonalDashboard: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToTransactionsWithSearch: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {},
    onNavigateToTransactionGroups: () -> Unit = {},
    onLoanClick: (Long) -> Unit = {},
    onNavigateToAddScreen: () -> Unit = {},
    onNavigateToManageAccounts: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onGroupClick: (Long) -> Unit = {},
    onGoalClick: (Long) -> Unit = {},
    onTransactionTypeClick: (String?) -> Unit = {},
    onFabPositioned: (Rect) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isProEntitled by viewModel.isProEntitled.collectAsState()
    val currentCycleWindow by viewModel.currentCycleWindow.collectAsState()
    var showUpgradeSheet by rememberSaveable { mutableStateOf(false) }
    val deletedTransaction by viewModel.deletedTransaction.collectAsState()
    val smsScanWorkInfo by viewModel.smsScanWorkInfo.collectAsState()
    val groupSummaries by viewModel.groupSummaries.collectAsState()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val homeSectionLayout by viewModel.homeSectionLayout.collectAsStateWithLifecycle()
    val showSharePrompt by viewModel.showSharePrompt.collectAsState()
    val activity = LocalActivity.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // State for full resync confirmation dialog
    var showFullResyncDialog by remember { mutableStateOf(false) }

    // Bottom sheet menu state
    var showMenuSheet by remember { mutableStateOf(false) }
    // Cashiro's currency chip opens a picker listing the currencies the user holds.
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showEditWidgetsSheet by remember { mutableStateOf(false) }
    // Period the sheet should open on. The monthly banner points it at the finished
    // month; opening from the overflow menu uses whatever the user saved.
    var sharePromptPeriod by remember { mutableStateOf<SharePeriod?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    val deletedMessage = stringResource(R.string.home_transaction_deleted)
    val undoLabel = stringResource(R.string.home_undo)

    // Haptic feedback
    val view = LocalView.current

    // Haptic on successful SMS scan completion
    LaunchedEffect(smsScanWorkInfo?.state) {
        if (smsScanWorkInfo?.state == WorkInfo.State.SUCCEEDED) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    // Scroll behaviors for collapsible TopAppBar
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Haze state for TopAppBar blur
    val hazeState = remember { HazeState() }

    // Haze state for banner blur effect
    val hazeStateBanner = remember { HazeState() }

    // LazyColumn scroll state for overscroll physics
    val lazyListState = rememberLazyListState()

    // Staggered entrance animation state — only animates on first composition
    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { ENTRANCE_SLIDE_DISTANCE.roundToPx() }

    // Mark entrance animation as complete after all stagger delays have fired
    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            delay(350) // slightly after the last stagger (300ms)
            hasAnimated = true
        }
    }

    // Check for app updates and reviews when the screen is first displayed
    LaunchedEffect(Unit) {
        // Refresh account balances to ensure proper currency conversion
        viewModel.refreshAccountBalances()

        activity?.let {
            val componentActivity = it as ComponentActivity
            
            // Check for app updates
            viewModel.checkForAppUpdate(
                activity = componentActivity,
                snackbarHostState = snackbarHostState,
                scope = scope
            )
            
            // Check for in-app review eligibility
            viewModel.checkForInAppReview(componentActivity)
        }
    }
    
    // Refresh hidden accounts whenever this screen becomes visible
    // This ensures changes from ManageAccountsScreen are reflected immediately
    DisposableEffect(Unit) {
        viewModel.refreshHiddenAccounts()
        onDispose { }
    }
    
    // Handle delete undo snackbar
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
    
    // Clear snackbar when navigating away
    DisposableEffect(Unit) {
        onDispose {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    
    // Avatar tap, shared by the compact bar: free users get the upgrade sheet
    // (the ring is their "tap to unlock" cue), Pro members their dashboard.
    val onAvatarClick: () -> Unit = {
        if (isProEntitled) {
            onNavigateToPersonalDashboard()
        } else {
            showUpgradeSheet = true
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        // The bottom nav bar is overlaid on top of this Scaffold by MainScreen,
        // so pad the host clear of it — otherwise snackbars (notably the
        // "restart to apply update" prompt) render behind the nav bar.
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = Dimensions.Component.bottomBarHeight)
            )
        },
        topBar = {
            HomeTopBar(
                title = stringResource(R.string.home_top_bar_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                hazeState = hazeState,
                blurEffects = blurEffects,
                // Compact bar, leading: the avatar with its Pro ring. Same tap
                // behaviour as the expanded greeting row — free users get the
                // upgrade sheet, Pro members their dashboard.
                navigationContent = {
                    ProfileAvatar(
                        userName = uiState.userName,
                        profileImageUri = uiState.profileImageUri,
                        profileBackgroundColor = uiState.profileBackgroundColor,
                        isProEntitled = isProEntitled,
                        onClick = onAvatarClick,
                        // The bar already insets its icons by 4dp; this brings the
                        // avatar to the 16dp screen gutter the cards below use.
                        modifier = Modifier.padding(start = Spacing.smd),
                        size = Dimensions.Component.iconButton
                    )
                },
                // Compact bar, trailing: just Cashiro's "…" menu. The Pro chip and
                // the profile switcher live in the expanded greeting row, so the
                // centred title never has to share the bar with them.
                actionContent = {
                    val containerColor = MaterialTheme.colorScheme.surfaceContainer
                    val buttonColor = if (blurEffects) containerColor.copy(alpha = 0.5f) else containerColor
                    HomeBarCircleButton(
                        onClick = { showMenuSheet = true },
                        containerColor = buttonColor,
                        modifier = Modifier.padding(end = Spacing.smd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = stringResource(R.string.home_more_options),
                            tint = MaterialTheme.colorScheme.inverseSurface,
                            modifier = Modifier.size(Dimensions.Icon.medium)
                        )
                    }
                },
                expandedContent = {
                    GreetingCard(
                        userName = uiState.userName,
                        profileImageUri = uiState.profileImageUri,
                        profileBackgroundColor = uiState.profileBackgroundColor,
                        onAvatarClick = onNavigateToPersonalDashboard,
                        onMenuClick = { showMenuSheet = true },
                        profiles = uiState.profiles,
                        selectedProfileId = uiState.selectedProfileId,
                        onProfileSelected = { viewModel.updateSelectedProfile(it) },
                        isProEntitled = isProEntitled,
                        onUpgradeClick = { showUpgradeSheet = true },
                        cycleEnd = currentCycleWindow.second
                    )
                }
            )
        }
    ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Banner at y=0 — paints behind the transparent TopAppBar.
        // Toggle on: the user's photo, else the bundled default banner (as in
        // Cashiro). Toggle off: the chosen cover style.
        if (uiState.showHomeBanner) {
            HomeBannerImage(
                imageUri = uiState.homeBannerUri,
                hazeStateBanner = hazeStateBanner,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        } else if (coverStyle != CoverStyle.NONE) {
            CoverGradientBanner(
                coverStyle = coverStyle,
                hazeStateBanner = hazeStateBanner,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        // LazyColumn scrolls over the banner
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .overScrollVertical(),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
            contentPadding = PaddingValues(
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                // Clears the nav bar plus the Add + Sync FAB stack above it.
                bottom = Dimensions.Component.bottomBarHeight +
                    Dimensions.Component.fabScrollClearance
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 1. Balance Card (0ms delay)
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
                    if (!uiState.isBalanceReady) {
                        BalanceCardSkeleton(
                            modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                        )
                    } else {
                        com.pennywiseai.tracker.ui.components.cards.BalanceCard(
                            modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                            blurEffects = blurEffects,
                            hazeState = hazeStateBanner,
                            userName = uiState.userName,
                            netWorth = uiState.netWorth,
                            netWorthAssets = uiState.netWorthAssets,
                            hasNetWorth = uiState.hasNetWorth,
                            isNetWorthApproximate = uiState.isNetWorthApproximate,
                            netWorthChangePercent = uiState.netWorthChangePercent,
                            currentYearTotal = uiState.currentYearTotal,
                            monthlyChange = uiState.monthlyChange,
                            monthlyChangePercent = uiState.monthlyChangePercent,
                            currency = uiState.selectedCurrency,
                            currentMonthIncome = uiState.currentMonthIncome,
                            currentMonthExpenses = uiState.currentMonthExpenses,
                            currentMonthLent = uiState.currentMonthLent,
                            currentMonthTotal = uiState.currentMonthTotal,
                            balanceHistory = uiState.balanceHistory,
                            balanceHistoryEndDate = uiState.balanceHistoryEndDate,
                            isBalanceHistoryApproximate = uiState.isBalanceHistoryApproximate,
                            spendingHistory = uiState.spendingHistory,
                            lastMonthSpendingHistory = uiState.lastMonthSpendingHistory,
                            lastMonthSpending = uiState.lastMonthExpenses,
                            availableCurrencies = uiState.availableCurrencies,
                            isUnifiedMode = uiState.isUnifiedMode,
                            isBalanceHidden = uiState.isBalanceHidden,
                            onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
                            onCurrencyClick = { showCurrencySheet = true },
                            onShowBreakdown = { viewModel.showBreakdownDialog() },
                            accountBalances = uiState.accountBalances,
                            creditCards = uiState.creditCards,
                            totalAvailableCredit = uiState.totalAvailableCredit,
                            onAccountClick = { bankName, accountLast4 ->
                                navController.navigate(
                                    com.pennywiseai.tracker.navigation.AccountDetail(
                                        bankName = bankName,
                                        accountLast4 = accountLast4
                                    )
                                ) { launchSingleTop = true }
                            }
                        )
                    }
                }
            }

            // 1.2. Monthly share prompt. Offers the user something ("your month, summed
            // up") rather than asking a favour, and only appears when the finished month
            // actually has enough in it to be worth sending.
            if (showSharePrompt) {
                item {
                    ShareMonthBanner(
                        onOpen = {
                            viewModel.markSharePromptHandled()
                            sharePromptPeriod = SharePeriod.LAST_MONTH
                            showShareSheet = true
                        },
                        onDismiss = { viewModel.markSharePromptHandled() },
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                    )
                }
            }

            // 1.5. Cash-flow card (25ms delay) — absent on dormant months. Skipped
            // as an item (not just drawn empty) so it leaves no list gap behind.
            // When card spend counts as expense (#705) it's already in "Spent this
            // month", so it's left out of this "outside cash flow" card.
            val cashFlowCreditCard = if (uiState.countCreditCardAsExpense) BigDecimal.ZERO else uiState.currentMonthCreditCard
            if (cashFlowHasChannels(cashFlowCreditCard, uiState.currentMonthInvestment, uiState.currentMonthTransfer)) item {
                val visible = remember { mutableStateOf(hasAnimated) }
                LaunchedEffect(Unit) {
                    if (!hasAnimated) { delay(25); visible.value = true }
                }
                AnimatedVisibility(
                    visible = visible.value,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        initialOffsetY = { slideOffsetPx },
                        animationSpec = tween(300)
                    )
                ) {
                    CashFlowCard(
                        currency = uiState.selectedCurrency,
                        creditCardSpend = cashFlowCreditCard,
                        investments = uiState.currentMonthInvestment,
                        transfers = uiState.currentMonthTransfer,
                        isBalanceHidden = uiState.isBalanceHidden,
                        onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                        blurEffects = blurEffects,
                        hazeState = hazeStateBanner
                    )
                }
            }

            // 2–6. User-orderable sections (#770). Hidden ones are absent from the
            // list entirely; the entrance stagger follows the *displayed* position so
            // it survives reordering. Each branch keeps its own data guard.
            val visibleSections = homeSectionLayout.filter { it.second }.map { it.first }
            for ((index, section) in visibleSections.withIndex()) {
                val delayMs = 50L * (index + 1)
                when (section) {
                    HomeSection.BUDGETS -> {
                        // 2. Budget Carousel
                        uiState.budgetSummary?.let { summary ->
                            item(key = section.name) {
                                val visible = remember { mutableStateOf(hasAnimated) }
                                LaunchedEffect(Unit) {
                                    if (!hasAnimated) { delay(delayMs); visible.value = true }
                                }
                                AnimatedVisibility(
                                    visible = visible.value,
                                    enter = fadeIn(tween(300)) + slideInVertically(
                                        initialOffsetY = { slideOffsetPx },
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    // Header-less, as in Cashiro: the carousel's cards name
                                    // their budgets and open the Budgets screen on tap.
                                    BudgetCarousel(
                                        summary = summary,
                                        onClick = onNavigateToBudgets,
                                        onCreateBudget = onNavigateToBudgets,
                                        blurEffects = blurEffects,
                                        hazeState = hazeStateBanner
                                    )
                                }
                            }
                        }
                    }
                    HomeSection.GOALS -> {
                        // 2.25. Goals (#914) — only when there's a non-archived goal.
                        if (goals.isNotEmpty()) {
                            item(key = section.name) {
                                val visible = remember { mutableStateOf(hasAnimated) }
                                LaunchedEffect(Unit) {
                                    if (!hasAnimated) { delay(delayMs); visible.value = true }
                                }
                                AnimatedVisibility(
                                    visible = visible.value,
                                    enter = fadeIn(tween(300)) + slideInVertically(
                                        initialOffsetY = { slideOffsetPx },
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    // Cashiro glass, header-less like Subscriptions and
                                    // Lent / Borrowed below Recent.
                                    com.pennywiseai.tracker.presentation.goals.GoalsHomeCard(
                                        goals = goals,
                                        onOpenGoal = onGoalClick,
                                        onViewAll = onNavigateToBudgets,
                                        blurEffects = blurEffects,
                                        hazeState = hazeStateBanner,
                                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                                    )
                                }
                            }
                        }
                    }
                    HomeSection.LOANS -> {
                        // 2.5. Loans Summary — only when active loans exist
                        uiState.loanSummary?.let { summary ->
                            item(key = section.name) {
                                val visible = remember { mutableStateOf(hasAnimated) }
                                LaunchedEffect(Unit) {
                                    if (!hasAnimated) { delay(delayMs); visible.value = true }
                                }
                                AnimatedVisibility(
                                    visible = visible.value,
                                    enter = fadeIn(tween(300)) + slideInVertically(
                                        initialOffsetY = { slideOffsetPx },
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    // Cashiro's Lent / Borrowed card: header-less, the whole card
                                    // opens Loans.
                                    LendBorrowCard(
                                        summary = summary,
                                        currency = uiState.selectedCurrency,
                                        onClick = onNavigateToLoans,
                                        blurEffects = blurEffects,
                                        hazeState = hazeStateBanner,
                                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                                    )
                                }
                            }
                        }
                    }
                    HomeSection.GROUPS -> {
                        // 2.5. Groups section (#664) — a horizontal rail of the user's
                        // transaction groups, absent entirely when none exist so Home
                        // stays uncluttered for everyone else. Discovery was the ask:
                        // groups only surfaced via Settings or a group card that happened
                        // to have recent activity.
                        if (groupSummaries.isNotEmpty()) {
                            item(key = section.name) {
                                val visible = remember { mutableStateOf(hasAnimated) }
                                LaunchedEffect(Unit) {
                                    if (!hasAnimated) { delay(delayMs); visible.value = true }
                                }
                                AnimatedVisibility(
                                    visible = visible.value,
                                    enter = fadeIn(tween(300)) + slideInVertically(
                                        initialOffsetY = { slideOffsetPx },
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)
                                    ) {
                                        SectionHeaderV2(
                                            title = stringResource(R.string.home_section_groups),
                                            modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                                            action = {
                                                TextButton(onClick = onNavigateToTransactionGroups) {
                                                    Text(stringResource(R.string.home_view_all))
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(Dimensions.Icon.small)
                                                    )
                                                }
                                            }
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                        ) {
                                            items(groupSummaries, key = { it.group.id }) { summary ->
                                                HomeGroupCard(
                                                    summary = summary,
                                                    onClick = { onGroupClick(summary.group.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HomeSection.RECENT_TRANSACTIONS -> {
                        // 3. Recent Transactions — Cashiro's block: the "Recent" header
                        // sits flush on a short grouped list, with a small "View All"
                        // pill beneath it, all in one item so no list gap splits them.
                        item(key = section.name) {
                            val visible = remember { mutableStateOf(hasAnimated) }
                            LaunchedEffect(Unit) {
                                if (!hasAnimated) { delay(delayMs); visible.value = true }
                            }
                            AnimatedVisibility(
                                visible = visible.value,
                                enter = fadeIn(tween(300)) + slideInVertically(
                                    initialOffsetY = { slideOffsetPx },
                                    animationSpec = tween(300)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    RecentSectionHeader(onSearchClick = onNavigateToTransactionsWithSearch)
                                    when {
                                        uiState.isLoading -> Column(
                                            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
                                        ) {
                                            repeat(HOME_RECENT_SKELETON_ROWS) {
                                                TransactionItemSkeleton()
                                            }
                                        }
                                        uiState.recentItems.isEmpty() -> PennyWiseEmptyState(
                                            icon = Icons.Default.Sync,
                                            headline = stringResource(R.string.home_empty_headline),
                                            description = stringResource(R.string.home_empty_description),
                                            actionLabel = stringResource(R.string.home_empty_action),
                                            onAction = { viewModel.scanSmsMessages() },
                                            ghostContent = {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
                                                ) {
                                                    repeat(HOME_RECENT_SKELETON_ROWS) {
                                                        TransactionItemSkeleton()
                                                    }
                                                }
                                            }
                                        )
                                        else -> {
                                            val profileAccountKeys = remember(uiState.accountBalances) {
                                                buildProfileAccountKeys(uiState.accountBalances)
                                            }
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
                                            ) {
                                                val items = uiState.recentItems
                                                items.forEachIndexed { index, item ->
                                                    val position = ListItemPosition.from(index, items.size)
                                                    when (item) {
                                                        is HomeRecentItem.SingleTransaction -> TransactionItem(
                                                            transaction = item.transaction,
                                                            convertedAmount = item.convertedAmount,
                                                            displayCurrency = if (uiState.isUnifiedMode) uiState.selectedCurrency else null,
                                                            showTypeLabel = false,
                                                            listItemPosition = position,
                                                            dense = true,
                                                            profileAccountKeys = profileAccountKeys,
                                                            onClick = { onTransactionClick(item.transaction.id) }
                                                        )
                                                        is HomeRecentItem.GroupItem -> GroupCard(
                                                            group = item.group,
                                                            transactions = item.transactions,
                                                            convertedAmounts = item.convertedAmounts,
                                                            displayCurrency = if (uiState.isUnifiedMode) uiState.selectedCurrency else null,
                                                            listItemPosition = position,
                                                            dense = true,
                                                            onClick = { onGroupClick(item.group.id) }
                                                        )
                                                    }
                                                }
                                            }
                                            ViewAllPill(
                                                onClick = onNavigateToTransactions,
                                                blurEffects = blurEffects
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HomeSection.ACCOUNTS -> {
                        // 4. Account Carousel
                        if (uiState.creditCards.isNotEmpty() || uiState.accountBalances.isNotEmpty()) {
                            item(key = section.name) {
                                val visible = remember { mutableStateOf(hasAnimated) }
                                LaunchedEffect(Unit) {
                                    if (!hasAnimated) { delay(delayMs); visible.value = true }
                                }
                                AnimatedVisibility(
                                    visible = visible.value,
                                    enter = fadeIn(tween(300)) + slideInVertically(
                                        initialOffsetY = { slideOffsetPx },
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    // No section header: like Cashiro, the account card sits
                                    // straight under the summary — its logo, "BANK ••1234" and
                                    // "View details" pill already say what it is. The old
                                    // "Manage" link moved to the More-options sheet.
                                    AccountCarousel(
                                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                                        bankAccounts = uiState.accountBalances,
                                        creditCards = uiState.creditCards,
                                        onAccountClick = { bankName, accountLast4 ->
                                            navController.navigate(
                                                com.pennywiseai.tracker.navigation.AccountDetail(
                                                    bankName = bankName,
                                                    accountLast4 = accountLast4
                                                )
                                            ) { launchSingleTop = true }
                                        },
                                        isUnifiedMode = uiState.isUnifiedMode,
                                        selectedCurrency = uiState.selectedCurrency,
                                        blurEffects = blurEffects,
                                        hazeState = hazeStateBanner
                                    )
                                }
                            }
                        }
                    }
                    HomeSection.SUBSCRIPTIONS -> {
                        // 5. Upcoming Subscriptions Alert
                        if (uiState.upcomingSubscriptions.isNotEmpty()) {
                            item(key = section.name) {
                                val visible = remember { mutableStateOf(hasAnimated) }
                                LaunchedEffect(Unit) {
                                    if (!hasAnimated) { delay(delayMs); visible.value = true }
                                }
                                AnimatedVisibility(
                                    visible = visible.value,
                                    enter = fadeIn(tween(300)) + slideInVertically(
                                        initialOffsetY = { slideOffsetPx },
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    // Header-less, as in Cashiro: the card states its own count
                                    // and total, and tapping anywhere on it opens Subscriptions.
                                    Box(modifier = Modifier.padding(horizontal = Dimensions.Padding.content)) {
                                        UpcomingSubscriptionsCard(
                                            subscriptions = uiState.upcomingSubscriptions,
                                            totalAmount = uiState.upcomingSubscriptionsTotal,
                                            totalByCurrency = uiState.upcomingSubscriptionsByCurrency,
                                            isUnified = uiState.isUnifiedMode,
                                            currency = uiState.selectedCurrency,
                                            onClick = onNavigateToSubscriptions,
                                            blurEffects = blurEffects,
                                            hazeState = hazeStateBanner
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HomeSection.ACTIVITY -> {
                        // 6. Heatmap Widget
                        item(key = section.name) {
                            val visible = remember { mutableStateOf(hasAnimated) }
                            LaunchedEffect(Unit) {
                                if (!hasAnimated) { delay(delayMs); visible.value = true }
                            }
                            AnimatedVisibility(
                                visible = visible.value,
                                enter = fadeIn(tween(300)) + slideInVertically(
                                    initialOffsetY = { slideOffsetPx },
                                    animationSpec = tween(300)
                                )
                            ) {
                                // Header-less, as in Cashiro: the month labels say what it is.
                                com.pennywiseai.tracker.ui.components.cards.HeatmapWidget(
                                    transactionHeatmap = uiState.transactionHeatmap,
                                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                                    blurEffects = blurEffects,
                                    hazeState = hazeStateBanner
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Scan FAB rotation animation
        val infiniteTransition = rememberInfiniteTransition(label = "scan_rotation")
        val rotationAngle by animateFloatAsState(
            targetValue = if (uiState.isScanning) 1f else 0f,
            animationSpec = tween(300),
            label = "scan_trigger"
        )
        val continuousRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scan_rotation"
        )
        val scanRotation = if (uiState.isScanning) continuousRotation else 0f

        // FABs - Direct access (no speed dial). A small Sync FAB stacked above
        // the large primary Add FAB.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = Dimensions.Padding.content,
                    bottom = Dimensions.Component.fabBottomInset
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.smd),
            horizontalAlignment = Alignment.End
        ) {
            // Sync FAB (top, small, secondary)
            // Single tap: incremental scan, Long press: full resync
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Hint for long-press functionality - only show for new users (no transactions yet)
                if (uiState.recentItems.isEmpty() && !uiState.isLoading) {
                    Text(
                        text = stringResource(R.string.home_fab_sync_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier
                        // The spotlight tutorial points at the scan action, so it
                        // follows the Sync FAB to its new position.
                        .spotlightTarget(onFabPositioned)
                        .size(Dimensions.Component.iconButton)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { viewModel.scanSmsMessages() },
                                onLongPress = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    showFullResyncDialog = true
                                }
                            )
                        },
                    shape = FloatingActionButtonDefaults.shape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shadowElevation = Dimensions.Elevation.fab,
                    tonalElevation = Dimensions.Elevation.fab,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = stringResource(R.string.home_fab_sync),
                            modifier = Modifier
                                .size(Dimensions.Icon.inline)
                                .then(if (uiState.isScanning) Modifier.rotate(scanRotation) else Modifier),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Add FAB (bottom, large, primary)
            FloatingActionButton(
                onClick = onNavigateToAddScreen,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_fab_add)
                )
            }
        }
        
        // Full Resync Confirmation Dialog
        if (showFullResyncDialog) {
            AlertDialog(
                onDismissRequest = { showFullResyncDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(stringResource(R.string.home_resync_title))
                },
                text = {
                    // Two ways to re-read: the gentle one first, since a full rebuild
                    // resets edits to SMS transactions.
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        ResyncOption(
                            title = stringResource(R.string.home_resync_scan_again),
                            description = stringResource(R.string.home_resync_scan_again_desc),
                            onClick = {
                                showFullResyncDialog = false
                                viewModel.scanSmsMessages(rescanAll = true)
                            }
                        )
                        ResyncOption(
                            title = stringResource(R.string.home_resync_rebuild),
                            description = stringResource(R.string.home_resync_rebuild_desc),
                            onClick = {
                                showFullResyncDialog = false
                                viewModel.scanSmsMessages(forceResync = true)
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showFullResyncDialog = false }
                    ) {
                        Text(stringResource(R.string.home_cancel))
                    }
                }
            )
        }

        // SMS Parsing Progress Dialog
        SmsParsingProgressDialog(
            isVisible = uiState.isScanning,
            workInfo = smsScanWorkInfo,
            onDismiss = { viewModel.cancelSmsScan() },
            onCancel = { viewModel.cancelSmsScan() }
        )
        
        // Breakdown Dialog
        if (uiState.showBreakdownDialog) {
            BreakdownDialog(
                currentMonthIncome = uiState.currentMonthIncome,
                currentMonthExpenses = uiState.currentMonthExpenses,
                currentMonthTotal = uiState.currentMonthTotal,
                lastMonthIncome = uiState.lastMonthIncome,
                lastMonthExpenses = uiState.lastMonthExpenses,
                lastMonthTotal = uiState.lastMonthTotal,
                currency = uiState.selectedCurrency,
                onDismiss = { viewModel.hideBreakdownDialog() }
            )
        }
    }

    // Share-card sheet, rendered at screen scope so dismissing the menu can't tear it down.
    if (showShareSheet) {
        ShareCardSheet(
            initialPeriod = sharePromptPeriod,
            onDismiss = { showShareSheet = false; sharePromptPeriod = null },
        )
    }

    // "More options": Cashiro's bottom sheet of rounded rows.
    if (showMenuSheet) {
        val versionName = remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) {
                null
            }
        }
        HomeOptionsMenu(
            blurEffects = blurEffects,
            versionName = versionName,
            showBannerImage = uiState.showHomeBanner,
            onShowBannerImageChange = viewModel::setShowHomeBanner,
            onDismiss = { showMenuSheet = false },
            onMonthlyRecap = { showMenuSheet = false; showShareSheet = true },
            onSettings = { showMenuSheet = false; onNavigateToSettings() },
            onManageAccounts = { showMenuSheet = false; onNavigateToManageAccounts() },
            onEditHome = { showMenuSheet = false; showEditWidgetsSheet = true },
            onDiscord = {
                showMenuSheet = false
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.Links.DISCORD_URL)))
            },
            onRate = {
                showMenuSheet = false
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
                } catch (_: android.content.ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                }
            },
        )
    }
    }

    if (showCurrencySheet) {
        HomeCurrencySheet(
            selected = uiState.selectedCurrency,
            currencies = uiState.availableCurrencies,
            onSelect = {
                viewModel.selectCurrency(it)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false },
        )
    }

    // Edit home screen sheet (#770). Every change persists immediately — no Save.
    if (showEditWidgetsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditWidgetsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
            ) {
                Text(
                    text = stringResource(R.string.home_edit_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.home_edit_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = Spacing.sm)
                        .fillMaxWidth()
                )
                homeSectionLayout.forEachIndexed { i, (section, isVisible) ->
                    GroupedRow(
                        position = ListItemPosition.from(i, homeSectionLayout.size),
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        val sectionLabel = stringResource(section.titleRes())
                        Text(
                            text = sectionLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isVisible) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            enabled = i > 0,
                            onClick = { viewModel.moveHomeSection(section, -1) }
                        ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.home_edit_move_up, sectionLabel)) }
                        IconButton(
                            enabled = i < homeSectionLayout.lastIndex,
                            onClick = { viewModel.moveHomeSection(section, +1) }
                        ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.home_edit_move_down, sectionLabel)) }
                        Switch(
                            checked = isVisible,
                            onCheckedChange = { on -> viewModel.setHomeSectionVisible(section, on) }
                        )
                    }
                }
            }
        }
    }

    // Pro upgrade sheet — triggered from the subtle ✨ chip in the top bar
    // for free users; reuses the same composable Settings uses.
    if (showUpgradeSheet) {
        com.pennywiseai.tracker.presentation.paywall.UpgradeSheet(
            onDismiss = { showUpgradeSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreakdownDialog(
    currentMonthIncome: BigDecimal,
    currentMonthExpenses: BigDecimal,
    currentMonthTotal: BigDecimal,
    lastMonthIncome: BigDecimal,
    lastMonthExpenses: BigDecimal,
    lastMonthTotal: BigDecimal,
    currency: String = "INR",
    onDismiss: () -> Unit
) {
    val now = LocalDate.now()
    val monthName = { m: java.time.Month -> m.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) }
    val currentPeriod = stringResource(R.string.home_breakdown_period, monthName(now.month), now.dayOfMonth)
    val lastMonth = now.minusMonths(1)
    val lastPeriod = stringResource(R.string.home_breakdown_period, monthName(lastMonth.month), now.dayOfMonth)
    
    Dialog(onDismissRequest = onDismiss) {
        PennyWiseCardV2(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            contentPadding = Dimensions.Padding.dialog
        ) {
            // Period blocks are the unit of meaning here, so the gap *between*
            // blocks (lg) is larger than the gap between a block's own rows
            // (sm). The previous uniform 16dp made the period label float
            // equidistant from both periods' figures.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                Text(
                    text = stringResource(R.string.home_breakdown_title),
                    style = MaterialTheme.typography.headlineSmall
                )

                BreakdownPeriod(
                    period = currentPeriod,
                    income = currentMonthIncome,
                    expenses = currentMonthExpenses,
                    net = currentMonthTotal,
                    currency = currency
                )

                BreakdownPeriod(
                    period = lastPeriod,
                    income = lastMonthIncome,
                    expenses = lastMonthExpenses,
                    net = lastMonthTotal,
                    currency = currency
                )

                PennyWiseCardV2(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = Spacing.smd
                ) {
                    Text(
                        text = stringResource(R.string.home_breakdown_formula),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.home_close))
                }
            }
        }
    }
}

/** One period's income / expenses / net block inside [BreakdownDialog]. */
@Composable
private fun BreakdownPeriod(
    period: String,
    income: BigDecimal,
    expenses: BigDecimal,
    net: BigDecimal,
    currency: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = period,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        BreakdownRow(label = stringResource(R.string.home_breakdown_income), amount = income, isIncome = true, currency = currency)
        BreakdownRow(label = stringResource(R.string.home_breakdown_expenses), amount = expenses, isIncome = false, currency = currency)
        HorizontalDivider()
        BreakdownRow(
            label = stringResource(R.string.home_breakdown_net_worth),
            amount = net,
            isIncome = net >= BigDecimal.ZERO,
            isBold = true,
            currency = currency
        )
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: BigDecimal,
    isIncome: Boolean,
    isBold: Boolean = false,
    currency: String = "INR"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = CurrencyFormatter.signed(if (isIncome) "+" else "-", CurrencyFormatter.formatCurrency(amount.abs(), currency)),
            style = if (isBold) PennyWiseText.amountRow else PennyWiseText.amountRow.copy(
                fontWeight = FontWeight.Normal
            ),
            color = if (isIncome) {
                if (!isAppInDarkTheme()) income_light else income_dark
            } else {
                if (!isAppInDarkTheme()) expense_light else expense_dark
            }
        )
    }
}

/**
 * "2 active subscriptions · ₹198 / MONTH" with the brands' logos stacked on
 * the right — Cashiro's subscriptions card. The whole card opens the
 * Subscriptions screen, so it needs no section header or "View" link of its own.
 *
 * The total is never summed across currencies: unified mode shows the single
 * converted figure, native mode lists one figure per currency.
 */
@Composable
private fun UpcomingSubscriptionsCard(
    subscriptions: List<SubscriptionEntity>,
    totalAmount: BigDecimal,
    totalByCurrency: Map<String, com.pennywiseai.tracker.utils.Money> = emptyMap(),
    isUnified: Boolean = false,
    currency: String = "INR",
    onClick: () -> Unit = {},
    blurEffects: Boolean = false,
    hazeState: HazeState? = null
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        // Cashiro's very round (48dp, so effectively pill-ended) card.
        shape = RoundedCornerShape(Spacing.xxl),
        blurEffects = blurEffects,
        hazeState = hazeState,
        tint = baseColor,
        contentPadding = Dimensions.Padding.content
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.smd)
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.home_active_subscriptions,
                        subscriptions.size,
                        subscriptions.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = Dimensions.Alpha.subtitle)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        // Unified mode: one converted figure. Native mode:
                        // per-currency ("₹499 · $10") so a mixed set isn't summed
                        // into a mislabel.
                        text = if (isUnified) {
                            CurrencyFormatter.formatCurrency(totalAmount, currency)
                        } else {
                            CurrencyFormatter.formatByCurrency(
                                totalByCurrency,
                                fallbackCurrency = currency
                            )
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        // A mixed-currency list can be long; let it wrap rather
                        // than shove the unit off the card.
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = stringResource(R.string.home_subscriptions_per_month).uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = Dimensions.Alpha.subtitle),
                        modifier = Modifier.padding(bottom = Spacing.xxs)
                    )
                }
            }

            if (subscriptions.isNotEmpty()) {
                SubscriptionIconsStack(
                    subscriptions = subscriptions,
                    iconSize = SUBSCRIPTION_ICON_SIZE,
                    modifier = Modifier.padding(start = Spacing.sm, end = Spacing.sm),
                    // Ring each logo in the surface it sits on so the overlap
                    // reads as a cut-out (the card is translucent over a blur,
                    // so use the opaque base tone).
                    borderColor = baseColor
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            }
        }
    }
}

/**
 * Cashiro's Lent / Borrowed card (`LendBorrowCard`): two tonal tiles side by
 * side, each with a small arrow badge, the label, the remaining amount and the
 * people it is with as an overlapping avatar stack. The whole card opens Loans.
 *
 * Both amounts are in [currency]: the ViewModel only counts loans in that
 * currency (or converts them in unified mode), never adding currencies.
 */
@Composable
internal fun LendBorrowCard(
    summary: LoanSummary,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    blurEffects: Boolean = false,
    hazeState: HazeState? = null
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(LEND_BORROW_CARD_RADIUS),
        blurEffects = blurEffects,
        hazeState = hazeState,
        contentPadding = Dimensions.Padding.content
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            LendBorrowTile(
                label = stringResource(R.string.loans_direction_lent),
                amount = summary.totalLentRemaining,
                people = summary.lentPeople,
                currency = currency,
                color = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.ArrowUpward,
                modifier = Modifier.weight(1f),
            )
            LendBorrowTile(
                label = stringResource(R.string.loans_direction_borrowed),
                amount = summary.totalBorrowedRemaining,
                people = summary.borrowedPeople,
                currency = currency,
                color = MaterialTheme.colorScheme.secondary,
                icon = Icons.Default.ArrowDownward,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LendBorrowTile(
    label: String,
    amount: java.math.BigDecimal,
    people: List<LoanPerson>,
    currency: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    val tileColor = color.copy(alpha = LEND_BORROW_TILE_ALPHA)
    // Cashiro tints the amount and arrow with the tile's own colour; nudge it
    // until it reads on the tile (a dark primary would otherwise vanish).
    val scheme = MaterialTheme.colorScheme
    val accent = remember(color, scheme.surfaceContainerLow, scheme.onSurface) {
        color.legibleOn(
            background = tileColor.compositeOver(scheme.surfaceContainerLow),
            towards = scheme.onSurface,
        )
    }
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(Dimensions.Glass.heroRadius),
        color = tileColor,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Surface(
                    color = color.copy(alpha = LEND_BORROW_TILE_ALPHA),
                    shape = RoundedCornerShape(LEND_BORROW_BADGE_RADIUS),
                    modifier = Modifier.size(Dimensions.Icon.inline)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(Dimensions.Icon.tiny)
                        )
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = CurrencyFormatter.formatCurrency(amount, currency),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = LEND_BORROW_AMOUNT_SIZE
                ),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LoanPeopleStack(
                people = people,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/** Cashiro's `PersonIconsStack`: up to four overlapping initials avatars, then "+N". */
@Composable
private fun LoanPeopleStack(
    people: List<LoanPerson>,
    modifier: Modifier = Modifier,
) {
    if (people.isEmpty()) return
    val shown = people.take(LOAN_PEOPLE_MAX)
    val remaining = people.size - shown.size
    val circles = shown.size + if (remaining > 0) 1 else 0
    val step = LOAN_PERSON_SIZE * LOAN_PERSON_OVERLAP
    val outer = LOAN_PERSON_SIZE + Spacing.xs
    Box(modifier = modifier.width(step * (circles - 1) + outer)) {
        shown.forEachIndexed { index, person ->
            val fill = person.colorHex?.let { hex ->
                runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
            } ?: MaterialTheme.colorScheme.primaryContainer
            Box(
                modifier = Modifier
                    .offset(x = step * index)
                    .zIndex(index.toFloat())
                    .size(outer)
                    .shadow(Spacing.xxs, CircleShape)
                    .clip(CircleShape)
                    .background(fill),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    // Same initials as the Loans screen draws for this person.
                    text = initialsOf(person.name),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (fill.luminance() > 0.5f) Color.Black else Color.White
                )
            }
        }
        if (remaining > 0) {
            Box(
                modifier = Modifier
                    .offset(x = step * shown.size)
                    .zIndex(shown.size.toFloat())
                    .size(outer)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remaining",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/** Cashiro's subscription-logo size on the Home card. */
private val SUBSCRIPTION_ICON_SIZE = 38.dp
private val LEND_BORROW_CARD_RADIUS = 32.dp
private val LEND_BORROW_BADGE_RADIUS = 6.dp
private const val LEND_BORROW_TILE_ALPHA = 0.2f
private val LEND_BORROW_AMOUNT_SIZE = 22.sp
private val LOAN_PERSON_SIZE = 24.dp
private const val LOAN_PERSON_OVERLAP = 0.55f
private const val LOAN_PEOPLE_MAX = 4

/**
 * Cashiro's "Recent" header: a `titleSmall` semibold title in the accent colour,
 * inset to line up with the row content below, and a plain Search text button.
 */
@Composable
private fun RecentSectionHeader(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.md, end = Spacing.sm)
            .semantics { heading() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.home_recent_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onSearchClick) {
            Icon(
                imageVector = Iconax.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimensions.Icon.small)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(stringResource(R.string.txn_list_search))
        }
    }
}

/**
 * Cashiro's small centred "View All" pill under the Recent list: 26dp tall,
 * `bodySmall` in the accent colour. The touch target is still padded out to
 * the 48dp minimum, which also supplies the gap above the pill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewAllPill(
    onClick: () -> Unit,
    blurEffects: Boolean,
    modifier: Modifier = Modifier
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        onClick = onClick,
        modifier = modifier
            // The 48dp touch target pads above and below the pill; drop the
            // padding below from the layout so nothing trails the pill, as in
            // Cashiro (touches there still reach it through the padding above).
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val trailing = ((placeable.height - VIEW_ALL_PILL_HEIGHT.roundToPx()) / 2)
                    .coerceAtLeast(0)
                layout(placeable.width, placeable.height - trailing) {
                    placeable.place(0, 0)
                }
            }
            .minimumInteractiveComponentSize()
            .height(VIEW_ALL_PILL_HEIGHT),
        shape = RoundedCornerShape(Dimensions.Glass.heroRadius),
        // Over the translucent banner a solid pill would look pasted on.
        color = if (blurEffects) baseColor.copy(alpha = VIEW_ALL_PILL_GLASS_ALPHA) else baseColor,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.home_view_all),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
        }
    }
}

/** Skeleton rows shown while Home's Recent list loads (it holds three). */
private const val HOME_RECENT_SKELETON_ROWS = 3
private val VIEW_ALL_PILL_HEIGHT = 26.dp
private const val VIEW_ALL_PILL_GLASS_ALPHA = 0.7f

/**
 * A 40dp round button for the compact Home bar (the "…" menu). The touch
 * target is still padded out to the 48dp minimum.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeBarCircleButton(
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(Dimensions.Component.iconButton)
            .clip(CircleShape)
            .background(color = containerColor, shape = CircleShape)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * The Home "More options" sheet, as Cashiro's: a bottom sheet titled "More
 * options" whose entries are rounded `surfaceContainerLow` rows (2dp gutter),
 * ending with the "Show Banner Image" switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeOptionsMenu(
    blurEffects: Boolean,
    versionName: String?,
    showBannerImage: Boolean,
    onShowBannerImageChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onMonthlyRecap: () -> Unit,
    onSettings: () -> Unit,
    onManageAccounts: () -> Unit,
    onEditHome: () -> Unit,
    onDiscord: () -> Unit,
    onRate: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    com.pennywiseai.tracker.ui.components.PennyWiseBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensions.Padding.content)
                .padding(bottom = Spacing.md)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.home_menu_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.md),
            )
            val shapes = MaterialTheme.shapes
            HomeMenuItem(stringResource(R.string.home_menu_monthly_recap), onMonthlyRecap, shape = shapes.large.top()) {
                Icon(Icons.Default.Share, contentDescription = null)
            }
            HomeMenuItem(stringResource(R.string.home_menu_settings), onSettings, shape = shapes.small) {
                Icon(Icons.Default.Settings, contentDescription = null)
            }
            HomeMenuItem(stringResource(R.string.settings_manage_accounts_title), onManageAccounts, shape = shapes.small) {
                Icon(Icons.Default.AccountBalance, contentDescription = null)
            }
            HomeMenuItem(stringResource(R.string.home_menu_edit_home), onEditHome, shape = shapes.small) {
                Icon(Icons.Default.Tune, contentDescription = null)
            }
            HomeMenuItem(stringResource(R.string.home_menu_discord), onDiscord, shape = shapes.small) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_discord),
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            }
            HomeMenuItem(stringResource(R.string.home_menu_rate), onRate, shape = shapes.small) {
                Icon(Icons.Default.Star, contentDescription = null)
            }
            HomeMenuItem(
                label = stringResource(R.string.home_menu_show_banner),
                onClick = { onShowBannerImageChange(!showBannerImage) },
                shape = shapes.large.bottom(),
                trailing = {
                    Switch(checked = showBannerImage, onCheckedChange = null)
                },
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
            }
            versionName?.let {
                Text(
                    text = stringResource(R.string.home_version, it),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun androidx.compose.foundation.shape.CornerBasedShape.top() =
    copy(bottomStart = CornerSize(4.dp), bottomEnd = CornerSize(4.dp))

private fun androidx.compose.foundation.shape.CornerBasedShape.bottom() =
    copy(topStart = CornerSize(4.dp), topEnd = CornerSize(4.dp))

/**
 * Cashiro's currency picker: a bottom sheet listing each currency the user
 * holds as a grouped row with a radio. Choosing one only changes which
 * currency Home displays; amounts are never converted or added here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCurrencySheet(
    selected: String,
    currencies: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    com.pennywiseai.tracker.ui.components.PennyWiseBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensions.Padding.content)
                .padding(bottom = Spacing.md)
                .navigationBarsPadding(),
        ) {
            Text(
                text = stringResource(R.string.home_currency_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.md),
            )
            GroupedList {
                currencies.forEachIndexed { index, code ->
                    val name = remember(code) {
                        runCatching { java.util.Currency.getInstance(code).displayName }.getOrNull()
                    }
                    GroupedRow(
                        position = ListItemPosition.from(index, currencies.size),
                        onClick = { onSelect(code) },
                    ) {
                        Text(
                            text = if (name != null && name != code) "$name ($code)" else code,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        RadioButton(selected = code == selected, onClick = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMenuItem(
    label: String,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape,
    trailing: (@Composable () -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick, role = Role.Button)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { icon() }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@StringRes
private fun HomeSection.titleRes(): Int = when (this) {
    HomeSection.BUDGETS -> R.string.home_section_budgets
    HomeSection.GOALS -> R.string.home_section_goals
    HomeSection.LOANS -> R.string.home_section_loans
    HomeSection.GROUPS -> R.string.home_section_groups
    HomeSection.RECENT_TRANSACTIONS -> R.string.home_section_recent_transactions
    HomeSection.ACCOUNTS -> R.string.home_section_accounts
    HomeSection.SUBSCRIPTIONS -> R.string.home_section_subscriptions
    HomeSection.ACTIVITY -> R.string.home_section_activity
}

/** One choice in the "Read messages again" dialog: a tappable title + what it does. */
@Composable
private fun ResyncOption(title: String, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        // The dialog itself is surfaceContainerHigh, so a step up keeps each
        // option reading as a tappable card.
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
