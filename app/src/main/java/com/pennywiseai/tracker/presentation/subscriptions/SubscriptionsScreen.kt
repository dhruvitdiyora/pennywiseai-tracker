package com.pennywiseai.tracker.presentation.subscriptions

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.components.skeleton.TransactionItemSkeleton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pennywiseai.tracker.data.contacts.LocalMerchantDisplay
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.ui.theme.Dimensions
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.SubscriptionDirection
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.SubscriptionState
import com.pennywiseai.tracker.domain.model.SubscriptionBillingCycle
import com.pennywiseai.tracker.domain.model.getAccountType
import com.pennywiseai.tracker.presentation.accounts.AccountType
import com.pennywiseai.tracker.ui.components.*
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.SummaryCardV2
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.icons.iconax.Calendar
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.VideoPlay
import com.pennywiseai.tracker.ui.theme.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.formatAmount
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * The semantic state of a subscription's persisted next-payment date.
 *
 * This deliberately does not mutate or roll the date forward. Advancing the
 * schedule belongs to the domain use case; the screen only presents the date
 * that was persisted by the domain layer.
 */
internal enum class SubscriptionDueStatusKind {
    NO_DATE,
    OVERDUE,
    DUE_TODAY,
    DUE_TOMORROW,
    DUE_IN_DAYS,
    LATER,
    PAID,
}

/** Fill alpha for a swipeable glass row, so the swipe colour behind can't tint through. */
private const val SWIPE_ROW_FILL_ALPHA = 1f

internal data class SubscriptionDueStatus(
    val kind: SubscriptionDueStatusKind,
    val date: LocalDate? = null,
    val daysUntilDue: Long? = null,
)

internal fun subscriptionDueStatus(
    nextPaymentDate: LocalDate?,
    today: LocalDate,
    isPaidThisCycle: Boolean,
): SubscriptionDueStatus {
    if (nextPaymentDate == null) {
        return SubscriptionDueStatus(SubscriptionDueStatusKind.NO_DATE)
    }

    val daysUntilDue = ChronoUnit.DAYS.between(today, nextPaymentDate)
    return when {
        nextPaymentDate.isBefore(today) && !isPaidThisCycle ->
            SubscriptionDueStatus(
                kind = SubscriptionDueStatusKind.OVERDUE,
                date = nextPaymentDate,
                daysUntilDue = daysUntilDue,
            )
        nextPaymentDate.isBefore(today) && isPaidThisCycle ->
            SubscriptionDueStatus(
                kind = SubscriptionDueStatusKind.PAID,
                date = nextPaymentDate,
                daysUntilDue = daysUntilDue,
            )
        daysUntilDue == 0L ->
            SubscriptionDueStatus(
                kind = SubscriptionDueStatusKind.DUE_TODAY,
                date = nextPaymentDate,
                daysUntilDue = daysUntilDue,
            )
        daysUntilDue == 1L ->
            SubscriptionDueStatus(
                kind = SubscriptionDueStatusKind.DUE_TOMORROW,
                date = nextPaymentDate,
                daysUntilDue = daysUntilDue,
            )
        daysUntilDue in 2L..7L ->
            SubscriptionDueStatus(
                kind = SubscriptionDueStatusKind.DUE_IN_DAYS,
                date = nextPaymentDate,
                daysUntilDue = daysUntilDue,
            )
        else ->
            SubscriptionDueStatus(
                kind = SubscriptionDueStatusKind.LATER,
                date = nextPaymentDate,
                daysUntilDue = daysUntilDue,
            )
    }
}

private fun formatSubscriptionDate(date: LocalDate): String =
    date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
    )

/**
 * The short date shown on a row ("Mar 27"). A date in another year keeps its
 * year so it cannot be mistaken for this one. The month/day order follows the
 * user's locale; if the platform cannot supply a pattern the format falls back
 * to the full localised date.
 */
