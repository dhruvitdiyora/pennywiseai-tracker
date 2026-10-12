package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.people.PeopleExtendedFab
import com.pennywiseai.tracker.presentation.people.PeopleSearchField
import com.pennywiseai.tracker.presentation.people.PeopleTonalActionButton
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.PennyWiseSegmentedSwitcher
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Balance
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** The segmented filter above the people list (Cashiro's All / You get / You owe tabs). */
internal enum class LoanListFilter { ALL, YOU_GET, YOU_OWE, SETTLED }

/**
 * The people a [filter] and a name [query] leave on the list. Pure, so the
 * screen stays a renderer. A person with balances both ways (in different
 * currencies) shows under both You get and You owe.
 */
internal fun filterLoanPeople(
    people: List<LoanPerson>,
    filter: LoanListFilter,
    query: String,
): List<LoanPerson> {
    val needle = query.trim()
    return people
        .filter { person ->
            when (filter) {
                LoanListFilter.ALL -> true
                LoanListFilter.YOU_GET -> person.net.values.any { it.isOwedToYou }
                LoanListFilter.YOU_OWE -> person.net.values.any { !it.isOwedToYou }
                LoanListFilter.SETTLED -> !person.hasActive
            }
        }
        .filter { needle.isEmpty() || it.name.contains(needle, ignoreCase = true) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToLoanDetail: (Long) -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    viewModel: LoansViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    // The add button carries its label only while the list is at the top.
    val fabExpanded by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }
    val openAddEntry = { viewModel.showAddEntrySheet(true) }

    var expandedPeople by remember { mutableStateOf(setOf<String>()) }
    var settleUpPerson by remember { mutableStateOf<LoanPerson?>(null) }
    var filter by rememberSaveable { mutableStateOf(LoanListFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }

    val isEmpty = uiState.activeLoans.isEmpty() && uiState.settledLoans.isEmpty()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.loans_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.accounts_back)
                    )
                },
                actionContent = {
                    PeopleTonalActionButton(
                        onClick = onNavigateToContacts,
                        icon = Icons.Default.People,
                        contentDescription = stringResource(R.string.people_open_contacts),
                    )
                },
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && !isEmpty) {
                PeopleExtendedFab(
                    label = stringResource(R.string.lend_borrow_add),
                    onClick = openAddEntry,
                    expanded = fabExpanded,
                )
            }
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            isEmpty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    PennyWiseEmptyState(
                        icon = Icons.Default.SwapHoriz,
                        headline = stringResource(R.string.loans_empty_title),
                        description = stringResource(R.string.loans_empty_description),
                        actionLabel = stringResource(R.string.lend_borrow_add),
                        onAction = openAddEntry,
                    )
                }
            }

            else -> {
                val balances = remember(uiState.activeLoans) { loanBalancesByCurrency(uiState.activeLoans) }
                val shownPeople = remember(uiState.people, filter, query) {
                    filterLoanPeople(uiState.people, filter, query)
                }
                val lentFaces = loanFaces(uiState.people, uiState.contacts, owedToYou = true)
                val borrowedFaces = loanFaces(uiState.people, uiState.contacts, owedToYou = false)
                val filters = LoanListFilter.entries
                val filterLabels = filters.map { stringResource(it.labelRes) }

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
                        bottom = paddingValues.calculateBottomPadding() + Dimensions.Component.fabScrollClearance,
                    ),
                    // Person rows sit 2dp apart as one connected list (Cashiro's grouped
                    // geometry); the summary, switcher and search carry their own gap.
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
                    flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
                ) {
                    item(key = "summary") {
                        LoanSummaryCard(
                            totalLent = uiState.totalLentRemaining,
                            totalBorrowed = uiState.totalBorrowedRemaining,
                            currency = uiState.summaryCurrency,
                            balances = balances,
                            lentFaces = lentFaces,
                            borrowedFaces = borrowedFaces,
                        )
                    }

                    item(key = "filter") {
                        PennyWiseSegmentedSwitcher(
                            options = filterLabels,
                            selectedIndex = filters.indexOf(filter),
                            onIndexChange = { filter = filters[it] },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.md - Spacing.Layout.groupedListGap),
                        )
                    }

                    item(key = "search") {
                        PeopleSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            placeholder = stringResource(R.string.loans_search_placeholder),
                            clearDescription = stringResource(R.string.loans_search_clear),
                            modifier = Modifier.padding(
                                top = Spacing.md - Spacing.Layout.groupedListGap,
                                bottom = Spacing.md - Spacing.Layout.groupedListGap,
                            ),
                        )
                    }

                    if (shownPeople.isEmpty()) {
                        item(key = "no-matches") { LoansNoMatches() }
                    } else {
                        itemsIndexed(shownPeople, key = { _, person -> "person-${person.name}" }) { index, person ->
                            LoanPersonBlock(
                                person = person,
                                contact = person.personId?.let { uiState.contacts[it] },
                                position = ListItemPosition.from(index, shownPeople.size),
                                expanded = person.name in expandedPeople,
                                onToggle = {
                                    expandedPeople = if (person.name in expandedPeople) {
                                        expandedPeople - person.name
                                    } else {
                                        expandedPeople + person.name
                                    }
                                },
                                onOpenLoan = onNavigateToLoanDetail,
                                onSettleUp = { settleUpPerson = person },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    settleUpPerson?.let { person ->
        SettleUpSheet(
            person = person,
            contact = person.personId?.let { uiState.contacts[it] },
            onConfirm = {
                viewModel.settleUp(person)
                settleUpPerson = null
            },
            onDismiss = { settleUpPerson = null },
        )
    }

    if (uiState.showAddEntrySheet) {
        LendBorrowEntrySheet(
            initialCurrency = uiState.defaultEntryCurrency,
            error = uiState.entryError,
            isSaving = uiState.isSavingEntry,
            onInputChanged = viewModel::clearEntryError,
            onDismiss = { viewModel.showAddEntrySheet(false) },
            onSubmit = viewModel::addManualEntry,
        )
    }
}

private val LoanListFilter.labelRes: Int
    get() = when (this) {
        LoanListFilter.ALL -> R.string.loans_filter_all
        LoanListFilter.YOU_GET -> R.string.loans_filter_you_get
        LoanListFilter.YOU_OWE -> R.string.loans_filter_you_owe
        LoanListFilter.SETTLED -> R.string.loans_filter_settled
    }

/** The faces behind a summary tile: everyone with an open balance in that direction. */
@Composable
private fun loanFaces(
    people: List<LoanPerson>,
    contacts: Map<Long, LoanContactStyle>,
    owedToYou: Boolean,
): List<LoanAvatarFace> {
    val accent = if (owedToYou) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    return people
        .filter { person -> person.net.values.any { it.isOwedToYou == owedToYou } }
        .map { person ->
            LoanAvatarFace(
                name = person.name,
                contact = person.personId?.let { contacts[it] },
                accent = accent,
            )
        }
}

/** Cashiro's inline empty state for a filter or search that matches nobody. */
@Composable
private fun LoansNoMatches() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.Padding.empty),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            imageVector = Iconax.Balance,
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = Spacing.smd)
                .size(Dimensions.Icon.avatarLarge),
            tint = scheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.loans_no_matches_title),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.loans_no_matches_description),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant.copy(alpha = Dimensions.Alpha.subtitle),
            textAlign = TextAlign.Center,
        )
    }
}
