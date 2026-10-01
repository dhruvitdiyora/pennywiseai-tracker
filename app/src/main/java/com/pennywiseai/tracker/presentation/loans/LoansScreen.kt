package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.people.PeopleExtendedFab
import com.pennywiseai.tracker.presentation.people.PeopleTonalActionButton
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

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
                // One block per person; tap to see their loans and settle up.
                val activePeople = uiState.people.filter { it.hasActive }
                val settledPeople = uiState.people.filterNot { it.hasActive }
                val balances = remember(uiState.activeLoans) { loanBalancesByCurrency(uiState.activeLoans) }

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
                    // Person blocks sit 2dp apart as one connected list; the summary and
                    // headers carry their own spacing.
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
                    flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
                ) {
                    item(key = "summary") {
                        LoanSummaryCard(
                            totalLent = uiState.totalLentRemaining,
                            totalBorrowed = uiState.totalBorrowedRemaining,
                            currency = uiState.summaryCurrency,
                            balances = balances,
                            modifier = Modifier.padding(bottom = Spacing.sm),
                        )
                    }

                    if (activePeople.isNotEmpty()) {
                        item(key = "active-header") {
                            SectionHeaderV2(
                                title = stringResource(R.string.loans_section_active),
                                modifier = Modifier.padding(bottom = Spacing.Layout.headerToContent),
                            )
                        }
                        personItems(
                            people = activePeople,
                            expanded = expandedPeople,
                            onToggle = { name ->
                                expandedPeople = if (name in expandedPeople) expandedPeople - name else expandedPeople + name
                            },
                            onOpenLoan = onNavigateToLoanDetail,
                            onSettleUp = { settleUpPerson = it },
                        )
                    }

                    if (settledPeople.isNotEmpty()) {
                        item(key = "settled-toggle") {
                            SettledToggle(
                                count = settledPeople.size,
                                expanded = uiState.showSettledLoans,
                                onClick = { viewModel.toggleShowSettled() },
                                modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
                            )
                        }
                        if (uiState.showSettledLoans) {
                            personItems(
                                people = settledPeople,
                                expanded = expandedPeople,
                                onToggle = { name ->
                                    expandedPeople = if (name in expandedPeople) expandedPeople - name else expandedPeople + name
                                },
                                onOpenLoan = onNavigateToLoanDetail,
                                onSettleUp = { settleUpPerson = it },
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

private fun LazyListScope.personItems(
    people: List<LoanPerson>,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onOpenLoan: (Long) -> Unit,
    onSettleUp: (LoanPerson) -> Unit,
) {
    itemsIndexed(people, key = { _, person -> "person-${person.name}" }) { index, person ->
        LoanPersonBlock(
            person = person,
            position = ListItemPosition.from(index, people.size),
            expanded = person.name in expanded,
            onToggle = { onToggle(person.name) },
            onOpenLoan = onOpenLoan,
            onSettleUp = { onSettleUp(person) },
        )
    }
}

/** The "Settled (n)" row that shows or hides the people with nothing open. */
@Composable
private fun SettledToggle(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimensions.Component.minTouchTarget)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.loans_settled_toggle, count),
                style = PennyWiseText.sectionHeader,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