private fun formatCompactSubscriptionDate(date: LocalDate, today: LocalDate): String {
    if (date.year != today.year) return formatSubscriptionDate(date)
    val locale = Locale.getDefault()
    return runCatching {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMd")
        date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }.getOrElse { formatSubscriptionDate(date) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onAddSubscriptionClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Subscription currently being marked-as-paid. Null = sheet closed.
    var markPaidTarget by remember { mutableStateOf<SubscriptionEntity?>(null) }
    // Suggested-payment candidates resolved when target changes. Empty list
    // = no recent SMS-derived matches (user falls through to date picker).
    var markPaidCandidates by remember {
        mutableStateOf<List<com.pennywiseai.tracker.data.database.entity.TransactionEntity>>(emptyList())
    }
    LaunchedEffect(markPaidTarget) {
        val target = markPaidTarget
        markPaidCandidates = if (target != null) viewModel.candidatesFor(target) else emptyList()
    }

    // Scroll behaviors for collapsible TopAppBar
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()

    // Staggered entrance animation state — only animates on first composition
    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 30.dp.roundToPx() }

    // Mark entrance animation as complete after all stagger delays have fired
    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            delay(600) // slightly after the last possible stagger
            hasAnimated = true
        }
    }

    val context = LocalContext.current
    val undoLabel = stringResource(R.string.subscriptions_undo)
    val markPaidText = uiState.markPaidMessage?.asString()

    // Show snackbar when subscription is hidden
    LaunchedEffect(uiState.lastHiddenSubscription) {
        uiState.lastHiddenSubscription?.let { subscription ->
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.subscriptions_snackbar_hidden, subscription.merchantName),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoHide()
            }
        }
    }

    // Mark-as-paid feedback snackbar (#412). The sheet itself closes
    // optimistically; the VM publishes the outcome string here.
    LaunchedEffect(uiState.markPaidMessage) {
        markPaidText?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            viewModel.clearMarkPaidMessage()
        }
    }

    // Show snackbar with undo action when a subscription is ended.
    // ENDED is intentionally a "soft action" — there's the Cancelled section
    // for later recovery — but mirroring the hide-undo flow keeps parity
    // with the rest of the screen and gives an instant escape hatch.
    LaunchedEffect(uiState.lastEndedSubscription) {
        uiState.lastEndedSubscription?.let { subscription ->
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.subscriptions_snackbar_ended, subscription.merchantName),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoEnd()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.subscriptions_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.subscriptions_back)
                    )
                },
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            // A square "+" like Cashiro's, in the same container tone as the
            // other primary actions.
            FloatingActionButton(
                onClick = onAddSubscriptionClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.subscriptions_add)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                // Leaves room to scroll the last row clear of the FAB.
                bottom = paddingValues.calculateBottomPadding() +
                    Dimensions.Component.fabScrollClearance
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            // Total Monthly Subscriptions Summary (0ms delay)
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
                    // Expense subscriptions only, per currency (or in the display
                    // currency in unified mode): never one figure across currencies.
                    val expenseTotals = remember(
                        uiState.activeSubscriptions,
                        uiState.isUnifiedMode,
                        uiState.displayCurrency,
                        uiState.convertedAmounts
                    ) {
                        expenseSubscriptionTotals(
                            subscriptions = uiState.activeSubscriptions,
                            isUnified = uiState.isUnifiedMode,
                            displayCurrency = uiState.displayCurrency,
                            convertedAmounts = uiState.convertedAmounts
                        )
                    }
                    TotalSubscriptionsSummary(
                        totals = expenseTotals,
                        activeCount = uiState.activeSubscriptions.size,
                        paidThisCycleCount = uiState.paidThisCycleCount,
                        fallbackCurrency = uiState.displayCurrency ?: "INR"
                    )
                }
            }

            // Active Subscriptions (staggered 50ms per item, starting at 50ms)
            if (uiState.activeSubscriptions.isNotEmpty()) {
                item {
                    SubscriptionsSectionHeader(
                        title = stringResource(R.string.subscriptions_active_section)
                    )
                }
                itemsIndexed(
                    items = uiState.activeSubscriptions,
                    key = { _, item -> item.id }
                ) { index, subscription ->
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
                        SwipeableSubscriptionItem(
                            subscription = subscription,
                            accounts = accounts,
                            isPaidThisCycle = subscription.id in uiState.paidThisCycleIds,
                            convertedAmount = uiState.convertedAmounts[subscription.id],
                            displayCurrency = uiState.displayCurrency,
                            onTap = { markPaidTarget = subscription },
                            onHide = { viewModel.hideSubscription(subscription.id) },
                            onMarkAsEnded = { viewModel.markAsEnded(subscription.id) },
                            onEdit = { merchantName, amount, nextDate, category, billingCycle, account, accountChanged ->
                                viewModel.updateSubscription(subscription.id, merchantName, amount, nextDate, category, billingCycle, account, accountChanged)
                            },
                            onDelete = { viewModel.deleteSubscription(subscription.id) }
                        )
                    }
                }
            }

            // Cancelled subscriptions (collapsible; only rendered when any
            // exist so the section disappears entirely on empty state).
            // Stable key so `rememberSaveable` below isn't bound to the
            // positional slot index — without it, expanding the section and
            // then reactivating / hiding an active subscription would shift
            // the index and silently collapse the section.
            if (uiState.endedSubscriptions.isNotEmpty()) {
                item(key = "cancelled-section") {
                    var expanded by rememberSaveable { mutableStateOf(false) }
                    EndedSubscriptionsSection(
                        endedSubscriptions = uiState.endedSubscriptions,
                        expanded = expanded,
                        onToggle = { expanded = !expanded },
                        onReactivate = { viewModel.reactivateSubscription(it) },
                        onDelete = { viewModel.deleteSubscription(it) }
                    )
                }
            }

            // Empty State — only when there's truly nothing to show. A user
            // who has ended every subscription would otherwise see the
            // "No subscriptions detected yet" message right above their
            // Cancelled section, which is contradictory.
            if (uiState.activeSubscriptions.isEmpty() &&
                uiState.endedSubscriptions.isEmpty() &&
                !uiState.isLoading
            ) {
                item {
                    PennyWiseEmptyState(
                        icon = Icons.Default.Subscriptions,
                        headline = stringResource(R.string.subscriptions_empty_headline),
                        description = stringResource(R.string.subscriptions_empty_description)
                    )
                }
            }

            // Loading State
            if (uiState.isLoading) {
                items(5) {
                    SubscriptionItemSkeleton()
                }
            }
        }
    }

    // Mark-as-paid sheet (#412). Rendered at screen scope so a list item
    // re-composition (snackbar, scroll) can't tear down the sheet.
    markPaidTarget?.let { target ->
        MarkAsPaidSheet(
            subscription = target,
            isPaidThisCycle = target.id in uiState.paidThisCycleIds,
            candidates = markPaidCandidates,
            onDismiss = { markPaidTarget = null },
            onConfirm = { paymentDate ->
                viewModel.markAsPaid(target.id, paymentDate)
            },
            onLinkExisting = { txn ->
                viewModel.linkExistingTransaction(target.id, txn.id)
            },
        )
    }
}

@Composable
private fun EndedSubscriptionsSection(
    endedSubscriptions: List<SubscriptionEntity>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onReactivate: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.subscriptions_cancelled_section, endedSubscriptions.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.subscriptions_collapse else R.string.subscriptions_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                endedSubscriptions.forEach { sub ->
                    EndedSubscriptionItem(
                        subscription = sub,
                        onReactivate = { onReactivate(sub.id) },
                        onDelete = { onDelete(sub.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EndedSubscriptionItem(
    subscription: SubscriptionEntity,
    onReactivate: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.merchantName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(
                        subscription.amount, subscription.currency
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onReactivate) { Text(stringResource(R.string.subscriptions_reactivate)) }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.subscriptions_action_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.subscriptions_delete_title)) },
            text = {
                Text(stringResource(R.string.subscriptions_delete_permanently_message, subscription.merchantName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.subscriptions_action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.subscriptions_action_cancel)) }
            }
        )
    }
}

/**
 * The header card: "Total Subscriptions" with how many are active (and how many
 * are already paid this cycle), a round glyph, and two tiles with the monthly
 * and yearly cost. [totals] carries one figure per currency, so a rupee and a
 * dollar subscription read "₹399 · $30" rather than being added together.
 */
@Composable
private fun TotalSubscriptionsSummary(
    totals: SubscriptionExpenseTotals,
    activeCount: Int,
    paidThisCycleCount: Int,
    fallbackCurrency: String,
) {
    // Subtitle shape:
    //   no paid yet  → "5 active subscriptions"
    //   some paid    → "3 of 5 paid this cycle"
    //   all paid     → "All 5 paid this cycle ✓"
    val subtitle = when {
        activeCount == 0 -> stringResource(R.string.subscriptions_summary_none_active)
        paidThisCycleCount == 0 -> pluralStringResource(R.plurals.subscriptions_summary_active, activeCount, activeCount)
        paidThisCycleCount == activeCount -> pluralStringResource(R.plurals.subscriptions_summary_all_paid, activeCount, activeCount)
        else -> stringResource(R.string.subscriptions_summary_some_paid, paidThisCycleCount, activeCount)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = Dimensions.Padding.card
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.subscriptions_total_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = Spacing.sm)
                    .size(Dimensions.Icon.list)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = Dimensions.Alpha.medium),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Iconax.VideoPlay,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.medium),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            SubscriptionCostTile(
                label = stringResource(R.string.subscriptions_monthly_label),
                amount = CurrencyFormatter.formatByCurrency(
                    totals.monthly,
                    fallbackCurrency = fallbackCurrency
                ),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            SubscriptionCostTile(
                label = stringResource(R.string.subscriptions_yearly_label),
                amount = CurrencyFormatter.formatByCurrency(
                    totals.yearly,
                    fallbackCurrency = fallbackCurrency
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

/** One of the two cost tiles: a small tracked label over a centred figure. */
@Composable
private fun SubscriptionCostTile(
    label: String,
    amount: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = containerColor.copy(alpha = Dimensions.Alpha.surface),
                shape = MaterialTheme.shapes.large
            )
            .padding(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = amount,
            // A mixed-currency figure ("₹3,999 · $300") is long: step the size
            // down and let it wrap at the separator instead of cutting it off.
            style = if (amount.length > 11) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.titleLarge
            },
            fontWeight = FontWeight.Bold,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The accent heading above the active list, like Home's "Recent". */
@Composable
private fun SubscriptionsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.sm, top = Spacing.sm)
            .semantics { heading() }
    )
}

/**
 * One metadata pill on a subscription row (Cashiro-style): a fully rounded
 * tonal chip with an optional leading icon. Single line, ellipsised, so a row
 * of pills never wraps.
 */
@Composable
private fun SubscriptionPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emphasized: Boolean = false,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableSubscriptionItem(
    subscription: SubscriptionEntity,
    accounts: List<AccountBalanceEntity> = emptyList(),
    isPaidThisCycle: Boolean = false,
    today: LocalDate = LocalDate.now(),
    convertedAmount: BigDecimal? = null,
    displayCurrency: String? = null,
    onTap: () -> Unit = {},
    onEditRequested: (() -> Unit)? = null,
    onHide: () -> Unit,
    onMarkAsEnded: () -> Unit = {},
    onEdit: (merchantName: String, amount: BigDecimal, nextDate: LocalDate?, category: String?, billingCycle: String, account: AccountBalanceEntity?, accountChanged: Boolean) -> Unit = { _, _, _, _, _, _, _ -> },
    onDelete: () -> Unit = {}
) {
    // Display-only alias for the stored merchant (e.g. a resolved contact for
    // a raw VPA); the stored name is never changed.
    val merchantDisplay = LocalMerchantDisplay.current
    var showSmsBody by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val requestEdit = { onEditRequested?.invoke() ?: run { showEditDialog = true } }

    // Money out reads in the expense colour, money in (an allowance, a top-up)
    // in the income colour.
    val isDark = isSystemInDarkTheme()
    val amountColor = when {
        subscription.direction == SubscriptionDirection.INCOME ->
            if (isDark) income_dark else income_light
        else -> if (isDark) expense_dark else expense_light
    }

    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.settledValue) {
        when (dismissState.settledValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                requestEdit()
            }
            SwipeToDismissBoxValue.EndToStart -> onHide()
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
    
    SwipeToDismissBox(
        modifier = Modifier.testTag("subscription_item_${subscription.id}"),
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "background color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Rounded like the card, so no square colour shows behind it.
                    .background(color, MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = Dimensions.Padding.content),
                contentAlignment = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }
            ) {
                Icon(
                    imageVector = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                        else -> Icons.Default.VisibilityOff
                    },
                    contentDescription = stringResource(
                        if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                            R.string.subscription_swipe_edit
                        } else {
                            R.string.subscription_swipe_hide
                        }
                    ),
                    tint = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onError
                    }
                )
            }
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    // Opaque glass: the swipe's edit/hide colour sits right
                    // behind this card and must not tint through it.
                    solidFillAlpha = SWIPE_ROW_FILL_ALPHA,
                    // Tap = mark as paid (#412). Existing SMS-body expand
                    // moved to the kebab menu's "View source" item so the
                    // primary tap action is meaningful for ALL subs, not
                    // only those that arrived via SMS.
                    onClick = onTap,
                    contentPadding = Dimensions.Padding.none
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Icon
                        BrandIcon(
                            merchantName = subscription.merchantName,
                            size = Dimensions.Icon.avatarLarge,
                            showBackground = true
                        )
                        
                        // Content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = Spacing.sm)
                        ) {
                            Text(
                                text = merchantDisplay(subscription.merchantName)
                                    ?: subscription.merchantName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            
                            // Metadata: ONE line of pills, like Cashiro's -
                            // [calendar] date or status, paid state, billing
                            // cycle, category. Overdue reads in the error
                            // role, due within three days in the warning role.
                            // The cycle / category pills give way (ellipsis)
                            // first so the date never wraps onto a second line.
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            val dueStatus = subscriptionDueStatus(
                                nextPaymentDate = subscription.nextPaymentDate,
                                today = today,
                                isPaidThisCycle = isPaidThisCycle,
                            )
                            val dueSoon = dueStatus.kind in setOf(
                                SubscriptionDueStatusKind.DUE_TODAY,
                                SubscriptionDueStatusKind.DUE_TOMORROW,
                                SubscriptionDueStatusKind.DUE_IN_DAYS,
                            ) && dueStatus.daysUntilDue != null && dueStatus.daysUntilDue <= 3L
                            val isOverdue = dueStatus.kind == SubscriptionDueStatusKind.OVERDUE
                            val dateContainer = when {
                                isOverdue -> MaterialTheme.colorScheme.errorContainer
                                dueSoon -> MaterialTheme.colorScheme.warning.copy(
                                    alpha = Dimensions.Alpha.tonalIconContainer
                                )
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                            val dateContent = when {
                                isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                                dueSoon -> MaterialTheme.colorScheme.warning
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            // One line of whole pills: any that don't fit are left
                            // out rather than squeezed to an unreadable "M…".
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                itemVerticalAlignment = Alignment.CenterVertically,
                                maxLines = 1,
                            ) {
                                SubscriptionPill(
                                    text = when (dueStatus.kind) {
                                        SubscriptionDueStatusKind.OVERDUE ->
                                            stringResource(R.string.subscription_overdue)
                                        SubscriptionDueStatusKind.DUE_TODAY ->
                                            stringResource(R.string.subscriptions_due_today)
                                        SubscriptionDueStatusKind.DUE_TOMORROW ->
                                            stringResource(R.string.subscriptions_due_tomorrow)
                                        SubscriptionDueStatusKind.DUE_IN_DAYS ->
                                            pluralStringResource(
                                                R.plurals.subscriptions_due_in_days,
                                                (dueStatus.daysUntilDue ?: 0L).toInt(),
                                                dueStatus.daysUntilDue ?: 0L,
                                            )
                                        SubscriptionDueStatusKind.PAID,
                                        SubscriptionDueStatusKind.LATER ->
                                            dueStatus.date
                                                ?.let { formatCompactSubscriptionDate(it, today) }
                                                .orEmpty()
                                        SubscriptionDueStatusKind.NO_DATE ->
                                            stringResource(R.string.subscriptions_no_date)
                                    },
                                    containerColor = dateContainer,
                                    contentColor = dateContent,
                                    icon = Iconax.Calendar.takeIf {
                                        dueStatus.kind != SubscriptionDueStatusKind.NO_DATE
                                    },
                                    emphasized = isOverdue || dueSoon,
                                )
                                // Paid this cycle (#412) - computed in the VM
                                // (today-anchored cycle check, shared with the
                                // partition sort).
                                if (isPaidThisCycle) {
                                    SubscriptionPill(
                                        text = subscription.lastPaidAt?.let {
                                            stringResource(
                                                R.string.subscriptions_paid_on,
                                                formatCompactSubscriptionDate(it, today),
                                            )
                                        } ?: stringResource(R.string.subscription_paid),
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        icon = Icons.Default.CheckCircle,
                                    )
                                }
                                SubscriptionPill(
                                    text = subscriptionBillingCycleLabel(subscription.billingCycle),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                subscription.category?.takeIf { it.isNotBlank() }?.let { category ->
                                    SubscriptionPill(
                                        text = category,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                                if (!subscription.smsBody.isNullOrBlank()) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = stringResource(R.string.subscriptions_sms_available),
                                        modifier = Modifier
                                            .padding(vertical = Spacing.xs)
                                            .size(Dimensions.Icon.small),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        if (convertedAmount != null && displayCurrency != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.formatCurrency(convertedAmount, displayCurrency),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = amountColor
                                )
                                Text(
                                    text = "(${subscription.formatAmount()})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = subscription.formatAmount(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = amountColor
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(Dimensions.Component.minTouchTarget)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.subscriptions_more_options),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.subscriptions_menu_mark_paid)) },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onTap()
                                    }
                                )
                                if (!subscription.smsBody.isNullOrBlank()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.subscriptions_menu_view_source)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            showSmsBody = !showSmsBody
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.subscriptions_menu_edit)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        requestEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.subscriptions_menu_end)) },
                                    leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onMarkAsEnded()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.subscriptions_action_delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
                
                // SMS Body Display (expandable)
                if (showSmsBody && !subscription.smsBody.isNullOrBlank()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        tint = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentPadding = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimensions.Padding.content)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(Dimensions.Icon.medium)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = stringResource(
                                        if (subscription.bankName == "Manual Entry") R.string.subscriptions_source_notes
                                        else R.string.subscriptions_source_original_sms
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            
                            // SMS text in monospace font
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = subscription.smsBody,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    modifier = Modifier.padding(Spacing.md)
                                )
                            }
                        }
                    }
                }
            }
        }
    )

    if (showEditDialog) {
        EditSubscriptionDialog(
            subscription = subscription,
            accounts = accounts,
            onDismiss = { showEditDialog = false },
            onSave = { merchantName, amount, nextDate, category, billingCycle, account, accountChanged ->
                onEdit(merchantName, amount, nextDate, category, billingCycle, account, accountChanged)
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.subscriptions_delete_title)) },
            text = {
                Text(stringResource(R.string.subscriptions_delete_message, subscription.merchantName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.subscriptions_action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.subscriptions_action_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSubscriptionDialog(
    subscription: SubscriptionEntity,
    accounts: List<AccountBalanceEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (merchantName: String, amount: BigDecimal, nextDate: LocalDate?, category: String?, billingCycle: String, account: AccountBalanceEntity?, accountChanged: Boolean) -> Unit
) {
    var merchantName by remember { mutableStateOf(subscription.merchantName) }
    var amountText by remember { mutableStateOf(subscription.amount.toPlainString()) }
    var category by remember { mutableStateOf(subscription.category.orEmpty()) }
    var billingCycle by remember(subscription.id) { mutableStateOf(subscription.billingCycle) }
    val parsedCycle = remember(billingCycle) { SubscriptionBillingCycle.parse(billingCycle) }
    var customCycleCount by remember(subscription.id) {
        mutableStateOf(parsedCycle.count.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt())
    }
    var customCycleCountInput by remember(subscription.id) { mutableStateOf(parsedCycle.count.toString()) }
    var customCycleUnit by remember(subscription.id) { mutableStateOf(parsedCycle.unit) }
    var showBillingCycleMenu by remember { mutableStateOf(false) }
    var nextDate by remember { mutableStateOf(subscription.nextPaymentDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    // Whether the user actually changed the account. Guards against wiping a
    // stored account the picker can't represent (e.g. one not yet in the
    // balance list) when the user edits other fields and saves.
    var accountChanged by remember(subscription.id) { mutableStateOf(false) }
    var selectedAccount by remember(subscription.id) {
        mutableStateOf<AccountBalanceEntity?>(null)
    }
    // Pre-select the subscription's current funding account by (bank, last4).
    // Runs when the async accounts list first resolves, but never once the user
    // has touched the picker — otherwise an accounts tick mid-edit would revert
    // a freshly-picked account while accountChanged stayed true, writing the
    // stale value on save (keyed both on subscription.id keeps the two in sync).
    LaunchedEffect(subscription.id, accounts) {
        if (!accountChanged) {
            selectedAccount = accounts.firstOrNull {
                it.bankName == subscription.bankName &&
                    it.accountLast4 == subscription.accountLast4
            }
        }
    }

    val parsedAmount = remember(amountText) {
        amountText.trim().takeIf { it.isNotEmpty() }?.let {
            try { BigDecimal(it) } catch (_: NumberFormatException) { null }
        }
    }
    val isValid = merchantName.isNotBlank() &&
        parsedAmount != null &&
        parsedAmount > BigDecimal.ZERO &&
        (!parsedCycle.isCustom || customCycleCountInput.toLongOrNull()?.let {
            it in 1..Int.MAX_VALUE.toLong()
        } == true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.subscriptions_edit_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text(stringResource(R.string.subscriptions_edit_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.subscriptions_edit_amount, subscription.currency)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = parsedAmount == null || parsedAmount <= BigDecimal.ZERO
                )
                OutlinedTextField(
                    value = nextDate?.format(DateTimeFormatter.ofPattern("d MMM yyyy")) ?: stringResource(R.string.subscriptions_edit_tap_to_set),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.subscriptions_edit_next_date)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.subscriptions_edit_pick_date))
                        }
                    }
                )
                ExposedDropdownMenuBox(
                    expanded = showBillingCycleMenu,
                    onExpandedChange = { showBillingCycleMenu = it },
                ) {
                    OutlinedTextField(
                        value = if (parsedCycle.isCustom) {
                            stringResource(R.string.subscription_cycle_custom)
                        } else {
                            subscriptionBillingCycleLabel(billingCycle)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.subscription_cycle_billing_cycle)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBillingCycleMenu)
                        },
                    )
                    ExposedDropdownMenu(
                        expanded = showBillingCycleMenu,
                        onDismissRequest = { showBillingCycleMenu = false },
                    ) {
                        listOf(
                            "Weekly" to R.string.subscription_cycle_weekly,
                            "Monthly" to R.string.subscription_cycle_monthly,
                            "Quarterly" to R.string.subscription_cycle_quarterly,
                            "Semi-Annual" to R.string.subscription_cycle_semi_annual,
                            "Annual" to R.string.subscription_cycle_annual,
                            "Custom" to R.string.subscription_cycle_custom,
                        ).forEach { (cycle, labelResource) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(labelResource)) },
                                    onClick = {
                                        if (cycle == "Custom") {
                                            billingCycle = SubscriptionBillingCycle.encodeCustom(
                                                customCycleCount.toLong(),
                                                customCycleUnit,
                                            )
                                        } else {
                                            billingCycle = cycle
                                        }
                                        showBillingCycleMenu = false
                                    },
                                )
                            }
                    }
                }
                if (parsedCycle.isCustom) {
                    CustomBillingCycleEditor(
                        countInput = customCycleCountInput,
                        unit = customCycleUnit,
                        onCountChanged = { input ->
                            customCycleCountInput = input
                            input.toLongOrNull()?.takeIf { it > 0 }?.let { count ->
                                customCycleCount = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                billingCycle = SubscriptionBillingCycle.encodeCustom(count, customCycleUnit)
                            }
                        },
                        onUnitChanged = { unit ->
                            customCycleUnit = unit
                            billingCycle = SubscriptionBillingCycle.encodeCustom(customCycleCount.toLong(), unit)
                        },
                    )
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.subscriptions_edit_category)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Funding account (#570). Marking this subscription paid moves
                // the chosen account's balance; "No account" keeps it unlinked.
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedAccount?.displayLabel ?: stringResource(R.string.subscriptions_edit_no_account),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.subscriptions_edit_paid_from)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccountMenu = true },
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = { showAccountMenu = true }) {
                                Icon(Icons.Default.AccountBalance, contentDescription = stringResource(R.string.subscriptions_edit_pick_account))
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = { showAccountMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.subscriptions_edit_no_account)) },
                            onClick = {
                                selectedAccount = null
                                accountChanged = true
                                showAccountMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) }
                        )
                        HorizontalDivider()
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(account.displayLabel)
                                        Text(
                                            CurrencyFormatter.formatCurrency(account.balance, account.currency),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    selectedAccount = account
                                    accountChanged = true
                                    showAccountMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        when (account.getAccountType()) {
                                            AccountType.CASH -> Icons.Default.Money
                                            AccountType.CREDIT -> Icons.Default.CreditCard
                                            else -> Icons.Default.AccountBalance
                                        },
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    if (selectedAccount?.id == account.id) {
                                        Icon(Icons.Default.Check, stringResource(R.string.subscriptions_edit_selected), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    parsedAmount?.let { amt ->
                        onSave(merchantName, amt, nextDate, category, billingCycle, selectedAccount, accountChanged)
                    }
                }
            ) { Text(stringResource(R.string.subscriptions_edit_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.subscriptions_action_cancel)) }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = nextDate?.toEpochDay()?.times(86_400_000)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        nextDate = LocalDate.ofEpochDay(millis / 86_400_000)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.subscriptions_action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.subscriptions_action_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SubscriptionItemSkeleton(
    modifier: Modifier = Modifier
) {
    TransactionItemSkeleton(modifier = modifier)
}

